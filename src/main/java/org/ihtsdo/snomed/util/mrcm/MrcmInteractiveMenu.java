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
	
	private static CHARACTERISTIC currentView = CHARACTERISTIC.INFERRED;

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
						new MrcmBuilder().determineAllLCAs(hierarchySCTID, currentView);
						break;
					case "c":
						printn("Restrict to Stated Issues Only? Y/N: ");
						boolean statedIssuesOnly = in.nextLine().trim().equalsIgnoreCase("Y") ? true : false;
						printn("New Issues Only? Y/N: ");
						boolean newIssuesOnly = in.nextLine().trim().equalsIgnoreCase("Y") ? true : false;
						new MrcmBuilder().findCrossovers(currentView, statedIssuesOnly, newIssuesOnly);
						break;
					case "d":
						printn("Enter SCTID to process: ");
						String sctid = in.nextLine().trim();
						new MrcmBuilder().determineMRCM(sctid, currentView, Concept.DEPTH_NOT_SET);
						break;
					case "e":
						EquivalencyChecker.detectEquivalencies();
						break;
					case "h":
						new MrcmBuilder().getHierarchyStats(currentView);
						break;
					case "l":
						SearchParameters sp = new SearchParameters().init(currentView, in);
						if (sp != null) {
							new MrcmBuilder().linguisticSearch(currentView, sp);
						}
						break;
					case "m":
						printn("Enter SCTID to process: ");
						sctid = in.nextLine().trim();
						new MrcmBuilder().determineMRCM(sctid, currentView, Concept.IMMEDIATE_CHILDREN_ONLY);
						break;
					case "n":
						printn("Calculate averge depth of concepts");
						new MrcmBuilder().calculateAverageDepth(currentView);
						break;
					case "p":
						new MrcmBuilder().findCrossHierarchyParents(currentView);
						break;
					case "r":
						printn("Enter Attribute Type SCTID to process: ");
						String attributeSCTID = in.nextLine().trim();
						printn("Found in which hierarchy? ");
						hierarchySCTID = in.nextLine().trim();
						new MrcmBuilder().determineValueRange(attributeSCTID, hierarchySCTID, currentView, true);
						break;
					case "s":
						printn("Enter SCTID to process: ");
						sctid = in.nextLine().trim();
						new MrcmBuilder().displayShape(sctid, currentView);
						break;
					case "v":
						currentView = (currentView == CHARACTERISTIC.INFERRED ? CHARACTERISTIC.STATED : CHARACTERISTIC.INFERRED );
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
		print("     Menu  - " + currentView + " view");
		print("---------------------------------------");
		print("a - LCA of all attribute values");
		print("c - find examples of crossovers");
		print("d - get mrcm for decendents of concept");
		print("e - check for equivalencies");
		print("h - report hierarchy stats");
		print("l - linguistic/model search");
		print("m - get mrcm for children of concept");
		print("n - get statistics on the depths of concepts");
		print("p - find instances of parents from different hiearchies");
		print("r - range (list) of attribute values in hierarchy");
		print("s - shape of concept and defined children");
		print("v - switch view (stated / inferred)");
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
