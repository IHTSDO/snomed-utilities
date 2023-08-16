package org.ihtsdo.snomed.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.snomed.util.rf2.schema.RF2SchemaConstants;
import org.ihtsdo.util.GlobalUtils;

public class Rf2ArchiveBuilder implements RF2SchemaConstants {
	
	String outputDirName = "output";
	String effectiveTime;
	String packageDir;
	
	public String conDeltaFilename;
	public String relDeltaFilename;
	public String sRelDeltaFilename;
	public String descDeltaFilename;
	public String langDeltaFilename;
	
	String[] conHeader = new String[] {"id","effectiveTime","active","moduleId","definitionStatusId"};
	String[] descHeader = new String[] {"id","effectiveTime","active","moduleId","conceptId","languageCode","typeId","term","caseSignificanceId"};
	String[] relHeader = new String[] {"id","effectiveTime","active","moduleId","sourceId","destinationId","relationshipGroup","typeId","characteristicTypeId","modifierId"};
	String[] langHeader = new String[] {"id","effectiveTime","active","moduleId","refsetId","referencedComponentId","acceptabilityId"};
	String[] dummyLangLine = new String[] { "549bdb3b-04f8-5da0-8890-8e9e6327d56","20080731","1","900000000000207008","900000000000509007","2754621019","900000000000548007"};
	
	public Rf2ArchiveBuilder(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}
	
	public String getFilename(RF2_FILE rf2File) throws SnomedUtilException {
        return switch (rf2File) {
            case CONCEPT -> conDeltaFilename;
            case STATED -> sRelDeltaFilename;
            case INFERRED -> relDeltaFilename;
            case LANGUAGE -> langDeltaFilename;
            case DESCRIPTION -> descDeltaFilename;
            default -> throw new SnomedUtilException("Unknown RF2 File: " + rf2File);
        };
	}
	
	public void init() throws IOException, SnomedUtilException {
		File outputDir = new File (outputDirName);
		int increment = 0;
		while (outputDir.exists()) {
			outputDirName = outputDirName + "_" + (++increment) ;
			outputDir = new File(outputDirName);
		}
		String packageRoot = outputDirName + File.separator + "SnomedCT_RF2Release_INT_";
		packageDir = packageRoot + effectiveTime + File.separator;
		GlobalUtils.print ("Outputting data to " + packageDir);
		initialiseFileHeaders();
	}
	
	private void initialiseFileHeaders() throws IOException, SnomedUtilException {
		String termDir = packageDir +"Delta/Terminology/";
		String refDir =  packageDir +"Delta/Refset/";
		conDeltaFilename = termDir + "sct2_Concept_Delta_INT_" + effectiveTime + ".txt";
		writeToRF2File(RF2_FILE.CONCEPT, conHeader);
		
		relDeltaFilename = termDir + "sct2_Relationship_Delta_INT_" + effectiveTime + ".txt";
		writeToRF2File(RF2_FILE.INFERRED, relHeader);
		
		sRelDeltaFilename = termDir + "sct2_StatedRelationship_Delta_INT_" + effectiveTime + ".txt";
		writeToRF2File(RF2_FILE.STATED, relHeader);
		
		descDeltaFilename = termDir + "sct2_Description_Delta-en_INT_" + effectiveTime + ".txt";
		writeToRF2File(RF2_FILE.DESCRIPTION, descHeader);
		
		langDeltaFilename = refDir + "Language/der2_cRefset_LanguageDelta-en_INT_" + effectiveTime + ".txt";
		writeToRF2File(RF2_FILE.LANGUAGE, langHeader);
		//TS Import chokes if it doesn't find at least 1 line in the language refset file
		writeToRF2File(RF2_FILE.LANGUAGE,dummyLangLine);
	}
	
	public void writeToRF2File(RF2_FILE rf2File, String[] columns) throws IOException, SnomedUtilException {
		File file = ensureFileExists(getFilename(rf2File));
		try(	OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				PrintWriter out = new PrintWriter(bw))
		{
			StringBuilder line = new StringBuilder();
			for (int x=0; x<columns.length; x++) {
				if (x > 0) {
					line.append(TSV_FIELD_DELIMITER);
				}
				line.append(columns[x]==null?"":columns[x]);
			}
			out.print(line + LINE_DELIMITER);
		} catch (Exception e) {
			GlobalUtils.print("Unable to output report rf2 line due to " + e.getMessage());
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
	
	public void createArchive() throws SnomedUtilException {
		File dirToZip = new File(outputDirName);
		try {
			// The zip filename will be the name of the first thing in the zip location
			// ie in this case the directory SnomedCT_RF1Release_INT_20150731
			String zipFileName = dirToZip.listFiles()[0].getName() + ".zip";
			int fileNameModifier = 1;
			while (new File(zipFileName).exists()) {
				zipFileName = dirToZip.listFiles()[0].getName() + "_" + fileNameModifier++ + ".zip";
			}
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
			String rootLocation = dirToZip.getAbsolutePath() + File.separator;
			GlobalUtils.print("Creating archive : " + zipFileName + " from files found in " + rootLocation);
			addDir(rootLocation, dirToZip, out);
			out.close();
		} catch (IOException e) {
			throw new SnomedUtilException("Failed to create archive from " + dirToZip, e);
		} finally {
			try {
				FileUtils.deleteDirectory(dirToZip);
			} catch (IOException e) {}
		}
	}
	
	public static void addDir(String rootLocation, File dirObj, ZipOutputStream out) throws IOException {
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[1024];

        for (File file : files) {
            if (file.isDirectory()) {
                addDir(rootLocation, file, out);
                continue;
            }
            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            String relativePath = file.getAbsolutePath().substring(rootLocation.length());
            GlobalUtils.print(" Adding: " + relativePath);
            out.putNextEntry(new ZipEntry(relativePath));
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            out.closeEntry();
            in.close();
        }
	}
}
