package org.ihtsdo.snomed.util.pojo;

import java.util.Set;

public class GroupShape {

	String id;
	Set<Integer> partialMatch;
	Set<Integer> abstractMatch;
	int popularity;

	// For a match with a more abstract (ie parent) type, we may go more
	// than one ancestor up the hierarchy. But this hasn't been needed yet.
	// Set<Integer, int> generationMatch

	public GroupShape (String id, Set<Integer> partialMatch, 
			Set<Integer> abstractMatch, int popularity){
		this.id = id;
		this.partialMatch = partialMatch;
		this.abstractMatch = abstractMatch;
		this.popularity = popularity;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<Integer> getPartialMatch() {
		return partialMatch;
	}

	public void setPartialMatch(Set<Integer> partialMatch) {
		this.partialMatch = partialMatch;
	}

	public Set<Integer> getAbstractMatch() {
		return abstractMatch;
	}

	public void setAbstractMatch(Set<Integer> abstractMatch) {
		this.abstractMatch = abstractMatch;
	}

	public int getPopularity() {
		return popularity;
	}

	public void setPopularity(int popularity) {
		this.popularity = popularity;
	}

}
