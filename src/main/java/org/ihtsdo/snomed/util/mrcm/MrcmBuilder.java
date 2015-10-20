package org.ihtsdo.snomed.util.mrcm;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.pojo.RelationshipGroup;
import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.ihtsdo.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class MrcmBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphLoader.class);

	public static int mb = 1024 * 1024;
	private static void doHelp() {
		LOGGER.info("Usage: <concept file location> <stated relationship file location> <inferred realtionship file location>");
		System.exit(-1);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			doHelp();
		}
		reportMemory();
		GraphLoader g = new GraphLoader(args[0], args[1], args[2]);
		g.loadRelationships();
		reportMemory();

		long conceptToExamine = 362958002L;
		// long conceptToExamine = 285579008L;
		// CHARACTERISTIC hierarchyToExamine = CHARACTERISTIC.INFERRED;
		CHARACTERISTIC hierarchyToExamine = CHARACTERISTIC.STATED;
		LOGGER.info("Examining Siblings of {} in the {} hierarchy to Determine MRCM Rules...", conceptToExamine, hierarchyToExamine);
		// Lets start with children of "Procedure by site"
		Concept c = Concept.getConcept(conceptToExamine, hierarchyToExamine);
		determineMRCM(c);
	}

	private static void determineMRCM(Concept c) throws UnsupportedEncodingException {
		Set<Concept> siblings = c.getChildren();
		Set<Concept> definedSiblings = c.getFullyDefinedChildren();
		LOGGER.info("Examining {} fully defined out of {} children of {}", definedSiblings.size(), siblings.size(), c.getSctId());

		final ConcurrentMap<String, AtomicInteger> shapePopularity = new ConcurrentHashMap<String, AtomicInteger>();
		final ConcurrentMap<String, AtomicInteger> groupsHashPopularity = new ConcurrentHashMap<String, AtomicInteger>();

		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				String groupShape = g.getGroupShape();
				LOGGER.info("{}:{} - {} ", sibling.getSctId(), g.getNumber(), groupShape);
				shapePopularity.putIfAbsent(groupShape, new AtomicInteger(0));
				shapePopularity.get(groupShape).incrementAndGet();
			}
			String groupsShapeHash = sibling.getGroupsShapeHash().toString();
			LOGGER.info("  Groups Shape: {}", groupsShapeHash);
			groupsHashPopularity.putIfAbsent(groupsShapeHash, new AtomicInteger(0));
			groupsHashPopularity.get(groupsShapeHash).incrementAndGet();
		}
		LOGGER.info("Shape Popularity:");
		CollectionUtils.printSortedMap(shapePopularity);
		
		LOGGER.info("Groups Hash Popularity:");
		CollectionUtils.printSortedMap(groupsHashPopularity);

		// Now loop through groups again and see if we can find a better shape by working
		// with the abstract types (ie the parent of the type)
		// Calculate a new popularity Map
		shapePopularity.clear();

		for (Concept sibling : definedSiblings) {
			List<RelationshipGroup> groups = sibling.getGroups();
			for (RelationshipGroup g : groups) {
				String preferredShape = g.getGroupShape();
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
						LOGGER.info("Abstract Group Shape better for {}: {} (was {})", sibling.getSctId(), groupAbstractShape,
								g.getGroupShape());
						preferredShape = groupAbstractShape;
					}
				}
				// Also put the original shape (which might remain zero popular) so we can compare it later
				shapePopularity.putIfAbsent(g.getGroupShape(), new AtomicInteger(0));
				shapePopularity.putIfAbsent(preferredShape, new AtomicInteger(0));
				shapePopularity.get(preferredShape).incrementAndGet();
			}
		}
		
		LOGGER.info("Shape Popularity after considering Abstract Shape:");
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
						LOGGER.info("Partial Group Shape more popular for {}: {} (was {})", sibling.getSctId(), groupPartialShape,
								g.getGroupShape());
					}
				}
			}
		}

		// Do both and try to find a more popular partial group abstract shape
		findPartialGroupAbstractShapes(definedSiblings, shapePopularity);

	}

	private static void findPartialGroupAbstractShapes(Set<Concept> definedSiblings, ConcurrentMap<String, AtomicInteger> shapePopularity)
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
							LOGGER.info("Partial Group Abstract Shape more popular for {}: {} (was {})", sibling.getSctId(),
									groupPartialAbstractShape, g.getGroupShape());
						}
					}
				}
			}
		}
	}

	private static void reportMemory() {
		Runtime runtime = Runtime.getRuntime();
		LOGGER.info("Used Memory: {} Mb", (runtime.totalMemory() - runtime.freeMemory()) / mb);
		LOGGER.info("Free Memory: {} Mb", runtime.freeMemory() / mb);
	}

}
