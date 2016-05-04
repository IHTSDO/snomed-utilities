package org.ihtsdo.snomed.util.pojo;

import org.ihtsdo.snomed.util.rf2.schema.RF1SchemaConstants;

public class RF1Relationship extends Relationship implements RF1SchemaConstants {
	
	Integer refinability;

	// Was originally splitting the string in the constructor, but expensive to create object
	// if active flag is zero, so check this before passing in
	public RF1Relationship(String[] lineValues, CHARACTERISTIC characteristic) throws Exception {
		typeId = new Long(lineValues[RF1_REL_IDX_TYPEID]);
		group = Integer.parseInt(lineValues[RF1_REL_IDX_RELATIONSHIPGROUP]);
		uuid = type5UuidFactory.get(
				lineValues[RF1_REL_IDX_SOURCEID] + lineValues[RF1_REL_IDX_DESTINATIONID] + lineValues[RF1_REL_IDX_TYPEID]
				+ lineValues[RF1_REL_IDX_RELATIONSHIPGROUP])
				.toString();
		sourceConcept = Concept.registerConcept(lineValues[RF1_REL_IDX_SOURCEID], characteristic);
		destinationConcept = Concept.registerConcept(lineValues[RF1_REL_IDX_DESTINATIONID], characteristic);
		sourceConcept.addAttribute(this);
		refinability = Integer.parseInt(lineValues[RF1_REL_IDX_REFINEABILITY]);
	}

	public Integer getRefinability() {
		return refinability;
	}

}
