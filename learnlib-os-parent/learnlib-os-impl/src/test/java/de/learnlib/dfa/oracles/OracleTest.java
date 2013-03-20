/* Copyright (C) 2013 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */

package de.learnlib.dfa.oracles;

import java.util.ArrayList;
import java.util.List;

import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.FastAlphabet;
import net.automatalib.words.impl.Symbol;

import org.junit.Assert;
import org.junit.Test;

import de.learnlib.api.Query;
import de.learnlib.oracles.SafeOracle;
import de.learnlib.oracles.SimulatorOracle;

/**
 *
 * @author Maik Merten <maikmerten@googlemail.com>
 */
public class OracleTest {

    private final static Symbol in_paul = new Symbol("Paul");
    private final static Symbol in_loves = new Symbol("loves");
    private final static Symbol in_mary = new Symbol("Mary");

    public FastDFA<Symbol> constructMachine() {
        
        Alphabet<Symbol> alpha = new FastAlphabet<>();
        alpha.add(in_paul);
        alpha.add(in_loves);
        alpha.add(in_mary);
        
        FastDFA<Symbol> dfa = new FastDFA<>(alpha);
        
        FastDFAState s0 = dfa.addInitialState(false),
                s1 = dfa.addState(false),
                s2 = dfa.addState(false),
                s3 = dfa.addState(true),
                s4 = dfa.addState(false);

        dfa.addTransition(s0, in_paul, s1);
        dfa.addTransition(s0, in_loves, s4);
        dfa.addTransition(s0, in_mary, s4);
        
        dfa.addTransition(s1, in_paul, s4);
        dfa.addTransition(s1, in_loves, s2);
        dfa.addTransition(s1, in_mary, s4);
        
        dfa.addTransition(s2, in_paul, s4);
        dfa.addTransition(s2, in_loves, s4);
        dfa.addTransition(s2, in_mary, s3);
        
        dfa.addTransition(s3, in_paul, s4);
        dfa.addTransition(s3, in_loves, s4);
        dfa.addTransition(s3, in_mary, s4);
        
        dfa.addTransition(s4, in_paul, s4);
        dfa.addTransition(s4, in_loves, s4);
        dfa.addTransition(s4, in_mary, s4);

        return dfa;
    }
    
    
    @Test
    public void testDFASimulatorOracle() {
        
        DFA<?, Symbol> dfa = constructMachine();
        
        SimulatorOracle<Symbol,Boolean> dso = new SimulatorOracle<>(dfa);
        SafeOracle<Symbol,Boolean> oracle = new SafeOracle<>(dso);
        
        List<Query<Symbol, Boolean>> queries = new ArrayList<>();
        
        Query<Symbol, Boolean> q1 = new Query<>(Word.fromSymbols(in_paul, in_loves, in_mary));
        Query<Symbol, Boolean> q2 = new Query<>(Word.fromSymbols(in_mary, in_loves, in_paul));
        queries.add(q1);
        queries.add(q2);
        
        Assert.assertEquals(queries.get(0).getInput().size(), 3);
        Assert.assertEquals(queries.get(1).getInput().size(), 3);
        
        oracle.processQueries(queries);
        
        // Paul loves Mary...
        Assert.assertEquals(queries.get(0).getOutput(), true);
        
        // ... but Mary does not love Paul :-(
        Assert.assertEquals(queries.get(1).getOutput(), false);
        
    }
    
    
}
