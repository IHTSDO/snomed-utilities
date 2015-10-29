package org.ihtsdo.snomed.util.mrcm;

import java.util.Scanner;

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
					case "e":
						EquivalencyChecker.detectEquivalencies();
						break;
					case "m":
						printn("Enter SCTID to process: ");
						String sctid = in.nextLine().trim();
						new MrcmBuilder().determineMRCM(sctid, CHARACTERISTIC.INFERRED);
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
		print("e - check for equivalencies");
		print("m - get mrcm for children of concept");
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
