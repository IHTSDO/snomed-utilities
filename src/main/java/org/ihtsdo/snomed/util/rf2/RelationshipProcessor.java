package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
				Relationship r;
				if (!isFirstLine) {
					r = new Relationship(line, characteristic);
				} else {
					isFirstLine = false;
					continue;
				}

				if (r.isActive()) {
					loadedRelationships.put(r.getUuid(), r);
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
