package org.ihtsdo.snomed.util.rf2.schema;

import java.util.ArrayList;
import java.util.List;

public class TableSchema {

	private String filenameNoExtension;
	private ComponentType componentType;
	private List<Field> fields;

	private static final String TXT_FILE_EXTENSION = ".txt";

	public TableSchema(ComponentType componentType, String filenameNoExtension) {
		this.componentType = componentType;
		this.filenameNoExtension = filenameNoExtension;

		fields = new ArrayList<>();
	}

	public TableSchema field(String name, DataType type) {
		fields.add(new Field(name, type));
		return this;
	}

	public TableSchema field(String name, DataType type, boolean mandatory) {
		fields.add(new Field(name, type, mandatory));
		return this;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public String getTableName() {
		// Table name will be the standard table name, without the beta release indicator
		String fileName = filenameNoExtension.startsWith("x") ? filenameNoExtension.substring(1) : filenameNoExtension;
		return fileName.replace("-", "");
	}

	public String getFilename() {
		return filenameNoExtension + TXT_FILE_EXTENSION;
	}

	public List<Field> getFields() {
		return fields;
	}

}
