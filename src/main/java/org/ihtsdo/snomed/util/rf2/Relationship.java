package org.ihtsdo.snomed.util.rf2;

import org.ihtsdo.snomed.util.Type5UuidFactory;

public class Relationship {

	// id effectiveTime active moduleId sourceId destinationId relationshipGroup typeId characteristicTypeId modifierId
	public static final long ISA_ID = 116680003;
	public static final String FIELD_DELIMITER = "\t";
	public static final String ACTIVE_FLAG = "1";

	public static enum CHARACTERISTIC {
		STATED, INFERRED
	};

	private static Type5UuidFactory type5UuidFactory;

	static {
		try {
			type5UuidFactory = new Type5UuidFactory();
		} catch (Exception e) {
			throw new RuntimeException("Unable to initialise UUID factory", e);
		}
	}


	Concept sourceConcept;
	Concept destinationConcept;

	Long typeId;
	String uuid;
	int group;
	Relationship replacement = null;

	boolean needsReplaced = false;

	public static final int IDX_ID = 0;
	public static final int IDX_EFFECTIVETIME = 1;
	public static final int IDX_ACTIVE = 2;
	public static final int IDX_MODULEID = 3;
	public static final int IDX_SOURCEID = 4;
	public static final int IDX_DESTINATIONID = 5;
	public static final int IDX_RELATIONSHIPGROUP = 6;
	public static final int IDX_TYPEID = 7;
	public static final int IDX_CHARACTERISTICTYPEID = 8;
	public static final int IDX_MODIFIERID = 9;

	private String[] lineValues;

	// Was originally splitting the string in the constructor, but expensive to create object
	// if active flag is zero, so check this before passing in
	public Relationship(String[] lineValues, CHARACTERISTIC characteristic) throws Exception {
		this.lineValues = lineValues;
		typeId = new Long(getField(IDX_TYPEID));
		group = Integer.parseInt(getField(IDX_RELATIONSHIPGROUP));
		uuid = type5UuidFactory.get(
				lineValues[IDX_SOURCEID] + lineValues[IDX_DESTINATIONID] + lineValues[IDX_TYPEID] + lineValues[IDX_RELATIONSHIPGROUP])
				.toString();
		// If this relationship is an "IS A" then add that to the concept
		Concept.addRelationship(this, characteristic);
	}

	boolean isISA() {
		return typeId == ISA_ID;
	}

	String getField(int fieldIdx) {
		return lineValues[fieldIdx];
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
		return lineValues[IDX_ACTIVE].equals(ACTIVE_FLAG);
	}

	public boolean isNeedsReplaced() {
		return needsReplaced;
	}

	public void setNeedsReplaced(boolean needsReplaced) {
		this.needsReplaced = needsReplaced;
	}

	public boolean hasReplacement() {
		return replacement != null;
	}

	public int getGroup() {
		return this.group;
	}

	public boolean matchesTypeAndGroup(Long typeId, int group) {
		return (this.group == group) && this.typeId.equals(typeId);
	}

	public void setReplacement(Relationship replacementRelationship) {
		this.replacement = replacementRelationship;

	}

	public Long getSourceId() {
		return new Long(lineValues[IDX_SOURCEID]);
	}

	public Long getDestinationId() {
		return new Long(lineValues[IDX_DESTINATIONID]);
	}

}
