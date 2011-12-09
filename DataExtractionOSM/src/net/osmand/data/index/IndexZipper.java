package net.osmand.data.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.IndexConstants;

/**
 * This helper will find obf and zip files, create description for them, and zip
 * them, or update the description.
 * 
 * @author Pavol Zibrita <pavol.zibrita@gmail.com>
 */
public class IndexZipper {

	protected static final Log log = LogUtil.getLog(IndexZipper.class);
	
	/**
	 * Something bad have happend
	 */
	public static class IndexZipperException extends Exception {

		private static final long serialVersionUID = 2343219168909577070L;

		public IndexZipperException(String message) {
			super(message);
		}

	}

	/**
	 * Processing of one file failed, but other files could continue
	 */
	public static class OneFileException extends Exception {

		private static final long serialVersionUID = 6463200194419498979L;

		public OneFileException(String message) {
			super(message);
		}

	}

	private File directory;

	public IndexZipper(String path) throws IndexZipperException {
		directory = new File(path);
		if (!directory.isDirectory()) {
			throw new IndexZipperException("Not a directory:" + path);
		}
	}

	private void run() {
		for (File f : directory.listFiles()) {
			try {
				if (!f.isFile()) {
					continue;
				}

				File unzipped = unzip(f);
				String description = getDescription(unzipped);
				zip(unzipped, getZipfileName(unzipped), description);
				unzipped.delete(); // delete the unzipped file
			} catch (OneFileException e) {
				log.error(f.getName() + ": " + e.getMessage());
			}
		}
	}

	public static File zip(File f, String zipFileName, String description)
			throws OneFileException {
		File zFile = new File(f.getParentFile(), zipFileName);
		try {
			log.info("Zipping to file: " + zipFileName + " file:" + f.getName() + " with desc:" + description);
			ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(
					zFile));
			zout.setLevel(9);
			ZipEntry zEntry = new ZipEntry(f.getName());
			zEntry.setSize(f.length());
			zEntry.setComment(description);
			zout.putNextEntry(zEntry);
			FileInputStream is = new FileInputStream(f);
			copyAndClose(is, zout);
		} catch (IOException e) {
			throw new OneFileException("cannot zip file:" + e.getMessage());
		}
		return zFile;
	}

	private String getZipfileName(File unzipped) {
		String fileName = unzipped.getName();
		String n = fileName;
		if (fileName.endsWith(".odb")) {
			n = fileName.substring(0, fileName.length() - 4);
		}
		return n + ".zip";
	}

	private String getDescription(File f) throws OneFileException {
		String fileName = f.getName();
		String summary = null;
		if (fileName.endsWith(IndexConstants.POI_INDEX_EXT)
				|| fileName.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)) {
			summary = "POI index for ";
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
				|| fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(f, "r");
				BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
				summary = " index for ";
				boolean fir = true;
				if (reader.containsAddressData()) {
					summary = "Address" + (fir ? "" : ", ") + summary;
					fir = false;
				}
				if (reader.hasTransportData()) {
					summary = "Transport" + (fir ? "" : ", ") + summary;
					fir = false;
				}
				if (reader.containsPoiData()) {
					summary = "POI" + (fir ? "" : ", ") + summary;
					fir = false;
				}
				if (reader.containsMapData()) {
					summary = "Map" + (fir ? "" : ", ") + summary;
					fir = false;
				}
				reader.close();
			} catch (IOException e) {
				if (raf != null) {
					try {
						raf.close();
					} catch (IOException e1) {
					}
				}
				throw new OneFileException("Reader could not read the index: "
						+ e.getMessage());
			}
		} else {
			throw new OneFileException("Not a processable file.");
		}

		String regionName = fileName.substring(0,
				fileName.lastIndexOf('_', fileName.indexOf('.')));
		summary += regionName;
		summary = summary.replace('_', ' ');

		return summary;
	}

	private File unzip(File f) throws OneFileException {
		try {
			if (!Algoritms.isZipFile(f)) {
				return f;
			}

			log.info("Unzipping file: " + f.getName());
			ZipFile zipFile;
			zipFile = new ZipFile(f);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File tempFile = new File(f.getParentFile(), entry.getName());
				copyAndClose(zipFile.getInputStream(entry),
						new FileOutputStream(tempFile));
				return tempFile;
			}
			return null;
		} catch (ZipException e) {
			throw new OneFileException("cannot unzip:" + e.getMessage());
		} catch (IOException e) {
			throw new OneFileException("cannot unzip:" + e.getMessage());
		}
	}

	public static void copyAndClose(InputStream in, OutputStream out)
			throws IOException {
		Algoritms.streamCopy(in, out);
		Algoritms.closeStream(in);
		Algoritms.closeStream(out);
	}

	public static void main(String[] args) {
		try {
			IndexZipper indexZipper = new IndexZipper(extractDirectory(args));
			indexZipper.run();
		} catch (IndexZipperException e) {
			log.error(e.getMessage());
		}
	}

	private static String extractDirectory(String[] args)
			throws IndexZipperException {
		if (args.length > 0) {
			if ("-h".equals(args[0])) {
				throw new IndexZipperException(
						"Usage: IndexZipper [directory] (if not specified, the current one will be taken)");
			} else {
				return args[0];
			}
		}
		return ".";
	}
}
