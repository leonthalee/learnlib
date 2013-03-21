package de.learnlib.lstar.dfa;

import java.util.ArrayList;
import java.util.List;

import net.automatalib.words.Word;

public class LStarDFAUtil {
	
	public static <I> List<Word<I>> ensureSuffixCompliancy(List<Word<I>> suffixes) {
		List<Word<I>> compSuffixes = new ArrayList<Word<I>>();
		compSuffixes.add(Word.<I>epsilon());
		for(Word<I> suff : suffixes) {
			if(!suff.isEmpty())
				compSuffixes.add(suff);
		}
		
		return compSuffixes;
	}
}
