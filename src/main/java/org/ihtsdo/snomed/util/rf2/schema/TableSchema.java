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

	public ComponentType getComponentType() {
		return componentType;
	}

	public String getTableName() {
		return filenameNoExtension.replace("-", "");
	}

	public String getFilename() {
		return filenameNoExtension + TXT_FILE_EXTENSION;
	}

	public List<Field> getFields() {
		return fields;
	}

}
