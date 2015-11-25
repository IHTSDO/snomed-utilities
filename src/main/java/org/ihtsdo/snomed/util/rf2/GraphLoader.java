package org.ihtsdo.snomed.util.rf2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

	private final Long SNOMED_ROOT_CONCEPT = 138875005L;
	private final String ADDITIONAL_RELATIONSHIP = "900000000000227009";


	private Map<String, Relationship> statedRelationships;
	private Map<String, Relationship> inferredRelationships;

	public GraphLoader(String conceptFile, String statedFile, String inferredFile, String descriptionFile) {
		this.conceptFile = conceptFile;
		this.statedFile = statedFile;
		this.inferredFile = inferredFile;
		this.descriptionFile = descriptionFile;
	}

	public void loadRelationships() throws Exception {
		
		LOGGER.debug("Loading Concept File: {}", conceptFile);
		loadConceptFile(conceptFile);

		LOGGER.debug("Loading Stated File: {}", statedFile);
		statedRelationships = loadRelationshipFile(statedFile, CHARACTERISTIC.STATED);

		LOGGER.debug("Loading Inferred File: {}", inferredFile);
		inferredRelationships = loadRelationshipFile(inferredFile, CHARACTERISTIC.INFERRED);

		LOGGER.debug("Loading Description File: {}", descriptionFile);
		loadDescriptionFile(descriptionFile);

		LOGGER.debug("Populating inferred hierarchy depth");
		Concept hierarchyRoot = Concept.getConcept(SNOMED_ROOT_CONCEPT, CHARACTERISTIC.INFERRED);
		populateHierarchyDepth(hierarchyRoot, 0);

		LOGGER.debug("Loading complete");
	}


	/**
	 * Recurse hierarchy and set shortest path depth for all concepts
	 */
	private void populateHierarchyDepth(Concept startingPoint, int currentDepth) {
		startingPoint.setDepth(currentDepth);
		for (Concept child : startingPoint.getDescendents(Concept.IMMEDIATE_CHILDREN_ONLY)) {
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


}
