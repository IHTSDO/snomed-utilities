package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Usage java -classpath /Users/Peter/code/snomed-utilities/target/snomed-utilities-1.0.10-SNAPSHOT.jar
 * org.ihtsdo.snomed.util.rf2.RelationshipProcessor
 * 
 * @author PGWilliams
 * 
 */
public class RelationshipProcessor {

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
		out("Loading Stated File: " + statedFile);
		Map<String, Relationship> statedRelationships = loadFile(statedFile, Relationship.CHARACTERISTIC.STATED);

		out("Loading Inferred File: " + inferredFile);
		Map<String, Relationship> inferredRelationships = loadFile(inferredFile, Relationship.CHARACTERISTIC.INFERRED);

		// Now for all the active stated relationships that don't exist as active rows in the inferred file,
		// find a suitable replacement

	}

	private Map<String, Relationship> loadFile(String filePath, Relationship.CHARACTERISTIC characteristic) throws Exception {
		// Does this file exist and not as a directory?
		File file = new File(filePath);
		if (file.exists() || file.isDirectory()) {
			throw new Exception("Unable to read file " + filePath);
		}
		Map<String, Relationship> loadedRelationships = new HashMap<String, Relationship>();

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				Relationship r = new Relationship(line, characteristic);

				if (r.isActive()) {
					loadedRelationships.put(r.getUuid(), r);
				}
			}
		}
		return loadedRelationships;
	}

	public static void out(String msg) {
		System.out.println(msg);
	}

	private static void doHelp() {
		out("Usage: <stated relationship file location>  <inferred realtionship file location> <output file location>");
		System.exit(-1);

	}

}
