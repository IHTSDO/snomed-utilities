package org.ihtsdo.snomed.util.pojo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GroupsHash {

	String id;
	int popularity = 0;
	Set<GroupShape> hashStructure = new HashSet<GroupShape>();
	Set<Concept> examples = new HashSet<Concept>();

	static ConcurrentMap<String, GroupsHash> knownHashes = new ConcurrentHashMap<String, GroupsHash>();

	// For a match with a more abstract (ie parent) type, we may go more
	// than one ancestor up the hierarchy. But this hasn't been needed yet.
	// Set<Integer, int> generationMatch
	public GroupsHash(String id, int popularity) {
		this.id = id;
		this.popularity = popularity;
	}

	private GroupsHash(String id) {
		this.id = id;
	}

	public static GroupsHash get(String groupsHash) {
		// Do we know about this shape?
		GroupsHash shape = knownHashes.get(groupsHash);
		if (shape == null) {
			shape = new GroupsHash(groupsHash);
			knownHashes.put(groupsHash, shape);
		}
		return shape;
	}

	public static void resetPopularities() {
		for (GroupsHash thisShape : knownHashes.values()) {
			thisShape.popularity = 0;
		}
	}

	public static void registerHash(String groupsHash) {
		get(groupsHash);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getPopularity() {
		return popularity;
	}

	public void incrementPopularity() {
		this.popularity++;
	}

	public void incrementPopularity(Concept example) {
		incrementPopularity();
		this.examples.add(example);
	}

	// Print all known shapes, ordered by popularity, with attributes and examples
	public static void print() {
		for (GroupsHash h : knownHashesSorted()) {
			out("", true);
			out(h.popularity + ": hash id - " + h.id, true);
			out("----------------------------------------", true);

			out("[", true);
			for (GroupShape thisGroup : h.hashStructure) {
				out(thisGroup.toStructure(), true);
			}
			out("]", true);


			out("[Examples: ", false);
			boolean isFirst = true;
			int count = 0;
			limit_examples:
			for (Concept example : h.examples) {
				if (isFirst) {
					isFirst = false;
				} else {
					out(", ", false);
				}
				out(Description.getFormattedConcept(example.getSctId()), false);
				count++;
				if (count > 5) {
					break limit_examples;
				}
			}
			out("]", true);
		}
	}

	private static GroupsHash[] knownHashesSorted() {
		GroupsHash[] hashes = knownHashes.values().toArray(new GroupsHash[knownHashes.size()]);
		Arrays.sort(hashes, new Comparator<GroupsHash>() {
			public int compare(GroupsHash o1, GroupsHash o2) {
				return o2.popularity - o1.popularity;
			}
		});
		return hashes;
	}

	private static void out(String msg, boolean newLine) {
		if (newLine) {
			System.out.println(msg);
		} else {
			System.out.print(msg);
		}
	}

	public Set<GroupShape> getHashStructure() {
		return hashStructure;
	}

	public void setHashStructure(Set<GroupShape> hashStructure) {
		this.hashStructure = hashStructure;
	}

	public String toString() {
		return id;
	}

}
