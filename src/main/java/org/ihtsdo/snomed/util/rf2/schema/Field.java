package org.ihtsdo.snomed.util.rf2.schema;

public class Field {

	private String name;
	private DataType type;
	private boolean mandatory;

	Field(String name, DataType type) {
		this.name = name;
		this.type = type;
		this.mandatory = true;
	}

	Field(String name, DataType type, boolean mandatory) {
		this(name, type);
		this.mandatory = mandatory;
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

	public boolean isMandatory() {
		return mandatory;
	}

}
