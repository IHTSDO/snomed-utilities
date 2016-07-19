package org.ihtsdo.snomed.util.mrcm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.ihtsdo.snomed.util.pojo.*;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;

public class AdHocQueries {
	
	File reportFile;
	static final String QUOTE = "\"";
	static final String QUOTE_COMMA = "\",";
	static final String QUOTE_COMMA_QUOTE = "\",\"";
	
	public AdHocQueries(String fileName) throws IOException {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String reportFilename = fileName + "_" + df.format(new Date()) + ".csv";
		reportFile = new File(reportFilename);
		reportFile.createNewFile();
		println ("Outputting Report to " + reportFile.getAbsolutePath());
	}

	public void conceptsWithStatedFDParent(long hierarchySCTID,
			CHARACTERISTIC currentView) {
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

}
