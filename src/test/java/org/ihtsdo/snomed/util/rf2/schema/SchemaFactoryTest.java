package org.ihtsdo.snomed.util.rf2.schema;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SchemaFactoryTest {

	private SchemaFactory schemaFactory;

	@Before
	public void setUp() {
		schemaFactory = new SchemaFactory();
	}
	
	@Test
	public void testCreateSchemaBeanForOWLOntologyRefset() throws Exception {
		String filename = "rel2_sRefset_OWLOntologyDelta_INT_20180731.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\towlExpression";
		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("sct2_sRefset_OWLOntologyDelta_INT_20180731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(7, fields.size());
		assertRelOwlRefsetFields(fields);
	}
	
	@Test
	public void testCreateSchemaBeanForOwlAxiomRefset() throws Exception {
		String filename = "rel2_sRefset_OWLAxiomDelta_INT_20180731.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\towlExpression";
		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("sct2_sRefset_OWLAxiomDelta_INT_20180731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(7, fields.size());
		assertRelOwlRefsetFields(fields);
	}
	

	@Test
	public void testCreateSchemaBeanRelSimpleRefset() throws Exception {
		String filename = "rel2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(6, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanRelSimpleBetaRefset() throws Exception {
		String filename = "xrel2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(6, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanDerSimpleRefset() throws Exception {
		String filename = "der2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(6, fields.size());
		assertFirstSixDerSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanRelAttributeValueRefset() throws Exception {
		String filename = "rel2_cRefset_AttributeValueDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tvalueId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_cRefset_AttributeValueDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(7, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);

		// Assert additional fields
		assertEquals("valueId", fields.get(6).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanDerAttributeValueRefset() throws Exception {
		String filename = "der2_cRefset_AttributeValueDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\tvalueId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_cRefset_AttributeValueDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(7, fields.size());
		assertFirstSixDerSimpleRefsetFields(fields);

		// Assert additional fields
		assertEquals("valueId", fields.get(6).getName());
		assertEquals(DataType.SCTID, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());
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

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_iisssccRefset_ExtendedMapDelta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(13, fields.size());
		assertFirstSixRelSimpleRefsetFields(fields);

		// Assert additional fields
		assertEquals("mapGroup", fields.get(6).getName());
		assertEquals(DataType.INTEGER, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());

		assertEquals("mapPriority", fields.get(7).getName());
		assertEquals(DataType.INTEGER, fields.get(7).getType());
		assertTrue(fields.get(7).isMandatory());

		assertEquals("mapRule", fields.get(8).getName());
		assertEquals(DataType.STRING, fields.get(8).getType());
		assertTrue(fields.get(8).isMandatory());

		assertEquals("mapAdvice", fields.get(9).getName());
		assertEquals(DataType.STRING, fields.get(9).getType());
		assertTrue(fields.get(9).isMandatory());

		assertEquals("mapTarget", fields.get(10).getName());
		assertEquals(DataType.STRING, fields.get(10).getType());
		assertTrue(fields.get(10).isMandatory());

		assertEquals("correlationId", fields.get(11).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(11).getType());
		assertTrue(fields.get(11).isMandatory());

		assertEquals("mapCategoryId", fields.get(12).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(12).getType());
		assertTrue(fields.get(12).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanDerExtendedMapRefset() throws Exception {
		String filename = "der2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefsetId\treferencedComponentId\t" +
				"mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		assertEquals("der2_iisssccRefset_ExtendedMapDelta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(13, fields.size());
		assertFirstSixDerSimpleRefsetFields(fields);

		// Assert additional fields
		assertEquals("mapGroup", fields.get(6).getName());
		assertEquals(DataType.INTEGER, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());

		assertEquals("mapPriority", fields.get(7).getName());
		assertEquals(DataType.INTEGER, fields.get(7).getType());
		assertTrue(fields.get(7).isMandatory());

		assertEquals("mapRule", fields.get(8).getName());
		assertEquals(DataType.STRING, fields.get(8).getType());
		assertTrue(fields.get(8).isMandatory());

		assertEquals("mapAdvice", fields.get(9).getName());
		assertEquals(DataType.STRING, fields.get(9).getType());
		assertTrue(fields.get(9).isMandatory());

		assertEquals("mapTarget", fields.get(10).getName());
		assertEquals(DataType.STRING, fields.get(10).getType());
		assertTrue(fields.get(10).isMandatory());

		assertEquals("correlationId", fields.get(11).getName());
		assertEquals(DataType.SCTID, fields.get(11).getType());
		assertTrue(fields.get(11).isMandatory());

		assertEquals("mapCategoryId", fields.get(12).getName());
		assertEquals(DataType.SCTID, fields.get(12).getType());
		assertTrue(fields.get(12).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanRelConcept() throws Exception {
		String filename = "rel2_Concept_Delta_INT_20140131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		assertEquals(ComponentType.CONCEPT, schemaBean.getComponentType());
		assertEquals("sct2_Concept_Delta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(5, fields.size());

		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertFalse(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("definitionStatusId", fields.get(4).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctConcept() throws Exception {
		String filename = "sct2_Concept_Delta_INT_20140131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		assertEquals(ComponentType.CONCEPT, schemaBean.getComponentType());
		assertEquals("sct2_Concept_Delta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(5, fields.size());

		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.SCTID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertTrue(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("definitionStatusId", fields.get(4).getName());
		assertEquals(DataType.SCTID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctDescriptionPre() throws Exception {
		String filename = "rel2_Description_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		assertEquals(ComponentType.DESCRIPTION, schemaBean.getComponentType());
		assertEquals("sct2_Description_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(9, fields.size());

		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertFalse(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("conceptId", fields.get(4).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("languageCode", fields.get(5).getName());
		assertEquals(DataType.STRING, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());

		assertEquals("typeId", fields.get(6).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());

		assertEquals("term", fields.get(7).getName());
		assertEquals(DataType.STRING, fields.get(7).getType());
		assertTrue(fields.get(7).isMandatory());

		assertEquals("caseSignificanceId", fields.get(8).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(8).getType());
		assertTrue(fields.get(8).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctDescriptionPost() throws Exception {
		String filename = "sct2_Description_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		assertEquals(ComponentType.DESCRIPTION, schemaBean.getComponentType());
		assertEquals("sct2_Description_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(9, fields.size());

		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.SCTID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertTrue(fields.get(1).isMandatory());
		assertTrue(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("conceptId", fields.get(4).getName());
		assertEquals(DataType.SCTID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("languageCode", fields.get(5).getName());
		assertEquals(DataType.STRING, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());

		assertEquals("typeId", fields.get(6).getName());
		assertEquals(DataType.SCTID, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());

		assertEquals("term", fields.get(7).getName());
		assertEquals(DataType.STRING, fields.get(7).getType());
		assertTrue(fields.get(7).isMandatory());

		assertEquals("caseSignificanceId", fields.get(8).getName());
		assertEquals(DataType.SCTID, fields.get(8).getType());
		assertTrue(fields.get(8).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctTextDefinitionPre() throws Exception {
		String filename = "rel2_TextDefinition_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		assertEquals(ComponentType.TEXT_DEFINITION, schemaBean.getComponentType());
		assertEquals("sct2_TextDefinition_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(9, fields.size());

		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertFalse(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("conceptId", fields.get(4).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("languageCode", fields.get(5).getName());
		assertEquals(DataType.STRING, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());

		assertEquals("typeId", fields.get(6).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());

		assertEquals("term", fields.get(7).getName());
		assertEquals(DataType.STRING, fields.get(7).getType());
		assertTrue(fields.get(7).isMandatory());

		assertEquals("caseSignificanceId", fields.get(8).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(8).getType());
		assertTrue(fields.get(8).isMandatory());
	}

	@Test
	public void testCreateSchemaBeanSctTextDefinitionPost() throws Exception {
		String filename = "sct2_TextDefinition_Delta-en_INT_20140731.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		assertEquals(ComponentType.TEXT_DEFINITION, schemaBean.getComponentType());
		assertEquals("sct2_TextDefinition_Deltaen_INT_20140731", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(9, fields.size());

		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.SCTID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertTrue(fields.get(1).isMandatory());
		assertTrue(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("conceptId", fields.get(4).getName());
		assertEquals(DataType.SCTID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("languageCode", fields.get(5).getName());
		assertEquals(DataType.STRING, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());

		assertEquals("typeId", fields.get(6).getName());
		assertEquals(DataType.SCTID, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());

		assertEquals("term", fields.get(7).getName());
		assertEquals(DataType.STRING, fields.get(7).getType());
		assertTrue(fields.get(7).isMandatory());

		assertEquals("caseSignificanceId", fields.get(8).getName());
		assertEquals(DataType.SCTID, fields.get(8).getType());
		assertTrue(fields.get(8).isMandatory());
	}

	@Test
	public void testCreateRelationshipConcreteValuesBeanPost() throws Exception {
		String filename = "sct2_RelationshipConcreteValues_Delta_INT_20210131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		assertEquals(ComponentType.RELATIONSHIP_CONCRETE_VALUES, schemaBean.getComponentType());
		assertEquals("sct2_RelationshipConcreteValues_Delta_INT_20210131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(10, fields.size());

		assertEquals("value", fields.get(5).getName());
		assertEquals(DataType.STRING, fields.get(5).getType());
	}


	@Test
	public void testCreateRelationshipBeanPost() throws Exception {
		String filename = "sct2_Relationship_Delta_INT_20210131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertNotNull("tableSchema should not be null", schemaBean);

		assertEquals(ComponentType.RELATIONSHIP, schemaBean.getComponentType());
		assertEquals("sct2_Relationship_Delta_INT_20210131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		assertEquals(10, fields.size());

		assertEquals("destinationId", fields.get(5).getName());
		assertEquals(DataType.SCTID, fields.get(5).getType());
	}

	@Test(expected = FileRecognitionException.class)
	public void testCreateSchemaBeanBadNameRefset() throws Exception {
		String filename = "rel2_aRefset_SimpleDelta_INT_20140831.txt";

		schemaFactory.createSchemaBean(filename);
	}
	
	
	private void assertRelOwlRefsetFields(List<Field> fields) {
		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.UUID, fields.get(0).getType());
		assertFalse(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertFalse(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("refsetId", fields.get(4).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("referencedComponentId", fields.get(5).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());
		
		assertEquals("owlExpression", fields.get(6).getName());
		assertEquals(DataType.STRING, fields.get(6).getType());
		assertTrue(fields.get(6).isMandatory());
	}
	

	private void assertFirstSixRelSimpleRefsetFields(List<Field> fields) {
		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.UUID, fields.get(0).getType());
		assertFalse(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertFalse(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("refsetId", fields.get(4).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("referencedComponentId", fields.get(5).getName());
		assertEquals(DataType.SCTID_OR_UUID, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());
	}

	private void assertFirstSixDerSimpleRefsetFields(List<Field> fields) {
		assertEquals("id", fields.get(0).getName());
		assertEquals(DataType.UUID, fields.get(0).getType());
		assertTrue(fields.get(0).isMandatory());

		assertEquals("effectiveTime", fields.get(1).getName());
		assertEquals(DataType.TIME, fields.get(1).getType());
		assertTrue(fields.get(1).isMandatory());

		assertEquals("active", fields.get(2).getName());
		assertEquals(DataType.BOOLEAN, fields.get(2).getType());
		assertTrue(fields.get(2).isMandatory());

		assertEquals("moduleId", fields.get(3).getName());
		assertEquals(DataType.SCTID, fields.get(3).getType());
		assertTrue(fields.get(3).isMandatory());

		assertEquals("refsetId", fields.get(4).getName());
		assertEquals(DataType.SCTID, fields.get(4).getType());
		assertTrue(fields.get(4).isMandatory());

		assertEquals("referencedComponentId", fields.get(5).getName());
		assertEquals(DataType.SCTID, fields.get(5).getType());
		assertTrue(fields.get(5).isMandatory());
	}

}
