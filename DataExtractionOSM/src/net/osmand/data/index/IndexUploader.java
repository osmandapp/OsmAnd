package net.osmand.data.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.IndexConstants;

/**
 * This helper will find obf and zip files, create description for them, and zip them, or update the description. This helper also can
 * upload files through ssh.
 * 
 * @author Pavol Zibrita <pavol.zibrita@gmail.com>
 */
public class IndexUploader {

	protected static final Log log = LogUtil.getLog(IndexUploader.class);
	private final static double MIN_SIZE_TO_UPLOAD = 0.015d;
	private final static double MAX_SIZE_TO_NOT_SPLIT = 190d;
	private final static double MAX_UPLOAD_SIZE = 195d;

	private final static int BUFFER_SIZE = 1 << 15;
	private final static int MB = 1 << 20;

	/**
	 * Something bad have happend
	 */
	public static class IndexUploadException extends Exception {

		private static final long serialVersionUID = 2343219168909577070L;

		public IndexUploadException(String message) {
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
	private File targetDirectory;

	public IndexUploader(String path, String targetPath) throws IndexUploadException {
		directory = new File(path);
		if (!directory.isDirectory()) {
			throw new IndexUploadException("Not a directory:" + path);
		}
		targetDirectory = new File(targetPath);
		if (!targetDirectory.isDirectory()) {
			throw new IndexUploadException("Not a directory:" + targetPath);
		}
	}

	public void run() {
		// take files before whole upload process
		File[] listFiles = directory.listFiles();
		for (File f : listFiles) {
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

	public static File zip(File f, String zipFileName, String description) throws OneFileException {
		File zFile = new File(f.getParentFile(), zipFileName);
		try {
			log.info("Zipping to file: " + zipFileName + " file:" + f.getName() + " with desc:" + description);
			ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zFile));
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
			throw new UnsupportedOperationException("Odb is not supported any more");
			// n = fileName.substring(0, fileName.length() - 4);
		}
		return n + ".zip";
	}

	private String getDescription(File f) throws OneFileException {
		String fileName = f.getName();
		String summary = null;
		if (fileName.endsWith(IndexConstants.POI_INDEX_EXT) || fileName.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)) {
			summary = "POI index for ";
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
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
				throw new OneFileException("Reader could not read the index: " + e.getMessage());
			}
		} else {
			throw new OneFileException("Not a processable file.");
		}

