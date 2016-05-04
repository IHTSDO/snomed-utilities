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
import org.ihtsdo.snomed.util.pojo.RF1Relationship;
import org.ihtsdo.snomed.util.pojo.Relationship;
import org.ihtsdo.snomed.util.rf2.schema.RF1SchemaConstants;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage java -classpath /Users/Peter/code/snomed-utilities/target/snomed-utilities-1.0.10-SNAPSHOT.jar
 * org.ihtsdo.snomed.util.GraphLoader
 * 
 * @author PGWilliams
 * 
 */
public class GraphLoader implements RF2SchemaConstants, RF1SchemaConstants {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphLoader.class);
	private static GraphLoader singletonGraphLoader = null;
	
	private final String conceptFile;
	private final String statedFile;
	private final String inferredFile;
	private final String descriptionFile;
	private final String qualifyingFile;
	private String releaseDate;

	private final Long SNOMED_ROOT_CONCEPT = 138875005L;
	private final String ADDITIONAL_RELATIONSHIP = "900000000000227009";


	private Map<String, Relationship> statedRelationships;
	private Map<String, Relationship> inferredRelationships;
	private Map<String, RF1Relationship> qualifyingRelationships;

	private GraphLoader(String conceptFile, String statedFile, String inferredFile, String descriptionFile, String qualifyingFile) {
		this.conceptFile = conceptFile;
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
		this.descriptionFile = descriptionFile;
		this.qualifyingFile = qualifyingFile;
	}
	
	public static void createGraphLoader(String conceptFile, String statedFile, String inferredFile, String descriptionFile, String qualifyingFile) {
		if (singletonGraphLoader == null) {
			singletonGraphLoader = new GraphLoader(conceptFile, statedFile, inferredFile, descriptionFile, qualifyingFile);
		}
	}
	
	public static GraphLoader get() {
		return singletonGraphLoader;
	}

	public void loadRelationships() throws Exception {
		
		releaseDate = determineReleaseDate(conceptFile);

		LOGGER.debug("Loading Concept File: {}", conceptFile);
		loadConceptFile(conceptFile);

		LOGGER.debug("Loading Stated File: {}", statedFile);
		statedRelationships = loadRelationshipFile(statedFile, CHARACTERISTIC.STATED);

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		inferredRelationships = loadRelationshipFile(inferredFile, CHARACTERISTIC.INFERRED);

		LOGGER.debug("Loading Description File: {}", descriptionFile);
		loadDescriptionFile(descriptionFile);
		
		//WE'll add Qualifying Relationships into the Inferred Hierarchy, since they don't 
		//define a connected graph on their own
		LOGGER.debug("Loading Qualifying File: {}", qualifyingFile);
		qualifyingRelationships = loadRF1RelationshipFile(qualifyingFile, CHARACTERISTIC.INFERRED);

		LOGGER.debug("Populating inferred hierarchy depth");
		Concept hierarchyRoot = Concept.getConcept(SNOMED_ROOT_CONCEPT, CHARACTERISTIC.INFERRED);
		populateHierarchyDepth(hierarchyRoot, 0);

		LOGGER.debug("Loading complete");
	}
	
	
	public Map<String, Relationship> getRelationships(CHARACTERISTIC characteristic) throws Exception {
		switch (characteristic) {
		case STATED: return statedRelationships;
		case INFERRED: return inferredRelationships;
		default: 
			throw new Exception ("Unsupported RF2 Characteristic: " + characteristic);
		}
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

	private Map<String, Relationship> loadRelationshipFile(String filePath, CHARACTERISTIC characteristic)
			throws Exception {
		// Does this file exist and not as a directory?
		File file = getFile(filePath);
		Map<String, Relationship> loadedRelationships = new HashMap<String, Relationship>();

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean isFirstLine = true;
			while ((line = br.readLine()) != null) {
				if (!isFirstLine) {
					String[] lineItems = line.split(FIELD_DELIMITER);
					// Only store active relationships
					if (lineItems[REL_IDX_ACTIVE].equals(ACTIVE_FLAG)
							&& !lineItems[REL_IDX_CHARACTERISTICTYPEID].equals(ADDITIONAL_RELATIONSHIP)) {
						Relationship r = new Relationship(lineItems, characteristic);
						loadedRelationships.put(r.getUuid(), r);
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
		return loadedRelationships;
	}
	
	private Map<String, RF1Relationship> loadRF1RelationshipFile(String filePath, CHARACTERISTIC characteristic)
			throws Exception {
		// Does this file exist and not as a directory?
		File file = getFile(filePath);
		Map<String, RF1Relationship> loadedRelationships = new HashMap<String, RF1Relationship>();
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			boolean isFirstLine = true;
			while ((line = br.readLine()) != null) {
				if (!isFirstLine) {
					String[] lineItems = line.split(FIELD_DELIMITER);
					// All rows are active in RF1
					if (lineItems[RF1_REL_IDX_CHARACTERISTICTYPE].equals(RF1_CHARACTERISTIC_TYPE_QUALIFIER)) {
						RF1Relationship r = new RF1Relationship(lineItems, characteristic);
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

	public Map<String, RF1Relationship> getRF1Relationships(CHARACTERISTIC c) {
		return qualifyingRelationships;
	}


}
