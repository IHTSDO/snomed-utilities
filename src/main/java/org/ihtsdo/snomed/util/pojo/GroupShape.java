package org.ihtsdo.snomed.util.pojo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GroupShape {

	String id;
	Set<Integer> partialMatch = new HashSet<>();
	Set<Integer> abstractMatch = new HashSet<>();
	int popularity = 0;
	Set<Concept> shapeStructure = new HashSet<>();
	Set<Concept> examples = new HashSet<>();

    static ConcurrentMap<String, GroupShape> knownShapes = new ConcurrentHashMap<>();

	// For a match with a more abstract (ie parent) type, we may go more
	// than one ancestor up the hierarchy. But this hasn't been needed yet.
	// Set<Integer, int> generationMatch
	public GroupShape (String id, Set<Integer> partialMatch, 
			Set<Integer> abstractMatch, int popularity){
		this.id = id;
		this.partialMatch = partialMatch;
		this.abstractMatch = abstractMatch;
		this.popularity = popularity;
	}

	private GroupShape(String id) {
		this.id = id;
	}

	public static GroupShape get(String groupShapeId) {
		// Do we know about this shape?
		GroupShape shape = knownShapes.get(groupShapeId);
		if (shape == null) {
			shape = new GroupShape(groupShapeId);
			knownShapes.put(groupShapeId, shape);
		}
		return shape;
	}

	public static void resetPopularities() {
		for (GroupShape thisShape : knownShapes.values()) {
			thisShape.popularity = 0;
		}
	}

	public static void registerShape(String groupShapeId) {
		get(groupShapeId);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<Integer> getPartialMatch() {
		return partialMatch;
	}

	public void setPartialMatch(Set<Integer> partialMatch) {
		this.partialMatch = partialMatch;
	}

	public Set<Integer> getAbstractMatch() {
		return abstractMatch;
	}

	public void setAbstractMatch(Set<Integer> abstractMatch) {
		this.abstractMatch = abstractMatch;
	}

	public int getPopularity() {
		return popularity;
	}

	public void incrementPopularity() {
		this.popularity++;
	}

	public Set<Concept> getShapeStructure() {
		return shapeStructure;
	}

	public void setShapeStructure(Set<Concept> shapeStructure) {
		this.shapeStructure = shapeStructure;
	}

	public void incrementPopularity(Concept example) {
		incrementPopularity();
		this.examples.add(example);
	}

	// Print all known shapes, ordered by popularity, with attributes and examples
	public static void print() {
		for (GroupShape s : knownShapesSorted()) {
			out("", true);
			out(s.popularity + ": Shape - " + s.id, true);
			out("----------------------------------------", true);
			out(s.toStructure(), true);

			out("[Examples: ", false);
			boolean isFirst = true;
			int count = 0;
			limit_examples:
			for (Concept example : s.examples) {
				if (isFirst) {
					isFirst = false;
				} else {
					out(", ", false);
				}
				out(Description.getFormattedConcept(example.getSctId()), false);
				count++;
				if (count >= 5) {
					break;
				}
			}
			out("]", true);
		}
	}

	public String toStructure() {
		String structureStr = "[Structure: ";
		boolean isFirst = true;
		for (Concept type : shapeStructure) {
			if (isFirst) {
				isFirst = false;
			} else {
				structureStr += ", ";
			}
			structureStr += Description.getFormattedConcept(type.getSctId());
		}
		if (shapeStructure.size() == 0) {
			structureStr += "Group with only 'is a' attributes";
		}
		structureStr += "]";
		return structureStr;
	}

	private static GroupShape[] knownShapesSorted() {
		GroupShape[] shapes = knownShapes.values().toArray(new GroupShape[0]);
		Arrays.sort(shapes, (o1, o2) -> o2.popularity - o1.popularity);
		return shapes;
	}

	private static void out(String msg, boolean newLine) {
		if (newLine) {
			System.out.println(msg);
		} else {
			System.out.print(msg);
		}
	}

	public static boolean isKnown(String shapeId) {
		return knownShapes.containsKey(shapeId);
	}

	public String toString() {
		return id;
	}


}
