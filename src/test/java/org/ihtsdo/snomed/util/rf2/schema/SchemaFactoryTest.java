package org.ihtsdo.snomed.util.rf2.schema;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class SchemaFactoryTest {

	private SchemaFactory schemaFactory;

	@Before
	public void setUp() throws Exception {
		schemaFactory = new SchemaFactory();
	}

	@Test
	public void testCreateSchemaBeanRelSimpleRefset() throws Exception {
		String filename = "rel2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(6, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanRelSimpleBetaRefset() throws Exception {
		String filename = "xrel2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(6, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanDerSimpleRefset() throws Exception {
		String filename = "der2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(6, fields.size());
		assertFirstSixDerSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanRelAttributeValueRefset() throws Exception {
		String filename = "rel2_cRefset_AttributeValueDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tvalueId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_cRefset_AttributeValueDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(7, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);

		// Assert additional fields
		Assert.assertEquals("valueId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanDerAttributeValueRefset() throws Exception {
		String filename = "der2_cRefset_AttributeValueDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tvalueId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_cRefset_AttributeValueDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(7, fields.size());
		assertFirstSixDerSimpleRefsetFields(fields);

		// Assert additional fields
		Assert.assertEquals("valueId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanRelExtendedMapRefset() throws Exception {
		String filename = "rel2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt";
		// First time standard release
		testCreateSchemaBeanRelExtendedMapRefsetImpl(filename);

		// Again as a beta release
		testCreateSchemaBeanRelExtendedMapRefsetImpl("x" + filename);

	}

	private void testCreateSchemaBeanRelExtendedMapRefsetImpl(String filename) throws Exception {
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\t" +
				"mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_iisssccRefset_ExtendedMapDelta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(13, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);

		// Assert additional fields
		Assert.assertEquals("mapGroup", fields.get(6).getName());
		Assert.assertEquals(DataType.INTEGER, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());

		Assert.assertEquals("mapPriority", fields.get(7).getName());
		Assert.assertEquals(DataType.INTEGER, fields.get(7).getType());
		Assert.assertEquals(true, fields.get(7).isMandatory());

		Assert.assertEquals("mapRule", fields.get(8).getName());
		Assert.assertEquals(DataType.STRING, fields.get(8).getType());
		Assert.assertEquals(true, fields.get(8).isMandatory());

		Assert.assertEquals("mapAdvice", fields.get(9).getName());
		Assert.assertEquals(DataType.STRING, fields.get(9).getType());
		Assert.assertEquals(true, fields.get(9).isMandatory());

		Assert.assertEquals("mapTarget", fields.get(10).getName());
		Assert.assertEquals(DataType.STRING, fields.get(10).getType());
		Assert.assertEquals(true, fields.get(10).isMandatory());

		Assert.assertEquals("correlationId", fields.get(11).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(11).getType());
		Assert.assertEquals(true, fields.get(11).isMandatory());

		Assert.assertEquals("mapCategoryId", fields.get(12).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(12).getType());
		Assert.assertEquals(true, fields.get(12).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanDerExtendedMapRefset() throws Exception {
		String filename = "der2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\t" +
				"mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_iisssccRefset_ExtendedMapDelta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(13, fields.size());
		assertFirstSixDerSimpleRefsetFields(fields);

		// Assert additional fields
		Assert.assertEquals("mapGroup", fields.get(6).getName());
		Assert.assertEquals(DataType.INTEGER, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());

		Assert.assertEquals("mapPriority", fields.get(7).getName());
		Assert.assertEquals(DataType.INTEGER, fields.get(7).getType());
		Assert.assertEquals(true, fields.get(7).isMandatory());

		Assert.assertEquals("mapRule", fields.get(8).getName());
		Assert.assertEquals(DataType.STRING, fields.get(8).getType());
		Assert.assertEquals(true, fields.get(8).isMandatory());

		Assert.assertEquals("mapAdvice", fields.get(9).getName());
		Assert.assertEquals(DataType.STRING, fields.get(9).getType());
		Assert.assertEquals(true, fields.get(9).isMandatory());

		Assert.assertEquals("mapTarget", fields.get(10).getName());
		Assert.assertEquals(DataType.STRING, fields.get(10).getType());
		Assert.assertEquals(true, fields.get(10).isMandatory());

		Assert.assertEquals("correlationId", fields.get(11).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(11).getType());
		Assert.assertEquals(true, fields.get(11).isMandatory());

		Assert.assertEquals("mapCategoryId", fields.get(12).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(12).getType());
		Assert.assertEquals(true, fields.get(12).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanRelConcept() throws Exception {
		String filename = "rel2_Concept_Delta_INT_20140131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertEquals(ComponentType.CONCEPT, schemaBean.getComponentType());
		Assert.assertEquals("sct2_Concept_Delta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(5, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(false, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("definitionStatusId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctConcept() throws Exception {
		String filename = "sct2_Concept_Delta_INT_20140131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertEquals(ComponentType.CONCEPT, schemaBean.getComponentType());
		Assert.assertEquals("sct2_Concept_Delta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(5, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(true, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("definitionStatusId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctDescriptionPre() throws Exception {
		String filename = "rel2_Description_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		Assert.assertEquals(ComponentType.DESCRIPTION, schemaBean.getComponentType());
		Assert.assertEquals("sct2_Description_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(9, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(false, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("conceptId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());

		Assert.assertEquals("languageCode", fields.get(5).getName());
		Assert.assertEquals(DataType.STRING, fields.get(5).getType());
		Assert.assertEquals(true, fields.get(5).isMandatory());

		Assert.assertEquals("typeId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());

		Assert.assertEquals("term", fields.get(7).getName());
		Assert.assertEquals(DataType.STRING, fields.get(7).getType());
		Assert.assertEquals(true, fields.get(7).isMandatory());

		Assert.assertEquals("caseSignificanceId", fields.get(8).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(8).getType());
		Assert.assertEquals(true, fields.get(8).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctDescriptionPost() throws Exception {
		String filename = "sct2_Description_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		Assert.assertEquals(ComponentType.DESCRIPTION, schemaBean.getComponentType());
		Assert.assertEquals("sct2_Description_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(9, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(true, fields.get(1).isMandatory());
		Assert.assertEquals(true, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("conceptId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());

		Assert.assertEquals("languageCode", fields.get(5).getName());
		Assert.assertEquals(DataType.STRING, fields.get(5).getType());
		Assert.assertEquals(true, fields.get(5).isMandatory());

		Assert.assertEquals("typeId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());

		Assert.assertEquals("term", fields.get(7).getName());
		Assert.assertEquals(DataType.STRING, fields.get(7).getType());
		Assert.assertEquals(true, fields.get(7).isMandatory());

		Assert.assertEquals("caseSignificanceId", fields.get(8).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(8).getType());
		Assert.assertEquals(true, fields.get(8).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctTextDefinitionPre() throws Exception {
		String filename = "rel2_TextDefinition_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		Assert.assertEquals(ComponentType.TEXT_DEFINITION, schemaBean.getComponentType());
		Assert.assertEquals("sct2_TextDefinition_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(9, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(false, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("conceptId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());

		Assert.assertEquals("languageCode", fields.get(5).getName());
		Assert.assertEquals(DataType.STRING, fields.get(5).getType());
		Assert.assertEquals(true, fields.get(5).isMandatory());

		Assert.assertEquals("typeId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());

		Assert.assertEquals("term", fields.get(7).getName());
		Assert.assertEquals(DataType.STRING, fields.get(7).getType());
		Assert.assertEquals(true, fields.get(7).isMandatory());

		Assert.assertEquals("caseSignificanceId", fields.get(8).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(8).getType());
		Assert.assertEquals(true, fields.get(8).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctTextDefinitionPost() throws Exception {
		String filename = "sct2_TextDefinition_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		Assert.assertEquals(ComponentType.TEXT_DEFINITION, schemaBean.getComponentType());
		Assert.assertEquals("sct2_TextDefinition_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(9, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(true, fields.get(1).isMandatory());
		Assert.assertEquals(true, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("conceptId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());

		Assert.assertEquals("languageCode", fields.get(5).getName());
		Assert.assertEquals(DataType.STRING, fields.get(5).getType());
		Assert.assertEquals(true, fields.get(5).isMandatory());

		Assert.assertEquals("typeId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(6).getType());
		Assert.assertEquals(true, fields.get(6).isMandatory());

		Assert.assertEquals("term", fields.get(7).getName());
		Assert.assertEquals(DataType.STRING, fields.get(7).getType());
		Assert.assertEquals(true, fields.get(7).isMandatory());

		Assert.assertEquals("caseSignificanceId", fields.get(8).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(8).getType());
		Assert.assertEquals(true, fields.get(8).isMandatory());
	}

	@Test(expected = FileRecognitionException.class)
	public void testCreateSchemaBeanBadNameRefset() throws Exception {
		String filename = "rel2_aRefset_SimpleDelta_INT_20140831.txt";

		schemaFactory.createSchemaBean(filename);
	}

	private void assertFirstSixRelSimpleRefsetFields(List<Field> fields) {
		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.UUID, fields.get(0).getType());
		Assert.assertEquals(false, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(false, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("refsetId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());

		Assert.assertEquals("referencedComponentId", fields.get(5).getName());
		Assert.assertEquals(DataType.SCTID_OR_UUID, fields.get(5).getType());
		Assert.assertEquals(true, fields.get(5).isMandatory());
	}

	private void assertFirstSixDerSimpleRefsetFields(List<Field> fields) {
		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.UUID, fields.get(0).getType());
		Assert.assertEquals(true, fields.get(0).isMandatory());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());
		Assert.assertEquals(true, fields.get(1).isMandatory());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		Assert.assertEquals(true, fields.get(2).isMandatory());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());
		Assert.assertEquals(true, fields.get(3).isMandatory());

		Assert.assertEquals("refsetId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());
		Assert.assertEquals(true, fields.get(4).isMandatory());

		Assert.assertEquals("referencedComponentId", fields.get(5).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(5).getType());
		Assert.assertEquals(true, fields.get(5).isMandatory());
	}

}
