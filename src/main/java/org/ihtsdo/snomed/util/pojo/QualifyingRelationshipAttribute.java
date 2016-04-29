package org.ihtsdo.snomed.util.pojo;

import java.util.ArrayList;
import java.util.List;

public class QualifyingRelationshipAttribute implements Comparable<QualifyingRelationshipAttribute>{
	
	public Concept getType() {
		return type;
	}

	public Concept getDestination() {
		return destination;
	}

	private Concept type;
	private Concept destination;
	private List<QualifyingRelationshipRule> rules;
	
	private transient int hash;
	
	private QualifyingRelationshipAttribute() {
		rules = new ArrayList<QualifyingRelationshipRule>();
	}

	public QualifyingRelationshipAttribute(Concept type, Concept destination) {
		this();
		this.type = type;
		this.destination = destination;
		String hashStr = type.getSctId().toString() + "_" + destination.getSctId().toString();
		hash = hashStr.hashCode();
	}
	
	public void addRule(QualifyingRelationshipRule rule) {
		this.rules.add(rule);
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof QualifyingRelationshipAttribute)) {
			return false;
		} else {
			QualifyingRelationshipAttribute thisTD = (QualifyingRelationshipAttribute)obj;
			if (this.type.equals(thisTD.type) && this.destination.equals(thisTD.destination)) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		return "[T: " + Description.getFormattedConcept(type.getSctId()) + 
				" D: " + Description.getFormattedConcept(destination.getSctId()) + "]";
	}

	public int hashCode() {
		return hash;
	}

	@Override
	public int compareTo(QualifyingRelationshipAttribute other) {
		return this.toString().compareTo(other.toString());
	}

}
