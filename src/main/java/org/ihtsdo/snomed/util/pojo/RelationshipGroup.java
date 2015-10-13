package org.ihtsdo.snomed.util.pojo;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.TreeSet;

import org.ihtsdo.snomed.util.Type5UuidFactory;

public class RelationshipGroup {

	private static Type5UuidFactory type5UuidFactory;
	private static String EMTPTY_SHAPE = "Empty Shape";
	static {
		try {
			type5UuidFactory = new Type5UuidFactory();
		} catch (Exception e) {
			throw new RuntimeException("Unable to initialise UUID factory", e);
		}
	}

	public RelationshipGroup(int number) {
		this.number = number;
	}

	private Set<Relationship> attributes = new TreeSet<Relationship>();
	private String groupTypesUUID = null;
	private int number;

	public void addAttribute(Relationship r) {
		attributes.add(r);
	}

	public int getNumber() {
		return number;
	}

	// Returns a hash of the relationship group's types this will hopefully
	// uniquely identify the shape of a group
	public String getGroupShape() throws UnsupportedEncodingException {
		if (groupTypesUUID == null) {
			if (attributes.size() > 0) {
				String typesConcatonated = "";
				for (Relationship r : attributes) {
					typesConcatonated += r.getTypeId();
				}
				groupTypesUUID = type5UuidFactory.get(typesConcatonated).toString();
			} else {
				groupTypesUUID = EMTPTY_SHAPE;
			}
		}
		return groupTypesUUID;
	}

}
