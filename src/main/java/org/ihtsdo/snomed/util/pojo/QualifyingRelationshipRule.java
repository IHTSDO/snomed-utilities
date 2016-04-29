package org.ihtsdo.snomed.util.pojo;

import java.util.Set;

import org.ihtsdo.snomed.util.rf2.schema.SnomedExpressions;

public class QualifyingRelationshipRule implements SnomedExpressions {

	String startPoint;  //A SNOMED EXPRESSION DESCENDENT or DESCENDENT AND SELF
	String endPoint;
	Set<Concept> exceptions;
	
	public QualifyingRelationshipRule (Concept startPointConcept, CONSTRAINT constraint, Set<Concept> exceptions) {
		this.startPoint = constraint + startPointConcept.toString();
		this.exceptions = exceptions;
	}
	
}
