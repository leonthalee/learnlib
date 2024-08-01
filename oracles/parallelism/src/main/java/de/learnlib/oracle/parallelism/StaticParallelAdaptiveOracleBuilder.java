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
package de.learnlib.oracle.parallelism;

import java.util.Collection;
import java.util.function.Supplier;

import de.learnlib.oracle.AdaptiveMembershipOracle;
import de.learnlib.oracle.ThreadPool.PoolPolicy;
import de.learnlib.query.AdaptiveQuery;

/**
 * A specialized {@link AbstractStaticBatchProcessorBuilder} for {@link AdaptiveMembershipOracle}s.
 *
 * @param <I>
 *         input symbol type
 * @param <O>
 *         output symbol type
 */
public class StaticParallelAdaptiveOracleBuilder<I, O>
        extends AbstractStaticBatchProcessorBuilder<AdaptiveQuery<I, O>, AdaptiveMembershipOracle<I, O>, StaticParallelAdaptiveOracle<I, O>> {

    public StaticParallelAdaptiveOracleBuilder(Supplier<? extends AdaptiveMembershipOracle<I, O>> oracleSupplier) {
        super(oracleSupplier);
    }

    public StaticParallelAdaptiveOracleBuilder(Collection<? extends AdaptiveMembershipOracle<I, O>> oracles) {
        super(oracles);
    }

    @Override
    protected StaticParallelAdaptiveOracle<I, O> buildOracle(Collection<? extends AdaptiveMembershipOracle<I, O>> oracleInstances,
                                                             int minBatchSize,
                                                             PoolPolicy poolPolicy) {
        return new StaticParallelAdaptiveOracle<>(oracleInstances, minBatchSize, poolPolicy);
    }
}