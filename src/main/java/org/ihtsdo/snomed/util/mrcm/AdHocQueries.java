package org.ihtsdo.snomed.util.mrcm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.ihtsdo.snomed.util.IdGenerator;
import org.ihtsdo.snomed.util.SnomedUtilException;
import org.ihtsdo.snomed.util.SnomedUtils;
import org.ihtsdo.snomed.util.pojo.*;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

public class AdHocQueries implements RF2SchemaConstants{
	
	File reportFile;
	static final String QUOTE = "\"";
	static final String QUOTE_COMMA = "\",";
	static final String QUOTE_COMMA_QUOTE = "\",\"";
	static final String COMMA_QUOTE = ",\"";
	
	public AdHocQueries(String fileName) throws IOException {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String reportFilename = fileName + "_" + df.format(new Date()) + ".csv";
		reportFile = new File(reportFilename);
		reportFile.createNewFile();
		println ("Outputting Report to " + reportFile.getAbsolutePath());
	}

	public void conceptsWithStatedFDParent(long hierarchySCTID) {
		Concept top = Concept.getConcept(hierarchySCTID, CHARACTERISTIC.INFERRED);
		Set<Concept> descendents = top.getDescendents(Concept.DEPTH_NOT_SET, false);
		writeToFile ("SCTID, FSN, PARENT_SCTID, PARENT_FSN");
		//Now which of these concepts has a parent that is fully defined?
		nextConcept:
		for (Concept thisInferredConcept : descendents) {
			Concept thisStatedConcept = Concept.getConcept(thisInferredConcept.getSctId(), CHARACTERISTIC.STATED);
			for (Concept thisParent : thisStatedConcept.getParents()) {
				if (thisParent.isFullyDefined()) {
					outputChildParent(thisStatedConcept, thisParent);
					continue nextConcept;
				}
			}
		}
	}
	
	private void outputChildParent(Concept child, Concept parent) {
		String line = QUOTE + child.getSctId() + QUOTE_COMMA_QUOTE
				+ Description.getDescription(child) + QUOTE_COMMA_QUOTE
				+ parent.getSctId() + QUOTE_COMMA_QUOTE
				+ Description.getDescription(parent) + QUOTE;
		writeToFile(line);
	}

