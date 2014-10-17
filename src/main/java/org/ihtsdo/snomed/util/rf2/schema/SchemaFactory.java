package org.ihtsdo.snomed.util.rf2.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SchemaFactory {

	public static final String REL_2 = "rel2";
	public static final String DER_2 = "der2";
	public static final String SCT_2 = "sct2";
	public static final String FILE_NAME_SEPARATOR = "_";
	public static final String COLUMN_SEPARATOR = "\t";

	public static final int SIMPLE_REFSET_FIELD_COUNT = 6;
	public static final char REFSET_FILENAME_CONCEPT_FIELD = 'c';
	public static final char REFSET_FILENAME_INTEGER_FIELD = 'i';
	public static final char REFSET_FILENAME_STRING_FIELD = 's';

	private static final String NO_MATCH_PREFIX = "Input file not RF2, ";
	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaFactory.class);

	public TableSchema createSchemaBean(String filename) throws FileRecognitionException {
		TableSchema schema = null;

		// General File Naming Pattern
		// <FileType>_<ContentType>_<ContentSubType>_<Country|Namespace>_<VersionDate>.<Extension>
		// See http://www.snomed.org/tig?t=fng2_convention

		if (filename.endsWith(".txt")) {

			String filenameNoExtension = filename.substring(0, filename.indexOf("."));

			String[] nameParts = filenameNoExtension.split(FILE_NAME_SEPARATOR);
			if (nameParts.length == 5) {
				String fileType = nameParts[0];
				String contentTypeString = nameParts[1];
				ComponentType componentType = ComponentType.lookup(contentTypeString);

				boolean relFile = fileType.equals(REL_2);
				boolean effectiveTimeMandatory = !relFile;

				DataType sctidType;
				if (relFile) {
					sctidType = DataType.SCTID_OR_UUID;
					if (componentType == ComponentType.REFSET) {
						// Reset
						fileType = DER_2;
					} else {
						// Core component
						fileType = SCT_2;
					}
					filenameNoExtension = filenameNoExtension.replace(REL_2, fileType);
				} else {
					sctidType = DataType.SCTID;
				}

				if (fileType.equals(DER_2)) {
					if (contentTypeString.equals(ComponentType.REFSET.toString())) {
						// Simple Refset
						schema = createSimpleRefsetSchema(filenameNoExtension, relFile, sctidType);
					} else if (contentTypeString.endsWith(ComponentType.REFSET.toString())) {
						// Other Refset

						// Start with Simple Refset
						schema = createSimpleRefsetSchema(filenameNoExtension, relFile, sctidType);

						// Use the contentType prefix characters for datatypes of additional fields without field names at this point.
						char[] additionalFieldTypes = contentTypeString.replace(ComponentType.REFSET.toString(), "").toCharArray();

						for (char additionalFieldType : additionalFieldTypes) {
							DataType type;
							switch (additionalFieldType) {
								case REFSET_FILENAME_CONCEPT_FIELD:
									type = sctidType;
									break;
								case REFSET_FILENAME_INTEGER_FIELD:
									type = DataType.INTEGER;
									break;
								case REFSET_FILENAME_STRING_FIELD:
									type = DataType.STRING;
									break;
								default:
									throw new FileRecognitionException("Unexpected character '" + additionalFieldType + "' within content " +
											"type section of Refset filename.");
							}
							schema.field(null, type);
						}
					} else {
						LOGGER.info(NO_MATCH_PREFIX + "file type {} with Content type {} is not supported.", fileType, contentTypeString);
					}
				} else if (fileType.equals(SCT_2)) {
					if (componentType == ComponentType.CONCEPT) {
						schema = new TableSchema(componentType, filenameNoExtension)
								.field("id", sctidType)
								.field("effectiveTime", DataType.TIME, effectiveTimeMandatory)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", sctidType)
								.field("definitionStatusId", sctidType);

					} else if (componentType == ComponentType.DESCRIPTION || componentType == ComponentType.TEXT_DEFINITION) {
						schema = new TableSchema(componentType, filenameNoExtension)
								.field("id", sctidType)
								.field("effectiveTime", DataType.TIME, effectiveTimeMandatory)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", sctidType)
								.field("conceptId", sctidType)
								.field("languageCode", DataType.STRING)
								.field("typeId", sctidType)
								.field("term", DataType.STRING)
								.field("caseSignificanceId", sctidType);

					} else if (componentType == ComponentType.STATED_RELATIONSHIP || componentType == ComponentType.RELATIONSHIP) {
						schema = new TableSchema(componentType, filenameNoExtension)
								.field("id", sctidType)
								.field("effectiveTime", DataType.TIME, effectiveTimeMandatory)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", sctidType)
								.field("sourceId", sctidType)
								.field("destinationId", sctidType)
								.field("relationshipGroup", DataType.INTEGER)
								.field("typeId", sctidType)
								.field("characteristicTypeId", sctidType)
								.field("modifierId", sctidType);

					} else if (componentType == ComponentType.IDENTIFIER) {
						schema = new TableSchema(componentType, filenameNoExtension)
								.field("identifierSchemeId", sctidType)
								.field("alternateIdentifier", DataType.STRING)
								.field("effectiveTime", DataType.TIME, effectiveTimeMandatory)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", sctidType)
								.field("referencedComponentId", sctidType);

					} else {
						LOGGER.info(NO_MATCH_PREFIX + "file type {} with Content type {} is not supported.", fileType, contentTypeString);
					}
				} else {
					LOGGER.info(NO_MATCH_PREFIX + "file type {} is not supported.", fileType);
				}
			} else {
				LOGGER.info(NO_MATCH_PREFIX + "unexpected filename format. Filename contains {} underscores, expected 4. Filename: {}", (nameParts.length - 1), filename);
			}
		} else {
			LOGGER.info(NO_MATCH_PREFIX + "incorrect file extension: {}", filename);
		}
		return schema;
	}

	public void populateExtendedRefsetAdditionalFieldNames(TableSchema schema, String headerLine) {
		String[] fieldNames = headerLine.split(COLUMN_SEPARATOR);
		List<Field> fields = schema.getFields();
		for (int i = 0; i < fields.size(); i++) {
			Field field = fields.get(i);
			if (field.getName() == null) {
				field.setName(fieldNames[i]);
			}
		}
	}

	private TableSchema createSimpleRefsetSchema(String filenameNoExtension, boolean relFile, DataType sctidType) {
		return new TableSchema(ComponentType.REFSET, filenameNoExtension)
								.field("id", DataType.UUID, !relFile)
								.field("effectiveTime", DataType.TIME, !relFile)
								.field("active", DataType.BOOLEAN)
								.field("moduleId", sctidType)
								.field("refsetId", sctidType)
								.field("referencedComponentId", sctidType);
	}

}
