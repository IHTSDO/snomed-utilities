package org.ihtsdo.snomed.util.rf2.srsi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.snomed.util.rf2.srsi.Relationship.CHARACTERISTIC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;

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
	// The additional file contains relationships we want to just pass straight through to the stated output
	private final String additionalFile;

	private static final boolean LEAVE_SCTID_AS_IS = false;
	private static final boolean SET_SCTID_TO_NULL = true;

	private Map<String, Relationship> statedRelationships;
	private Map<String, Relationship> inferredRelationships;
	private Map<String, Relationship> additionalRelationships;

	// Counters to track how many replacements made by each algorithm
	private int a1Count = 0;
	private int a2Count = 0;
	private int a3Count = 0;
	private int a4Count = 0;
	private int a5Count = 0;
	private int a3_1Count = 0;
	private int a3_2Count = 0;

	private String outputEffectiveTime;

	public RelationshipProcessor(String statedFile, String inferredFile, String additionalFile, String outputFile) {
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
		this.additionalFile = additionalFile;
		this.outputFile = outputFile;
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 4) {
			doHelp();
		}

		RelationshipProcessor rp = new RelationshipProcessor(args[0], args[1], args[2], args[3]);
		// Make sure we have a valid effectiveTime for output, before we start
		rp.outputEffectiveTime = extractEffectiveTime(args[3]);
		rp.loadRelationships();
		rp.substituteInferredRelationships();

		// Are we running in interactive mode? Query memory structures if so
		if (args.length == 5 && args[4].equals("-i")) {
			rp.goInteractive();
		}
	}

	/**
	 * Lookup the concept in both the stated and inferred graphs and output all the relationships (sorted naturally) for each
	 * 
	 * @param conceptSCTID
	 * @throws Exception
	 */
	private void outputConcept(Long conceptSCTID) throws Exception {

		// Output all the parents of this concept in the
		Concept conceptInferred = Concept.getConcept(conceptSCTID, CHARACTERISTIC.INFERRED);

		if (conceptInferred == null) {
			LOGGER.info("Concept {} not found in inferred view.", conceptSCTID);
			return;
		}
		LOGGER.info(conceptInferred + " - Inferred IS A Hierarchy: ");
		Iterable<Concept> parents = conceptInferred.listParents(new LinkedHashSet<>());
		for (Concept thisParent : parents) {
			LOGGER.info(thisParent.toString());
		}
		LOGGER.info("");

		// Now find the concept in the Stated View
		Concept conceptStated = Concept.getConcept(conceptSCTID, CHARACTERISTIC.STATED);

		if (conceptStated == null) {
			LOGGER.info("Concept {} not found in stated view.", conceptSCTID);
			return;
		}

		TreeSet<Relationship> statedRels = conceptStated.getAttributes();

		LOGGER.info(conceptSCTID.toString() + " - attributes stated view: ");
		boolean addStar = false;
		for (Relationship thisRelationship : statedRels) {
			addStar = thisRelationship.needsReplaced();
			LOGGER.info(thisRelationship.toString(addStar));

		}

		TreeSet<Relationship> inferredRels = conceptInferred.getAttributes();
		LOGGER.info(conceptSCTID + " - attributes inferred view: ");
		for (Relationship thisRelationship : inferredRels) {
			addStar = thisRelationship.isReplacement();
			LOGGER.info(thisRelationship.toString(addStar));
		}
	}

	private void loadRelationships() throws Exception {

		LOGGER.debug("Loading Stated File: {}", statedFile);
		statedRelationships = loadFile(statedFile, Relationship.CHARACTERISTIC.STATED, false); // Not optional

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		inferredRelationships = loadFile(inferredFile, Relationship.CHARACTERISTIC.INFERRED, false);

		LOGGER.debug("Loading Additional File: {}", additionalFile);
		additionalRelationships = loadFile(additionalFile, Relationship.CHARACTERISTIC.ADDITIONAL, true); // Optional

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

		// We'll do a first pass dry run so as to capture any replacements which we don't infact want to
		// output because they'd replace already present stated relationships leading to the same triple/group
		// being activated/inactivated in the same file - with the results being file order dependent
		outputFile(true);
		outputFile(false);

		// And lets find out about the relationships we've failed to sort
		reportFailures();
	}

	private void findReplacements()
			throws UnsupportedEncodingException {

		LOGGER.debug("Pass 1");
		// First pass, mark all stated relationships that might need replaced. Do this now so we can
		// allow potential duplicates to temporarily exist if the prior existing duplicate is also going to be changed.
		for (Relationship thisStatedRelationship : statedRelationships.values()) {
			// Does this relationship exist in the inferred file? If not, find it a replacement
			if (!inferredRelationships.containsKey(thisStatedRelationship.getUuid())) {
				thisStatedRelationship.needsReplaced(true);
			}
		}

		LOGGER.debug("Pass 2");
		for (Relationship thisStatedRelationship : statedRelationships.values()) {
			// If it's already been replaced (because it already moved as part of a group) then skip
			if (thisStatedRelationship.needsReplaced() && !thisStatedRelationship.hasReplacement()) {
				findReplacement(thisStatedRelationship);
			}
		}

		LOGGER.debug("Pass 3");// Third pass for replacements that have been removed by more confident subsequent matches
		for (Relationship thisStatedRelationship : statedRelationships.values()) {
			// If it's already been replaced (because it already moved as part of a group) then skip
			if (thisStatedRelationship.needsReplaced() && !thisStatedRelationship.hasReplacement()) {
				findReplacement(thisStatedRelationship);
			}
		}

	}

	private void findReplacement(Relationship thisStatedRelationship) throws UnsupportedEncodingException {
		boolean successfulReplacement = false;
		// Try Algorithm 1
		successfulReplacement = matchGroupPlusChildDestination(thisStatedRelationship);

		// Try Algorithm 2
		if (!successfulReplacement) {
			successfulReplacement = matchTriplesAcrossGroups(thisStatedRelationship);
		}

		// Try Algorithm 3
		if (!successfulReplacement) {
			successfulReplacement = matchChildDestinationInOtherGroups(thisStatedRelationship);
		}

		// Try Algorithm 4
		if (!successfulReplacement) {
			successfulReplacement = matchLooselyInOtherGroups(thisStatedRelationship);
		}

		// Try Algorithm ...er you can count I'm sure.
		if (!successfulReplacement) {
			successfulReplacement = matchMoreProximateTypeAndDestination(thisStatedRelationship);
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
				sRelationship.getDestinationId(), sRelationship.getGroup(), true, false);
		success = attemptReplacement(sRelationship, replacements, "Alg1", true);
		if (success)
			a1Count++;
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
		success = attemptReplacement(sRelationship, replacements, "Alg2", true);
		if (success)
			a2Count++;
		return success;
	}
	
	/*
	 * Algorithm 3 - find an inferred relationship in a similar group where the destination is the same or a more proximate child of the
	 * stated destination
	 */
	private boolean matchChildDestinationInOtherGroups(Relationship sRelationship) throws UnsupportedEncodingException {
		boolean success = false;
		//For a target group to match, all the relationship types in the stated group must
		//at least be present in the target (others may have been also added, so can't use triple hash)
		// We need to filter 'Is A' relationships out, because they don't take part in groups
		boolean filterIsAs = true;
		List<Relationship> siblingRels = sRelationship.getSourceConcept().findMatchingRelationships(sRelationship.getGroup(), filterIsAs);
		List<Long> groupTypes = new ArrayList<>();
		for (Relationship thisSibling : siblingRels) {
			groupTypes.add(thisSibling.getTypeId());
		}
		//Find groups containing those types in the Inferred Graph
		Concept sourceConceptInf = Concept.getConcept(sRelationship.getSourceId(), CHARACTERISTIC.INFERRED);
		List<Relationship> potentialGroups = sourceConceptInf.findMatchingRelationships(groupTypes);
		
		//Then this stated relationship must be present in that group as itself or a more proximate destination
		List<Relationship> replacements = new ArrayList<>();
		
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
 potentialReplacement.getDestinationConcept().hasParent(sRelationship.getDestinationConcept()))
						replacements.add(potentialReplacement);
				a3_2Count++;
			}			
		}
		success = attemptReplacement(sRelationship, replacements, "Alg3", false);
		if (success)
			a3Count++;
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
		success = attemptReplacement(sRelationship, replacements, "Alg4", false);
		if (success)
			a4Count++;

		return success;
	}

	/*
	 * Algorithm 5 - find an inferred relationship where the relationship type is a child of the stated type and if that fails, try also
	 * matching on the destination being a child of the stated destination
	 */
	private boolean matchMoreProximateTypeAndDestination(Relationship sRel) {
		boolean success = false;
		Concept sourceInferred = Concept.getConcept(sRel.getSourceId(), Relationship.CHARACTERISTIC.INFERRED);
		List<Relationship> replacements = sourceInferred.findMatchingRelationships(sRel.getTypeId(), sRel.getDestinationId(), true, true);
		success = attemptReplacement(sRel, replacements, "Alg5", false);
		if (success)
			a5Count++;

		return success;
	}

	private boolean attemptReplacement(Relationship sRel, List<Relationship> replacements, String algorithmUsed, boolean algorithmIsCertain) {
		boolean replacementMade = false;

		int attempt = 0;
		for (; replacements != null && !replacementMade && attempt < replacements.size(); attempt++) {
			Relationship potentialReplacement = replacements.get(attempt);
			if (sRel.isSafelyReplacedBy(potentialReplacement, algorithmIsCertain)) {
				sRel.setReplacement(potentialReplacement, algorithmUsed);
				replacementMade = true;
				// If we've moved group, then any group siblings should move to the same group if AT ALL POSSIBLE ie match on
				// more proximate destination and type
				if (sRel.getGroup() != potentialReplacement.getGroup()) {
					moveGroupSiblings(sRel, potentialReplacement.getGroup());
				}
			} else {
				LOGGER.warn("Avoided potentially unsafe replacement for {} - {} in {}", sRel, potentialReplacement, algorithmUsed);
			}
		}

		if (replacementMade) {
			if (replacements.size() > attempt) {
				LOGGER.warn("Found multiple potential replacements for {} in {}", sRel, algorithmUsed);
			}
		}
		return replacementMade;
	}

	/**
	 * When one relationship in a stated group moves group, try to move all it's siblings together EVEN IF that relationship has already
	 * been replaced - keeping siblings together in a group is paramount, and trumps other apparently better matches eg exact triple matches
	 * in other groups.
	 */
	private void moveGroupSiblings(Relationship sRel, int targetGroup) {
		//Find all the group siblings of this stated relationship
		List<Relationship> statedSiblings = sRel.getSourceConcept().findMatchingRelationships(sRel.getGroup(), true);
		
		//For each one that's not the current relationship, try and find an inferred match (on Type + Destination, Type + more proximate destination,
		//more proximate type + destination, both more proximate) in that same target group
		Concept inferredSource = Concept.getConcept(sRel.getSourceId(), CHARACTERISTIC.INFERRED);
		for (Relationship statedSibling : statedSiblings) {
			if (!statedSibling.equals(sRel)) {
				List<Relationship> potentialMatches = inferredSource.findMatchingRelationships(statedSibling.getTypeId(),
						statedSibling.getDestinationId(), targetGroup, true, true);
				if (potentialMatches.size() > 0) {
					// We will not "try" this replacement because group moves take priority
					statedSibling.setReplacement(potentialMatches.get(0), "AlgMGS");
				}
			}
		}
	}

	private void reportProgress() {
		long needsReplaced = 0;
		long hasBeenReplaced = 0;

		for (Relationship thisStatedRelationship : statedRelationships.values()) {

			if (thisStatedRelationship.needsReplaced()) {
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

	private Map<String, Relationship> loadFile(String filePath, Relationship.CHARACTERISTIC characteristic, boolean optional)
			throws Exception {
		// Does this file exist and not as a directory?
		File file = new File(filePath);
		Map<String, Relationship> loadedRelationships = new HashMap<>();

		if (!file.exists() || file.isDirectory()) {
			if (optional) {
				LOGGER.info("Skipping {} relationship file: {}", characteristic.toString(), filePath);
				return loadedRelationships;
			} else {
				throw new IOException("Unable to read file " + filePath);
			}
		}

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
                }

			}
		}
		return loadedRelationships;
	}

	private void outputFile(boolean dryRun) throws IOException {

		LOGGER.info("Writing file to {} with effective time {}", (dryRun? "Dry Run Only" : outputFile), outputEffectiveTime);

		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(dryRun ? NULL_OUTPUT_STREAM: new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
			// Loop through all stated relationships and disable the replaced ones
			// and output the replacements all with effective time which matches the output file

			writer.write(Relationship.HEADER_ROW);
			for (Relationship thisRelationship : statedRelationships.values()) {
				// If a stated relationship doesn't exist in the inferred file, then we need to remove it in any event.
				if (thisRelationship.needsReplaced()) {
					// If this exact triple+group has been assigned as the replacement of another relationship, then
					// we don't want to inactivate it. In fact at that point, we can suppress both lines as this
					// stated relationship will remain active
					Relationship inferredEquivalent = inferredRelationships.get(thisRelationship.getUuid());

					if (inferredEquivalent != null && inferredEquivalent.isReplacement()) {
						LOGGER.info("Suppressing inactivation of {}, as it's also a replacement for {}", thisRelationship,
								inferredEquivalent.isReplacementFor());
						inferredEquivalent.suppressOutput(true);
					} else {
						writer.write(thisRelationship.getRF2(outputEffectiveTime, Relationship.INACTIVE_FLAG,
							Relationship.CHARACTERISTIC_STATED_SCTID, LEAVE_SCTID_AS_IS));
					}
				}
				// if it has a replacement, we can write that
				if (thisRelationship.hasReplacement() && !thisRelationship.getReplacement().suppressOutput()) {
					writer.write(thisRelationship.getReplacement().getRF2(outputEffectiveTime, Relationship.ACTIVE_FLAG,
							Relationship.CHARACTERISTIC_STATED_SCTID, SET_SCTID_TO_NULL));
				}
			}

			// And output any additional relationships we may be have asked to include
			for (Relationship thisRelationship : additionalRelationships.values()) {
				writer.write(thisRelationship
						.getRF2(outputEffectiveTime, null, Relationship.CHARACTERISTIC_STATED_SCTID, SET_SCTID_TO_NULL));
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
			if (thisRelationship.needsReplaced() && !thisRelationship.hasReplacement()) {
				LOGGER.info(thisRelationship.toString());
				relationshipsReported++;
				lastRelationship = thisRelationship;
				if (relationshipsReported >= 10) {
					break;
				}
			}
		}

	}

	private void goInteractive() throws Exception {
		String sctid = "";
		try (Scanner in = new Scanner(System.in)) {
			while (!sctid.equals("quit")) {
				System.out.println("Enter source concept sctid: ");
				sctid = in.nextLine().trim();
				if (!sctid.equals("quit")) {
					outputConcept(Long.valueOf(sctid));
				}
			}
		}
	}

	private static void doHelp() {
		LOGGER.info("Usage: <stated relationship file location>  <inferred realtionship file location> <output file location>");
		System.exit(-1);
	}

}
