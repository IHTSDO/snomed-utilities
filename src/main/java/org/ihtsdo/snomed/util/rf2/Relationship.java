package org.ihtsdo.snomed.util.rf2;

import org.ihtsdo.snomed.util.Type5UuidFactory;

public class Relationship implements Comparable<Relationship> {

	// id effectiveTime active moduleId sourceId destinationId relationshipGroup typeId characteristicTypeId modifierId
	public static final Long ISA_ID = new Long(116680003);
	public static final String CHARACTERISTIC_STATED_SCTID = "900000000000010007";
	public static final String FIELD_DELIMITER = "\t";
	public static final String LINE_DELIMITER = "\r\n";
	public static final String ACTIVE_FLAG = "1";
	public static final String INACTIVE_FLAG = "0";
	public static final String HEADER_ROW = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\r\n";

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

	private Long typeId;
	private String uuid;
	private int group;
	private Relationship replacement = null;

	private boolean needsReplaced = false;

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
	public static final int MAX_COLUMN = 9;


	private String[] lineValues;

	// Was originally splitting the string in the constructor, but expensive to create object
	// if active flag is zero, so check this before passing in
	public Relationship(String[] lineValues, CHARACTERISTIC characteristic) throws Exception {
		this.lineValues = lineValues;
		typeId = new Long(getField(IDX_TYPEID));
		group = Integer.parseInt(getField(IDX_RELATIONSHIPGROUP));
		uuid = type5UuidFactory.get(
				getTripleString() + lineValues[IDX_RELATIONSHIPGROUP])
				.toString();
		// If this relationship is an "IS A" then add that to the concept
		Concept.addRelationship(this, characteristic);
	}

	public String getTripleString() {
		return lineValues[IDX_SOURCEID] + lineValues[IDX_DESTINATIONID] + lineValues[IDX_TYPEID];
	}

	boolean isISA() {
		return typeId.equals(ISA_ID);
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
		return matchesGroup(group) && this.typeId.equals(typeId);
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

	public boolean matchesGroup(int group) {
		return (this.group == group);
	}

	public boolean isType(Long thisType) {
		return this.typeId.equals(thisType);
	}

	public String toString() {
		return "[S: " + lineValues[IDX_SOURCEID] + ", D: " + lineValues[IDX_DESTINATIONID] + ", T: " + lineValues[IDX_TYPEID] + ", G: "
				+ lineValues[IDX_RELATIONSHIPGROUP] + "] ";
	}

	public String getRF2(String effectiveTime, String activeFlag, String chacteristicTypeId) {
		StringBuffer sb = new StringBuffer();
		// Output all columns, replacing effectiveTime, active and chacteristicTypeId to passed in values
		for (int columnIdx = 0; columnIdx <= MAX_COLUMN; columnIdx++) {
			if (columnIdx == IDX_EFFECTIVETIME) {
				sb.append(effectiveTime);
			} else if (columnIdx == IDX_ACTIVE) {
				sb.append(activeFlag);
			} else if (columnIdx == IDX_CHARACTERISTICTYPEID) {
				sb.append(chacteristicTypeId);
			} else {
				sb.append(lineValues[columnIdx]);
			}

			if (columnIdx < MAX_COLUMN) {
				sb.append(FIELD_DELIMITER);
			}
		}

		sb.append(LINE_DELIMITER);
		return sb.toString();
	}

	public Relationship getReplacement() {
		return replacement;
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

}