	protected void writeToFile(String line) {
		try(FileWriter fw = new FileWriter(reportFile, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.println(line);
		} catch (Exception e) {
			println ("Unable to output report line: " + line + " due to " + e.getMessage());
		}
	}

	private void println(String msg) {
		System.out.println(msg);
	}

	private void print(String msg) {
		System.out.print(msg);
	}

	/**
	 * Find descendents of the top concept where the two attributes are present, but not grouped together.
	 */
	public void attributesNotGroupedTogether(long hierarchySCTID,
			long attribute1, long attribute2, CHARACTERISTIC currentView) {
		Concept top = Concept.getConcept(hierarchySCTID, CHARACTERISTIC.INFERRED);
		Set<Concept> descendents = top.getDescendents(Concept.DEPTH_NOT_SET, false);
		long[] attributes = new long[] { attribute1, attribute2};
		writeToFile ("SCTID, GROUP, DEF, FSN");
		nextConcept:
		for (Concept thisInferredConcept : descendents) {
			//We work with inferred hierarchy for working out what things are, but switch
			//to current view when considering groups and attributes
			Concept thisConcept = Concept.getConcept(thisInferredConcept.getSctId(), currentView);
			if (conceptHasAttributes(thisConcept, attributes)) {
				//Loop through the groups and see if attribute1 exists without attribute2
				for (RelationshipGroup thisGroup : thisConcept.getGroups()) {
					//How many of our target attributes are in this group?
					int found = 0;
					for (Relationship thisAttribute : thisGroup.getAttributes()) {
						if (thisAttribute.getTypeId().equals(attribute1) || thisAttribute.getTypeId().equals(attribute2)) {
							found++;
						}
					}
					//If we have one but not the other in this group, output
					if (found == 1) {
						outputConcept(thisConcept, thisGroup.getNumber());
						continue nextConcept;
					}
				}
			}
		}
		
	}

	private void outputConcept(Concept concept, int groupNum) {
		String line = QUOTE + concept.getSctId() + QUOTE_COMMA + groupNum
				+ COMMA_QUOTE + (concept.isFullyDefined()?"FD":"P") + QUOTE_COMMA_QUOTE
				+ Description.getDescription(concept) + QUOTE;
		writeToFile(line);
		
	}

	private boolean conceptHasAttributes(Concept concept, long[] attributes) {
		boolean hasAllAttributes = true;
		for (long thisRequiredAttribute : attributes) {
			boolean attributeFound = false;
			for (Relationship thisAttribute : concept.getAllAttributes()) {
				if (thisAttribute.getTypeId().equals(thisRequiredAttribute)) {
					attributeFound = true;
				}
			}
			if (!attributeFound) {
				hasAllAttributes = false;
			}
		}
		return hasAllAttributes;
	}

	public void generateStatedPartOfs(String sctidFilePath, String effectiveTime) throws SnomedUtilException, IOException {
		//Find all concepts but remove any that are descendants of Body Structure
		Concept top = Concept.getConcept(SNOMED_ROOT_CONCEPT, CHARACTERISTIC.INFERRED);
		Set<Concept> ontology = top.getDescendents(Concept.DEPTH_NOT_SET, false);
		long partOfAttributeSCTID = 123005000L;
		IdGenerator idGen = IdGenerator.initiateIdGenerator(sctidFilePath);
		String fileDate = new SimpleDateFormat("yyyyMMdd").format(new Date());
		if (!effectiveTime.isEmpty()) {
			fileDate = effectiveTime;
		}
		String outputFileName = "sct2_StatedRelationship_Delta_INT_" + fileDate + ".txt";
		
		//Output any PART OF relationships as a Stated File with no effective Time
		//Part Ofs are always in group 0 so we can speed up processing by just checking that group
		println ("Examining " + ontology.size() + " concepts");
		for (Concept c : ontology) {
			for (Relationship r : c.getGroup(0)) {
				if (r.getTypeId().longValue() == partOfAttributeSCTID && r.isActive()) {
					//Any concept that has "Structure" in the FSN (other than in the semantic tag)
					//gets filtered out
					String fsn = Description.getDescription(r.getSourceConcept());
					String [] fsnParts = SnomedUtils.deconstructFSN(fsn);
					if (fsnParts[0].toLowerCase().contains("structure")) {
						println ("No " + r.toPrettyString(true));
					} else {
						println ("Yes " + r.toPrettyString(true));
						outputStatedPartOf(outputFileName,r, idGen, effectiveTime);
					}
				}
			}
		}
	}

	private void outputStatedPartOf(String outputFileName, Relationship r, IdGenerator idGen, String effectiveTime) throws IOException, SnomedUtilException {
		//Recover the columns of the relationship in the inferred form and tweak for stated
		String[] columns = r.toRF2();
		//New relationships so give a new ID and set the effective time to blank
		columns[REL_IDX_ID] = idGen.getSCTID(PartionIdentifier.RELATIONSHIP);
		columns[REL_IDX_EFFECTIVETIME] = effectiveTime;
		columns[REL_IDX_CHARACTERISTICTYPEID] = SCTID_STATED_RELATIONSHIP;
		writeToRF2File(outputFileName, columns);
	}
	
	protected void writeToRF2File(String fileName, String[] columns) throws IOException {
		File file = ensureFileExists(fileName);
		try(	OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				PrintWriter out = new PrintWriter(bw))
		{
			StringBuffer line = new StringBuffer();
			for (int x=0; x<columns.length; x++) {
				if (x > 0) {
					line.append(TSV_FIELD_DELIMITER);
				}
				line.append(columns[x]==null?"":columns[x]);
			}
			out.print(line.toString() + LINE_DELIMITER);
		} catch (Exception e) {
			println ("Unable to output report rf2 line due to " + e.getMessage());
		}
	}
	
	protected File ensureFileExists(String fileName) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
		}
		return file;
	}

}
