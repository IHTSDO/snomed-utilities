package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private String outputFile;

	private Map<String, Relationship> statedRelationships;
	private Map<String, Relationship> inferredRelationships;

	// Counters to track how many replacements made by each algorithm
	private int a1Count = 0;
	private int a2Count = 0;
	private int a3Count = 0;
	private int a4Count = 0;
	private int a5Count = 0;
	private int a3_1Count = 0;
	private int a3_2Count = 0;

	private String outputEffectiveTime;

	public RelationshipProcessor(String statedFile, String inferredFile, String outputFile) {
		this(statedFile, inferredFile);
		this.outputFile = outputFile;
	}

	public RelationshipProcessor(String statedFile, String inferredFile) {
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			doHelp();
		}

		// Is the 3rd parameter a number? Output the relationships for that SCTID if so
		try {
			Long conceptSCTID = new Long(args[2]);
			RelationshipProcessor rp = new RelationshipProcessor(args[0], args[1]);
			rp.loadRelationships();
			rp.outputRelationships(conceptSCTID);

		} catch (NumberFormatException e) {
			RelationshipProcessor rp = new RelationshipProcessor(args[0], args[1], args[2]);
			// Make sure we have a valid effectiveTime for output, before we start
			rp.outputEffectiveTime = extractEffectiveTime(args[2]);
			rp.loadRelationships();
			rp.substituteInferredRelationships();
		}
	}

	/**
	 * Lookup the concept in both the stated and inferred graphs and output all the relationships (sorted naturally) for each
	 * 
	 * @param conceptSCTID
	 */
	private void outputRelationships(Long conceptSCTID) {
		// First find the concept in the Stated View
		Concept conceptStated = Concept.getConcept(conceptSCTID, CHARACTERISTIC.STATED);
		TreeSet<Relationship> statedRels = conceptStated.getAttributes();
		LOGGER.info(conceptSCTID.toString() + " stated view: ");
		for (Relationship thisRelationship : statedRels) {
			LOGGER.info(thisRelationship.toString());
		}

		Concept conceptInferred = Concept.getConcept(conceptSCTID, CHARACTERISTIC.INFERRED);
		TreeSet<Relationship> inferredRels = conceptInferred.getAttributes();
		LOGGER.info(conceptSCTID.toString() + " inferred view: ");
		for (Relationship thisRelationship : inferredRels) {
			LOGGER.info(thisRelationship.toString());
		}
	}

	private void loadRelationships() throws Exception {

		LOGGER.debug("Loading Stated File: {}", statedFile);
		statedRelationships = loadFile(statedFile, Relationship.CHARACTERISTIC.STATED);

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		inferredRelationships = loadFile(inferredFile, Relationship.CHARACTERISTIC.INFERRED);

		LOGGER.debug("Loading complete");

	}

	private void substituteInferredRelationships() throws Exception {

		// Validation check that both trees only have 1 concept that has no parents
		Concept.ensureParents(Relationship.CHARACTERISTIC.STATED);
		Concept.ensureParents(Relationship.CHARACTERISTIC.INFERRED);

		// Now for all the active stated relationships that don't exist as active rows in the inferred file,
		// find a suitable replacement
		findReplacements();

		// What progress have we made?
		reportProgress();

		// Output the file, deactivating the replaced, adding the replacments and both should
		// have the target effectivetime
		outputFile();

		// And lets find out about the relationships we've failed to sort
		reportFailures();
	}

	private void findReplacements()
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
					successfulReplacement = matchChildDestinationInOtherGroups(thisStatedRelationship);
				}

				// Try Algorithm 4
				if (!successfulReplacement) {
					successfulReplacement = matchLooselyInOtherGroups(thisStatedRelationship);
				}

				// Try Algorithm ...er you can count I'm sure.
				if (!successfulReplacement) {
					successfulReplacement = matchMoreProximateType(thisStatedRelationship);
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
	
	/*
	 * Algorithm 3 - find an inferred relationship in a similar group where the destination is a more proximate child of the stated
	 * destination
	 */
	private boolean matchChildDestinationInOtherGroups(Relationship sRelationship) throws UnsupportedEncodingException {
		boolean success = false;
		//For a target group to match, all the relationship types in the stated group must
		//at least be present in the target (others may have been also added, so can't use triple hash)
		// We need to filter 'Is A' relationships out, because they don't take part in groups
		boolean filterIsAs = true;
		List<Relationship> siblingRels = sRelationship.getSourceConcept().findMatchingRelationships(sRelationship.getGroup(), filterIsAs);
		List<Long> groupTypes = new ArrayList<Long>();
		for (Relationship thisSibling : siblingRels) {
			groupTypes.add(thisSibling.getTypeId());
		}
		//Find groups containing those types in the Inferred Graph
		Concept sourceConceptInf = Concept.getConcept(sRelationship.getSourceId(), CHARACTERISTIC.INFERRED);
		List<Relationship> potentialGroups = sourceConceptInf.findMatchingRelationships(groupTypes);
		
		//Then this stated relationship must be present in that group as itself or a more proximate destination
		List<Relationship> replacements = new ArrayList<Relationship>();
		
		//So for all relationships in potentially compatible groups, see if we can match on type and destination
		//First because the destination is the same...
		for (Relationship potentialReplacement : potentialGroups) {
			if (potentialReplacement.isType(sRelationship.getTypeId()) && 
 potentialReplacement.getDestinationConcept().equals(sRelationship.getDestinationConcept())) {
					replacements.add(potentialReplacement);
				a3_1Count++;
			}
		}
		
		//And then if no success because it's a child of the stated destination
		if (replacements.size() == 0) {
			for (Relationship potentialReplacement : potentialGroups) {
				if (potentialReplacement.isType(sRelationship.getTypeId()) && 
						potentialReplacement.getDestinationConcept().hasParent(sRelationship.getSourceConcept()))
						replacements.add(potentialReplacement);
				a3_2Count++;
			}			
		}
		
		if (replacements.size() > 0) {
			// Shouldn't matter which matching relationship we replace with.
			sRelationship.setReplacement(replacements.get(0));
			success = true;
			a3Count++;
			if (replacements.size() > 1) {
				LOGGER.warn("Found multiple potential replacements for {}", sRelationship.toString());
			}
		}
		return success;
	}

	/*
	 * Algorithm 4 - find an inferred relationship in a non-similar group where the destination is the same or a more proximate child of the
	 * stated destination
	 */
	private boolean matchLooselyInOtherGroups(Relationship sRelationship) throws UnsupportedEncodingException {
		boolean success = false;
		// Can we find an inferred relationship in a different group with the same triple;

		Concept sourceInferred = Concept.getConcept(sRelationship.getSourceId(), Relationship.CHARACTERISTIC.INFERRED);
		List<Relationship> replacements = sourceInferred.findMatchingRelationships(sRelationship.getTypeId(), sRelationship.getDestinationConcept());
		if (replacements.size() > 0) {
			// Shouldn't matter which matching relationship we replace with.
			sRelationship.setReplacement(replacements.get(0));
			success = true;
			a4Count++;
		}
		return success;
	}

	/*
	 * Algorithm 5 - find an inferred relationship where the relationship type is a child of the stated type and if that fails, try also
	 * matching on the destination being a child of the stated destination
	 */
	private boolean matchMoreProximateType(Relationship sRelationship) {
		boolean success = false;
		Concept sourceInferred = Concept.getConcept(sRelationship.getSourceId(), Relationship.CHARACTERISTIC.INFERRED);
		Concept relationshipType = Concept.getConcept(sRelationship.getTypeId(), Relationship.CHARACTERISTIC.INFERRED);
		List<Relationship> replacements = sourceInferred.findMatchingRelationships(relationshipType);

		// Now first try and find in that list a relationship where we have the same destination
		for (Relationship thisPotentialReplacement : replacements) {
			if (thisPotentialReplacement.getDestinationConcept().equals(sRelationship.getDestinationConcept())) {
				sRelationship.setReplacement(thisPotentialReplacement);
				a5Count++;
				success = true;
				break;
			}
		}
		// And if not, try to find one with a child destination of the stated destination
		if (!success) {
			for (Relationship thisPotentialReplacement : replacements) {
				if (thisPotentialReplacement.getDestinationConcept().hasParent(sRelationship.getDestinationConcept())) {
					sRelationship.setReplacement(thisPotentialReplacement);
					a5Count++;
					success = true;
					break;
				}
			}
		}

		return success;
	}


	private void reportProgress() {
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
		LOGGER.info("Algorithm success rates 1: {}, 2: {}, 3: {}, 4: {}, 5: {}", a1Count, a2Count, a3Count, a4Count, a5Count);
		LOGGER.info("Algorithm 3 breakdown - potential Exact Match: {}, More Proximate: {}", a3_1Count, a3_2Count);

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

	private void outputFile() throws FileNotFoundException, IOException {

		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
			// Loop through all stated relationships and disable the replaced ones
			// and output the replacements all with effective time which matches the output file
			writer.write(Relationship.HEADER_ROW);
			for (Relationship thisRelationship : statedRelationships.values()) {
				if (thisRelationship.hasReplacement()) {
					writer.write(thisRelationship.getRF2(outputEffectiveTime, Relationship.INACTIVE_FLAG,
							Relationship.CHARACTERISTIC_STATED_SCTID));
					writer.write(thisRelationship.getReplacement().getRF2(outputEffectiveTime, Relationship.ACTIVE_FLAG,
							Relationship.CHARACTERISTIC_STATED_SCTID));
				}
			}
		}

	}

	private static String extractEffectiveTime(String fileName) throws Exception {
		Pattern p = Pattern.compile("[0-9]{8}");
		Matcher m = p.matcher(fileName);
		while (m.find()) {
			return m.group();
		}
		throw new Exception("Unable to parse effective time from " + fileName);
	}

	private void reportFailures() {
		LOGGER.info("First 10 failures: ");
		int relationshipsReported = 0;

		Relationship lastRelationship = null;
		for (Relationship thisRelationship : statedRelationships.values()) {
			if (thisRelationship.isNeedsReplaced() && !thisRelationship.hasReplacement()) {
				LOGGER.info(thisRelationship.toString());
				relationshipsReported++;
				lastRelationship = thisRelationship;
				if (relationshipsReported >= 10) {
					break;
				}
			}
		}
		// Output the full definition for the last concept mentioned
		if (lastRelationship != null) {
			outputRelationships(lastRelationship.getSourceId());
		}

	}

	private static void doHelp() {
		LOGGER.info("Usage: <stated relationship file location>  <inferred realtionship file location> <output file location>");
		System.exit(-1);

	}

}
