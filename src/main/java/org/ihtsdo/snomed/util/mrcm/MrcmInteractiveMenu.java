package org.ihtsdo.snomed.util.mrcm;

import java.util.Scanner;

import org.ihtsdo.snomed.util.pojo.Concept;
import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants.CHARACTERISTIC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MrcmInteractiveMenu {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphLoader.class);

	public static int mb = 1024 * 1024;

	private static void doHelp() {
		LOGGER.info("Usage: <concept file location> <stated relationship file location> <inferred realtionship file location> <description file location>");
		System.exit(-1);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			doHelp();
		}
		reportMemory();
		GraphLoader g = new GraphLoader(args[0], args[1], args[2], args[3]);
		g.loadRelationships();
		reportMemory();

		new MrcmInteractiveMenu().start();
		/*
		 * // long conceptToExamine = 128927009L; // Procedure by Method long conceptToExamine = 362958002L; // Procedure by Site // long
		 * conceptToExamine = 285579008L; //Taking swab from body site CHARACTERISTIC hierarchyToExamine = CHARACTERISTIC.INFERRED; //
		 * CHARACTERISTIC hierarchyToExamine = CHARACTERISTIC.STATED;
		 * LOGGER.info("Examining Siblings of {} in the {} hierarchy to Determine MRCM Rules...", conceptToExamine, hierarchyToExamine); //
		 * Lets start with children of "Procedure by site" Concept c = Concept.getConcept(conceptToExamine, hierarchyToExamine);
		 * determineMRCM(c);
		 */
	}

	public void start() throws NumberFormatException, Exception {

		try (Scanner in = new Scanner(System.in)) {
			while (true) {
				displayMenu();
				String functionChosen = in.nextLine().trim();
				switch (functionChosen) {
					case "a":
						printn("Look at which hierarchy? ");
						String hierarchySCTID = in.nextLine().trim();
						new MrcmBuilder().determineAllLCAs(hierarchySCTID, CHARACTERISTIC.INFERRED);
						break;
					case "c":
						printn("Restrict to Stated Issues Only? Y/N: ");
						String restriction = in.nextLine().trim();
						new MrcmBuilder().findCrossovers(CHARACTERISTIC.INFERRED, restriction.equalsIgnoreCase("Y")?true:false);
						break;
					case "d":
						printn("Enter SCTID to process: ");
						String sctid = in.nextLine().trim();
						new MrcmBuilder().determineMRCM(sctid, CHARACTERISTIC.INFERRED, Concept.DEPTH_NOT_SET);
						break;
					case "e":
						EquivalencyChecker.detectEquivalencies();
						break;
					case "m":
						printn("Enter SCTID to process: ");
						sctid = in.nextLine().trim();
						new MrcmBuilder().determineMRCM(sctid, CHARACTERISTIC.INFERRED, Concept.IMMEDIATE_CHILDREN_ONLY);
						break;
					case "n":
						printn("Calculate averge depth of concepts");
						new MrcmBuilder().calculateAverageDepth(CHARACTERISTIC.INFERRED);
						break;
					case "p":
						new MrcmBuilder().findCrossHierarchyParents(CHARACTERISTIC.INFERRED);
						break;
					case "r":
						printn("Enter Attribute Type SCTID to process: ");
						String attributeSCTID = in.nextLine().trim();
						printn("Found in which hierarchy? ");
						hierarchySCTID = in.nextLine().trim();
						new MrcmBuilder().determineValueRange(attributeSCTID, hierarchySCTID, CHARACTERISTIC.INFERRED, true);
						break;
					case "s":
						printn("Enter SCTID to process: ");
						sctid = in.nextLine().trim();
						new MrcmBuilder().displayShape(sctid, CHARACTERISTIC.INFERRED);
						break;
					case "q":
						System.exit(0);
						break;
					default:
						print("Function not recognised");
				}

			}
		}
	}

	private void displayMenu() {
		print("\n");
		print("     Menu    ");
		print("--------------");
		print("a - LCA of all attributes");
		print("c - find examples of crossovers");
		print("d - get mrcm for decendents of concept");
		print("e - check for equivalencies");
		print("m - get mrcm for children of concept");
		print("n - get statistics on the depths of concepts");
		print("p - find instances of parents from different hiearchies");
		print("r - range of attribute values");
		print("s - shape of concept and defined children");
		print("q - quit");
		printn("Choose a function: ");

	}

	private void print(String msg) {
		System.out.println(msg);
	}

	private void printn(String msg) {
		System.out.print(msg);
	}

	private static void reportMemory() {
		Runtime runtime = Runtime.getRuntime();
		LOGGER.info("Used Memory: {} Mb", (runtime.totalMemory() - runtime.freeMemory()) / mb);
		LOGGER.info("Free Memory: {} Mb", runtime.freeMemory() / mb);
	}
}
