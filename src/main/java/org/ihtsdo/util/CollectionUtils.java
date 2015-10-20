package org.ihtsdo.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class CollectionUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CollectionUtils.class);

	@SuppressWarnings("unchecked")
	public static void printSortedMap(Map<String, AtomicInteger> map) {
		Object[] entries = map.entrySet().toArray();
		Arrays.sort(entries, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Map.Entry<String, AtomicInteger>) o2).getValue().intValue()
						- ((Map.Entry<String, AtomicInteger>) o1).getValue().intValue();
			}
		});
		for (Object e : entries) {
			System.out.println(((Map.Entry<String, AtomicInteger>) e).getKey() + " : " + ((Map.Entry<String, AtomicInteger>) e).getValue());
		}
	}

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
