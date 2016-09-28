package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.Description;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;
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

	private final String conceptFile;
	private final String statedFile;
	private final String inferredFile;
	private final String descriptionFile;
	private String releaseDate;

	public GraphLoader(String conceptFile, String statedFile, String inferredFile, String descriptionFile) {
		this.conceptFile = conceptFile;
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
		this.descriptionFile = descriptionFile;
	}

	public void loadRelationships() throws Exception {
		
		releaseDate = determineReleaseDate(conceptFile);

		LOGGER.debug("Loading Concept File: {}", conceptFile);
		loadConceptFile(conceptFile);

		LOGGER.debug("Loading Stated File: {}", statedFile);
		loadRelationshipFile(statedFile, CHARACTERISTIC.STATED);

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		loadRelationshipFile(inferredFile, CHARACTERISTIC.INFERRED);

		LOGGER.debug("Loading Description File: {}", descriptionFile);
		loadDescriptionFile(descriptionFile);

		LOGGER.debug("Populating inferred hierarchy depth - inferred concept hierarchy");
		Concept hierarchyRoot = Concept.getConcept(SNOMED_ROOT_CONCEPT, CHARACTERISTIC.INFERRED);
		populateHierarchyDepth(hierarchyRoot, 0);
		
		LOGGER.debug("Populating inferred hierarchy depth - stated concept hierarchy");
		hierarchyRoot = Concept.getConcept(SNOMED_ROOT_CONCEPT, CHARACTERISTIC.STATED);
		populateHierarchyDepth(hierarchyRoot, 0);

		LOGGER.debug("Loading complete");
	}


	private String determineReleaseDate(String filePath) throws Exception {
		// Might have a date in the directory path, so trim to filename
		String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
		Pattern p = Pattern.compile("\\d{8}");
		Matcher m = p.matcher(filename);
		if (m.find()) {
			return m.group();
		}
		throw new Exception("Failed to determine release date from " + filePath);
	}

	/**
	 * Recurse hierarchy and set shortest path depth for all concepts
	 */
	private void populateHierarchyDepth(Concept startingPoint, int currentDepth) {
		startingPoint.setDepth(currentDepth);
		for (Concept child : startingPoint.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY, false)) {
			populateHierarchyDepth(child, currentDepth + 1);
		}
	}

	private void loadRelationshipFile(String filePath, CHARACTERISTIC characteristic)
			throws Exception {
		// Does this file exist and not as a directory?
		File file = getFile(filePath);

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean isFirstLine = true;
			while ((line = br.readLine()) != null) {

				if (!isFirstLine) {
					String[] lineItems = line.split(FIELD_DELIMITER);
					// Only store active relationships
					if (lineItems[REL_IDX_ACTIVE].equals(ACTIVE_FLAG)) {
						Relationship r = new Relationship(lineItems, characteristic);
						r.isActive(true);
						if (lineItems[REL_IDX_EFFECTIVETIME].equals(this.releaseDate)) {
							r.setChangedThisRelease(true);
						}
					}
				} else {
					isFirstLine = false;
					continue;
				}

			}
		}
	}
	
	private void loadConceptFile(String filePath) throws Exception {
		File file = getFile(filePath);
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] lineItems = line.split(FIELD_DELIMITER);
				// Only store active relationships
				if (lineItems[CON_IDX_ACTIVE].equals(ACTIVE_FLAG) 
					&& lineItems[CON_IDX_DEFINITIONSTATUSID].equals(FULLY_DEFINED_SCTID)) {
					Concept.addFullyDefined(lineItems[CON_IDX_ID]);
				}
			}
		}
	}
	
	private void loadDescriptionFile(String filePath) throws IOException {
		File file = getFile(filePath);
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] lineItems = line.split(FIELD_DELIMITER);
				// Only store active relationships
				if (lineItems[DES_IDX_ACTIVE].equals(ACTIVE_FLAG) && lineItems[DES_IDX_TYPEID].equals(FULLY_SPECIFIED_NAME)) {
					new Description(lineItems);
				}
			}
		}
	}

	private File getFile(String filePath) throws IOException {
		// Does this file exist and not as a directory?
		File file = new File(filePath);
		if (!file.exists() || file.isDirectory()) {
			throw new IOException("Unable to read file " + filePath);
		}
		return file;
	}


}
