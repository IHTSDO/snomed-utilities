package org.ihtsdo.snomed.util.mrcm;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.RelationshipGroup;
import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.ihtsdo.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		LOGGER.info("Examining Siblings to Determine MRCM Rules...");
		// Lets start with children of "Procedure by site"
		Concept c = Concept.getConcept(362958002L, CHARACTERISTIC.INFERRED);
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

	}

	private static void reportMemory() {
		Runtime runtime = Runtime.getRuntime();
		LOGGER.info("Used Memory: {} Mb", (runtime.totalMemory() - runtime.freeMemory()) / mb);
		LOGGER.info("Free Memory: {} Mb", runtime.freeMemory() / mb);
	}

}
