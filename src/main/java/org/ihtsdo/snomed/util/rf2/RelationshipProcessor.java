package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ihtsdo.snomed.util.rf2.Relationship.CHARACTERISTIC;
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

	private int a1Count = 0;
	private int a2Count = 0;
	private int a3Count = 0;

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

	private void findReplacements(Map<String, Relationship> statedRelationships, Map<String, Relationship> inferredRelationships)
			throws UnsupportedEncodingException {

		for (Relationship thisStatedRelationship : statedRelationships.values()) {
			// Does this relationship exist in the inferred file? If not, find it a replacement
			if (!inferredRelationships.containsKey(thisStatedRelationship.getUuid())) {
				thisStatedRelationship.setNeedsReplaced(true);
				boolean successfulReplacement = false;
				//Try Algorithm 1
				successfulReplacement = matchGroupPlusChildDestination(thisStatedRelationship);
				
				//Try Algorithm 2
				if (!successfulReplacement) {
					successfulReplacement = matchTriplesAcrossGroups(thisStatedRelationship);
				}
				
				//Try Algorithm 3
				if (!successfulReplacement) {
					//successfulReplacement = matchTriplesAcrossGroups(thisStatedRelationship);
				}
			}
		}

	}
	
	/*
	 * Algorithm 1 - find an inferred relationship with the same source, type and group but a 
	 * more proximate destination
	 */
	private boolean matchGroupPlusChildDestination(Relationship sRelationship) {
		boolean success = false;
		// Can we find an inferred relationship in the same group with the same type where the destination
		// is a child of the stated relationship's destination?
		Concept sourceInferred = Concept.getConcept(sRelationship.getSourceId(), Relationship.CHARACTERISTIC.INFERRED);
		List<Relationship> replacements = sourceInferred.findMatchingRelationships(sRelationship.getTypeId(),
				sRelationship.getGroup(), sRelationship.getDestinationConcept());
		if (replacements.size() > 0) {
			// Shouldn't matter which matching relationship we replace with.
			sRelationship.setReplacement(replacements.get(0));
			success = true;
			a1Count++;
		}
		return success;
	}
	
	/*
	 * Algorithm 2 - find an inferred relationship where all members of the stated group exist as the same triples
	 * in the inferred group
	 */
	private boolean matchTriplesAcrossGroups(Relationship sRelationship) throws UnsupportedEncodingException {
		boolean success = false;
		//What is the triples hash of the stated group?
		String triplesHash = sRelationship.getSourceConcept().getTriplesHash(sRelationship.getGroup());
		
		//Are there any inferred groups for the same source concept that feature the same triples hash?
		//Use the concept in the inferred graph
		Concept sourceConceptInf = Concept.getConcept(sRelationship.getSourceId(), CHARACTERISTIC.INFERRED);
		List<Relationship> replacements = sourceConceptInf.findMatchingRelationships(triplesHash, sRelationship);
		if (replacements != null && replacements.size() > 0) {
			// Shouldn't matter which matching relationship we replace with.
			sRelationship.setReplacement(replacements.get(0));
			success = true;
			a2Count++;
		}
		return success;
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
		LOGGER.info("Algorithm success rates 1: {}, 2: {} 3: {}", a1Count, a2Count, a3Count);

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
