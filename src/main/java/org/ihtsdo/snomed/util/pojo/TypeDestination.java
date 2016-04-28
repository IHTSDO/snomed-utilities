package org.ihtsdo.snomed.util.pojo;

public class TypeDestination implements Comparable<TypeDestination>{
	
	private Concept type;
	private Concept destination;
	private int hash;

	public TypeDestination(Concept type, Concept destination) {
		this.type = type;
		this.destination = destination;
		String hashStr = type.getSctId().toString() + "_" + destination.getSctId().toString();
		hash = hashStr.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof TypeDestination)) {
			return false;
		} else {
			TypeDestination thisTD = (TypeDestination)obj;
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
	public int compareTo(TypeDestination other) {
		return this.toString().compareTo(other.toString());
	}

}
