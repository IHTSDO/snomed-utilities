package org.ihtsdo.snomed.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

public class SnomedUtils implements RF2SchemaConstants{
	
	private static VerhoeffCheckDigit verhoeffCheck = new VerhoeffCheckDigit();

	public static String isValid(String sctId, PartionIdentifier partitionIdentifier) {
		String errorMsg=null;
		int partitionNumber = Integer.valueOf("" + sctId.charAt(sctId.length() -2));
		if ( partitionNumber != partitionIdentifier.ordinal()) {
			errorMsg = sctId + " does not exist in partition " + partitionIdentifier.toString();
		}
		if (!verhoeffCheck.isValid(sctId)) {
			errorMsg = sctId + " does not exhibit a valid check digit";
		}
		return errorMsg;
	}
	
	public static String[] deconstructFSN(String fsn) {
		String[] elements = new String[2];
		int cutPoint = fsn.lastIndexOf(SEMANTIC_TAG_START);
		elements[0] = fsn.substring(0, cutPoint).trim();
		elements[1] = fsn.substring(cutPoint);
		return elements;
	}
	
	public static String toString(Map<String, ACCEPTABILITY> acceptabilityMap) throws SnomedUtilException {
		String US = "N";
		String GB = "N";
		if (acceptabilityMap.containsKey(US_ENG_LANG_REFSET)) {
			US = translatAcceptability(acceptabilityMap.get(US_ENG_LANG_REFSET));
		}
		
		if (acceptabilityMap.containsKey(GB_ENG_LANG_REFSET)) {
			GB = translatAcceptability(acceptabilityMap.get(GB_ENG_LANG_REFSET));
		}
		
		return "US: " + US + ", GB: " + GB;
	}
	
	public static String translatAcceptability (ACCEPTABILITY a) throws SnomedUtilException {
		if (a.equals(ACCEPTABILITY.PREFERRED)) {
			return "P";
		}
		
		if (a.equals(ACCEPTABILITY.ACCEPTABLE)) {
			return "A";
		}
		throw new SnomedUtilException("Unable to translate acceptability " + a);
	}

	public static String substitute(String str,
			Map<String, String> wordSubstitution) {
		//Replace any instances of the map key with the corresponding value
		for (Map.Entry<String, String> substitution : wordSubstitution.entrySet()) {
			str = str.replace(substitution.getKey(), substitution.getValue());
		}
		return str;
	}
	
	public static String capitalize (String str) {
		if (str == null || str.isEmpty() || str.length() < 2) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static List<String> removeBlankLines(List<String> lines) {
		List<String> unixLines = new ArrayList<String>();
		for (String thisLine : lines) {
			if (!thisLine.isEmpty()) {
				unixLines.add(thisLine);
			}
		}
		return unixLines;
	}

	/**
	 * @return an array of 3 elements containing:  The path, the filename, the file extension (if it exists) or empty strings
	 */
	public static String[] deconstructFilename(File file) {
		String[] parts = new String[] {"","",""};
		
		if (file== null) {
			return parts;
		}
		parts[0] = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
		if (file.getName().lastIndexOf(".") > 0) {
			parts[1] = file.getName().substring(0, file.getName().lastIndexOf("."));
			parts[2] = file.getName().substring(file.getName().lastIndexOf(".") + 1);
		} else {
			parts[1] = file.getName();
		}
		
		return parts;
	}

}
