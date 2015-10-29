package org.ihtsdo.snomed.util.pojo;

import java.util.HashMap;
import java.util.Map;

import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

public class Description implements RF2SchemaConstants {

	private final Long conceptId;
	private final String term;

	private static Map<Long, Description> allFSNs = new HashMap<Long, Description>();

	public Description(String[] lineItems) {
		conceptId = Long.valueOf(lineItems[DES_IDX_CONCEPTID]);
		term = lineItems[DES_IDX_TERM];
		if (lineItems[DES_IDX_TYPEID].equals(FULLY_SPECIFIED_NAME)) {
			allFSNs.put(conceptId, this);
		}
	}

	public static String getFormattedConcept(Long conceptId) {
		String formattedConcept = Long.toString(conceptId);
		if (allFSNs.containsKey(conceptId)) {
			formattedConcept += "|" + allFSNs.get(conceptId).term + "|";
		}
		return formattedConcept;
	}
}
