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
	public void testCreateSchemaBeanSimpleRefset() throws Exception {
		String filename = "rel2_Refset_SimpleDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_Refset_SimpleDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(6, fields.size());
		assertFirstSixSimpleRefsetFields(fields);
	}

	@Test
	public void testCreateSchemaBeanAttributeValueRefset() throws Exception {
		String filename = "rel2_cRefset_AttributeValueDelta_INT_20140831.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\tvalueId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_cRefset_AttributeValueDelta_INT_20140831", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(7, fields.size());
		assertFirstSixSimpleRefsetFields(fields);

		// Assert additional fields
		Assert.assertEquals("valueId", fields.get(6).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(6).getType());
	}

	@Test
	public void testCreateSchemaBeanExtendedMapRefset() throws Exception {
		String filename = "rel2_iisssccRefset_ExtendedMapDelta_INT_20140131.txt";
		String headerLine = "id\teffectiveTime\tactive\tmoduleId\trefSetId\treferencedComponentId\t" +
				"mapGroup\tmapPriority\tmapRule\tmapAdvice\tmapTarget\tcorrelationId\tmapCategoryId";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);
		schemaFactory.populateExtendedRefsetAdditionalFieldNames(schemaBean, headerLine);

		Assert.assertEquals(ComponentType.REFSET, schemaBean.getComponentType());
		Assert.assertEquals("der2_iisssccRefset_ExtendedMapDelta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(13, fields.size());
		assertFirstSixSimpleRefsetFields(fields);

		// Assert additional fields
		Assert.assertEquals("mapGroup", fields.get(6).getName());
		Assert.assertEquals(DataType.INTEGER, fields.get(6).getType());

		Assert.assertEquals("mapPriority", fields.get(7).getName());
		Assert.assertEquals(DataType.INTEGER, fields.get(7).getType());

		Assert.assertEquals("mapRule", fields.get(8).getName());
		Assert.assertEquals(DataType.STRING, fields.get(8).getType());

		Assert.assertEquals("mapAdvice", fields.get(9).getName());
		Assert.assertEquals(DataType.STRING, fields.get(9).getType());

		Assert.assertEquals("mapTarget", fields.get(10).getName());
		Assert.assertEquals(DataType.STRING, fields.get(10).getType());

		Assert.assertEquals("correlationId", fields.get(11).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(11).getType());

		Assert.assertEquals("mapCategoryId", fields.get(12).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(12).getType());
	}

	@Test
	public void testCreateSchemaBeanConcept() throws Exception {
		String filename = "rel2_Concept_Delta_INT_20140131.txt";

		TableSchema schemaBean = schemaFactory.createSchemaBean(filename);

		Assert.assertEquals(ComponentType.CONCEPT, schemaBean.getComponentType());
		Assert.assertEquals("sct2_Concept_Delta_INT_20140131", schemaBean.getTableName());
		List<Field> fields = schemaBean.getFields();
		Assert.assertEquals(5, fields.size());

		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(0).getType());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());

		Assert.assertEquals("definitionStatusId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());
	}

	@Test(expected = FileRecognitionException.class)
	public void testCreateSchemaBeanBadNameRefset() throws Exception {
		String filename = "rel2_aRefset_SimpleDelta_INT_20140831.txt";

		schemaFactory.createSchemaBean(filename);
	}

	private void assertFirstSixSimpleRefsetFields(List<Field> fields) {
		Assert.assertEquals("id", fields.get(0).getName());
		Assert.assertEquals(DataType.UUID, fields.get(0).getType());

		Assert.assertEquals("effectiveTime", fields.get(1).getName());
		Assert.assertEquals(DataType.TIME, fields.get(1).getType());

		Assert.assertEquals("active", fields.get(2).getName());
		Assert.assertEquals(DataType.BOOLEAN, fields.get(2).getType());

		Assert.assertEquals("moduleId", fields.get(3).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(3).getType());

		Assert.assertEquals("refSetId", fields.get(4).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(4).getType());

		Assert.assertEquals("referencedComponentId", fields.get(5).getName());
		Assert.assertEquals(DataType.SCTID, fields.get(5).getType());
	}

}
