package org.ihtsdo.snomed.util.mrcm;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.Description;
import org.ihtsdo.snomed.util.pojo.GroupShape;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.pojo.RelationshipGroup;
import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.ihtsdo.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class MrcmBuilder {

	private static String INDENT_0 = "";
	private static String INDENT_1 = "\t";

	private final Logger logger = LoggerFactory.getLogger(GraphLoader.class);

	public void determineMRCM(Concept c) throws UnsupportedEncodingException {
		Set<Concept> siblings = c.getChildren();
		Set<Concept> definedSiblings = c.getFullyDefinedChildren();
		logger.info("Examining {} fully defined out of {} children of {}", definedSiblings.size(), siblings.size(), c.getSctId());

		final ConcurrentMap<String, AtomicInteger> shapePopularity = new ConcurrentHashMap<String, AtomicInteger>();
		final ConcurrentMap<String, AtomicInteger> groupsHashPopularity = new ConcurrentHashMap<String, AtomicInteger>();

		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				String groupShape = g.getGroupShape();
				logger.info("{}:{} - {} ", sibling.getSctId(), g.getNumber(), groupShape);
				shapePopularity.putIfAbsent(groupShape, new AtomicInteger(0));
				shapePopularity.get(groupShape).incrementAndGet();
			}
			String groupsShapeHash = sibling.getGroupsShapeHash().toString();
			logger.info("  Groups Shape: {}", groupsShapeHash);
			groupsHashPopularity.putIfAbsent(groupsShapeHash, new AtomicInteger(0));
			groupsHashPopularity.get(groupsShapeHash).incrementAndGet();
		}
		logger.info("Shape Popularity:");
		CollectionUtils.printSortedMap(shapePopularity);
		
		logger.info("Groups Hash Popularity:");
		CollectionUtils.printSortedMap(groupsHashPopularity);

		// Now loop through groups again and see if we can find a better shape by working
		// with the abstract types (ie the parent of the type)
		// Calculate a new popularity Map
		shapePopularity.clear();

		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				String preferredShapeId = g.getGroupShape();
				Set<Integer> preferredAbstractCombination = null;
				// Loop through all the combinations of attributes to express as type ancestors
				Set<Set<Integer>> allCombinations = CollectionUtils.getIndexCombinations(g.size());
				for (Set<Integer> thisCombination : allCombinations) {
					// Skip the empty set ie no abstractions
					if (thisCombination.size() == 0) {
						continue;
					}
					String groupAbstractShape = g.getGroupAbstractShape(thisCombination);
					// If ANY sibling already uses the more abstract model, then that's preferable to have in common
					if (shapePopularity.containsKey(groupAbstractShape)) {
						logger.info("Abstract Group Shape better for {}: {} (was {})", sibling.getSctId(), groupAbstractShape,
								g.getGroupShape());
						preferredShapeId = groupAbstractShape;
						preferredAbstractCombination = thisCombination;
					}
				}
				// Also put the original shape (which might remain zero popular) so we can compare it later
				shapePopularity.putIfAbsent(g.getGroupShape(), new AtomicInteger(0));
				shapePopularity.putIfAbsent(preferredShapeId, new AtomicInteger(0));
				int newPopularity = shapePopularity.get(preferredShapeId).incrementAndGet();
				GroupShape preferredShape = new GroupShape(preferredShapeId, null, preferredAbstractCombination, newPopularity);
				g.setMostPopularShape(preferredShape);
			}
		}
		
		logger.info("Shape Popularity after considering Abstract Shape:");
		CollectionUtils.printSortedMap(shapePopularity);

		// Now loop through groups again and see if we can find a more popular shape by working
		// with partial group matches
		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				// Loop through all the combinations of attributes
				Set<Set<Integer>> allCombinations = CollectionUtils.getIndexCombinations(g.size());
				for (Set<Integer> thisCombination : allCombinations) {
					// Skip the empty set ie no attributes in group
					if (thisCombination.size() == 0) {
						continue;
					}
					String groupPartialShape = g.getGroupPartialShape(thisCombination);
					// If ANY sibling already uses the more abstract model, then that's preferable to have in common
					if (shapePopularity.containsKey(groupPartialShape) && shapePopularity.get(groupPartialShape).get() > shapePopularity.get(g.getGroupShape()).get()) {
						logger.info("Partial Group Shape more popular for {}: {} (was {})", sibling.getSctId(), groupPartialShape,
								g.getGroupShape());
					}
				}
			}
		}

		// Do both and try to find a more popular partial group abstract shape
		findPartialGroupAbstractShapes(definedSiblings, shapePopularity);

	}

	private void findPartialGroupAbstractShapes(Set<Concept> definedSiblings, ConcurrentMap<String, AtomicInteger> shapePopularity)
			throws UnsupportedEncodingException {
		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				// Loop through all the combinations of attributes
				Set<Set<Integer>> allAttributeCombinations = CollectionUtils.getIndexCombinations(g.size());
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
						if (shapePopularity.containsKey(groupPartialAbstractShape)
								&& shapePopularity.get(groupPartialAbstractShape).get() > shapePopularity.get(g.getGroupShape()).get()) {
							logger.info("Partial Group Abstract Shape more popular for {}: {} (was {})", sibling.getSctId(),
									groupPartialAbstractShape, g.getGroupShape());
						}
					}
				}
			}
		}
	}

	public void determineMRCM(String sctid, CHARACTERISTIC hierarchyToExamine) throws UnsupportedEncodingException {
		Long conceptToExamine = new Long(sctid);
		Concept c = Concept.getConcept(conceptToExamine, hierarchyToExamine);
		determineMRCM(c);
	}

	public void displayShape(String sctid, CHARACTERISTIC hierarchyToExamine) {
		Long conceptToExamine = new Long(sctid);
		Concept c = Concept.getConcept(conceptToExamine, hierarchyToExamine);
		prettyPrint(c, INDENT_0);
		for (Concept child : c.getFullyDefinedChildren()) {
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
				allCommonAncestors = thisConcept.getAllAncestorsAndSelf();
			} else {
				allCommonAncestors.retainAll(thisConcept.getAllAncestorsAndSelf());
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

		for (Concept thisChild : parent.getChildren()) {
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
			allAttributeTypes.add(thisRelationship.getType());
		}

		for (Concept thisChild : parent.getChildren()) {
			populateAllAttributeTypes(thisChild, allAttributeTypes);
		}
	}

}
