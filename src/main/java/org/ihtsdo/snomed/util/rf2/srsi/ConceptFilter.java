package org.ihtsdo.snomed.util.rf2.srsi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.TreeSet;

/**
 * Usage: java -classpath /Users/Peter/code/snomed-utilities/target/snomed-utilities-1.0.10-SNAPSHOT.jar
 * org.ihtsdo.snomed.util.rf2.ConceptFilter sct2_Concept_Snapshot_INT_20150731.txt sct2_Description_Snapshot-en_INT_20150731.txt 4
 * filtered.out
 * 
 * @author PGWilliams
 */
public class ConceptFilter {

	public static int IDX_SCTID = 0;

	public static final String FIELD_DELIMITER = "\t";
	public static final String LINE_TERMINATOR = "\r\n";

	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			doHelp();
		}

		File componentFile = new File(args[0]);
		File fileToFilter = new File(args[1]);
		int filterColumn = Integer.parseInt(args[2]);
		File outputFile = new File(args[3]);

		// Read through the concept file and extract a list of SCTIDs
		out("Loading SCTIDs from 1st column of: " + componentFile.getName());
		Set<String> conceptSCTIDs = loadComponents(componentFile);

		// Filter the file based on values obtained
		out("Filtering file: " + fileToFilter.getName());
		filterFile(fileToFilter, filterColumn, conceptSCTIDs, outputFile);

	}

	private static void filterFile(File fileToFilter, int filterColumn, Set conceptSCTIDs, File outputFile) throws FileNotFoundException,
			IOException {
		FileOutputStream fos = new FileOutputStream(outputFile);

		try (BufferedReader br = new BufferedReader(new FileReader(fileToFilter));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos))) {
			String line;
			boolean isFirstLine = true;
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(FIELD_DELIMITER);
				// Is this concept one of the ones we loaded? Output this line if so
				if (isFirstLine || conceptSCTIDs.contains(columns[filterColumn])) {
					bw.write(line);
					bw.write(LINE_TERMINATOR);
				}
				isFirstLine = false;
			}
		}

	}

	private static Set<String> loadComponents(File conceptFile) throws IOException {
		Set<String> loadedComponents = new TreeSet<String>();

		try (BufferedReader br = new BufferedReader(new FileReader(conceptFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(FIELD_DELIMITER);
				loadedComponents.add(columns[IDX_SCTID]);
			}
		}
		return loadedComponents;
	}

	public static void out(String msg) {
		System.out.println(msg);
	}

	private static void doHelp() {

		out("Usage: <Component file location>  <file to filter location> <zero-based column index for concept match> <output file location>");
		System.exit(-1);

	}

}
