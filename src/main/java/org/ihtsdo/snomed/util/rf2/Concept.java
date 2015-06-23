package org.ihtsdo.snomed.util.rf2;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ihtsdo.snomed.util.Type5UuidFactory;
import org.ihtsdo.snomed.util.rf2.Relationship.CHARACTERISTIC;
import org.ihtsdo.snomed.util.rf2.schema.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Concept implements Comparable<Concept> {

	private Long sctId;

	private static final Logger LOGGER = LoggerFactory.getLogger(Concept.class);

	private static Type5UuidFactory type5UuidFactory;

	private int maxGroupId = 0; // How many groups are defined for this source concept?

	static {
		try {
			type5UuidFactory = new Type5UuidFactory();
		} catch (Exception e) {
			throw new RuntimeException("Unable to initialise UUID factory", e);
		}
	}

	Set<Concept> parents = new TreeSet<Concept>();
	TreeSet<Relationship> attributes = new TreeSet<Relationship>();

	public Concept(Long id) {
		this.sctId = id;
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
		sourceConcept.addAttribute(relationship);
	}

	private void addAttribute(Relationship relationship) {
		attributes.add(relationship);
		if (relationship.getGroup() > this.maxGroupId) {
			this.maxGroupId = relationship.getGroup();
		}
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
			LOGGER.debug(thisConcept.toString());
		}

	}

	@Override
	public int compareTo(Concept other) {
		return this.sctId.compareTo(other.sctId);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Concept) {
			Concept otherConcept = (Concept) other;
			return this.sctId.equals(otherConcept.sctId);
		}
		return false;
	}

	public static Concept getConcept(Long conceptId, CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;
		return allConcepts.get(conceptId);
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

	public List<Relationship> findMatchingRelationships(Long typeId, Concept statedDestinationConcept) {
		// find relationships of this concept with the same type and group, and where the destination has the
		// statedDestinationConcept as a parent.

		// lets match on type first since it's cheap
		List<Relationship> firstPassMatches = findMatchingRelationships(typeId);

		// Now we'll try for an exact match with the destination concept, and if not found,
		// run again looking for more proximate children
		List<Relationship> secondPassMatches = new ArrayList<Relationship>();
		for (Relationship thisRelationship : firstPassMatches) {
			if (thisRelationship.getDestinationConcept().equals(statedDestinationConcept)) {
				secondPassMatches.add(thisRelationship);
			}
		}

		if (secondPassMatches.size() == 0) {
			for (Relationship thisRelationship : firstPassMatches) {
				if (thisRelationship.getDestinationConcept().hasParent(statedDestinationConcept)) {
					secondPassMatches.add(thisRelationship);
				}
			}
		}

		return secondPassMatches;
	}

	private List<Relationship> findMatchingRelationships(Long typeId) {
		// find relationships of this concept with the same type
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship thisRelationship : attributes) {
			if (thisRelationship.isType(typeId)) {
				matches.add(thisRelationship);
			}
		}
		return matches;
	}

	public boolean hasParent(Concept targetConcept) {
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

	public List<Relationship> findMatchingRelationships(Long typeId, Long destinationId, int group) {
		// find relationships of this concept with the same type and group
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship thisRelationship : attributes) {
			if (thisRelationship.matchesTypeAndGroup(typeId, group) && thisRelationship.getDestinationId().equals(destinationId)) {
				matches.add(thisRelationship);
			}
		}
		return matches;
	}

	public List<Relationship> findMatchingRelationships(int group, boolean filterIsAs) {
		// find relationships of this concept with the group
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship thisRelationship : attributes) {
			if (thisRelationship.matchesGroup(group)) {
				// Are we filtering out Is A relationships?
				if (!filterIsAs || (filterIsAs && !thisRelationship.isType(Relationship.ISA_ID))) {
					matches.add(thisRelationship);
				}
			}
		}
		return matches;
	}

	/**
	 * @return a predictable UUID such that a given set of triples (source + destination + type) can be uniquely identified as a group,
	 *         regardless of the group id assigned.
	 * @throws UnsupportedEncodingException
	 */
	public String getTriplesHash(int group) throws UnsupportedEncodingException {
		String stringToHash = "";
		for (Relationship thisRelationship : attributes) {
			if (thisRelationship.matchesGroup(group)) {
				stringToHash += thisRelationship.getTripleString();
			}
		}
		return type5UuidFactory.get(stringToHash).toString();
	}

	/*
	 * Finds a relationship that matches on triple where the triple belongs to a group that matches on triples Hash
	 */
	public List<Relationship> findMatchingRelationships(String triplesHash, Relationship sRelationship) throws UnsupportedEncodingException {
		// Can we find a group where every triple matches - triples Hash?
		for (int groupId = 1; groupId <= this.maxGroupId; groupId++) {
			if (triplesHash.equals(getTriplesHash(groupId))) {
				// Now find a relationship within that matching group which has this triple. Source is taken
				// for granted since we're working from the concept
				return findMatchingRelationships(sRelationship.getTypeId(), sRelationship.getDestinationId(), groupId);
			}
		}
		return null;
	}

	/**
	 * @return a list of relationships in groups where the group contains at least ALL the given relationship types
	 */
	public List<Relationship> findMatchingRelationships(List<Long> groupTypes) {
		List<Relationship> allGroupRelationships = new ArrayList<Relationship>();
		// Work through all the groups
		for (int groupId = 1; groupId <= this.maxGroupId; groupId++) {
			boolean allMatch = true;
			List<Relationship> thisGroupRelationships = findMatchingRelationships(groupId, false);
			// Work through all the types and make sure each one is represented
			second_loop:
			for (Long thisType : groupTypes) {
				for (Relationship thisRelationship : thisGroupRelationships) {
					if (thisRelationship.isType(thisType)) {
						continue second_loop; // Move on to next type
					}
				}
				// If we've not broken out of the loop by here, then we've failed to find a match for this Type
				allMatch = false;
				// No need to carry on through other types if we can't match this one
				break;
			}

			// If all types are represented, then add these relationships to our list of potential matches
			if (allMatch) {
				allGroupRelationships.addAll(thisGroupRelationships);
			}
		}
		return allGroupRelationships;
	}

	public TreeSet<Relationship> getAttributes() {
		return attributes;
	}

	/**
	 * @return a list of relationships for this concept where the type is the same or a child of the target relationship type
	 */
	public List<Relationship> findMatchingRelationships(Concept relType) {
		List<Relationship> matches = new ArrayList<Relationship>();
		for (Relationship thisRel : this.attributes) {
			if (thisRel.isType(relType.getSctId())) {
				matches.add(thisRel);
			} else {
				// Don't do this for "Is A" since we know we don't have any more specific cases of that attribute
				if (!thisRel.isISA()) {
					// Find the concept for this potential match relationship in the inferred graph
					Concept potentialMatchingType = Concept.getConcept(thisRel.getTypeId(), CHARACTERISTIC.INFERRED);
					// does this potential relationship's type have the target type as a parent?
					if (potentialMatchingType.hasParent(relType)) {
						matches.add(thisRel);
					}
				}
			}
		}
		return matches;
	}

	public Long getSctId() {
		return sctId;
	}

	public String toString() {
		return getSctId().toString();
	}

}
