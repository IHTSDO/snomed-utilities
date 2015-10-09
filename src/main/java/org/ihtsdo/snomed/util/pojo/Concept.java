package org.ihtsdo.snomed.util.pojo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

public class Concept implements Comparable<Concept>, RF2SchemaConstants {

	private static Map<Long, Concept> allStatedConcepts = new HashMap<Long, Concept>();
	private static Map<Long, Concept> allInferredConcepts = new HashMap<Long, Concept>();

	private Long sctId;
	private int maxGroupId = 0; // How many groups are defined for this source concept?
	Set<Concept> parents = new TreeSet<Concept>();
	TreeSet<Relationship> attributes = new TreeSet<Relationship>();

	public Concept(Long id) {
		this.sctId = id;
	}

	public static Concept registerConcept(String sctIdStr, CHARACTERISTIC characteristic) {
		Map<Long, Concept> allConcepts = characteristic.equals(Relationship.CHARACTERISTIC.STATED) ? allStatedConcepts
				: allInferredConcepts;
		Long sctId = new Long(sctIdStr);
		// Do we know about this concept?
		Concept concept;
		if (!allConcepts.containsKey(sctId)) {
			concept = new Concept(sctId);
			allConcepts.put(sctId, concept);
		} else {
			concept = allConcepts.get(sctId);
		}
		return concept;
	}


	public void addAttribute(Relationship relationship) {
		assert this.equals(relationship.getSourceConcept());

		// Is this an IS A relationship? Add as a parent if so
		if (relationship.isISA()) {
			parents.add(relationship.getDestinationConcept());
		} else {
			attributes.add(relationship);
			if (relationship.getGroup() > this.maxGroupId) {
				this.maxGroupId = relationship.getGroup();
			}
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

	public Long getSctId() {
		return sctId;
	}


}
