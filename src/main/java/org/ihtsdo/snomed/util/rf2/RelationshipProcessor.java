package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage java -classpath /Users/Peter/code/snomed-utilities/target/snomed-utilities-1.0.10-SNAPSHOT.jar
 * org.ihtsdo.snomed.util.rf2.RelationshipProcessor
 * 
 * @author PGWilliams
 * 
 */
public class RelationshipProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RelationshipProcessor.class);

	private final String statedFile;
	private final String inferredFile;
	private final String outputFile;

	public RelationshipProcessor(String statedFile, String inferredFile, String outputFile) {
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
		this.outputFile = outputFile;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			doHelp();
		}

		RelationshipProcessor rp = new RelationshipProcessor(args[0], args[1], args[2]);
		rp.process();
	}

	private void process() throws Exception {
		LOGGER.debug("Loading Stated File: {}", statedFile);
		Map<String, Relationship> statedRelationships = loadFile(statedFile, Relationship.CHARACTERISTIC.STATED);

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		Map<String, Relationship> inferredRelationships = loadFile(inferredFile, Relationship.CHARACTERISTIC.INFERRED);

		LOGGER.debug("Loading complete");
		// Validation check that both trees only have 1 concept that has no parents
		Concept.ensureParents(Relationship.CHARACTERISTIC.STATED);
		Concept.ensureParents(Relationship.CHARACTERISTIC.INFERRED);

		// Now for all the active stated relationships that don't exist as active rows in the inferred file,
		// find a suitable replacement
		findReplacements(statedRelationships, inferredRelationships);

		// What progress have we made?
		reportProgress(statedRelationships);
	}

	private void findReplacements(Map<String, Relationship> statedRelationships, Map<String, Relationship> inferredRelationships) {

		for (Relationship thisStatedRelationship : statedRelationships.values()) {
			// Does this relationship exist in the inferred file? If not, find it a replacement
			if (!inferredRelationships.containsKey(thisStatedRelationship.getUuid())) {
				thisStatedRelationship.setNeedsReplaced(true);
				// Can we find an inferred relationship in the same group with the same type where the destination
				// is a child of the stated relationship's destination?
				Concept sourceInferred = Concept.getConcept(thisStatedRelationship.getSourceId(), Relationship.CHARACTERISTIC.INFERRED);
				List<Relationship> replacements = sourceInferred.findMatchingRelationships(thisStatedRelationship.getTypeId(),
						thisStatedRelationship.getGroup(), thisStatedRelationship.getDestinationConcept());
				if (replacements.size() > 0) {
					// Shouldn't matter which matching relationship we replace with.
					thisStatedRelationship.setReplacement(replacements.get(0));
				}
			}
		}

	}

	private void reportProgress(Map<String, Relationship> statedRelationships) {
		long needsReplaced = 0;
		long hasBeenReplaced = 0;

		for (Relationship thisStatedRelationship : statedRelationships.values()) {

			if (thisStatedRelationship.isNeedsReplaced()) {
				needsReplaced++;
			}

			if (thisStatedRelationship.hasReplacement()) {
				hasBeenReplaced++;
			}
		}

		long remainder = needsReplaced - hasBeenReplaced;
		LOGGER.info("Of the {} stated relationships, {} needed replaced, {} have been replaced, leaving {} to work with",
				statedRelationships.size(), needsReplaced, hasBeenReplaced, remainder);

	}

	private Map<String, Relationship> loadFile(String filePath, Relationship.CHARACTERISTIC characteristic) throws Exception {
		// Does this file exist and not as a directory?
		File file = new File(filePath);
		if (!file.exists() || file.isDirectory()) {
			throw new IOException("Unable to read file " + filePath);
		}
		Map<String, Relationship> loadedRelationships = new HashMap<String, Relationship>();

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean isFirstLine = true;
			while ((line = br.readLine()) != null) {

				if (!isFirstLine) {
					String[] lineItems = line.split(Relationship.FIELD_DELIMITER);
					// Only store active relationships
					if (lineItems[Relationship.IDX_ACTIVE].equals(Relationship.ACTIVE_FLAG)) {
						Relationship r = new Relationship(lineItems, characteristic);
						loadedRelationships.put(r.getUuid(), r);
					}
				} else {
					isFirstLine = false;
					continue;
				}

			}
		}
		return loadedRelationships;
	}

	private static void doHelp() {
		LOGGER.info("Usage: <stated relationship file location>  <inferred realtionship file location> <output file location>");
		System.exit(-1);

	}

}
