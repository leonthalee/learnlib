/* Copyright (C) 2013-2024 TU Dortmund University
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
package de.learnlib.query;

import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;

public class PresetAdaptiveQuery<I, O> implements AdaptiveQuery<I, O> {

    private final WordBuilder<O> builder;
    private final Query<I, Word<O>> query;

    private final Word<I> prefix;
    private final Word<I> suffix;

    private int prefixIdx;
    private int suffixIdx;

    public PresetAdaptiveQuery(Query<I, Word<O>> query) {
        this.builder = new WordBuilder<>();
        this.query = query;
        this.prefix = query.getPrefix();
        this.suffix = query.getSuffix();
        this.prefixIdx = 0;
        this.suffixIdx = 0;
    }

    @Override
    public I getInput() {
        if (prefixIdx < prefix.size()) {
            return prefix.getSymbol(prefixIdx++);
        } else if (suffixIdx < suffix.size()) {
            return suffix.getSymbol(suffixIdx++);
        } else {
            throw new IllegalStateException("Indices out of bounds for query");
        }
    }

    @Override
    public Response processOutput(O out) {

        if (suffixIdx > 0) {
            builder.add(out);

            if (suffixIdx >= suffix.size()) {
                query.answer(builder.toWord());
                return Response.FINISHED;
            } else {
                return Response.SYMBOL;
            }
        }

        return Response.SYMBOL;
    }
}
