package org.ihtsdo.util;

import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Sets;

public class SnomedCollectionUtils {

	/**
	 * Return a set containing every combination of indexes for a given sized collection
	 * 
	 * @param size
	 * @param includeEmptySet
	 * @return
	 */
	public static Set<Set<Integer>> getIndexCombinations(int size) {
		Set<Integer> indexes = new TreeSet<Integer>();
		for (int x = 0; x < size; x++) {
			indexes.add(x);
		}

		return Sets.powerSet(indexes);
	}
}