		String regionName = fileName.substring(0, fileName.lastIndexOf('_', fileName.indexOf('.')));
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
				copyAndClose(zipFile.getInputStream(entry), new FileOutputStream(tempFile));
				return tempFile;
			}
			return null;
		} catch (ZipException e) {
			throw new OneFileException("cannot unzip:" + e.getMessage());
		} catch (IOException e) {
			throw new OneFileException("cannot unzip:" + e.getMessage());
		}
	}

	public static void copyAndClose(InputStream in, OutputStream out) throws IOException {
		Algoritms.streamCopy(in, out);
		Algoritms.closeStream(in);
		Algoritms.closeStream(out);
	}

	private List<File> splitFiles(File f) throws IOException {
		double mbLengh = (double) f.length() / MB;
		if (mbLengh < MAX_SIZE_TO_NOT_SPLIT) {
			return Collections.singletonList(f);
		} else {
			ArrayList<File> arrayList = new ArrayList<File>();
			FileInputStream in = new FileInputStream(f);
			byte[] buffer = new byte[BUFFER_SIZE];

			int i = 1;
			int read = 0;
			while (read != -1) {
				File fout = new File(f.getParent(), f.getName() + "-" + i);
				arrayList.add(fout);
				FileOutputStream fo = new FileOutputStream(fout);
				int limit = (int) (MAX_SIZE_TO_NOT_SPLIT * MB);
				while (limit > 0 && ((read = in.read(buffer)) != -1)) {
					fo.write(buffer, 0, read);
					limit -= read;
				}
				fo.flush();
				fo.close();
				i++;
			}

			in.close();

			return arrayList;
		}

	}

	public void uploadIndex(File indexFile, File dirToBackup, Set<String> alreadyUploadedFiles, UploadCredentials uc) {
		double mbLengh = (double) indexFile.length() / MB;
		String fileName = indexFile.getName();
		if (mbLengh < MIN_SIZE_TO_UPLOAD) {
			log.info("Skip uploading index due to size " + fileName);
			// do not upload small files
			return;
		}
		try {
			log.info("Upload index " + fileName);
			String summary = getDescription(indexFile);
			File toUpload = indexFile;
			if (fileName.endsWith(".obf")) {
				String zipFileName = fileName + ".zip";
				log.info("Zipping file " + fileName);

				toUpload = IndexUploader.zip(indexFile, zipFileName, summary);
				if (indexFile.delete()) {
					log.info("Source obf file was deleted.");
				}
			}

			boolean uploaded = uploadFileToServer(toUpload, summary, uc);
			// remove source file if file was splitted
			if (uploaded && dirToBackup != null) {
				File toBackup = new File(dirToBackup, toUpload.getName());
				if (toBackup.exists()) {
					toBackup.delete();
				}
				toUpload.renameTo(toBackup);
			}
			alreadyUploadedFiles.add(toUpload.getName());
		} catch (OneFileException e) {
			log.error("Exception ", e);
			return; // do not continue if error
		} catch (IOException e) {
			log.error("Input/output exception uploading " + fileName, e);
		}
	}

	public static void main(String[] args) {
		try {
			String srcPath = extractDirectory(args, 0);
			String targetPath = srcPath;
			if (args.length > 1) {
				targetPath = extractDirectory(args, 1);
			}
			IndexUploader indexUploader = new IndexUploader(srcPath, targetPath);
			indexUploader.run();
		} catch (IndexUploadException e) {
			log.error(e.getMessage());
		}
	}

	private static String extractDirectory(String[] args, int ind) throws IndexUploadException {
		if (args.length > ind) {
			if ("-h".equals(args[0])) {
				throw new IndexUploadException("Usage: IndexZipper [directory] (if not specified, the current one will be taken)");
			} else {
				return args[ind];
			}
		}
		return ".";
	}
	
	
	public static class UploadCredentials {
		
		String password;
		String user;
		String url;
		String path;
	}
	
	public static class UploadSSHCredentials extends UploadCredentials {
		String privateKey;
		String knownHosts;
	}
	
	public static class UploadToGoogleCodeCredentials extends UploadCredentials {
		String token;
		String pagegen;
		String cookieHSID;
		String cookieSID;
	}
	

	@Deprecated
	public void uploadToGoogleCode(File f, String summary, UploadToGoogleCodeCredentials gc) throws IOException {
		if (f.length() / MB > MAX_UPLOAD_SIZE) {
			System.err.println("ERROR : file " + f.getName() + " exceeded 200 mb!!! Could not be uploaded.");
			throw new IOException("ERROR : file " + f.getName() + " exceeded 200 mb!!! Could not be uploaded.");
			// restriction for google code
		}
		try {
			DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName(), gc.token, gc.pagegen, gc.cookieHSID, gc.cookieSID);
			if (f.getName().endsWith("obf.zip") && f.length() / MB < 5) {
				// try to delete without .zip part
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 4), gc.token,
						gc.pagegen, gc.cookieHSID, gc.cookieSID);
			} else if (f.getName().endsWith("poi.zip") && f.length() / MB < 5) {
				// try to delete without .zip part
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 3) + "odb",
						gc.token, gc.pagegen, gc.cookieHSID, gc.cookieSID);
			} else if (f.getName().endsWith(".zip-1")) {
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 2), gc.token,
						gc.pagegen, gc.cookieHSID, gc.cookieSID);
			}
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// wait 5 seconds
			}
		} catch (IOException e) {
			log.warn("Deleting file from downloads" + f.getName() + " " + e.getMessage());
		}

		GoogleCodeUploadIndex uploader = new GoogleCodeUploadIndex();
		uploader.setFileName(f.getAbsolutePath());
		uploader.setTargetFileName(f.getName());
		uploader.setProjectName("osmand");
		uploader.setUserName(gc.user);
		uploader.setPassword(gc.password);
		uploader.setLabels("Type-Archive, Testdata");
		uploader.setSummary(summary);
		uploader.setDescription(summary);
		uploader.upload();
	}

	public boolean uploadFileToServer(File original, String summary, UploadCredentials credentials) throws IOException {
		double originalLength = (double) original.length() / MB;
		MessageFormat dateFormat = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US);
		MessageFormat numberFormat = new MessageFormat("{0,number,##.#}", Locale.US);
		String size = numberFormat.format(new Object[] { originalLength });
		String date = dateFormat.format(new Object[] { new Date(original.lastModified()) });
		try {
			if (credentials instanceof UploadToGoogleCodeCredentials) {
				
				String descriptionFile = "{" + date + " : " + size + " MB}";
				summary += " " + descriptionFile;

				List<File> splittedFiles = Collections.emptyList();
				try {
					splittedFiles = splitFiles(original);
					for (File fs : splittedFiles) {
						uploadToGoogleCode(fs, summary, (UploadToGoogleCodeCredentials) credentials);
					}

				} finally {
					// remove all splitted files
					for (File fs : splittedFiles) {
						if (!fs.equals(original)) {
							fs.delete();
						}
					}
				}
			} else if(credentials instanceof UploadCredentials){
				uploadToSSH(original, summary, size, date, (UploadSSHCredentials) credentials);
			} else {
				uploadToFTP(original, summary, size, date, credentials);
			}
		} catch (IOException e) {
			log.error("Input/output exception uploading " + original.getName(), e);
			return false;
		} catch (JSchException e) {
			log.error("Input/output exception uploading " + original.getName(), e);
			return false;
		}
		return true;
	}

	public void uploadToFTP(File f, String description, String size, String date, UploadCredentials credentials)
			throws IOException {
		log.info("Uploading file " + f.getName() + " " + size + " MB " + date + " of " + description);
		// Upload to ftp
		FTPFileUpload upload = new FTPFileUpload();
		String serverName = credentials.url;
		if(serverName.startsWith("ftp://")){
			serverName = serverName.substring("ftp://".length());
		}
		upload.upload(serverName, credentials.password, credentials.password, credentials.path + "" + f.getName(), f, 1 << 15);
		log.info("Finish uploading file index");
	}
	
	public void uploadToSSH(File f, String description, String size, String date, UploadSSHCredentials cred) throws IOException,
			JSchException {
		log.info("Uploading file " + f.getName() + " " + size + " MB " + date + " of " + description);
		// Upload to ftp
		JSch jSch = new JSch();
		if (cred.knownHosts != null) {
			jSch.setKnownHosts(cred.knownHosts);
		}
		if (cred.privateKey != null) {
			jSch.addIdentity(cred.privateKey);
		}
		String serverName = cred.url;
		if (serverName.startsWith("ssh://")) {
			serverName = serverName.substring("ssh://".length());
		}
		Session session = jSch.getSession(cred.user, cred.url);
		if (cred.password != null) {
			session.setPassword(cred.password);
		}
		String rfile = cred.path + f.getName();
		String lfile = f.getAbsolutePath();
		session.connect();

		// exec 'scp -t rfile' remotely
		String command = "scp -p -t " + rfile;
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(command);

		// get I/O streams for remote scp
		OutputStream out = channel.getOutputStream();
		InputStream in = channel.getInputStream();

		channel.connect();

		if (checkAck(in) != 0) {
			channel.disconnect();
			session.disconnect();
			return;
		}

		// send "C0644 filesize filename", where filename should not include '/'
		long filesize = (new File(lfile)).length();
		command = "C0644 " + filesize + " ";
		if (lfile.lastIndexOf('/') > 0) {
			command += lfile.substring(lfile.lastIndexOf('/') + 1);
		} else {
			command += lfile;
		}
		command += "\n";
		out.write(command.getBytes());
		out.flush();
		if (checkAck(in) != 0) {
			channel.disconnect();
			session.disconnect();
			return;
		}

		// send a content of lfile
		FileInputStream fis = new FileInputStream(lfile);
		byte[] buf = new byte[1024];
		try {
			int len;
			while ((len = fis.read(buf, 0, buf.length)) > 0) {
				out.write(buf, 0, len); // out.flush();
			}
		} finally {
			fis.close();
		}
		fis = null;
		// send '\0'
		buf[0] = 0;
		out.write(buf, 0, 1);
		out.flush();
		if (checkAck(in) != 0) {
			channel.disconnect();
			session.disconnect();
			return;
		}
		out.close();

		channel.disconnect();
		session.disconnect();
		log.info("Finish uploading file index");
	}

	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}

}
