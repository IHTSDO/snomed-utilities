package org.ihtsdo.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.ihtsdo.snomed.util.SnomedUtilException;

public class GlobalUtils {
	
	public static final String LINE_DELIMITER = "\r\n";
	public static final String QUOTE = "\"";
	
	public static void print(String msg) {
		System.out.println(msg);
	}
	
	public static void outputToFile(String fileName, String[] columns, String delimiter, boolean quoteFields) throws IOException {
		File file = ensureFileExists(fileName);
		try(	OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				PrintWriter out = new PrintWriter(bw))
		{
			StringBuffer line = new StringBuffer();
			for (int x=0; x<columns.length; x++) {
				if (x > 0) {
					line.append(delimiter);
				}
				line.append(quoteFields?QUOTE:"");
				line.append(columns[x]==null?"":columns[x]);
				line.append(quoteFields?QUOTE:"");
			}
			out.print(line.toString() + LINE_DELIMITER);
		} catch (Exception e) {
			print ("Unable to output to " + file.getAbsolutePath() + " due to " + e.getMessage());
		}
	}
	
	public static void outputToFile(String fileName, String line) throws IOException {
		File file = ensureFileExists(fileName);
		try(	OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				PrintWriter out = new PrintWriter(bw))
		{
			out.print(line.toString() + LINE_DELIMITER);
		} catch (Exception e) {
			print ("Unable to output to " + file.getAbsolutePath() + " due to " + e.getMessage());
		}
	}
	
	public static void outputToFile(String fileName, List<String> lines) throws IOException {
		File file = ensureFileExists(fileName);
		try(	OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
				BufferedWriter bw = new BufferedWriter(osw);
				PrintWriter out = new PrintWriter(bw))
		{
			for (String line : lines) {
				out.print(line.toString() + LINE_DELIMITER);
			}
		} catch (Exception e) {
			print ("Unable to output to " + file.getAbsolutePath() + " due to " + e.getMessage());
		}
	}


	public static File ensureFileExists(String fileName) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			if (file.getParentFile() != null) {
				file.getParentFile().mkdirs();
			} 
			file.createNewFile();
		}
		return file;
	}
	
	/**
	 * @return an array of 3 elements containing:  The path, the filename, the file extension (if it exists) or empty strings
	 */
	public static String[] deconstructFilename(File file) {
		String[] parts = new String[] {"","",""};
		
		if (file== null) {
			return parts;
		}
		parts[0] = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
		if (file.getName().lastIndexOf(".") > 0) {
			parts[1] = file.getName().substring(0, file.getName().lastIndexOf("."));
			parts[2] = file.getName().substring(file.getName().lastIndexOf(".") + 1);
		} else {
			parts[1] = file.getName();
		}
		
		return parts;
	}
	
	public static void createArchive(File dirToZip, String zipFileOrigName) throws SnomedUtilException {
		try {
			// The zip filename will be the name of the first thing in the zip location
			// ie in this case the directory SnomedCT_RF1Release_INT_20150731
			String zipFileName = getZipFilename(dirToZip, zipFileOrigName);
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
			String rootLocation = dirToZip.getAbsolutePath() + File.separator;
			print("Creating archive : " + zipFileName + " from files found in " + rootLocation);
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
	
	private static String getZipFilename(File dirToZip, String zipFileOrigName) {
		String zipFileName = null;
		if (zipFileOrigName == null || zipFileOrigName.isEmpty()) {
			zipFileOrigName = dirToZip.listFiles()[0].getName();
		}
		zipFileName = zipFileOrigName + ".zip";
		int fileNameModifier = 1;
		while (new File(zipFileName).exists()) {
			zipFileName = zipFileOrigName + "_" + fileNameModifier++ + ".zip";
		}
		return zipFileName;
	}

	public static void addDir(String rootLocation, File dirObj, ZipOutputStream out) throws IOException {
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[1024];

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				addDir(rootLocation, files[i], out);
				continue;
			}
			FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
			String relativePath = files[i].getAbsolutePath().substring(rootLocation.length());
			print(" Adding: " + relativePath);
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
