package org.ihtsdo.snomed.util.mrcm;

import org.ihtsdo.snomed.util.rf2.GraphLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MrcmBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphLoader.class);
	public static int mb = 1024 * 1024;
	private static void doHelp() {
		LOGGER.info("Usage: <stated relationship file location> <inferred realtionship file location>");
		System.exit(-1);
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			doHelp();
		}
		reportMemory();
		GraphLoader rp = new GraphLoader(args[0], args[1]);
		rp.loadRelationships();
		reportMemory();

		LOGGER.info("Examining Siblings to Determine MRCM Rules...");
	}

	private static void reportMemory() {
		Runtime runtime = Runtime.getRuntime();
		LOGGER.info("Used Memory: {} Mb", (runtime.totalMemory() - runtime.freeMemory()) / mb);
		LOGGER.info("Free Memory: {} Mb", runtime.freeMemory() / mb);
	}

}
