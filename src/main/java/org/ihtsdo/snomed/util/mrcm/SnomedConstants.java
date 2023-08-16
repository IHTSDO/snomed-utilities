package org.ihtsdo.snomed.util.mrcm;

public interface SnomedConstants {

	enum DefinitionStatus {
		FULLY_DEFINED("F"),
		PRIMITIVE("P"),
		ALL("A");
		private final String shortForm;
		DefinitionStatus(String shortForm){
			this.shortForm = shortForm;
		}
		public static DefinitionStatus getStatus(String shortForm) {
            return switch (shortForm.toUpperCase()) {
                case ("F") -> FULLY_DEFINED;
                case ("P") -> PRIMITIVE;
                case ("A") -> ALL;
                default -> null;
            };
		}
	}
}
