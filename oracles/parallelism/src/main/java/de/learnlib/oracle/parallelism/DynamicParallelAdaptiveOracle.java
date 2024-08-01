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
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import de.learnlib.oracle.AdaptiveMembershipOracle;
import de.learnlib.oracle.ParallelAdaptiveOracle;
import de.learnlib.oracle.ParallelOracle;
import de.learnlib.query.AdaptiveQuery;
import org.checkerframework.checker.index.qual.NonNegative;

/**
 * A specialized {@link AbstractDynamicBatchProcessor} for {@link AdaptiveMembershipOracle}s that implements
 * {@link ParallelOracle}.
 *
 * @param <I>
 *         input symbol type
 * @param <O>
 *         output symbol type
 */
public class DynamicParallelAdaptiveOracle<I, O>
        extends AbstractDynamicBatchProcessor<AdaptiveQuery<I, O>, AdaptiveMembershipOracle<I, O>>
        implements ParallelAdaptiveOracle<I, O> {

    public DynamicParallelAdaptiveOracle(Supplier<? extends AdaptiveMembershipOracle<I, O>> oracleSupplier,
                                         @NonNegative int batchSize,
                                         ExecutorService executor) {
        super(oracleSupplier, batchSize, executor);
    }

    @Override
    public void processQueries(Collection<? extends AdaptiveQuery<I, O>> queries) {
        processBatch(queries);
    }
}