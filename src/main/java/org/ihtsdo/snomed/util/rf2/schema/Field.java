package org.ihtsdo.snomed.util.rf2.schema;

public class Field {

	private String name;
	private DataType type;

	Field(String name, DataType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataType getType() {
		return type;
	}

}
