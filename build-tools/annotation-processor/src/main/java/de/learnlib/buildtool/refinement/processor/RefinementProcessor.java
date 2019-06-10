/* Copyright (C) 2013-2019 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.buildtool.refinement.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import com.github.misberner.apcommons.util.ElementUtils;
import com.github.misberner.apcommons.util.types.TypeUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import de.learnlib.buildtool.refinement.annotation.GenerateRefinement;
import de.learnlib.buildtool.refinement.annotation.GenerateRefinements;
import de.learnlib.buildtool.refinement.annotation.Generic;
import de.learnlib.buildtool.refinement.annotation.Interface;
import de.learnlib.buildtool.refinement.annotation.Map;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RefinementProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateRefinements.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(GenerateRefinements.class);

        for (Element elem : elements) {

            validateAnnotation(elem);

            final GenerateRefinements generateRefinements = elem.getAnnotation(GenerateRefinements.class);

            int idx = 0;

            for (final GenerateRefinement annotation : generateRefinements.value()) {

                TypeElement annotatedClass = (TypeElement) elem;

                final TypeSpec.Builder builder = createClass(annotatedClass, annotation);
                addGenerics(builder, annotation);
                addSuperClass(builder, annotatedClass, annotation);
                addInterfaces(builder, annotation);
                addConstructors(builder, annotatedClass, annotation, idx);

                try {
                    JavaFile.builder(ElementUtils.getPackageName(elem), builder.build())
                            .build()
                            .writeTo(super.processingEnv.getFiler());
                } catch (IOException e) {
                    error("Could not writer source: " + e.getMessage());
                }

                idx++;
            }
        }
        return true;
    }

    private void validateAnnotation(final Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            error("Annotation " + GenerateRefinement.class + " is only supported on class level");
            throw new IllegalArgumentException();
        }
    }

    private TypeSpec.Builder createClass(TypeElement annotatedClass, GenerateRefinement annotation) {
        return TypeSpec.classBuilder(annotation.name())
                       .addModifiers(Modifier.PUBLIC)
                       .addJavadoc("This is an auto-generated refinement. See the {@link $T original class}.\n",
                                   processingEnv.getTypeUtils().erasure(annotatedClass.asType()));
    }

    private void addGenerics(TypeSpec.Builder builder, GenerateRefinement annotation) {
        for (String typeParameter : annotation.generics()) {
            builder.addTypeVariable(TypeVariableName.get(typeParameter));
        }
    }

    private void addSuperClass(TypeSpec.Builder builder, TypeElement annotatedClass, GenerateRefinement annotation) {

        final List<TypeName> generics = new ArrayList<>(annotation.parentGenerics().length);
        for (Generic generic : annotation.parentGenerics()) {
            generics.add(extractGeneric(generic));
        }

        builder.superclass(ParameterizedTypeName.get(ClassName.get(annotatedClass), generics.toArray(new TypeName[0])));
    }

    private void addInterfaces(TypeSpec.Builder builder, GenerateRefinement annotation) {

        for (Interface inter : annotation.interfaces()) {

            final ClassName className = extractClass(inter, Interface::clazz);

            final List<TypeName> generics = new ArrayList<>(annotation.interfaces().length);
            for (String generic : inter.generics()) {
                generics.add(TypeVariableName.get(generic));
            }

            builder.addSuperinterface(ParameterizedTypeName.get(className, generics.toArray(new TypeName[0])));
        }
    }

    private void addConstructors(TypeSpec.Builder builder,
                                 TypeElement annotatedClass,
                                 GenerateRefinement annotation,
                                 int idx) {

        for (final ExecutableElement constructor : TypeUtils.getConstructors(annotatedClass)) {

            final MethodSpec.Builder mBuilder = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            final StringJoiner javadocJoiner = new StringJoiner(", ",
                                                                "This is an auto-generated constructor. See the {@link $T#$T(",
                                                                ") original constructor}.\n");

            final int numOfConstructorParams = constructor.getParameters().size();
            final List<String> parameterNames = new ArrayList<>(numOfConstructorParams);
            final List<TypeMirror> javadocTypes = new ArrayList<>(numOfConstructorParams + 2);

            javadocTypes.add(processingEnv.getTypeUtils().erasure(annotatedClass.asType()));
            javadocTypes.add(processingEnv.getTypeUtils().erasure(annotatedClass.asType()));

            for (final VariableElement variableElement : constructor.getParameters()) {

                javadocJoiner.add("$T");
                javadocTypes.add(processingEnv.getTypeUtils().erasure(variableElement.asType()));
                parameterNames.add(variableElement.getSimpleName().toString());

                final AnnotationMirror generateSubImpls = annotatedClass.getAnnotationMirrors().get(0);

                final List<? extends AnnotationValue> values = find(generateSubImpls, "value");
                final AnnotationMirror generateSubImpl = (AnnotationMirror) values.get(idx).getValue();

                final List<? extends AnnotationValue> parameters = find(generateSubImpl, "parameterMapping");
                final Map[] parametersAnn = annotation.parameterMapping();

                AnnotationMirror parameterMatch = null;
                int i = 0;
                for (AnnotationValue parameter : parameters) {
                    AnnotationMirror parameterMirror = (AnnotationMirror) parameter.getValue();
                    TypeMirror replaceAttribute = find(parameterMirror, "from");
                    if (processingEnv.getTypeUtils()
                                     .isSameType(processingEnv.getTypeUtils().erasure(variableElement.asType()),
                                                 replaceAttribute)) {
                        parameterMatch = parameterMirror;
                        break;
                    }
                    i++;
                }

                if (parameterMatch != null) {
                    Map parameterAnn = parametersAnn[i];
                    final TypeMirror withAttribute = find(parameterMatch, "to");
                    final ClassName className = ClassName.get(this.processingEnv.getElementUtils()
                                                                                .getTypeElement(withAttribute.toString()));
                    final List<TypeName> generics = new ArrayList<>(annotation.interfaces().length);

                    if (parameterAnn.withGenerics().length > 0) {
                        for (String generic : parameterAnn.withGenerics()) {
                            generics.add(TypeVariableName.get(generic));
                        }
                    } else if (parameterAnn.withComplexGenerics().length > 0) {
                        for (Generic generic : parameterAnn.withComplexGenerics()) {
                            generics.add(extractGeneric(generic));
                        }
                    }

                    mBuilder.addParameter(ParameterizedTypeName.get(className, generics.toArray(new TypeName[0])),
                                          variableElement.getSimpleName().toString());
                } else {
                    mBuilder.addParameter(ParameterSpec.get(variableElement));
                }
            }

            final StringJoiner sj = new StringJoiner(", ", "super(", ")");
            parameterNames.forEach(sj::add);

            mBuilder.addStatement(CodeBlock.of(sj.toString(), parameterNames.toArray()));
            mBuilder.addJavadoc(javadocJoiner.toString(), javadocTypes.toArray());
            builder.addMethod(mBuilder.build());
        }
    }

    private TypeName extractGeneric(final Generic annotation) {

        final ClassName className = extractClass(annotation, Generic::clazz);

        if (!ClassName.get(Void.class).equals(className)) { // no default value

            if (annotation.generics().length > 0) {
                final List<TypeName> genericModels = new ArrayList<>(annotation.generics().length);

                for (final String generic : annotation.generics()) {
                    genericModels.add(TypeVariableName.get(generic));
                }

                return ParameterizedTypeName.get(className, genericModels.toArray(new TypeName[0]));
            }

            return className;
        } else if (!annotation.value().isEmpty()) {
            return TypeVariableName.get(annotation.value());
        } else {
            throw new IllegalArgumentException();
        }
    }

    private <T> ClassName extractClass(final T obj, Function<T, Class<?>> extractor) {
        try {
            final Class<?> clazz = extractor.apply(obj);
            return ClassName.get(clazz);
        } catch (MirroredTypeException mte) {
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            return ClassName.get(classTypeElement);
        }
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }

    private <T> T find(AnnotationMirror mirror, String name) {

        for (java.util.Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> root : processingEnv.getElementUtils()
                                                                                                             .getElementValuesWithDefaults(
                                                                                                                     mirror)
                                                                                                             .entrySet()) {
            if (root.getKey().getSimpleName().toString().equals(name)) {
                return (T) root.getValue().getValue();
            }
        }
        return null;
    }
}
