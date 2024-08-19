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
package de.learnlib.algorithm.adt.learner;

import java.util.ArrayList;
import java.util.Collection;

import de.learnlib.oracle.AdaptiveMembershipOracle;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.AdaptiveQuery;
import de.learnlib.query.PresetAdaptiveQuery;
import de.learnlib.query.Query;
import net.automatalib.word.Word;

class Adaptive2MembershipWrapper<I, O> implements MembershipOracle.MealyMembershipOracle<I, O> {

    private final AdaptiveMembershipOracle<I, O> oracle;

    Adaptive2MembershipWrapper(AdaptiveMembershipOracle<I, O> oracle) {
        this.oracle = oracle;
    }

    @Override
    public void processQueries(Collection<? extends Query<I, Word<O>>> queries) {

        Collection<AdaptiveQuery<I, O>> adaptiveQueries = new ArrayList<>();

        for (Query<I, Word<O>> query : queries) {
            if (query.getSuffix().isEmpty()) {
                query.answer(Word.epsilon());
            } else {
                adaptiveQueries.add(new PresetAdaptiveQuery<>(query));
            }
        }

        this.oracle.processQueries(adaptiveQueries);
    }
}