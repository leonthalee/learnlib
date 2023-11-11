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
package de.learnlib.sul;

import de.learnlib.exception.SULException;

/**
 * A context executable input is an input that performs a specific action on a {@link SUL} itself, given a specific
 * execution context.
 *
 * @param <O>
 *         output
 * @param <C>
 *         context
 */
public interface ContextExecutableInput<O, C> {

    /**
     * Executes {@code this} input symbol with a given context.
     *
     * @param context
     *         the context for {@code this} input symbol
     *
     * @return the output generated by the {@link SUL}
     *
     * @throws SULException
     *         if {@code this} input cannot be executed on the {@link SUL}
     */
    O execute(C context);
}