package org.ihtsdo.snomed.util.rf2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ihtsdo.snomed.util.rf2.Relationship.CHARACTERISTIC;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Concept implements Comparable<Concept> {

	private Long id;

	private static final Logger LOGGER = LoggerFactory.getLogger(Concept.class);

	Set<Concept> parents = new TreeSet<Concept>();
	Set<Relationship> attributes = new HashSet<Relationship>();

	public Concept(Long id) {
		this.id = id;
	}

	private static Map<Long, Concept> allStatedConcepts = new HashMap<Long, Concept>();
	private static Map<Long, Concept> allInferredConcepts = new HashMap<Long, Concept>();

	public static void addRelationship(Relationship relationship, Relationship.CHARACTERISTIC characteristic) throws Exception {

		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;

		// Do we know about the source concept?
		Concept sourceConcept;
		if (!allConcepts.containsKey(relationship.getSourceId())) {
			sourceConcept = new Concept(relationship.getSourceId());
			allConcepts.put(relationship.getSourceId(), sourceConcept);
		} else {
			sourceConcept = allConcepts.get(relationship.getSourceId());
		}
		relationship.setSourceConcept(sourceConcept);

		// Do we already know about the destination ?
		Concept destinationConcept;
		Long destinationId = relationship.getDestinationId();
		if (!allConcepts.containsKey(destinationId)) {
			destinationConcept = new Concept(destinationId);
			allConcepts.put(destinationId, destinationConcept);
		} else {
			destinationConcept = allConcepts.get(destinationId);
		}
		relationship.setDestinationConcept(destinationConcept);

		// We're only interested in 'Is a' relationships for the graph
		if (relationship.isISA()) {
			sourceConcept.parents.add(destinationConcept);
		}

		// But all relationships get recorded as attributes
		sourceConcept.attributes.add(relationship);
	}

	/**
	 * Loop through all concepts known in that graph and ensure only 1 (hopefully the root) has no parents.
	 * 
	 * @param stated
	 */
	public static void ensureParents(CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;

		List<Concept> noParents = new ArrayList<Concept>();
		for (Concept thisConcept : allConcepts.values()) {
			if (thisConcept.parents.size() == 0) {
				noParents.add(thisConcept);
			}
		}

		LOGGER.debug("The following concepts have no parent in graph {}: ", characteristic.toString());
		for (Concept thisConcept : noParents) {
			LOGGER.debug(thisConcept.id.toString());
		}

	}

	@Override
	public int compareTo(Concept other) {
		return this.id.compareTo(other.id);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Concept) {
			Concept otherConcept = (Concept) other;
			return this.id.equals(otherConcept.id);
		}
		return false;
	}

	public static Concept getConcept(Long destinationId, CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;
		return allConcepts.get(destinationId);
	}

	public List<Relationship> findMatchingRelationships(Long typeId, int group, Concept statedDestinationConcept) {
		// find relationships of this concept with the same type and group, and where the destination has the
		// statedDestinationConcept as a parent.

		// lets match on type and group first since it's cheap
		List<Relationship> firstPassMatches = findMatchingRelationships(typeId, group);
		List<Relationship> secondPassMatches = new ArrayList<Relationship>();

		for (Relationship thisRelationship : firstPassMatches) {
			if (thisRelationship.getDestinationConcept().hasParent(statedDestinationConcept)) {
				secondPassMatches.add(thisRelationship);
			}
		}

		return secondPassMatches;
	}

	private boolean hasParent(Concept targetConcept) {
		// Recurse through my parents to find if one of them is the targetConcept
		// Will stop at the root concept, returning false, as it has no parents
		for (Concept thisParent : parents) {
			if (thisParent.equals(targetConcept) || thisParent.hasParent(targetConcept)) {
				return true;
			}
		}
		return false;
	}

	public List<Relationship> findMatchingRelationships(Long typeId, int group) {
		//find relationships of this concept with the same type and group
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship thisRelationship : attributes) {
			if (thisRelationship.matchesTypeAndGroup(typeId, group)) {
				matches.add(thisRelationship);
			}
		}
		return matches;
	}

}
