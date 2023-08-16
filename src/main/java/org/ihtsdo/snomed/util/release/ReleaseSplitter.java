package org.ihtsdo.snomed.util.release;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.ihtsdo.snomed.util.SnomedUtilException;
import org.ihtsdo.util.GlobalUtils;

import com.google.common.collect.Lists;

import static org.ihtsdo.util.GlobalUtils.print;

public class ReleaseSplitter {
	
	File archive;
	int splitCount;
	List <File> splitArchives = new ArrayList<>();

	private static void doHelp() {
		print("Usage: <release archive file location> <Number of pieces to split into>");
		System.exit(-1);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			doHelp();
		}
		ReleaseSplitter app = new ReleaseSplitter();
		app.archive = new File(args[0]);
		if (!app.archive.canRead()) {
			throw new SnomedUtilException("Unable to read " + app.archive.getAbsolutePath());
		}
		app.splitCount = Integer.parseInt(args[1]);
		app.generateSplitDirectories();
		app.splitArchive();
		app.zipArchives();
	}

	private void generateSplitDirectories() throws SnomedUtilException {
		String[] pathParts = GlobalUtils.deconstructFilename(archive);
		for (int x=1; x <= splitCount; x++) {
			String splitArchivePath = pathParts[0] + File.separator + pathParts[1] + "_" + x;
			File splitArchive = new File(splitArchivePath);
			//Check it doesn't already exist!
			if (splitArchive.exists()) {
				throw new SnomedUtilException("Split archive " + splitArchivePath + " already exists!");
			}
			splitArchives.add(splitArchive);
		}
	}

	private void splitArchive() throws SnomedUtilException {
		
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(archive));
			ZipEntry ze = zis.getNextEntry();
			try {
				while (ze != null) {
					if (!ze.isDirectory()) {
						Path p = Paths.get(ze.getName());
						String fileName = p.toString();
						splitFile(fileName, zis);
					}
					ze = zis.getNextEntry();
				}
			} finally {
				try{
					zis.closeEntry();
					zis.close();
				} catch (Exception e){} //Well, we tried.
			}
		} catch (IOException e) {
			throw new SnomedUtilException("Failed to extract data from archive " + archive.getName(), e);
		}
	}

	private void splitFile(String fileName, InputStream is) throws IOException {
		List<String> lines = IOUtils.readLines(is, "UTF-8");
		List<List<String>> chunks = chunkList(lines);
		
		print("Original line count: " + lines.size());
		String header = null;
		for (int x=0; x < splitCount; x++) {
			String fullPath = splitArchives.get(x) + File.separator + fileName;
			int rowsWritten = chunks.get(x).size();
			if (header == null) {
				header = chunks.get(0).get(0);
			} else {
				rowsWritten++;
				GlobalUtils.outputToFile(fullPath, header);
			}
			GlobalUtils.outputToFile(fullPath, chunks.get(x));
			print("\t" + splitArchives.get(x) + "/" + fileName + " - count " + rowsWritten);
		}
	}
	
	private List<List<String>> chunkList(List<String> lines) {
		List<List<String>> chunks;
		int chunkSize = (int) Math.ceil((double)lines.size() / (double)splitCount);
		if (lines.size() > splitCount) {
			chunks = Lists.partition(lines, chunkSize);
		} else {
			chunks = new ArrayList<>();
			chunks.add(lines);
			for (int x=1; x < splitCount; x++) {
				chunks.add(new ArrayList<>());
			}
		}
		return chunks;
	}

	private void zipArchives() throws SnomedUtilException {
		for (File splitArchive : splitArchives) {
			GlobalUtils.createArchive(splitArchive, splitArchive.getAbsolutePath());
		}
	}
}
