/* Copyright (C) 2013-2023 TU Dortmund
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
package de.learnlib.algorithms.procedural.spmm.it;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.procedural.SymbolWrapper;
import de.learnlib.algorithms.procedural.adapter.mealy.DiscriminationTreeAdapterMealy;
import de.learnlib.algorithms.procedural.adapter.mealy.KearnsVaziraniAdapterMealy;
import de.learnlib.algorithms.procedural.adapter.mealy.LStarBaseAdapterMealy;
import de.learnlib.algorithms.procedural.adapter.mealy.OptimalTTTAdapterMealy;
import de.learnlib.algorithms.procedural.adapter.mealy.RivestSchapireAdapterMealy;
import de.learnlib.algorithms.procedural.adapter.mealy.TTTAdapterMealy;
import de.learnlib.algorithms.procedural.spmm.ATManager;
import de.learnlib.algorithms.procedural.spmm.SPMMLearner;
import de.learnlib.algorithms.procedural.spmm.manager.DefaultATManager;
import de.learnlib.algorithms.procedural.spmm.manager.OptimizingATManager;
import de.learnlib.api.AccessSequenceTransformer;
import de.learnlib.api.algorithm.LearnerConstructor;
import de.learnlib.api.algorithm.LearningAlgorithm.MealyLearner;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.testsupport.it.learner.AbstractSPMMLearnerIT;
import de.learnlib.testsupport.it.learner.LearnerVariantList.SPMMLearnerVariantList;
import net.automatalib.SupportsGrowingAlphabet;
import net.automatalib.words.ProceduralInputAlphabet;
import net.automatalib.words.Word;

public class SPMMIT extends AbstractSPMMLearnerIT {

    @Override
    protected <I, O> void addLearnerVariants(ProceduralInputAlphabet<I> alphabet,
                                             O errorOutput,
                                             MembershipOracle<I, Word<O>> mqOracle,
                                             SPMMLearnerVariantList<I, O> variants) {

        final Builder<I, O> builder = new Builder<>(alphabet, errorOutput, mqOracle, variants);

        builder.addLearnerVariant(DiscriminationTreeAdapterMealy::new);
        builder.addLearnerVariant(MealyDHC::new);
        builder.addLearnerVariant(KearnsVaziraniAdapterMealy::new);
        builder.addLearnerVariant(LStarBaseAdapterMealy::new);
        builder.addLearnerVariant(OptimalTTTAdapterMealy::new);
        builder.addLearnerVariant(RivestSchapireAdapterMealy::new);
        builder.addLearnerVariant(TTTAdapterMealy::new);
    }

    private static class Builder<I, O> {

        private final ProceduralInputAlphabet<I> alphabet;
        private final O errorOutput;
        private final MembershipOracle<I, Word<O>> mqOracle;
        private final SPMMLearnerVariantList<I, O> variants;
        private final List<BiFunction<ProceduralInputAlphabet<I>, O, ATManager<I, O>>> atProviders;

        Builder(ProceduralInputAlphabet<I> alphabet,
                O errorOutput,
                MembershipOracle<I, Word<O>> mqOracle,
                SPMMLearnerVariantList<I, O> variants) {
            this.alphabet = alphabet;
            this.errorOutput = errorOutput;
            this.mqOracle = mqOracle;
            this.variants = variants;
            this.atProviders = Arrays.asList(DefaultATManager::new, OptimizingATManager::new);
        }

        <L extends MealyLearner<SymbolWrapper<I>, O> & SupportsGrowingAlphabet<SymbolWrapper<I>> & AccessSequenceTransformer<SymbolWrapper<I>>> void addLearnerVariant(
                LearnerConstructor<L, SymbolWrapper<I>, Word<O>> provider) {

            for (BiFunction<ProceduralInputAlphabet<I>, O, ATManager<I, O>> atProvider : atProviders) {
                final SPMMLearner<I, O, L> learner = new SPMMLearner<>(alphabet,
                                                                       errorOutput,
                                                                       mqOracle,
                                                                       (i) -> provider,
                                                                       atProvider.apply(alphabet, errorOutput));
                final String name = String.format("adapter=%s,manager=%s", provider, atProvider);
                variants.addLearnerVariant(name, learner);
            }
        }
    }

}