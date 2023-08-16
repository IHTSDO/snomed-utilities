package org.ihtsdo.snomed.util.qa;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.ihtsdo.util.GlobalUtils.print;

import org.ihtsdo.snomed.util.SnomedUtilException;
import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.pojo.Description;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.ihtsdo.util.GlobalUtils;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class PatternCollator {

	private final File patternDir;
	private final File outputDir;
	private static final String JSON = ".json";
	private final String outputFileName;
	private final String[] headers = new String[] {"PatternName","Concept","FSN (commas stripped)","hasChanged","isNew","Hierarchy1","Hierarchy2","Hierarchy3"};
	private int duplicatesRemoved = 0;
	
	public PatternCollator (String patternDir, String outputDir) {
		this.patternDir = new File(patternDir);
		this.outputDir = new File (outputDir);
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
		this.outputFileName = this.outputDir + File.separator + "qa_patterns_" + df.format(new Date())  + ".csv";

	}
	
	public void collatePatternOutput () throws SnomedUtilException, IOException {
		if (!patternDir.isDirectory()) {
			throw new SnomedUtilException ("Unable to read from pattern directory: " + patternDir.getAbsolutePath());
		}
		if (!outputDir.isDirectory()) {
			throw new SnomedUtilException ("Unable to secure output directory: " + outputDir.getAbsolutePath());
		}
		GlobalUtils.outputToFile(outputFileName, headers, ",", true);
		for (File file : patternDir.listFiles()) {
			if (file.isDirectory() || !file.canRead() || !file.getName().endsWith(JSON)) {
				print("Skipping " + file.getName());
			} else {
				try {
					collateFile(file);
				} catch (Exception e) {
					throw new SnomedUtilException("Failure while processing " + file.getName(),e);
				}
			}
		}
		print ("Process complete.  Skipped " + duplicatesRemoved + " duplicates");
	}

	private void collateFile(File file) throws JsonSyntaxException, JsonIOException, IOException {
		Set<String> conceptsSeen = new HashSet<>();
		String pattern = file.getName().replace(JSON, "").replace("_", " ");
		print ("Processing " + pattern);
		
		Gson gson = new Gson();
		PatternResult[] concepts = gson.fromJson(new FileReader(file), PatternResult[].class);
		print ("Detected " + concepts.length + " concepts that match " + pattern);
		for (PatternResult concept : concepts) {
			String conceptId = concept.getConceptId();
			if (conceptsSeen.contains(conceptId)) {
				print ("Already seen " + pattern + " - " + conceptId);
				duplicatesRemoved++;
			} else {
				String[] hierarchy = getHierarchyPath(conceptId);
				String[] lineItems = new String[] { pattern,
													conceptId,
													concept.getTerm().replace(",", ""),
													concept.getChanged(),
													concept.getIsNew(),
													hierarchy[0],
													hierarchy[1],
													hierarchy[2]};
				GlobalUtils.outputToFile(outputFileName, lineItems, ",", true);
				conceptsSeen.add(conceptId);
			}
		}
	}

	private String[] getHierarchyPath(String conceptId) {
		String[] hierarchy = new String[] {"","",""};
		Long sctid = Long.parseLong(conceptId);
		Concept c = Concept.getConcept(sctid, CHARACTERISTIC.STATED);
		Set<Concept> ancestors = c.getAncestors(Concept.DEPTH_NOT_SET);
		for (int x=0; x < 3 ; x++) {
			for (Concept ancestor : ancestors) {
				if (ancestor.getDepth() == (x+1)) {
					hierarchy[x] += Description.getFormattedConcept(ancestor.getSctId());
				}
			}
		}
		return hierarchy;
	}
	

}
