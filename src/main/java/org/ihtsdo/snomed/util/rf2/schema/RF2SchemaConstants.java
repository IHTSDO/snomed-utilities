package org.ihtsdo.snomed.util.rf2.schema;

public interface RF2SchemaConstants {

	// id effectiveTime active moduleId sourceId destinationId relationshipGroup typeId characteristicTypeId modifierId
	public static final Long ISA_ID = new Long(116680003);
	public static final Long SNOMED_ROOT_CONCEPT = new Long(138875005);
	public static final String CHARACTERISTIC_STATED_SCTID = "900000000000010007";
	public static final String FIELD_DELIMITER = "\t";
	public static final String LINE_DELIMITER = "\r\n";
	public static final String ACTIVE_FLAG = "1";
	public static final String INACTIVE_FLAG = "0";
	public static final String HEADER_ROW = "id\teffectiveTime\tactive\tmoduleId\tsourceId\tdestinationId\trelationshipGroup\ttypeId\tcharacteristicTypeId\tmodifierId\r\n";

	static int NOT_SET = -1;
	static int IMMEDIATE_CHILD = 1;
	static int NA = -1;
	static String FSN = "900000000000003001";
	static final String FULLY_DEFINED_SCTID = "900000000000073002";
	static final String FULLY_SPECIFIED_NAME = "900000000000003001";
	final String ADDITIONAL_RELATIONSHIP = "900000000000227009";
	final String SPACE = " ";
	final String COMMA = ",";
	final String COMMA_QUOTE = ",\"";
	final String QUOTE_COMMA = "\",";
	final String QUOTE_COMMA_QUOTE = "\",\"";
	final String TAB = "\t";
	final String CSV_FIELD_DELIMITER = COMMA;
	final String TSV_FIELD_DELIMITER = TAB;
	final String QUOTE = "\"";
	final String INGREDIENT_SEPARATOR = "+";
	final String INGREDIENT_SEPARATOR_ESCAPED = "\\+";
	
	public enum RF2_FILE { CONCEPT, STATED, INFERRED, LANGUAGE, DESCRIPTION };
	
	public enum PartionIdentifier {CONCEPT, DESCRIPTION, RELATIONSHIP};
	
	public enum InactivationIndicator {DUPLICATE, OUTDATED, ERRONEOUS, LIMITED, MOVED_ELSEWHERE, 
		PENDING_MOVE, INAPPROPRIATE, CONCEPT_NON_CURRENT, RETIRED};
	
	static final String GB_ENG_LANG_REFSET = "900000000000508004";
	static final String US_ENG_LANG_REFSET = "900000000000509007";
	
	final public String SEMANTIC_TAG_START = "(";
	
	public enum CHARACTERISTIC { STATED, INFERRED, QUALIFYING, ADDITIONAL, ALL};
	public static String SCTID_STATED_RELATIONSHIP = "900000000000010007";
	public static String SCTID_INFERRED_RELATIONSHIP = "900000000000011006";
	public static String SCTID_QUALIFYING_RELATIONSHIP = "900000000000225001";
	public static String SCTID_ADDITIONAL_RELATIONSHIP = "900000000000227009";
	
	public enum DEFINITION_STATUS { PRIMITIVE, FULLY_DEFINED };
	
	public enum MODIFER { EXISTENTIAL, UNIVERSAL};
	public static String SCTID_EXISTENTIAL_MODIFIER = "900000000000451002";
	public static String SCTID_UNIVERSAL_MODIFIER = "900000000000450001";

	public enum ACTIVE_STATE { ACTIVE, INACTIVE, BOTH };
	public enum ACCEPTABILITY { ACCEPTABLE, PREFERRED };
	public enum CARDINALITY { AT_LEAST_ONE, EXACTLY_ONE };
	public enum DESCRIPTION_TYPE { FSN, SYNONYM, DEFINTION };
	
	
	// Relationship columns
	public static final int REL_IDX_ID = 0;
	public static final int REL_IDX_EFFECTIVETIME = 1;
	public static final int REL_IDX_ACTIVE = 2;
	public static final int REL_IDX_MODULEID = 3;
	public static final int REL_IDX_SOURCEID = 4;
	public static final int REL_IDX_DESTINATIONID = 5;
	public static final int REL_IDX_RELATIONSHIPGROUP = 6;
	public static final int REL_IDX_TYPEID = 7;
	public static final int REL_IDX_CHARACTERISTICTYPEID = 8;
	public static final int REL_IDX_MODIFIERID = 9;
	public static final int REL_MAX_COLUMN = 9;

	// Concept columns
	// id effectiveTime active moduleId definitionStatusId
	public static final int CON_IDX_ID = 0;
	public static final int CON_IDX_EFFECTIVETIME = 1;
	public static final int CON_IDX_ACTIVE = 2;
	public static final int CON_IDX_MODULID = 3;
	public static final int CON_IDX_DEFINITIONSTATUSID = 4;

	// Description columns
	// id effectiveTime active moduleId conceptId languageCode typeId term caseSignificanceId
	public static final int DES_IDX_ID = 0;
	public static final int DES_IDX_EFFECTIVETIME = 1;
	public static final int DES_IDX_ACTIVE = 2;
	public static final int DES_IDX_MODULID = 3;
	public static final int DES_IDX_CONCEPTID = 4;
	public static final int DES_IDX_LANGUAGECODE = 5;
	public static final int DES_IDX_TYPEID = 6;
	public static final int DES_IDX_TERM = 7;
	public static final int DES_IDX_CASESIGNIFICANCEID = 8;

}
