package org.ihtsdo.snomed.util.pojo;

import java.util.List;

import org.ihtsdo.snomed.util.Type5UuidFactory;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Relationship implements Comparable<Relationship>, RF2SchemaConstants {

	private static Type5UuidFactory type5UuidFactory;
	static {
		try {
			type5UuidFactory = new Type5UuidFactory();
		} catch (Exception e) {
			throw new RuntimeException("Unable to initialise UUID factory", e);
		}
	}

	// private String[] lineValues;
	private Concept sourceConcept;
	private Concept destinationConcept;
	private Long typeId;
	private String uuid;
	private int group;
	private boolean active;

	// Was originally splitting the string in the constructor, but expensive to create object
	// if active flag is zero, so check this before passing in
	public Relationship(String[] lineValues, CHARACTERISTIC characteristic) throws Exception {
		//lineValues = lineValues;
		typeId = new Long(lineValues[REL_IDX_TYPEID]);
		group = Integer.parseInt(lineValues[REL_IDX_RELATIONSHIPGROUP]);
		uuid = type5UuidFactory.get(
				lineValues[REL_IDX_SOURCEID] + lineValues[REL_IDX_DESTINATIONID] + lineValues[REL_IDX_TYPEID]
				+ lineValues[REL_IDX_RELATIONSHIPGROUP])
				.toString();
		sourceConcept = Concept.registerConcept(lineValues[REL_IDX_SOURCEID], characteristic);
		destinationConcept = Concept.registerConcept(lineValues[REL_IDX_DESTINATIONID], characteristic);
		sourceConcept.addAttribute(this);
	}

	boolean isISA() {
		return typeId.equals(ISA_ID);
	}

	public Long getTypeId() {
		return typeId;
	}

	public String getUuid() {
		return uuid;
	}

	public Concept getSourceConcept() {
		return sourceConcept;
	}

	public void setSourceConcept(Concept sourceConcept) {
		this.sourceConcept = sourceConcept;
	}

	public Concept getDestinationConcept() {
		return destinationConcept;
	}

	public void setDestinationConcept(Concept destinationConcept) {
		this.destinationConcept = destinationConcept;
	}

	public boolean isActive() {
		return active;
	}

	public int getGroup() {
		return this.group;
	}

	public Long getSourceId() {
		return sourceConcept.getSctId();
	}

	public Long getDestinationId() {
		return destinationConcept.getSctId();
	}

	public boolean isType(Long thisType) {
		return this.typeId.equals(thisType);
	}

	public String toString() {
		return toString(false);
	}

	@Override
	public int compareTo(Relationship other) {
		// Sort on source sctid, group, type, destination
		int i = this.getSourceId().compareTo(other.getSourceId());
		if (i != 0)
			return i;

		i = this.getGroup() - other.getGroup();
		if (i != 0)
			return i;

		i = this.getTypeId().compareTo(other.getTypeId());
		if (i != 0)
			return i;

		return this.getDestinationId().compareTo(other.getDestinationId());
	}

	public String toString(boolean addStar) {
		StringBuilder sb = new StringBuilder();
		sb.append("[S: ")
.append(getSourceId())
			.append(", D: ")
.append(getDestinationId())
			.append(", T: ")
.append(getTypeId())
			.append( ", G: ")
.append(getGroup())
			.append("] ");
		return sb.toString();
	}

	public boolean isGroup(int group) {
		return this.group == group;
	}

}
