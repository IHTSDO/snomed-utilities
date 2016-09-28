package org.ihtsdo.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

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
}
