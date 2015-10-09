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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.output.NullOutputStream;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;
import org.ihtsdo.snomed.util.rf2.srsi.Relationship.CHARACTERISTIC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage java -classpath /Users/Peter/code/snomed-utilities/target/snomed-utilities-1.0.10-SNAPSHOT.jar
 * org.ihtsdo.snomed.util.GraphLoader
 * 
 * @author PGWilliams
 * 
 */
public class GraphLoader implements RF2SchemaConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphLoader.class);

	private final String statedFile;
	private final String inferredFile;

	private Map<String, Relationship> statedRelationships;
	private Map<String, Relationship> inferredRelationships;

	public GraphLoader(String statedFile, String inferredFile) {
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
	}

	public void loadRelationships() throws Exception {

		LOGGER.debug("Loading Stated File: {}", statedFile);
		statedRelationships = loadFile(statedFile, CHARACTERISTIC.STATED);

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		inferredRelationships = loadFile(inferredFile, CHARACTERISTIC.INFERRED);

		LOGGER.debug("Loading complete");
	}

	
	private Map<String, Relationship> loadFile(String filePath, CHARACTERISTIC characteristic)
			throws Exception {
		// Does this file exist and not as a directory?
		File file = new File(filePath);
		Map<String, Relationship> loadedRelationships = new HashMap<String, Relationship>();

		if (!file.exists() || file.isDirectory()) {
			throw new IOException("Unable to read file " + filePath);
		}

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean isFirstLine = true;
			while ((line = br.readLine()) != null) {

				if (!isFirstLine) {
					String[] lineItems = line.split(Relationship.FIELD_DELIMITER);
					// Only store active relationships
					if (lineItems[Relationship.REL_IDX_ACTIVE].equals(ACTIVE_FLAG)) {
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


}
