package org.ihtsdo.snomed.util.mrcm;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.ConceptSerializer;
import org.ihtsdo.snomed.util.pojo.Description;
import org.ihtsdo.snomed.util.pojo.GroupShape;
import org.ihtsdo.snomed.util.pojo.GroupsHash;
import org.ihtsdo.snomed.util.pojo.QualifyingRelationshipRule;
import org.ihtsdo.snomed.util.pojo.RF1Relationship;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.pojo.RelationshipGroup;
import org.ihtsdo.snomed.util.pojo.QualifyingRelationshipAttribute;
import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.ihtsdo.snomed.util.rf2.schema.SnomedExpressions.CONSTRAINT;
import org.ihtsdo.util.SnomedCollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MrcmBuilder {

	private static String INDENT_0 = "";
	private static String INDENT_1 = "\t";
	private static int MAX_MATCH_RELAXATION = 3;
	private static int MAX_EXCEPTIONS = 30;
	
	long qualifierIgnore = 182353008;  //Ignore Side ie laterality as we'll pull that from Yong's spreadsheet.

	private static enum CROSSOVER_STATUS {
		NOT_CROSSOVER, TYPE_CROSSOVER, DESTINATION_CROSSOVER
	};

	public static Long ROOT_SNOMED_CONCEPT_ID = 138875005L;

	private final Logger logger = LoggerFactory.getLogger(GraphLoader.class);

	public void determineMRCM(Concept c, int depth) throws UnsupportedEncodingException {
		Set<Concept> siblings = c.getDescendents(depth, false);
		Set<Concept> definedSiblings = c.getDescendents(depth, true);
		logger.info("Examining {} fully defined out of {} children of {}", definedSiblings.size(), siblings.size(),
				Description.getFormattedConcept(c.getSctId()));

		examineBasicGroupShape(definedSiblings);

		// examineAbstractShape(definedSiblings);

		// examinePartialGroupMatch(definedSiblings);

		// examinePartialGroupAbstractShapes(definedSiblings);
	}

	private void examineBasicGroupShape(Set<Concept> definedSiblings) throws UnsupportedEncodingException {

		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				// Is this a really empty group because of non-contiguous group numbers?
				GroupShape groupShape = g.getGroupBasicShape();
				// logger.info("{}:{} - {} ", sibling.getSctId(), g.getNumber(), groupShape);
				groupShape.incrementPopularity(sibling);
			}
			GroupsHash groupsHash = sibling.getGroupsShapeHash();
			// logger.info("  Groups Shape Hash: {}", groupsHash);
			groupsHash.incrementPopularity(sibling);
		}

		// Shape is misleading because really you've got to consider all the groups at the same
		// time, so we're just going to focus on Hash
		// logger.info("*****************\nShape Popularity:");
		// GroupShape.print();

		logger.info("*****************\nGroups Hash Popularity:");
		GroupsHash.print();

	}

	private void examineAbstractShape(Set<Concept> definedSiblings) throws UnsupportedEncodingException {
		// Now loop through groups again and see if we can find a better shape by working
		// with the abstract types (ie the parent of the type)
		// Calculate a new popularity Map
		GroupShape.resetPopularities();

		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				String preferredShapeId = g.getGroupBasicShape().getId();
				Set<Integer> preferredAbstractCombination = null;
				// Loop through all the combinations of attributes to express as type ancestors
				Set<Set<Integer>> allCombinations = SnomedCollectionUtils.getIndexCombinations(g.size());
				for (Set<Integer> thisCombination : allCombinations) {
					// Skip the empty set ie no abstractions
					if (thisCombination.size() == 0) {
						continue;
					}
					String groupAbstractShape = g.getGroupAbstractShape(thisCombination);
					// If ANY sibling already uses the more abstract model, then that's preferable to have in common
					if (GroupShape.isKnown(groupAbstractShape)) {
						logger.info("Abstract Group Shape better for {}: {} (was {})", sibling.getSctId(), groupAbstractShape,
								g.getGroupBasicShape());
						preferredShapeId = groupAbstractShape;
						preferredAbstractCombination = thisCombination;
					}
				}
				// Also put the original shape (which might remain zero popular) so we can compare it later
				GroupShape.registerShape(g.getGroupBasicShape().getId());
				GroupShape preferredShape = GroupShape.get(preferredShapeId);
				preferredShape.incrementPopularity();
				preferredShape.setAbstractMatch(preferredAbstractCombination);
				g.setMostPopularShape(preferredShape);
			}
		}
		
		logger.info("Shape Popularity after considering Abstract Shape:");
		GroupShape.print();

	}

	private void examinePartialGroupMatch(Set<Concept> definedSiblings) throws UnsupportedEncodingException {
		// Now loop through groups again and see if we can find a more popular shape by working
		// with partial group matches
		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				// Loop through all the combinations of attributes
				Set<Set<Integer>> allCombinations = SnomedCollectionUtils.getIndexCombinations(g.size());
				for (Set<Integer> thisCombination : allCombinations) {
					// Skip the empty set ie no attributes in group
					if (thisCombination.size() == 0) {
						continue;
					}
					String groupPartialShape = g.getGroupPartialShape(thisCombination);
					// If ANY sibling already uses the more abstract model, then that's preferable to have in common
					if (GroupShape.isKnown(groupPartialShape)
							&& GroupShape.get(groupPartialShape).getPopularity() > g.getGroupBasicShape().getPopularity()) {
						logger.info("Partial Group Shape more popular for {}: {} (was {})", sibling.getSctId(), groupPartialShape,
								g.getGroupBasicShape());
					}
				}
			}
		}

	}

	private void examinePartialGroupAbstractShapes(Set<Concept> definedSiblings)
			throws UnsupportedEncodingException {
		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				// Loop through all the combinations of attributes
				Set<Set<Integer>> allAttributeCombinations = SnomedCollectionUtils.getIndexCombinations(g.size());
				for (Set<Integer> thisAttributeCombination : allAttributeCombinations) {
					// Skip the empty set ie no attributes in group
					if (thisAttributeCombination.size() == 0) {
						continue;
					}

					// And also work through all combinations of using the more abstract relationship type
					Set<Set<Integer>> allAbstractCombinations = Sets.powerSet(thisAttributeCombination);
					for (Set<Integer> thisAbstractCombination : allAbstractCombinations) {
						// Skip the empty set ie no attributes replaced with more abstract types
						if (thisAbstractCombination.size() == 0) {
							continue;
						}
						String groupPartialAbstractShape = g
								.getGroupPartialAbstractShape(thisAttributeCombination, thisAbstractCombination);
						// If ANY sibling already uses the more abstract model, then that's preferable to have in common
						if (GroupShape.isKnown(groupPartialAbstractShape)
								&& GroupShape.get(groupPartialAbstractShape).getPopularity() > g.getGroupBasicShape().getPopularity()) {
							logger.info("Partial Group Abstract Shape more popular for {}: {} (was {})", sibling.getSctId(),
									groupPartialAbstractShape, g.getGroupBasicShape());
						}
					}
				}
			}
		}
	}

	public void determineMRCM(String sctid, CHARACTERISTIC hierarchyToExamine, int depth)
			throws UnsupportedEncodingException {
		Long conceptToExamine = null;
		try {
			conceptToExamine = new Long(sctid);
		} catch (NumberFormatException e) {
			print("Unable to parse SCTID  from '" + sctid + "' due to " + e.getMessage(), "");
			return;
		}
		Concept c = Concept.getConcept(conceptToExamine, hierarchyToExamine);
		determineMRCM(c, depth);
	}

	public void displayShape(String sctid, CHARACTERISTIC hierarchyToExamine) {
		Long conceptToExamine = new Long(sctid);
		Concept c = Concept.getConcept(conceptToExamine, hierarchyToExamine);
		prettyPrint(c, INDENT_0);
		for (Concept child : c.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY, true)) {
			prettyPrint(child, INDENT_1);
		}

	}

	private void prettyPrint(Concept c, String indent) {
		print("", indent);
		print(Description.getFormattedConcept(c.getSctId()), indent);
		print("-----------------------------------------------------", indent);

		for (Concept parent : c.getParents()) {
			print("  IS A " + Description.getFormattedConcept(parent.getSctId()), indent);
		}
		for (RelationshipGroup group : c.getGroups()) {
			if (group.getAttributes().size() > 0) {
				print(group.prettyPrint(), indent);
			}
		}
	}

	private void print(String msg, String indent) {
		System.out.println(indent + msg);
	}

	private void printn(String msg, String indent) {
		System.out.print(indent + msg);
	}

	public void determineValueRange(String attributeSCTID, String hierarchySCTID, CHARACTERISTIC hierarchyToExamine, boolean verbose) {
		Set<Concept> allDestinations = new HashSet<Concept>();
		Concept hierarchyStart = Concept.getConcept(new Long(hierarchySCTID), hierarchyToExamine);
		Concept targetRelationshipType = Concept.getConcept(new Long(attributeSCTID), hierarchyToExamine);
		populateAllDestinations(allDestinations, hierarchyStart, targetRelationshipType);
		if (verbose) {
			logger.info("Collected {} possible values for attribute {} in hierarchy {}", allDestinations.size(),
					Description.getFormattedConcept(targetRelationshipType.getSctId()),
					Description.getFormattedConcept(hierarchyStart.getSctId()));

			for (Concept thisDestination : allDestinations) {
				logger.info("\t{}", Description.getFormattedConcept(thisDestination.getSctId()));
			}
		}

		Concept commonAncestor = findCommonAncestor(allDestinations);
		if (commonAncestor != null) {
			logger.info("{} values LCA: {}", Description.getFormattedConcept(targetRelationshipType.getSctId()),
					Description.getFormattedConcept(commonAncestor.getSctId()));
		} else {
			logger.warn("Unable to find lca for values of {}", Description.getFormattedConcept(targetRelationshipType.getSctId()));
		}
	}

	private Concept findCommonAncestor(Set<Concept> concepts) {
		// We must have at least two concepts to search for common ancestor
		if (concepts.size() < 2) {
			return null;
		}
		Set<Concept> allCommonAncestors = null; // We'll remove not-common ancestors from first result set
		for (Concept thisConcept : concepts) {
			if (allCommonAncestors == null) {
				allCommonAncestors = thisConcept.getAncestorsAndSelf(Concept.DEPTH_NOT_SET);
			} else {
				allCommonAncestors.retainAll(thisConcept.getAncestorsAndSelf(Concept.DEPTH_NOT_SET));
			}
			if (allCommonAncestors.size() == 0) {
				return null;
			}
		}

		// Now find the common ancestor that has the greatest depth
		Concept deepestAncestor = null;
		for (Concept thisCommonAncestor : allCommonAncestors) {
			if (deepestAncestor == null || thisCommonAncestor.getDepth() > deepestAncestor.getDepth()) {
				deepestAncestor = thisCommonAncestor;
			}
		}
		return deepestAncestor;
	}

	private void populateAllDestinations(Set<Concept> allDestinations, Concept parent, Concept targetRelationshipType) {
		// Look through this concept to see if it has any attributes with the target attribute type
		// and populate the destination in the supplied set if so. Recurse through all children
		for (Relationship thisRelationship : parent.getAllAttributes()) {
			if (thisRelationship.getTypeId().equals(targetRelationshipType.getSctId())) {
				allDestinations.add(thisRelationship.getDestinationConcept());
			}
		}

		for (Concept thisChild : parent.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY, false)) {
			populateAllDestinations(allDestinations, thisChild, targetRelationshipType);
		}
	}

	public void determineAllLCAs(String hierarchySCTID, CHARACTERISTIC hierarchyToExamine) {
		// Collect all relationship types for this concept down
		Concept hierarchyStart = Concept.getConcept(new Long(hierarchySCTID), hierarchyToExamine);
		Set<Concept> allAttributeTypes = new TreeSet<Concept>();
		populateAllAttributeTypes(hierarchyStart, allAttributeTypes);

		// Now work through these attributes and report just the LCA for the attribute range
		for (Concept thisAttributeType : allAttributeTypes) {
			determineValueRange(thisAttributeType.getSctId().toString(), hierarchySCTID, hierarchyToExamine, false);
		}
	}

	private void populateAllAttributeTypes(Concept parent, Set<Concept> allAttributeTypes) {
		for (Relationship thisRelationship : parent.getAllAttributes()) {
			allAttributeTypes.add(thisRelationship.getTypeConcept());
		}

		for (Concept thisChild : parent.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY, false)) {
			populateAllAttributeTypes(thisChild, allAttributeTypes);
		}
	}

	public void findCrossHierarchyParents(CHARACTERISTIC hierarchyToExamine) {
		// Work through all known concepts.
		int count = 0;
		Stopwatch stopwatch = Stopwatch.createStarted();
		for (Concept thisConcept : Concept.getAllConcepts(hierarchyToExamine)) {
			Concept lcaParent = findCommonAncestor(thisConcept.getParents());
			if (lcaParent != null && lcaParent.getSctId().equals(ROOT_SNOMED_CONCEPT_ID)) {
				print(Description.getFormattedConcept(thisConcept.getSctId()), "");
				for (Concept p : thisConcept.getParents()) {
					print(Description.getFormattedConcept(p.getSctId()), "\t");
				}
			}
			if (++count % 50000 == 0) {
				print("Checked " + count, "");
			}
		}
		print("Completed checking " + count + " concepts in " + stopwatch, "");
	}

	public void findCrossovers(CHARACTERISTIC hierarchyToExamine, boolean causedByStatedOnly, boolean newIssuesOnly) {
		// Work through all known concepts.
		print("Looking for " + (causedByStatedOnly ? " only stated relationship causes" : " all causes."), "");
		int count = 0;
		Stopwatch stopwatch = Stopwatch.createStarted();
		int typeCrossoversDetected = 0;
		int destinationCrossoversDetected = 0;
		for (Concept thisConcept : Concept.getAllConcepts(hierarchyToExamine)) {
			CROSSOVER_STATUS crossoverStatus = hasCrossover(thisConcept, causedByStatedOnly, newIssuesOnly);
			if (!crossoverStatus.equals(CROSSOVER_STATUS.NOT_CROSSOVER)) {
				if (crossoverStatus.equals(CROSSOVER_STATUS.TYPE_CROSSOVER)) {
					typeCrossoversDetected++;
				} else {
					destinationCrossoversDetected++;
				}
			}
			++count;
		}
		print("Completed checking " + count + " concepts in " + stopwatch, "");
		print("Found " + typeCrossoversDetected + " crossovers in attribute type.", "");
		print("Found " + destinationCrossoversDetected + " crossovers in attribute destination,", "");
		print("Total: " + (typeCrossoversDetected + destinationCrossoversDetected), "");

	}

	private CROSSOVER_STATUS hasCrossover(Concept thisConcept, boolean causedByStatedOnly, boolean newIssuesOnly) {
		// We can easily match the ungrouped attributes first.
		for (Concept thisParent : thisConcept.getParents()) {
			CROSSOVER_STATUS status = checkAttributesForCrossovers(thisConcept, 0, new ParentGroup(thisParent, 0), causedByStatedOnly,
					newIssuesOnly);
			if (!status.equals(CROSSOVER_STATUS.NOT_CROSSOVER)) {
				return status;
			}
		}

		// Now loop through all our groups and work out which parent that group came from
		for (RelationshipGroup thisGroup : thisConcept.getGroups()) {
			if (thisGroup.getNumber() != 0) {
				ParentGroup parentGroup = findBestFitOrigin(thisConcept, thisGroup, 0);
				CROSSOVER_STATUS status = checkAttributesForCrossovers(thisConcept, thisGroup.getNumber(), parentGroup, causedByStatedOnly,
						newIssuesOnly);
				if (!status.equals(CROSSOVER_STATUS.NOT_CROSSOVER)) {
					return status;
				}
			}
		}
		return CROSSOVER_STATUS.NOT_CROSSOVER;
	}

	/**
	 * @return true if at least one of the relationships in the group also exists in a stated group
	 */
	private boolean groupHasStatedCounterpart(Concept thisConcept, RelationshipGroup thisGroup) {
		Concept statedConcept = Concept.getConcept(thisConcept.getSctId(), CHARACTERISTIC.STATED);
		for (Relationship inferredRelationship : thisGroup.getAttributes()) {
			for (RelationshipGroup statedRelationshipGroup : statedConcept.getGroups()) {
				if (statedRelationshipGroup.getNumber() == 0) {
					continue;
				}
				for (Relationship statedRelationship : statedRelationshipGroup.getAttributes()) {
					if (inferredRelationship.getTypeConcept().equals(statedRelationship.getTypeConcept())
							&& inferredRelationship.getDestinationConcept().equals(statedRelationship.getDestinationConcept())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean attributeHasStatedCounterpart(Concept thisConcept, Relationship thisRelationship) {
		// Basically we're looking to match a relationship group of 1
		RelationshipGroup tmp = new RelationshipGroup(0);
		tmp.addAttribute(thisRelationship);
		return groupHasStatedCounterpart(thisConcept, tmp);
	}

	/**
	 * Find which parent and which parent group number a child's relationship group has most likely come from. This may be a partial match
	 * (fewer attributes in the parent), and it may also match more or less specific relationship types and destinations. Recurse call
	 * getting more relaxed each time.
	 * 
	 * @param thisConcept
	 * @param thisGroup
	 * @return
	 */
	private ParentGroup findBestFitOrigin(Concept childConcept, RelationshipGroup childGroup, int relaxationLevel) {
		ParentGroup result = null;
		Map<ParentGroup, Integer> matches = new HashMap<ParentGroup, Integer>();
		// Work through all combinations of the child's group attributes
		Set<Set<Relationship>> attributeCombinations = Sets.powerSet(childGroup.getAttributes());
		// Work through all parents, and all groups of that parent
		for (Concept thisParent : childConcept.getParents()) {
			for (RelationshipGroup thisParentGroup : thisParent.getGroups()) {
				// Now work through all combinations of the child groups attributes, matching with the required level
				// of relaxation. Ignore the empty set.
				// Skip the ungrouped attributes
				if (thisParentGroup.getNumber() != 0) {
					for (Set<Relationship> thisCombination : attributeCombinations) {
						if (matchGroups(thisCombination, thisParentGroup, relaxationLevel)) {
							matches.put(new ParentGroup(thisParent, thisParentGroup.getNumber()), thisCombination.size());
						}
					}
				}
			}
		}
		// Now did we find any matches and if so, who matched the most attributes?
		if (matches.size() == 0 && relaxationLevel < MAX_MATCH_RELAXATION) {
			return findBestFitOrigin(childConcept, childGroup, relaxationLevel + 1);
		} else {
			Integer greatestMatches = 0;
			for (Map.Entry<ParentGroup, Integer> entry : matches.entrySet()) {
				if (entry.getValue().compareTo(greatestMatches) > 0) {
					result = entry.getKey();
				}
			}
		}
		// TODO Need to have a warning if we equally match from two parents or two groups
		return result;
	}

	/**
	 * Match all attributes in both groups, but widen up "thisCombination" type and destination ie both descendents and ancestors to the
	 * level of relaxationLevel
	 */
	private boolean matchGroups(Set<Relationship> thisCombination, RelationshipGroup thisParentGroup, int relaxationLevel) {
		for (Relationship thisAttribute : thisCombination) {
			// Now cast our net to the required width for matching on type and destination
			Set<Concept> matchType = widenNet(thisAttribute.getTypeConcept(), relaxationLevel);
			Set<Concept> matchDestination = widenNet(thisAttribute.getDestinationConcept(), relaxationLevel);
			boolean thisMatch = false;
			for (Relationship parentAttribute : thisParentGroup.getAttributes()) {
				if (matchType.contains(parentAttribute.getTypeConcept()) && matchDestination.contains(parentAttribute.getDestinationConcept())) {
					thisMatch = true;
					break;
				}
			}
			// If one fails, they all failed
			if (!thisMatch) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return a set containing both the self, parents and children of a concept to a depth given by the relaxationLevel
	 */
	private Set<Concept> widenNet(Concept concept, int netWidth) {
		Set<Concept> net = new HashSet<Concept>();
		net.add(concept);
		net.addAll(concept.getAncestors(netWidth));
		net.addAll(concept.getDescendents(netWidth, false));
		return net;
	}

	private CROSSOVER_STATUS checkAttributesForCrossovers(Concept thisConcept, int groupId, ParentGroup parentGroup,
			boolean causedByStatedOnly, boolean newIssuesOnly) {
		CROSSOVER_STATUS status = CROSSOVER_STATUS.NOT_CROSSOVER;
		if (parentGroup == null) {
			return status;
		}
		Concept thisParent = parentGroup.parent;
		int parentGroupId = parentGroup.groupId;
		foundone:
		for (Relationship thisAttribute : thisConcept.getGroup(groupId)) {
			// Are we only looking for new issues?
			if (newIssuesOnly && !thisAttribute.isChangedThisRelease()) {
				continue;
			}

			// if ANY attribute is less specific than the equivalent in the parent, then
			// declare a crossover
			for (Relationship thisParentsAttribute : thisParent.getGroup(parentGroupId)) {
				boolean blameElsewhere = false;
				String errorReport = "";
				// If this attribute type is an ancestor of the parent type, then we have a crossover
				if (thisParentsAttribute.getTypeConcept().equals(thisAttribute.getTypeConcept())) {
					// but if it's the same, then check if the destination is an ancestor.
					if (thisParentsAttribute.getDestinationConcept().getAncestors(Concept.DEPTH_NOT_SET)
							.contains(thisAttribute.getDestinationConcept())) {
						status = CROSSOVER_STATUS.DESTINATION_CROSSOVER;
						errorReport += (Description.getFormattedConcept(thisConcept.getSctId()) + " group " + groupId + " - " + status);
						errorReport += ("\n\tParent: " + Description.getFormattedConcept(thisParent.getSctId()) + " group " + parentGroupId);
						errorReport += ("\n\tAttribute destination " + Description.getFormattedConcept((thisAttribute
								.getDestinationConcept()
								.getSctId())));
						errorReport += ("\n\tMore specific parent's destination " + Description.getFormattedConcept(thisParentsAttribute
								.getDestinationConcept().getSctId()));
					}
				} else if (thisParentsAttribute.getTypeConcept().getAncestors(Concept.DEPTH_NOT_SET).contains(thisAttribute.getTypeConcept())) {
					// OR is it the case that this type is actually coming from one of the other parents?

					for (Concept parent : thisConcept.getParents()) {
						for (Relationship parentAttribute : parent.getGroup(parentGroupId)) {
							if (parentAttribute.getTypeConcept().equals(thisAttribute.getTypeConcept())) {
								blameElsewhere = true;
							}
						}
					}

					if (!blameElsewhere) {
						status = CROSSOVER_STATUS.TYPE_CROSSOVER;
						errorReport += (Description.getFormattedConcept(thisConcept.getSctId()) + " group " + groupId + " - " + status);
						errorReport += ("\n\tParent: " + Description.getFormattedConcept(thisParent.getSctId()) + " group " + parentGroupId);
						errorReport += ("\n\tAttribute type " + Description.getFormattedConcept((thisAttribute.getTypeConcept().getSctId())));
						errorReport += ("\n\tMore specific parent's type " + Description.getFormattedConcept(thisParentsAttribute.getTypeConcept()
								.getSctId()));
					}
				}

				if (!errorReport.isEmpty()) {

					// Are we only checking for crossovers that have been caused by the stated relationships?
					if (causedByStatedOnly) {
						if (!attributeHasStatedCounterpart(thisConcept, thisAttribute)) {
							blameElsewhere = true;
						}
					}

					if (!blameElsewhere) {
						print(errorReport + "\n", "");
						break foundone;
					} else {
						status = CROSSOVER_STATUS.NOT_CROSSOVER;
					}
				}
			}
		}
		return status;
	}

	public void calculateAverageDepth(CHARACTERISTIC hierarchyToExamine) {
		Collection<Concept> allConcepts = Concept.getAllConcepts(hierarchyToExamine);
		double[] depths = new double[allConcepts.size()];
		int idx = 0;
		for (Concept thisConcept : allConcepts) {
			depths[idx++] = thisConcept.getDepth();
		}
		print("Items: " + depths.length, "");
		print("Max: " + StatUtils.max(depths), "");
		print("Mean: " + (double) Math.round(new Mean().evaluate(depths) * 100) / 100.00, "");
		print("Median: " + new Median().evaluate(depths), "");
		print("Mode: " + StatUtils.mode(depths)[0], "");
		print("Variance: " + (double) Math.round(StatUtils.variance(depths) * 100) / 100.00, "");
	}

	// Grouper class identifying both a parent and a group id
	private class ParentGroup {
		Concept parent;
		int groupId;

		ParentGroup(Concept parent, int groupId) {
			this.parent = parent;
			this.groupId = groupId;
		}
	}

	public void getHierarchyStats(CHARACTERISTIC hierarchyToExamine) {
		Concept rootConcept = Concept.getConcept(ROOT_SNOMED_CONCEPT_ID, hierarchyToExamine);
		for (Concept thisTopLevelHierarchy : rootConcept.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY, false)) {
			print(Description.getFormattedConcept(thisTopLevelHierarchy.getSctId()), "");
			print("Percentage Fully Defined: " + getDefinedStats(thisTopLevelHierarchy), "\t");
		}
	}

	private String getDefinedStats(Concept c) {
		// Get all the children, how many of them are defined?
		Set<Concept> allDescendents = c.getDescendents(Concept.DEPTH_NOT_SET, false);
		int definedCount = 0;
		for (Concept thisDescendent : allDescendents) {
			if (thisDescendent.isFullyDefined()) {
				definedCount++;
			}
		}
		String percentage = new DecimalFormat("#0.0%").format((float) definedCount / allDescendents.size());
		return definedCount + " / " + allDescendents.size() + " = " + percentage;
	}

	public void linguisticSearch(CHARACTERISTIC currentView, SearchParameters searchParams) {
		// Start with all concepts in the hierarchy and filter out unwanted by description and attribute
		Set<Concept> matches = searchParams.hierarchy.getDescendents(Concept.DEPTH_NOT_SET, searchParams.fullyDefinedOnly);

		matches = filterOutDescriptions(matches, searchParams.description, searchParams.descriptionPresent);
		matches = filterOutAttributes(matches, searchParams);

		for (Concept thisMatch : matches) {
			print(Description.getFormattedConcept(thisMatch.getSctId()), "");
		}

		print("Found " + matches.size() + " matching concepts", "");
	}

	private Set<Concept> filterOutDescriptions(Set<Concept> matches, String searchTerm, boolean mustBePresent) {
		Set<Concept> filteredConcepts = new HashSet<Concept>();
		for (Concept thisMatch : matches) {
			String desc = Description.getDescription(thisMatch).toLowerCase();
			if (desc.contains(searchTerm)) {
				if (mustBePresent) {
					filteredConcepts.add(thisMatch);
				}
			} else if (!mustBePresent) {
				filteredConcepts.add(thisMatch);
			}
		}
		return filteredConcepts;
	}


	private Set<Concept> filterOutAttributes(Set<Concept> matches, SearchParameters params) {
		Set<Concept> filteredConcepts = new HashSet<Concept>();
		for (Concept thisMatch : matches) {
			if (conceptHasAttribute(thisMatch, params)) {
				if (params.attributeSearchPresent) {
					filteredConcepts.add(thisMatch);
				}
			} else if (!params.attributeSearchPresent) {
				filteredConcepts.add(thisMatch);
			}
		}
		return filteredConcepts;
	}

	private boolean conceptHasAttribute(Concept c, SearchParameters params) {
		switch (params.attributeSearch) {
			case AttributeType : 
				return conceptHasAttributeType(c, params.attributeType);
			case AttributeValue:
				return conceptHasAttributeValue(c, params.attributeValue);
			case AttributeValueString:
				return conceptHasAttributeValueString(c, params.attributeValueString);
			default :
				throw new RuntimeException ("Unexpected attribute search criteria " + params.attributeSearch);
		}
	}

	private boolean conceptHasAttributeType(Concept c, Concept attributeType) {
		for (Relationship r : c.getAllAttributes()) {
			if (r.isType(attributeType.getSctId())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean conceptHasAttributeValue(Concept c, Concept attributeValue) {
		for (Relationship r : c.getAllAttributes()) {
			if (r.getDestinationConcept().equals(attributeValue)) {
				return true;
			}
		}
		return false;
	}

	private boolean conceptHasAttributeValueString(Concept c, String attributeValueString) {
		for (Relationship r : c.getAllAttributes()) {
			String attributeValueFSN = Description.getDescription(r.getDestinationConcept()).toLowerCase();
			if (attributeValueFSN.contains(attributeValueString)) {
				return true;
			}
		}
		return false;
	}

	public void findDuplicateTypes(CHARACTERISTIC currentView, String whiteList, boolean includeDescendants, Long typeId) {
		long conceptsChecked = 0;
		long duplicatesDetected = 0;
		Map<String, Map<String, Integer>> duplicateTypeBag = new HashMap<String, Map<String, Integer>>();
		for (Concept thisConcept : Concept.getAllConcepts(currentView)) {
			for (RelationshipGroup thisGroup : thisConcept.getGroups()){
				// Not worried about duplicate types in group 0
				if (thisGroup.getNumber() == 0) {
					continue;
				}
				duplicatesDetected += findDuplicateTypes(thisGroup, whiteList, includeDescendants, duplicateTypeBag, typeId);
			}
			if (++conceptsChecked % 5000 == 0) {
				print("Checked "+conceptsChecked+" concepts", "");
			}
		}

		duplicateTypeSummary(duplicateTypeBag);
		print("\nDetected " + duplicatesDetected + " duplicate attribute types", "");
	}


	private void duplicateTypeSummary(Map<String, Map<String, Integer>> duplicateTypeBag) {

		print("\n\nSummary\n==========", "");

		for (final Map.Entry<String, Map<String, Integer>> entry : duplicateTypeBag.entrySet()) {
			print(Description.getFormattedConcept(Long.parseLong(entry.getKey())), "");
			String[] values = entry.getValue().keySet().toArray(new String[entry.getValue().size()]);
			Arrays.sort(values, new Comparator<String>() {
				public int compare(String o1, String o2) {
					return entry.getValue().get(o1).intValue() - entry.getValue().get(o2).intValue();
				}
			});

			for (String thisValueDuplicate : values) {
				String[] duplicatedElements = thisValueDuplicate.split(",");
				print(Description.getFormattedConcept(Long.parseLong(duplicatedElements[0])) + " plus "
						+ Description.getFormattedConcept(Long.parseLong(duplicatedElements[1])) + " - "
						+ entry.getValue().get(thisValueDuplicate), "\t");
			}
		}
	}

	private int findDuplicateTypes(RelationshipGroup thisGroup, String whiteList, boolean includeDescendants,
			Map<String, Map<String, Integer>> duplicateTypeBag, Long typeId) {
		//For every attribute, see if there's another attribute in this group with the same (or parent) type,
		//but (to prevent matching to self) a different value
		for (Relationship thisAttribute : thisGroup.getAttributes()) {
			if (whiteList.contains(thisAttribute.getTypeId().toString()) || (typeId != null && !thisAttribute.getTypeId().equals(typeId))) {
				continue;
			}
			for (Relationship comparisonAttribute : thisGroup.getAttributes()) {
				if (thisAttribute.getTypeId().equals(comparisonAttribute.getTypeId()) || (
						includeDescendants == true && thisAttribute.getTypeConcept().getParents().contains(comparisonAttribute.getTypeConcept()))){
					if (!thisAttribute.getDestinationId().equals(comparisonAttribute.getDestinationId())) {
						print ("Found duplicate attribute types in concept: " + Description.getFormattedConcept(thisAttribute.getSourceId()),"");
						print(thisAttribute.toPrettyString(), "");
						print(comparisonAttribute.toPrettyString(), "");

						// Have we seen this attribute type before?
						String valueCombo = thisAttribute.getDestinationId() + "," + comparisonAttribute.getDestinationId();
						Map<String, Integer> valueCounts = duplicateTypeBag.get(thisAttribute.getTypeId().toString());
						if (valueCounts == null) {
							valueCounts = new HashMap<String, Integer>();
							duplicateTypeBag.put(thisAttribute.getTypeId().toString(), valueCounts);
						}
						// Now have we see this attribute type + value combination before?
						Integer thisCounter = valueCounts.get(valueCombo);
						if (thisCounter == null) {
							thisCounter = new Integer(0);
						}
						thisCounter = new Integer(thisCounter.intValue() + 1);
						valueCounts.put(valueCombo, thisCounter);
						return 1;
					}
				}
			}
		}
		return 0;
	}

	public void findConceptsUsingAttributeValue(long targetSCTID, CHARACTERISTIC currentView) {
		long conceptsChecked = 0;
		long AVDetected = 0;

		for (Concept thisConcept : Concept.getAllConcepts(currentView)) {
			for (Relationship thisAttribute : thisConcept.getAllAttributes()) {
				if (thisAttribute.getDestinationId().equals(targetSCTID)) {
					print(Description.getFormattedConcept(thisConcept.getSctId()), "");
					print("  Attribute: " + thisAttribute.toPrettyString(), "");
					AVDetected++;
				}
			}
			if (++conceptsChecked % 5000 == 0) {
				print("Checked " + conceptsChecked + " concepts", "");
			}
		}
		print("Detected " + AVDetected + " instances of attribute with value/destination: " + Description.getFormattedConcept(targetSCTID),
				"");

	}

	/**
	 * 
	 * @param stripOff
	 *            - The term to strip off
	 * @param primaryHierarchy
	 *            - the hierarchy in which to find the words used in the search
	 * @param secondaryHierarchy
	 *            - the hierarchy in which to search
	 */
	public void textualSubstringMatch(String stripOff, Long primaryHierarchy, Long secondaryHierarchy, CHARACTERISTIC currentView) {
		Concept startingPoint = Concept.getConcept(primaryHierarchy, currentView);
		int searchesAttempted = 0;
		for (Concept thisConcept : startingPoint.getDescendents(Concept.DEPTH_NOT_SET, false)) {
			String fsn = Description.getDescription(thisConcept);
			if (fsn.contains(stripOff)) {
				String pt = fsn.replaceAll("\\(.*\\)", "");
				pt = pt.replace(stripOff, "").replace("- ", "");
				String[] searchTerms = pt.split(" ");
				// Now find all OTHER concepts within this hierarchy that contain those words
				Concept searchingPoint = Concept.getConcept(primaryHierarchy, currentView);
				matchAllWords(searchTerms, searchingPoint, thisConcept, stripOff);
				searchesAttempted++;
			}
		}
		print("Completed searches of matches to " + searchesAttempted + " concepts", "");
	}

	private void matchAllWords(String[] searchTerms, Concept searchingPoint, Concept sourceConcept, String strippedTerm) {
		boolean firstFound = false;
		String leafNodeIndicator = sourceConcept.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY, false).size() > 0 ? "" : "* ";
		for (Concept thisConcept : searchingPoint.getDescendents(Concept.DEPTH_NOT_SET, false)) {
			String thisDescription = Description.getDescription(thisConcept);
			// Don't match the source for this search process or terms including the term we're stripping
			if (thisConcept.equals(sourceConcept) || thisDescription.contains(strippedTerm)) {
				continue;
			}

			boolean containsAllTerms = true;
			for (String thisWord : searchTerms) {
				if (!thisDescription.contains(" " + thisWord + " ")) {
					containsAllTerms = false;
				}
			}

			if (containsAllTerms) {
				firstFound = true;
				print(leafNodeIndicator + Description.getFormattedConcept(sourceConcept.getSctId()) + " exists in "
						+ Description.getFormattedConcept(thisConcept.getSctId()), "");
			}
		}

		if (!firstFound) {
			// print(leafNodeIndicator + Description.getFormattedConcept(sourceConcept.getSctId()) + " no matches found", "");
		}
	}

	/**
	 * Method:	Get list of all qualifying relationships
	 *			Group them into type/destination 
	 *			For each type/destination see if there's a common parent
	 *			If the parent is too high, find clusters of sub-hierarchies
	 *			Given the sub hierarchies, detect Exceptions who do not possess that attribute
	 * @throws Exception 
	 */
	public void reverseEngineerQualifyingRules() throws Exception {
		print ("Reverse Engineering Qualifying Rules","");
		Map<QualifyingRelationshipAttribute, List<Relationship>> qrGroup = groupQualifyingRelationships();
		for (Map.Entry<QualifyingRelationshipAttribute, List<Relationship>> thisEntry : qrGroup.entrySet()) {
			QualifyingRelationshipAttribute td = thisEntry.getKey();
			List<Relationship> rels = thisEntry.getValue();
			print (td.toString() +  " - " + rels.size(), "");
			determineQualifyingRuleDomains(td, rels);
		}
		
		//Now output the Attributes with Rules and Exceptions to a JSON file
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String filename = "qr-rules_" + df.format(new Date()) + ".json";
		try(Writer writer = new OutputStreamWriter(new FileOutputStream(filename) , "UTF-8")){
			Gson gson = new GsonBuilder()
							.setPrettyPrinting()
							.registerTypeAdapter(Concept.class, new ConceptSerializer())
							.create();
			Set<QualifyingRelationshipAttribute> allAttributeRules = qrGroup.keySet();
			gson.toJson(allAttributeRules, writer);
		}
	}

	private void determineQualifyingRuleDomains(QualifyingRelationshipAttribute qra,
			List<Relationship> rels) {
		Set<Concept> sourceConcepts = extractSourceConcepts(rels);
		//What's the common ancestor?
		Concept commonAncestor = null;
		if (sourceConcepts.size() > 1) {
			commonAncestor = findCommonAncestor(sourceConcepts);
			print ("Common ancestor: " + Description.getFormattedConcept(commonAncestor.getSctId()), "   ");
		} else {
			commonAncestor = sourceConcepts.toArray(new Concept[0])[0];
			print ("Single: " + Description.getFormattedConcept(commonAncestor.getSctId()),"   ");
		}
		Set<Concept> exceptions = determineQualifyingExceptions(sourceConcepts, commonAncestor);
		print ("Exceptions: " + exceptions.size(), "   ");
		
		if (qra.getDestination().getSctId().equals(qualifierIgnore)) {
			print ("Marked to ignore for now","   ");
			return;
		}
		
		if (exceptions.size() > rels.size() || exceptions.size() > MAX_EXCEPTIONS) {
			//Lets try a bottom up approach instead
			determineQualifyingRuleDomainStartPoints(qra, sourceConcepts);
		} else {
			QualifyingRelationshipRule newRule = new QualifyingRelationshipRule(commonAncestor, CONSTRAINT.DESCENDENT_OR_SELF, null, exceptions);
			qra.addRule(newRule);
		}
		
	}

	private void determineQualifyingRuleDomainStartPoints(QualifyingRelationshipAttribute qra,
			Set<Concept> conceptsWithQrAttribute) {
		Set<Concept> remaining = new TreeSet<Concept>(conceptsWithQrAttribute);
		while (remaining.size() > 0) {
			Concept deepestConcept = getDeepestConcept(remaining);
			Concept currentPosition = deepestConcept;
			Set<Concept> allExceptions = new HashSet<Concept>();
			//Do any of my parents also feature this Qualifying Relationship?  
			//Move up and remove all descendents of the parent if so.
			Collection<Concept> intersection = null;
			do {
				intersection = CollectionUtils.intersection(currentPosition.getParents(), remaining);
				Concept bestParent = null;
				if (intersection.size() > 1) {
					//print ("Warning at " + Description.getFormattedConcept(currentPosition.getSctId()) + " has multiple parents with Qualifying attribute.","     ");
					//have a peek at all parents to see which ones also feature the Qualifying Relationship.
					Collection<Concept> preview;
					for (Concept thisParent : intersection) {
						preview = CollectionUtils.intersection(thisParent.getParents(), remaining);
						if (preview.size() > 0) {
							if (bestParent != null) {
								print (currentPosition + " has multiple matching parents: " + bestParent + " and " + thisParent, "      ");
							}
							bestParent = thisParent;
						}
					}
				} else if (intersection.size() > 0) {
					bestParent = intersection.iterator().next();
				}
				
				if (bestParent != null) {
					currentPosition = bestParent;
					//And remove everything down from here as covered under this parent
					remaining.removeAll(currentPosition.getAllDescendents(Concept.DEPTH_NOT_SET));
					
					//Work out the exceptions at each level - which of the children should NOT have this attribute?
					
					//TODO Problem here that immediate children might have attribute, but not below that.
					Set<Concept> children = currentPosition.getAllDescendents(Concept.DEPTH_NOT_SET);
					Collection<Concept> exceptions = CollectionUtils.subtract(children, conceptsWithQrAttribute);
					allExceptions.addAll(exceptions);
					
					//children of the parent which have the attribute but whos children do NOT, are stop points
					
					if (!exceptions.isEmpty()) {
						print (currentPosition + " " + exceptions.size() + " exceptions.", "       ");
					}
				}
			} while (intersection.size() > 0);
			//Once we've finished working up the tree, this is our highest point for this rule
			
			//Can we go one higher to capture our siblings and do a "Descendents" rule without too many exceptions?
			Concept higherPosition = checkOneLevelHigherQualifyingRelationships(currentPosition, conceptsWithQrAttribute);
			Set<Concept> desc = currentPosition.getAllDescendents(Concept.DEPTH_NOT_SET);
			CONSTRAINT constraint = CONSTRAINT.DESCENDENT_OR_SELF;
			
			//So we can also remove this one from our list, and its descendents if we went one higher
			remaining.remove(currentPosition);
			if (!higherPosition.equals(currentPosition)) {
				currentPosition = higherPosition;
				remaining.removeAll(desc);
				constraint = CONSTRAINT.DESCENDENT;
			}
			
			print ("Rule start point at: " + Description.getFormattedConcept(currentPosition.getSctId()) + " - " + desc.size(), "    ");
			QualifyingRelationshipRule newRule = new QualifyingRelationshipRule(currentPosition, constraint, null, allExceptions);
			qra.addRule(newRule);
		}
		
	}

	/** 
	 * Although we know the parent doesn't feature the attribute, check if more
	 * of it's children do than don't.  In whcih case it's a better option.
	 */
	private Concept checkOneLevelHigherQualifyingRelationships(
			Concept currentPosition, Set<Concept> conceptsWithQrAttribute) {
		Concept bestMatch = currentPosition;  //If we don't get a positive score, stay where we are
		Set<Concept> oneLevelHigher = currentPosition.getParents();
		int bestScore = 0; //Score here is number matching minus number not matching ie exceptions
		int matchHits = 0;
		int matchMisses = 0;
		for (Concept thisParent : oneLevelHigher) {
			Set<Concept> children = thisParent.getAllDescendents(Concept.IMMEDIATE_CHILDREN_ONLY);
			Collection<Concept> hasQRel = CollectionUtils.intersection(children, conceptsWithQrAttribute);
			Collection<Concept> noQRel  = CollectionUtils.subtract(children, conceptsWithQrAttribute);
			int score = hasQRel.size() - noQRel.size();
			if (score > 0 && score > bestScore) {
				bestScore = score;
				bestMatch = thisParent;
				matchHits = hasQRel.size();
				matchMisses = noQRel.size();
			}
		}
		if (!bestMatch.equals(currentPosition)) {
			print (currentPosition + " moved to next higher start point " + bestMatch + " [" + matchHits + "/" + matchMisses +"]", "   ");
		}
		return bestMatch;
	}

	private Concept getDeepestConcept(Set<Concept> concepts) {
		Concept deepestConcept = null;
		for (Concept thisConcept : concepts) {
			if (deepestConcept == null || thisConcept.getDepth() > deepestConcept.getDepth()) {
				deepestConcept = thisConcept;
			}
		}
		return deepestConcept;
	}

	/**
	 * @return The set of descendents of the commonAncestor which do not feature the Qualifying attributes
	 */
	private Set<Concept> determineQualifyingExceptions(
			Set<Concept> sourceConcepts, Concept commonAncestor) {
		Set<Concept> exceptions = commonAncestor.getAllDescendents(Concept.DEPTH_NOT_SET);
		exceptions.removeAll(sourceConcepts);
		//Where an exception has no descendents that have the qualifying attribute, we can ignore them.
		Set<Concept> exceptionDriver = new HashSet<Concept>(exceptions);
		for (Concept thisException : exceptionDriver) {
			//If we've already removed this exception from the list, no need to process
			if (!exceptions.contains(thisException)) {
				continue;
			}
			Set<Concept> exceptionDescendents = thisException.getAllDescendents(Concept.DEPTH_NOT_SET);
			if (Collections.disjoint(exceptionDescendents, sourceConcepts)) {
				exceptions.removeAll(exceptionDescendents);
			}
		}
		return exceptions;
	}

	private Set<Concept> extractSourceConcepts(List<Relationship> rels) {
		Set<Concept> concepts = new HashSet<Concept> ();
		for (Relationship r : rels) {
			concepts.add(r.getSourceConcept());
		}
		return concepts;
	}

	private Map<QualifyingRelationshipAttribute, List<Relationship>> groupQualifyingRelationships() throws Exception {
		Map<QualifyingRelationshipAttribute, List<Relationship>> groups = new TreeMap<QualifyingRelationshipAttribute, List<Relationship>>();
		for (RF1Relationship r : GraphLoader.get().getRF1Relationships(CHARACTERISTIC.QUALIFYING).values()){
			QualifyingRelationshipAttribute thisTD = new QualifyingRelationshipAttribute(r.getTypeConcept(), r.getDestinationConcept(), r.getRefinability());
			List<Relationship> thisGroupList = null;
			if (groups.containsKey(thisTD)) {
				thisGroupList = groups.get(thisTD);
			} else {
				thisGroupList = new ArrayList<Relationship>();
				groups.put(thisTD, thisGroupList);
			}
			thisGroupList.add(r);
		}
		return groups;
	}
	
}
