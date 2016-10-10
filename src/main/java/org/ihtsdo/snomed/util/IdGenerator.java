package org.ihtsdo.snomed.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.VerhoeffCheckDigit;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;

public class IdGenerator implements RF2SchemaConstants{
	private String fileName;
	private BufferedReader availableSctIds;
	private int dummySequence = 100;
	private boolean useDummySequence = false;
	int idsAssigned = 0;
	
	public static IdGenerator initiateIdGenerator(String sctidFilename) throws SnomedUtilException {
		if (sctidFilename.equals("dummy")) {
			return new IdGenerator();
		}
		
		File sctIdFile = new File (sctidFilename);
		try {
			if (sctIdFile.canRead()) {
				return new IdGenerator(sctIdFile);
			}
		} catch (Exception e) {}
		
		throw new SnomedUtilException("Unable to read sctids from " + sctidFilename);
	}
	private IdGenerator(File sctidFile) throws FileNotFoundException {
		fileName = sctidFile.getAbsolutePath();
		availableSctIds = new BufferedReader(new FileReader(sctidFile));
	}
	private IdGenerator() {
		useDummySequence = true;
	}
	
	public String getSCTID(PartionIdentifier partitionIdentifier) throws IOException, SnomedUtilException {
		if (useDummySequence) {
			idsAssigned++;
			return getDummySCTID(partitionIdentifier);
		}
		
		String sctId;
		try {
			sctId = availableSctIds.readLine();
		} catch (IOException e) {
			throw new SnomedUtilException("Unable to recover SCTID from file " + fileName);
		}
		if (sctId == null || sctId.isEmpty()) {
			throw new SnomedUtilException("No more SCTIDs in file " + fileName + " need more than " + idsAssigned);
		}
		//Check the SCTID is valid, and belongs to the correct partition
		SnomedUtils.isValid(sctId, partitionIdentifier, true);  //throw exception if not valid
		idsAssigned++;
		return sctId;
	}
	private String getDummySCTID(PartionIdentifier partitionIdentifier) throws SnomedUtilException  {
		try {
			String sctIdBase = ++dummySequence + "0" + partitionIdentifier.ordinal();
			String checkDigit = new VerhoeffCheckDigit().calculate(sctIdBase);
			return sctIdBase + checkDigit;
		} catch (CheckDigitException e) {
			throw new SnomedUtilException ("Failed to generate dummy sctid",e);
		}
	}
	public String finish() {
		try {
			availableSctIds.close();
		} catch (Exception e){}
		
		return "IdGenerator supplied " + idsAssigned + " sctids.";
		
	}
}
