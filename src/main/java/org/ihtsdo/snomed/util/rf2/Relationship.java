package org.ihtsdo.snomed.util.rf2;

import java.util.List;

import org.ihtsdo.snomed.util.Type5UuidFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Relationship implements Comparable<Relationship> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Relationship.class);


	// id effectiveTime active moduleId sourceId destinationId relationshipGroup typeId characteristicTypeId modifierId
	public static final Long ISA_ID = new Long(116680003);
	public static final String CHARACTERISTIC_STATED_SCTID = "900000000000010007";
	public static final String FIELD_DELIMITER = "\t";
	public static final String LINE_DELIMITER = "\r\n";
	public static final String ACTIVE_FLAG = "1";
	public static final String INACTIVE_FLAG = "0";
	public static final String HEADER_ROW = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\r\n";

	public static enum CHARACTERISTIC {
		STATED, INFERRED, ADDITIONAL
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
	private Relationship isReplacementFor = null;
	private int replacementNumber = 0;

	private boolean needsReplaced = false;
	private String replacedByAlg = "";

	// If we're replacing a stated relationship that already exists, we can just
	// suppress this replacement and allow the stated relationship to remain
	private boolean suppressOutput = false;

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

	public static final String STATED_UUID_MODIFIER = "S";
	
	private String[] lineValues;

	// Was originally splitting the string in the constructor, but expensive to create object
	// if active flag is zero, so check this before passing in
	public Relationship(String[] lineValues, CHARACTERISTIC characteristic) throws Exception {
		this.lineValues = lineValues;
		typeId = new Long(getField(IDX_TYPEID));
		group = Integer.parseInt(getField(IDX_RELATIONSHIPGROUP));
		uuid = type5UuidFactory.get(
				getTripleString() 
				+ lineValues[IDX_RELATIONSHIPGROUP]
				+ STATED_UUID_MODIFIER)
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

	public boolean needsReplaced() {
		return needsReplaced;
	}

	public void needsReplaced(boolean needsReplaced) {
		this.needsReplaced = needsReplaced;
	}

	public boolean hasReplacement() {
		return replacement != null;
	}

	public int getGroup() {
		return this.group;
	}

	public void setReplacement(Relationship replacementRelationship, String replacementAlgorithm) {

		// Is this replacement already in use? Remove it from the original stated relationship if so.
		if (replacementRelationship.isReplacement()) {
			Relationship originallyReplacedRelationship = replacementRelationship.isReplacementFor();
			LOGGER.warn("Replacement {} removed from {} ", replacementRelationship, originallyReplacedRelationship);
			originallyReplacedRelationship.removeReplacement();
		}

		this.replacement = replacementRelationship;
		replacementRelationship.replacedByAlg = replacementAlgorithm;
		// Sometimes we replace relationships early if the entire group needs to move, so say we needed replacement in
		// that case to keep the counts sane.
		this.needsReplaced(true);
		// Both the stated and the inferred relationship will take the same replacement number so we can match them up
		int replacementNumber = this.sourceConcept.getNextReplacmentNumber();
		this.replacementNumber = replacementNumber;
		replacementRelationship.replacementNumber = replacementNumber;

		// The replacement will also need to know what it's replacing
		replacementRelationship.isReplacementFor(this);
	}

	private void removeReplacement() {
		this.replacedByAlg = "";
		this.replacement = null;
		this.replacementNumber = 0;
	}

	public Long getSourceId() {
		return new Long(lineValues[IDX_SOURCEID]);
	}

	public Long getDestinationId() {
		return new Long(lineValues[IDX_DESTINATIONID]);
	}

	public boolean isType(Long thisType) {
		return this.typeId.equals(thisType);
	}

	public String toString() {
		return toString(false);
	}

	public String getRF2(String effectiveTime, String activeFlag, String chacteristicTypeId, boolean wipeSCTID) {
		StringBuffer sb = new StringBuffer();
		// Output all columns, replacing effectiveTime, active and chacteristicTypeId to passed in values
		for (int columnIdx = 0; columnIdx <= MAX_COLUMN; columnIdx++) {
			if (columnIdx == IDX_ID) {
				if (!wipeSCTID) {
					sb.append(lineValues[IDX_ID]);
				} // otherwise we'll drop straight through to the tab separator
			} else if (columnIdx == IDX_EFFECTIVETIME) {
				sb.append(effectiveTime);
			} else if (columnIdx == IDX_ACTIVE) {
				// If active flag has not been specified, use current value
				if (activeFlag == null) {
					sb.append(lineValues[IDX_ACTIVE]);
				} else {
					sb.append(activeFlag);
				}
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

	public String toString(boolean addStar) {
		StringBuilder sb = new StringBuilder();
		sb.append("[S: ")
			.append(lineValues[IDX_SOURCEID])
			.append(", D: ")
			.append(lineValues[IDX_DESTINATIONID])
			.append(", T: ")
			.append(lineValues[IDX_TYPEID])
			.append( ", G: ")
			.append(lineValues[IDX_RELATIONSHIPGROUP])
			.append("] ");

		if (hasReplacement() || isReplacement()) {
			sb.append("(").append(replacementNumber);
			if (isReplacement()) {
				sb.append(" by ").append(replacedByAlg);
			}
			sb.append(")");
		}
	
		if (addStar) {
			sb.append("*");
		}
		
		if (isReplacement()) {
			sb.append(" [")
.append(uuid).append("]");
		}
		return sb.toString();
	}

	public boolean isGroup(int group) {
		return this.group == group;
	}

	public int getReplacementNumber() {
		return replacementNumber;
	}

	/*
	 * If we're trying to replace a stated relationship with an inferred relationship in another group where that same type already exists
	 * as a STATED group, then we'll try another match. UNLESS that stated relationship we're going to collide with also requires
	 * replacment, in which case it will probably move out of the way as we often see group numbers incrementing through the classification
	 * process. BUT for "is a" relationships, we need to check we're not picking an inferred relationship that already exists as a stated
	 * relationship - ie check matching destination also since there are often many relationships of that type in group 0 The earlier
	 * algorithm - like a match on exact triple (algorithm 2) is pretty certain, so we won't worry about causing a duplicate with and
	 * existing stated relationship in that case.
	 */
	public boolean isSafelyReplacedBy(Relationship potentialReplacement, boolean isCertain) {
		boolean isSafeReplacement = false;
		// Firstly, has this potential Replacement already been assigned as a replacement? Try and avoid if so

		if (potentialReplacement.isReplacement()) {
			isSafeReplacement = false;
		} else if (this.isISA() && this.getSourceConcept().findMatchingRelationships(potentialReplacement).size() == 0) {
			//This is an 'Is A' relationship, but there's no current stated 'Is A' relationships 
			//identical to the proposed inferred relationship
			isSafeReplacement = true;
		} else {
			// Are we replacing with different group? Is not a problem if we're staying in the same group
			if (!this.isGroup(potentialReplacement.getGroup())) {
				//Does the intended type/group already exist as a stated relationship, ie
				//are we going to create a duplicate stated relationship?
				List<Relationship> potentialDuplicates = this.getSourceConcept()
						.findMatchingRelationships(potentialReplacement.getTypeId(), 
													potentialReplacement.getGroup());
				if (potentialDuplicates.size() == 0) {
					isSafeReplacement = true;
				} else {
					//If the first potential duplicate is itself also needs replaced, then it
					//will most likely shift group too (often the group increments) and so we'll 
					// risk it. Or if we can be pretty certain the algorithm is correct (eg Alg2)
					if (potentialDuplicates.get(0).needsReplaced() || isCertain) {
						isSafeReplacement = true;
					}
				}
			} else {
				// Replacing a stated relationship with one in the same group is safe
				isSafeReplacement = true;
			}
		}
		return isSafeReplacement;
	}

	public Relationship isReplacementFor() {
		return isReplacementFor;
	}

	public boolean isReplacement() {
		return isReplacementFor != null;
	}

	public void isReplacementFor(Relationship isReplacementFor) {
		this.isReplacementFor = isReplacementFor;
	}

	public boolean suppressOutput() {
		return suppressOutput;
	}

	public void suppressOutput(boolean suppressOutput) {
		this.suppressOutput = suppressOutput;
	}

}
