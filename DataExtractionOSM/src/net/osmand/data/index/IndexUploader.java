package net.osmand.data.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.IndexConstants;
import net.osmand.data.index.ExtractGooglecodeAuthorization.GooglecodeUploadTokens;

import org.apache.commons.logging.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * This helper will find obf and zip files, create description for them, and zip them, or update the description. 
 * This helper also can upload files through ssh,ftp or to googlecode.
 * 
 * IndexUploader dir targetDir [--ff=file] [--fp=ptns] [--dp=ptns] [-ssh|-ftp|-google] [--password=|--user=|--url=|--path=|--gpassword=|--privKey=|--knownHosts=]
 *    --ff     file with names of files to be uploaded from the dir, supporting regexp
 *    --fp     comma separated names of files to be uploaded from the dir, supporting regexp  
 *    --dp     comma separated names of files to be delete on remote system, supporting regexp
 *    One of:
 *    -ssh    to upload to ssh site
 *    -fpt    to upload to ftp site
 *    -google to upload to googlecode of osmand
 *    Additional params:
 *    --user       user to use for ftp,ssh,google
 *    --password   password to use for ftp,ssh,google (gmail password to retrieve tokens for deleting files)
 *    --url        url to use for ssh,ftp
 *    --path       path in url to use for ssh,ftp
 *    --gpassword  googlecode password to use for google
 *    --privKey    priv key for ssh
 *    --knownHosts known hosts for ssh
 * 
 * @author Pavol Zibrita <pavol.zibrita@gmail.com>
 */
public class IndexUploader {

	protected static final Log log = LogUtil.getLog(IndexUploader.class);
	private final static double MIN_SIZE_TO_UPLOAD = 0.001d;
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
	private UploadCredentials uploadCredentials = new DummyCredentials();
	private FileFilter fileFilter = new FileFilter();
	private FileFilter deleteFileFilter = null;

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
	
	public static void main(String[] args) {
		try {
			String srcPath = extractDirectory(args, 0);
			String targetPath = srcPath;
			if (args.length > 1) {
				targetPath = extractDirectory(args, 1);
			}
			IndexUploader indexUploader = new IndexUploader(srcPath, targetPath);
			if(args.length > 2) {
				indexUploader.parseParameters(args, 2);
			}
			indexUploader.run();
		} catch (IndexUploadException e) {
			log.error(e.getMessage());
		}
	}
	
	private void parseParameters(String[] args, int start) throws IndexUploadException {
		int idx = parseFilter(args, start);
		if (args.length > idx) {
			parseUploadCredentials(args, idx);
		}
	}
	
	private int parseFilter(String[] args, int start) throws IndexUploadException {
		FileFilter fileFilter = null;
		if (args[start].startsWith("--ff=")) {
			try {
				if(fileFilter == null) {
					fileFilter = new FileFilter();
				}
				fileFilter.parseFile(args[start].substring("--ff=".length()));
			} catch (IOException e) {
				throw new IndexUploadException(e.getMessage());
			}
			start++;
		}
		if (args[start].startsWith("--fp=")) {
			if(fileFilter == null) {
				fileFilter = new FileFilter();
			}
			fileFilter.parseCSV(args[start].substring("--fp=".length()));
			start++;
		}
		if (args[start].startsWith("--dp=")) {
			deleteFileFilter = new FileFilter();
			deleteFileFilter.parseCSV(args[start].substring("--dp=".length()));
			start++;
		}
		if(fileFilter != null) {
			this.fileFilter = fileFilter;
		}
		return start;
	}

	protected void parseUploadCredentials(String[] args, int start) throws IndexUploadException {
		if ("-ssh".equals(args[start])) {
			uploadCredentials = new UploadSSHCredentials();
		} else if ("-ftp".equals(args[start])) {
			uploadCredentials = new UploadFTPCredentials();
		} else if ("-google".equals(args[start])){
			uploadCredentials = new UploadToGoogleCodeCredentials();
		} else {
			return;
		}
		for (int i = start + 1; i < args.length; i++) {
			uploadCredentials.parseParameter(args[i]);
		}
	}

