package org.ihtsdo.snomed.util.pojo;

import java.util.Set;

import org.ihtsdo.snomed.util.rf2.schema.SnomedExpressions;

public class QualifyingRelationshipRule implements SnomedExpressions {

	String startPoint;  //A SNOMED EXPRESSION DESCENDENT or DESCENDENT AND SELF
	Set<Concept> endPoints;
	Set<Concept> exceptions;
	
	public QualifyingRelationshipRule (Concept startPointConcept, CONSTRAINT constraint, Set<Concept> endPoints, Set<Concept> exceptions) {
		this.startPoint = constraint + startPointConcept.toString();
		this.endPoints = endPoints;
		this.exceptions = exceptions;
	}
	
}
