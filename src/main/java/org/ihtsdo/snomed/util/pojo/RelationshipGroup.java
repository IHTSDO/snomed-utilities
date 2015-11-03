package org.ihtsdo.snomed.util.pojo;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.ihtsdo.snomed.util.Type5UuidFactory;

import com.google.common.collect.Sets;

public class RelationshipGroup {

	private static Type5UuidFactory type5UuidFactory;
	private static String EMTPTY_SHAPE = "Empty Shape";
	private static final int IMMEDIATE_PARENT = 1;
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
	private GroupShape mostPopularShape;

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
			groupTypesUUID = getGroupAbstractShape(new TreeSet<Integer>()); // default - no type made more general
		} 
		return groupTypesUUID;
	}
	
	public String getGroupAbstractShape(Set<Integer> indexCombination) throws UnsupportedEncodingException {
		ArrayList<Relationship> attributeList = new ArrayList<Relationship>(attributes);
		String result;
		if (attributeList.size() > 0) {
			String typesConcatonated = "";
			for (int x = 0; x < attributeList.size(); x++) {
				Relationship r = attributeList.get(x);
				if (indexCombination.contains(x)) {
					typesConcatonated += r.getType().getAncestor(IMMEDIATE_PARENT).getSctId(); // just immediate parent
				} else {
					typesConcatonated += r.getTypeId();
				}
			}
			result = type5UuidFactory.get(typesConcatonated).toString();
		} else {
			result = EMTPTY_SHAPE;
		}
		return result;
	}

	public int size() {
		return attributes.size();
	}

	public String toString() {
		return "Group - " + number + " (size " + attributes.size() + ")";
	}

	public String getGroupPartialShape(Set<Integer> indexCombination) throws UnsupportedEncodingException {
		ArrayList<Relationship> attributeList = new ArrayList<Relationship>(attributes);
		String result;
		if (attributeList.size() > 0 && indexCombination.size() > 0) {
			String typesConcatonated = "";
			for (int x = 0; x < attributeList.size(); x++) {
				Relationship r = attributeList.get(x);
				if (indexCombination.contains(x)) {
					typesConcatonated += r.getTypeId();
				}
			}
			result = type5UuidFactory.get(typesConcatonated).toString();
		} else {
			result = EMTPTY_SHAPE;
		}
		return result;
	}

	public String getGroupPartialAbstractShape(Set<Integer> thisAttributeCombination, Set<Integer> thisAbstractCombination)
			throws UnsupportedEncodingException {
		ArrayList<Relationship> attributeList = new ArrayList<Relationship>(attributes);
		String result;
		if (attributeList.size() > 0 && thisAttributeCombination.size() > 0) {
			String typesConcatonated = "";
			for (int x = 0; x < attributeList.size(); x++) {
				Relationship r = attributeList.get(x);
				if (thisAttributeCombination.contains(x)) {
					if (thisAbstractCombination.contains(x)) {
						typesConcatonated += r.getType().getAncestor(IMMEDIATE_PARENT).getSctId();
					} else {
						typesConcatonated += r.getTypeId();
					}
				}
			}
			result = type5UuidFactory.get(typesConcatonated).toString();
		} else {
			result = EMTPTY_SHAPE;
		}
		return result;
	}

	public GroupShape getMostPopularShape() {
		return mostPopularShape;
	}

	public void setMostPopularShape(GroupShape mostPopularShape) {
		this.mostPopularShape = mostPopularShape;
	}

	public Set<Relationship> getAttributes() {
		return attributes;
	}

	public String prettyPrint() {
		String str = "     [ ";
		boolean isFirst = true;
		for (Relationship rel : attributes) {
			if (!isFirst) {
				str += ", ";
			} else {
				isFirst = false;
			}
			str += Description.getFormattedConcept(rel.getTypeId());
		}
		str += " ]";
		return str;
	}

}
