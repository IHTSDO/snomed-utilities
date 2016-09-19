package org.ihtsdo.snomed.util.rf2.refset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.ihtsdo.snomed.util.SnomedUtilException;
import org.ihtsdo.snomed.util.SnomedUtils;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

/**
 * Takes a flat file of Concept SCTIDs and converts
 * that to SimpleRefset
 * with format: id	effectiveTime	active	moduleId	refsetId	referencedComponentId
 * Generate input file with:
 * cat LateralityReferenceJuly2016.txt | awk -F $'\t' '$3=="Y"' | cut -f1 > LateralityReferenceJuly2016_sctidOnly.txt
 */
public class TextToSimpleRefset implements RF2SchemaConstants{
	
	String refsetId;
	File inputFile;
	File outputFile;
	String outputFilenameRoot = "der2_Refset_SimpleDelta_INT_";
	String effectiveDate;
	String moduleId = "900000000000207008";  //Default to International Edition
	String[] simpleRefsetHeader = new String[] { "id","effectiveTime","active","moduleId","refsetId","referencedComponentId"};
	
	public static void main (String[] args) throws SnomedUtilException, FileNotFoundException, IOException{
		TextToSimpleRefset app = new TextToSimpleRefset();
		app.init(args);
		app.processFile();
	}
	
	private void processFile() throws FileNotFoundException, IOException, SnomedUtilException {
		writeToRF2File(outputFile, simpleRefsetHeader);
		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			String sctidStr;
			while ((sctidStr = br.readLine()) != null) {
				//For now, just expecting a single concept sctid on each line
				//For concept SCTID
				String errMsg = SnomedUtils.isValid(sctidStr, PartionIdentifier.CONCEPT);
				if (errMsg != null) {
					throw new SnomedUtilException(sctidStr + " is not valid: " + errMsg);
				}
				String uuid = UUID.randomUUID().toString();
				String[] columns = new String[] { uuid, effectiveDate, "1",moduleId,refsetId,sctidStr};
				writeToRF2File(outputFile, columns);
			}
		}
		print ("Process complete.  See output file " + outputFile.getAbsolutePath());
	}

	public static void print (String msg) {
		System.out.println (msg);
	}
	
	protected void init(String[] args) throws SnomedUtilException {
		
		if (args.length < 3) {
			print("Usage: java TextToSimpleRefset -r <refsetId> -e <effectiveDate> [-m <moduleId>] <concept file Location>");
			System.exit(-1);
		}
	
		for (int x=0; x < args.length; x++) {
			if (args[x].equals("-r")) {
				x++;
				refsetId = args[x];
			} else if (args[x].equals("-e")) {
				x++;
				effectiveDate = args[x];
				outputFile = new File (outputFilenameRoot + effectiveDate + ".txt");
				int fileNameModifier = 0;
				while (outputFile.exists()) {
					outputFile = new File (outputFilenameRoot + effectiveDate + "_" + (++fileNameModifier) + ".txt");
				}
			} else if (args[x].equals("-m")) {
				x++;
				moduleId = args[x];
			} else{
				File possibleFile = new File(args[x]);
				if (possibleFile.exists() && !possibleFile.isDirectory() && possibleFile.canRead()) {
					inputFile = possibleFile;
				}
			}
		}
		
		if (inputFile == null) {
			throw new SnomedUtilException ("Did not determine valid input file from command line parameters");
		}
		
		if (effectiveDate == null) {
			throw new SnomedUtilException ("Did not determine effective date from command line parameters");
		}
	}
	
	protected void writeToRF2File(File outputFile, String[] columns) {
		try(	OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outputFile, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				PrintWriter out = new PrintWriter(bw))
		{
			StringBuffer line = new StringBuffer();
			for (int x=0; x<columns.length; x++) {
				if (x > 0) {
					line.append(FIELD_DELIMITER);
				}
				line.append(columns[x]);
			}
			out.print(line.toString() + LINE_DELIMITER);
		} catch (Exception e) {
			print ("Unable to output report rf2 line due to " + e.getMessage());
		}
	}
}
