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
package de.learnlib.filter.cache.mealy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import de.learnlib.Resumable;
import de.learnlib.filter.cache.LearningCache;
import de.learnlib.filter.cache.mealy.AdaptiveQueryCache.AdaptiveQueryCacheState;
import de.learnlib.oracle.AdaptiveMembershipOracle;
import de.learnlib.oracle.BatchProcessor;
import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.query.AdaptiveQuery;
import de.learnlib.query.AdaptiveQuery.Response;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.SupportsGrowingAlphabet;
import net.automatalib.automaton.impl.CompactTransition;
import net.automatalib.automaton.transducer.MealyMachine;
import net.automatalib.automaton.transducer.impl.CompactMealy;
import net.automatalib.util.automaton.equivalence.NearLinearEquivalenceTest;
import net.automatalib.word.Word;
import net.automatalib.word.WordBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A cache for an {@link AdaptiveMembershipOracle}. Upon construction, it is provided with a delegate oracle. Queries
 * that can be answered from the cache are answered directly, others are forwarded to the delegate oracle. Queried
 * symbols that have to be delegated are incorporated into the cache directly.
 * <p>
 * Internally, an incrementally growing tree (in form of a mealy automaton) is used for caching.
 * <p>
 * Note that due to the step-wise processing of {@link AdaptiveQuery adaptive queries}, duplicates within a single
 * {@link BatchProcessor#processBatch(Collection) batch} cannot be cached. If you want to maximize cache efficiency, you
 * would have to give up potential parallelization and pose queries one by one.
 *
 * @param <I>
 *         input symbol type
 * @param <O>
 *         output symbol type
 */
public class AdaptiveQueryCache<I, O> implements AdaptiveMembershipOracle<I, O>,
                                                 LearningCache<MealyMachine<?, I, ?, O>, I, Word<O>>,
                                                 SupportsGrowingAlphabet<I>,
                                                 Resumable<AdaptiveQueryCacheState<I, O>> {

    private final AdaptiveMembershipOracle<I, O> delegate;
    private CompactMealy<I, O> cache;

    public AdaptiveQueryCache(AdaptiveMembershipOracle<I, O> delegate, Alphabet<I> alphabet) {
        this.delegate = delegate;
        this.cache = new CompactMealy<>(alphabet);
        this.cache.addInitialState();
    }

    @Override
    public void processQueries(Collection<? extends AdaptiveQuery<I, O>> queries) {

        final Deque<AdaptiveQuery<I, O>> queue = new ArrayDeque<>(queries);
        final List<TrackingQuery> unanswered = new ArrayList<>(queue.size());

        while (!queue.isEmpty()) {

            // try to answer queries from cache
            cacheLoop:
            while (!queue.isEmpty()) {
                final AdaptiveQuery<I, O> query = queue.poll();
                final WordBuilder<I> trace = new WordBuilder<>();
                Integer curr = this.cache.getInitialState();
                Response response;

                do {
                    final I input = query.getInput();
                    final CompactTransition<O> trans = this.cache.getTransition(curr, input);

                    trace.add(input);

                    if (trans == null) {
                        unanswered.add(new TrackingQuery(query, trace));
                        continue cacheLoop;
                    }

                    final O output = this.cache.getTransitionOutput(trans);
                    response = query.processOutput(output);

                    if (response == Response.RESET) {
                        curr = this.cache.getInitialState();
                        trace.clear();
                    } else {
                        curr = this.cache.getSuccessor(trans);
                    }
                } while (response != Response.FINISHED);
            }

            // delegate non-answered queries
            this.delegate.processQueries(unanswered);

            // feed back information into cache
            for (TrackingQuery query : unanswered) {
                final Word<I> input = query.inputBuilder.toWord();
                final Word<O> output = query.outputBuilder.toWord();

                assert input.length() == output.length();

                insert(input, output);

                if (!query.isFinished) { // re-queue reset successor
                    queue.add(query.delegate);
                }
            }

            unanswered.clear();
        }
    }

    @Override
    public EquivalenceOracle<MealyMachine<?, I, ?, O>, I, Word<O>> createCacheConsistencyTest() {
        return this::findCounterexample;
    }

    private @Nullable DefaultQuery<I, Word<O>> findCounterexample(MealyMachine<?, I, ?, O> hypothesis,
                                                                  Collection<? extends I> alphabet) {
        /*
        TODO: potential optimization: If the hypothesis has undefined transitions, but the cache doesn't, it is a clear
        counterexample!
         */
        final Word<I> sepWord = NearLinearEquivalenceTest.findSeparatingWord(cache, hypothesis, alphabet, true);

        if (sepWord != null) {
            return new DefaultQuery<>(sepWord, cache.computeOutput(sepWord));
        }

        return null;
    }

    @Override
    public AdaptiveQueryCacheState<I, O> suspend() {
        return new AdaptiveQueryCacheState<>(cache);
    }

    @Override
    public void resume(AdaptiveQueryCacheState<I, O> state) {
        this.cache = state.getCache();
    }

    @Override
    public void addAlphabetSymbol(I symbol) {
        this.cache.addAlphabetSymbol(symbol);
    }

    public MealyMachine<Integer, I, ?, O> getCache() {
        return this.cache;
    }

    public Integer insert(Word<I> input, Word<O> output) {
        return insert(this.cache.getInitialState(), input, output);
    }

    public Integer insert(Integer state, Word<I> input, Word<O> output) {
        assert input.length() == output.length();

        Integer curr = state;

        for (int i = 0; i < input.size(); i++) {
            final I in = input.getSymbol(i);
            final O out = output.getSymbol(i);
            final CompactTransition<O> trans = this.cache.getTransition(curr, in);

            if (trans == null) {
                Integer next = this.cache.addState();
                this.cache.addTransition(curr, in, next, out);
                curr = next;
            } else {
                assert Objects.equals(out, this.cache.getTransitionOutput(trans)) : "Inconsistent observations";
                curr = this.cache.getSuccessor(trans);
            }
        }

        return curr;
    }

    private class TrackingQuery implements AdaptiveQuery<I, O> {

        private final AdaptiveQuery<I, O> delegate;
        private final WordBuilder<I> inputBuilder;
        private final WordBuilder<O> outputBuilder;

        private final int prefixLength;
        private int prefixIdx;
        private boolean isFinished;

        TrackingQuery(AdaptiveQuery<I, O> delegate, WordBuilder<I> inputBuilder) {
            this.delegate = delegate;
            this.inputBuilder = inputBuilder;
            this.outputBuilder = new WordBuilder<>();
            this.prefixLength = inputBuilder.size();
            this.prefixIdx = 0;
            this.isFinished = false;
        }

        @Override
        public I getInput() {
            // we are still processing the backlog
            if (prefixIdx < prefixLength) {
                return inputBuilder.getSymbol(prefixIdx);
            }

            final I input = delegate.getInput();
            inputBuilder.append(input);
            return input;
        }

        @Override
        public Response processOutput(O out) {
            outputBuilder.append(out);
            prefixIdx++;

            // in case of backlog, the last but one input hasn't been processed yet
            if (prefixIdx < prefixLength) {
                return Response.SYMBOL;
            }

            final Response response = delegate.processOutput(out);

            switch (response) {
                case FINISHED:
                    isFinished = true;
                    return Response.FINISHED;
                case RESET:
                    return Response.FINISHED;
                default:
                    return response;
            }
        }
    }

    public static class AdaptiveQueryCacheState<I, O> {

        private final CompactMealy<I, O> cache;

        AdaptiveQueryCacheState(CompactMealy<I, O> cache) {
            this.cache = cache;
        }

        CompactMealy<I, O> getCache() {
            return cache;
        }
    }
}