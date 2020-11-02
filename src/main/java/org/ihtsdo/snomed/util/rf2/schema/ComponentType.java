package org.ihtsdo.snomed.util.rf2.schema;

public enum ComponentType {

	CONCEPT("Concept"),
	DESCRIPTION("Description"),
	TEXT_DEFINITION("TextDefinition"),
	STATED_RELATIONSHIP("StatedRelationship"),
	RELATIONSHIP("Relationship"),
	RELATIONSHIP_CONCRETE_VALUES("RelationshipConcreteValues"),
	IDENTIFIER("Identifier"),
	REFSET("Refset");

	private String name;

	ComponentType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Lookup ComponentType enum from "ContentType" part of filename. Refset "ContentType" can include prepended characters.
	 * @param contentTypeString
	 * @return Equivalent ComponentType or null if contentTypeString not recognised.
	 */
	public static ComponentType lookup(String contentTypeString) {
		ComponentType type = null;
		if (contentTypeString.equals(ComponentType.CONCEPT.toString())) {
			type = ComponentType.CONCEPT;
		} else if (contentTypeString.equals(ComponentType.DESCRIPTION.toString())) {
			type = ComponentType.DESCRIPTION;
		} else if (contentTypeString.equals(ComponentType.TEXT_DEFINITION.toString())) {
			type = ComponentType.TEXT_DEFINITION;
		} else if (contentTypeString.equals(ComponentType.STATED_RELATIONSHIP.toString())) {
			type = ComponentType.STATED_RELATIONSHIP;
		} else if (contentTypeString.equals(ComponentType.RELATIONSHIP.toString())) {
			type = ComponentType.RELATIONSHIP;
		} else if (contentTypeString.equals(ComponentType.RELATIONSHIP_CONCRETE_VALUES.toString())) {
			type = ComponentType.RELATIONSHIP_CONCRETE_VALUES;
		} else if (contentTypeString.equals(ComponentType.IDENTIFIER.toString())) {
			type = ComponentType.IDENTIFIER;
		} else if (contentTypeString.endsWith(ComponentType.REFSET.toString())) {
			type = ComponentType.REFSET;
		}
		return type;
	}
}
