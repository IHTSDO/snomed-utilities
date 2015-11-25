package org.ihtsdo.snomed.util.mrcm;

import java.util.HashSet;
import java.util.Set;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquivalencyChecker {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphLoader.class);

	public static void detectEquivalencies() {
		// For every concept (inferred for now?) if it is defined, look for all defined children.
		// If the children have no additional parents AND not additional attributes
		// then they are logically equivalent - report as an error

		// Expect this to report no instances, as classifier should already detect these.
		long childrenChecked = 0;
		for (Concept thisConcept : Concept.getAllConcepts(CHARACTERISTIC.INFERRED)) {
			if (thisConcept.isFullyDefined()) {
				for (Concept thisChild : thisConcept.getFullyDefinedDescendents(Concept.IMMEDIATE_CHILDREN_ONLY)) {
					checkForEquivalency(thisChild, thisConcept);
					if (++childrenChecked % 5000 == 0) {
						LOGGER.info("Checked {} children", childrenChecked);
					}
				}
			}
		}
	}

	private static void checkForEquivalency(Concept child, Concept parent) {
		// Firstly, does the child have any other parent? Cannot be equivalent if so.
		// Classifier would remove if other parent was shared with first parent
		if (child.getParents().size() == 1) {
			// Now the child needs to have more, or more specific attributes than the parent
			// work out what attributes it doesn't have in common, but removing all those that are
			Set<Relationship> unmatchedAttributes = new HashSet<Relationship>(child.getAllAttributes());
			for (Relationship thisParentAttribute : parent.getAllAttributes()) {
				Relationship matchedAttribute = findMatchingTypeDestination(thisParentAttribute, unmatchedAttributes);
				if (matchedAttribute != null) {
					unmatchedAttributes.remove(matchedAttribute);
				}
			}
			// Now if we have no unmatched attributes remaining, then there's a potential equivalency
			if (unmatchedAttributes.size() == 0) {
				LOGGER.warn("Possible equivalency of child {} to parent {}", child, parent);
			}
		}

	}

	private static Relationship findMatchingTypeDestination(Relationship matchMe, Set<Relationship> searchMe) {
		for (Relationship thisAttribute : searchMe) {
			if (thisAttribute.getTypeId() == matchMe.getTypeId() && thisAttribute.getDestinationId() == matchMe.getDestinationId()) {
				return thisAttribute;
			}
		}
		return null;
	}

}
