package org.ihtsdo.snomed.util.mrcm;

import java.util.Scanner;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;

public class SearchParameters {
	
	public enum ATTRIBUTE_SEARCH {
		AttributeType, AttributeValue, AttributeValueString
	}

	boolean fullyDefinedOnly;
	Concept hierarchy;

	String description;
	boolean descriptionPresent;

	ATTRIBUTE_SEARCH attributeSearch;
	Concept attributeType;
	Concept attributeValue;
	String attributeValueString;
	boolean attributeSearchPresent;

	public SearchParameters init(CHARACTERISTIC characteristic, Scanner in) {

		printn("Enter hierarchy to process (eg 404684003 Clinical Finding): ");
		long sctid = Long.parseLong(in.nextLine().trim());
		hierarchy = Concept.getConcept(sctid, characteristic);
		
		printn("Fully Defined only? Y/N: ");
		String response = in.nextLine().trim().toLowerCase();
		fullyDefinedOnly = response.equals("y");

		printn("Enter term of interest (currently FSN only): ");
		description = in.nextLine().trim().toLowerCase();
		printn("Should term be present? Y/N: ");
		response = in.nextLine().trim().toLowerCase();
		descriptionPresent = response.equals("y");

		printn("Attribute Match - (T)ype (V)alue (S)tring: ");
		response = in.nextLine().trim().toUpperCase();
        switch (response) {
            case "T" -> {
                attributeSearch = ATTRIBUTE_SEARCH.AttributeType;
                printn("Enter attribute type of interest: ");
                sctid = Long.parseLong(in.nextLine().trim());
                attributeType = Concept.getConcept(sctid, characteristic);
            }
            case "V" -> {
                attributeSearch = ATTRIBUTE_SEARCH.AttributeValue;
                printn("Enter attribute value of interest (eg 424124008 Sudden onset AND/OR short duration): ");
                sctid = Long.parseLong(in.nextLine().trim());
                attributeValue = Concept.getConcept(sctid, characteristic);
            }
            case "S" -> {
                attributeSearch = ATTRIBUTE_SEARCH.AttributeValueString;
                printn("Enter attribute value string term: ");
                attributeValueString = in.nextLine().trim();
            }
            default -> {
                printn("Unrecognised option " + response);
                return null;
            }
        }

		printn("Should attribute search term be present? Y/N: ");
		response = in.nextLine().trim().toLowerCase();
		attributeSearchPresent = response.equals("y");

		return this;
	}

	private void printn(String msg) {
		System.out.print(msg);
	}

}