	public void setUploadCredentials(UploadCredentials uploadCredentials) {
		this.uploadCredentials = uploadCredentials;
	}

	protected static class FileFilter {
		
		private List<Pattern> matchers = null;
		
		public void parseCSV(String patterns){
			String[] s = patterns.split(",");
			if(matchers == null) {
				matchers = new ArrayList<Pattern>();
			}
			for(String p : s) {
				matchers.add(Pattern.compile(p.trim()));
			}
		}
		
		public void parseFile(String file) throws IOException{
			File f = new File(file);
			if (f.exists()) {
				readPatterns(f);
			}
		}

		private void readPatterns(File f) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			try {
				String line;
				if(matchers == null) {
					matchers = new ArrayList<Pattern>();
				}
				while ((line = reader.readLine()) != null) {
					matchers.add(Pattern.compile(line));
				}
			} finally {
				Algoritms.closeStream(reader);
			}
		}
		
		public boolean patternMatches(String name){
			if(matchers == null) {
				return true;
			}
			for (Pattern p : matchers)  {
				if (p.matcher(name).matches()) {
					return true;
				}
			}
			return false;
		}
		
		protected boolean fileCanBeUploaded(File f) {
			if (!f.isFile() && !f.getName().endsWith(IndexBatchCreator.GEN_LOG_EXT)) {
				return false;
			}
			return patternMatches(f.getName());
		}
	}
	
	public void run() throws IndexUploadException {
		// take files before whole upload process
		try {
			uploadCredentials.connect();
			File[] listFiles = directory.listFiles();
			for (File f : listFiles) {
				try {
					if (!fileFilter.fileCanBeUploaded(f)) {
						continue;
					}
					log.info("Process file " + f.getName());
					File unzipped = unzip(f);
					String description = getDescription(unzipped);
					File logFile = new File(f.getParentFile(), unzipped.getName() + IndexBatchCreator.GEN_LOG_EXT);
					List<File> files = new ArrayList<File>();
					files.add(unzipped);
					if(logFile.exists()) {
						files.add(logFile);
					}
					File zFile = new File(f.getParentFile(), unzipped.getName() + ".zip");
					zip(files, zFile, description, unzipped.lastModified());
					unzipped.delete(); // delete the unzipped file
					if(logFile.exists()){
						logFile.delete();
					}
					uploadIndex(zFile, description, uploadCredentials);
				} catch (OneFileException e) {
					log.error(f.getName() + ": " + e.getMessage(), e);
				} catch (RuntimeException e) {
					log.error(f.getName() + ": " + e.getMessage(), e);
				}
			}
			if(deleteFileFilter != null) {
				if(uploadCredentials instanceof UploadToGoogleCodeCredentials) {
					log.info("About to delete files from googlecode");
					Map<String, String> files = DownloaderIndexFromGoogleCode.getIndexFiles(new LinkedHashMap<String, String>());
					for (String f : files.keySet()) {
						System.out.println("File on googlecode " + f + " " + files.get(f));
						if (deleteFileFilter.patternMatches(f)) {
							log.info("About to delete " + f);
						}
					}
					
					//DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName(), ((UploadToGoogleCodeCredentials)uploadCredentials).ggtokens);
				} else {
					log.error("Delete file filter is not supported with this credentions (method) " + uploadCredentials);
				}
			}
			
		} finally {
			uploadCredentials.disconnect();
		}
	}

	public static File zip(List<File> fs, File zFile, String description, long lastModifiedTime) throws OneFileException {
		try {
			ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zFile));
			zout.setLevel(9);
			for (File f : fs) {
				log.info("Zipping to file: " + zFile.getName() + " file:" + f.getName() + " with desc:" + description);

				ZipEntry zEntry = new ZipEntry(f.getName());
				zEntry.setSize(f.length());
				zEntry.setComment(description);
				zout.putNextEntry(zEntry);
				FileInputStream is = new FileInputStream(f);
				Algoritms.streamCopy(is, zout);
				Algoritms.closeStream(is);
			}
			Algoritms.closeStream(zout);
			zFile.setLastModified(lastModifiedTime);
		} catch (IOException e) {
			throw new OneFileException("cannot zip file:" + e.getMessage());
		}
		return zFile;
	}

	private String getDescription(File f) throws OneFileException {
		String fileName = f.getName();
		String summary = null;
		if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
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
			throw new OneFileException("Not supported file format " + fileName);
		}

		int last = fileName.lastIndexOf('_', fileName.indexOf('.'));
		if (last == -1) {
			last = fileName.indexOf('.');
		}
		String regionName = fileName.substring(0, last);
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
			File mainFile = null;
			long slastModified = f.lastModified();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				long lastModified = slastModified;
				if(entry.getTime() < lastModified) {
					lastModified = entry.getTime();
				}
				File tempFile = new File(f.getParentFile(), entry.getName());
				InputStream zin = zipFile.getInputStream(entry);
				FileOutputStream out = new FileOutputStream(tempFile);
				Algoritms.streamCopy(zin, out);
				Algoritms.closeStream(zin);
				Algoritms.closeStream(out);
				if (!tempFile.getName().endsWith(IndexConstants.GEN_LOG_EXT)) {
					mainFile = tempFile;
				}
				tempFile.setLastModified(lastModified);
			}
			return mainFile;
		} catch (ZipException e) {
			throw new OneFileException("cannot unzip:" + e.getMessage());
		} catch (IOException e) {
			throw new OneFileException("cannot unzip:" + e.getMessage());
		}
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

	private void uploadIndex(File zipFile, String summary, UploadCredentials uc) {
		double mbLengh = (double) zipFile.length() / MB;
		String fileName = zipFile.getName();
		if (mbLengh < MIN_SIZE_TO_UPLOAD) {
			log.info("Skip uploading index due to size " + fileName);
			// do not upload small files
			return;
		}
		try {
			log.info("Upload index " + fileName);
			File toUpload = zipFile;
			boolean uploaded = uploadFileToServer(toUpload, summary, uc);
			// remove source file if file was splitted
			if (uploaded && targetDirectory != null && !targetDirectory.equals(directory)) {
				File toBackup = new File(targetDirectory, toUpload.getName());
				if (toBackup.exists()) {
					toBackup.delete();
				}
				toUpload.renameTo(toBackup);
			}
		} catch (IOException e) {
			log.error("Input/output exception uploading " + fileName, e);
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
	
	
	public abstract static class UploadCredentials {
		String password;
		String user;
		String url;
		String path;
		
		protected void parseParameter(String param) throws IndexUploadException {
			if (param.startsWith("--url=")) {
				url = param.substring("--url=".length());
			} else if (param.startsWith("--password=")) {
				password = param.substring("--password=".length());
			} else if (param.startsWith("--user=")) {
				user = param.substring("--user=".length());
			} else if (param.startsWith("--path=")) {
				path = param.substring("--path=".length());
			}
		}

		public void disconnect() {
			//if the uploading needs to close a session
		}

		public void connect() throws IndexUploadException {
			//if the uploading needs to open a session
		}

		public abstract void upload(IndexUploader uploader, File toUpload, String summary, String size,	String date) throws IOException, JSchException;
	}
	
	public static class DummyCredentials extends UploadCredentials {
		@Override
		public void upload(IndexUploader uploader, File toUpload,
				String summary, String size, String date) throws IOException,
				JSchException {
			//do nothing
		}	
	}
	
	public static class UploadFTPCredentials extends UploadCredentials {
		@Override
		public void upload(IndexUploader uploader, File toUpload,
				String summary, String size, String date) throws IOException,
				JSchException {
			uploader.uploadToFTP(toUpload, summary, size, date, this);
		}
	}
	
	public static class UploadSSHCredentials extends UploadCredentials {
		String privateKey;
		String knownHosts;
		
		@Override
		protected void parseParameter(String param) throws IndexUploadException {
			super.parseParameter(param);
			
			if (param.startsWith("--privKey=")) {
				privateKey = param.substring("--privKey=".length());
			} else if (param.startsWith("--knownHosts=")) {
				knownHosts = param.substring("--knownHosts=".length());
			}
		}
		
		@Override
		public void upload(IndexUploader uploader, File toUpload,
				String summary, String size, String date) throws IOException, JSchException {
			uploader.uploadToSSH(toUpload, summary, size, date, this);
		}
	}
	
	public static class UploadToGoogleCodeCredentials extends UploadCredentials {
		GooglecodeUploadTokens ggtokens;
		String gpassword;
		
		@Override
		protected void parseParameter(String param) throws IndexUploadException {
			super.parseParameter(param);

			if (param.startsWith("--gpassword=")) {
				gpassword = param.substring("--gpassword=".length());
			}
			if (gpassword != null && user != null && ggtokens == null) {
				ExtractGooglecodeAuthorization tool = new ExtractGooglecodeAuthorization();
				try {
					ggtokens = tool.getGooglecodeTokensForUpload(user, gpassword);
				} catch (IOException e) {
					throw new IndexUploadException(e.getMessage());
				}
			}
		}
		
		@Override
		public void connect() throws IndexUploadException {
			if (ggtokens == null || user == null || password == null) {
				throw new IndexUploadException("Not enought googlecode credentials entered!");
			}
		}
		
		@Override
		public void upload(IndexUploader uploader, File toUpload,
				String summary, String size, String date) throws IOException,
				JSchException {
			String descriptionFile = "{" + date + " : " + size + " MB}";
			summary += " " + descriptionFile;

			List<File> splittedFiles = Collections.emptyList();
			try {
				splittedFiles = uploader.splitFiles(toUpload);
				for (File fs : splittedFiles) {
					uploader.uploadToGoogleCode(fs, summary, this);
				}
			} finally {
				// remove all splitted files
				for (File fs : splittedFiles) {
					if (!fs.equals(toUpload)) {
						fs.delete();
					}
				}
			}
		}
	}
	

	public void uploadToGoogleCode(File f, String summary, UploadToGoogleCodeCredentials gc) throws IOException {
		if (f.length() / MB > MAX_UPLOAD_SIZE) {
			System.err.println("ERROR : file " + f.getName() + " exceeded 200 mb!!! Could not be uploaded.");
			throw new IOException("ERROR : file " + f.getName() + " exceeded 200 mb!!! Could not be uploaded.");
			// restriction for google code
		}
		try {
			DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName(), gc.ggtokens);
			if (f.getName().endsWith("obf.zip") && f.length() / MB < 5) {
				// try to delete without .zip part
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 4), gc.ggtokens);
			} else if (f.getName().endsWith("poi.zip") && f.length() / MB < 5) {
				// try to delete without .zip part
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 3) + "odb", gc.ggtokens);
			} else if (f.getName().endsWith(".zip-1")) {
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 2), gc.ggtokens);
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

	public boolean uploadFileToServer(File toUpload, String summary, UploadCredentials credentials) throws IOException {
		double originalLength = (double) toUpload.length() / MB;
		MessageFormat dateFormat = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US);
		MessageFormat numberFormat = new MessageFormat("{0,number,##.#}", Locale.US);
		String size = numberFormat.format(new Object[] { originalLength });
		String date = dateFormat.format(new Object[] { new Date(toUpload.lastModified()) });
		try {
			credentials.upload(this,toUpload,summary,size,date);
		} catch (IOException e) {
			log.error("Input/output exception uploading " + toUpload.getName(), e);
			return false;
		} catch (JSchException e) {
			log.error("Input/output exception uploading " + toUpload.getName(), e);
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
		boolean knownHosts = false;
		if (cred.knownHosts != null) {
			jSch.setKnownHosts(cred.knownHosts);
			knownHosts = true;
		}
		if (cred.privateKey != null) {
			jSch.addIdentity(cred.privateKey);
		}
		String serverName = cred.url;
		if (serverName.startsWith("ssh://")) {
			serverName = serverName.substring("ssh://".length());
		}
		Session session = jSch.getSession(cred.user, serverName);
		if (cred.password != null) {
			session.setPassword(cred.password);
		}
		if(!knownHosts) {
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
		}
		String rfile = cred.path + "/"+ f.getName();
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
