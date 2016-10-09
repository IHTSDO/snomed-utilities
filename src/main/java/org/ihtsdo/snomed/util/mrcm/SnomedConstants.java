package org.ihtsdo.snomed.util.mrcm;

public interface SnomedConstants {

	enum DefinitionStatus {
		FULLY_DEFINED("F"),
		PRIMITIVE("P"),
		ALL("A");
		private String shortForm;
		DefinitionStatus(String shortForm){
			this.shortForm = shortForm;
		}
		public static DefinitionStatus getStatus(String shortForm) {
			switch (shortForm.toUpperCase()) {
			case("F") : return FULLY_DEFINED;
			case("P") : return PRIMITIVE;
			case("A") : return ALL;
			default : return null;
			}
		}
	}
}
