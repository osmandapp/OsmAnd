package net.osmand.data.index;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.IndexConstants;
import net.osmand.data.index.ExtractGooglecodeAuthorization.GooglecodeUploadTokens;
import net.osmand.data.preparation.DBDialect;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.data.preparation.MapZooms;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.MapRenderingTypes;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import rtree.RTree;



public class IndexBatchCreator {
	
	
	protected static final Log log = LogUtil.getLog(IndexBatchCreator.class);
	private final static double MIN_SIZE_TO_UPLOAD = 0.015d;
	private final static double MIN_SIZE_TO_NOT_ZIP = 2d;
	private final static double MAX_SIZE_TO_NOT_SPLIT = 190d; 
	private final static double MAX_UPLOAD_SIZE = 195d;
	
	
	public static class RegionCountries {
		String namePrefix = ""; // for states of the country
		String nameSuffix = "";
		Map<String, RegionSpecificData> regionNames = new LinkedHashMap<String, RegionSpecificData>();
		String siteToDownload = "";
	}
	
	private static class RegionSpecificData {
		public String cityAdminLevel;
	}
	
	private boolean uploadToOsmandGooglecode = true;
	
	
	// process atributtes
	boolean downloadFiles = false;
	boolean generateIndexes = false;
	boolean uploadIndexes = false;
	MapZooms mapZooms = null;
	Integer zoomWaySmoothness = null; 
	MapRenderingTypes types = MapRenderingTypes.getDefault();
	boolean deleteFilesAfterUploading = true;
	
	File osmDirFiles;
	File indexDirFiles;
	File backupUploadedFiles;
	
	boolean indexPOI = false;
	boolean indexTransport = false;
	boolean indexAddress = false;
	boolean indexMap = false;
	
	
	String user;
	String password;
	
	private String wget;
	
	String googlePassword = "";
	String cookieHSID = "";
	String cookieSID = "";
	String pagegen = "";
	String token = "";
	private boolean local;
	
	
	public static void main(String[] args) {
		
		IndexBatchCreator creator = new IndexBatchCreator();
		if(args == null || args.length == 0){
			System.out.println("Please specify -local parameter or path to batch.xml configuration file as 1 argument.");
			throw new IllegalArgumentException("Please specify -local parameter or path to batch.xml configuration file as 1 argument.");
		}
		String name = args[0];
		InputStream stream;
		if(name.equals("-local")){
			stream = IndexBatchCreator.class.getResourceAsStream("batch.xml");
			creator.setLocal(true);
		} else {
			try {
				stream = new FileInputStream(name);
			} catch (FileNotFoundException e) {
				System.out.println("XML configuration file not found : " + name);
				return;
			}
		}
		
		if(args.length >= 2 &&  args[1] != null && args[1].startsWith("-gp")){
			creator.googlePassword = args[1].substring(3);
		}
				
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
			creator.runBatch(doc);
		} catch (SAXException e) {
			System.out.println("XML configuration file could not be read from " + name);
			e.printStackTrace();
			log.error("XML configuration file could not be read from " + name, e);
		} catch (IOException e) {
			System.out.println("XML configuration file could not be read from " + name);
			e.printStackTrace();
			log.error("XML configuration file could not be read from " + name, e);
		} catch (ParserConfigurationException e) {
			System.out.println("XML configuration file could not be read from " + name);
			e.printStackTrace();
			log.error("XML configuration file could not be read from " + name, e);
		} finally {
			safeClose(stream, "Error closing stream for " + name);
		}
	}
	
	private void setLocal(boolean local) {
		this.local = local;
	}

	public void runBatch(Document doc) throws SAXException, IOException, ParserConfigurationException{
		NodeList list = doc.getElementsByTagName("process");
		if(list.getLength() != 1){
			 throw new IllegalArgumentException("You should specify exactly 1 process element!");
		}
		Element process = (Element) list.item(0);
		downloadFiles = Boolean.parseBoolean(process.getAttribute("downloadOsmFiles"));
		generateIndexes = Boolean.parseBoolean(process.getAttribute("generateIndexes"));
		uploadIndexes = Boolean.parseBoolean(process.getAttribute("uploadIndexes"));
		deleteFilesAfterUploading = Boolean.parseBoolean(process.getAttribute("deleteFilesAfterUploading"));
		IndexCreator.REMOVE_POI_DB = !Boolean.parseBoolean(process.getAttribute("keepPoiOdb"));
		wget = process.getAttribute("wget");
		
		indexPOI = Boolean.parseBoolean(process.getAttribute("indexPOI"));
		indexMap = Boolean.parseBoolean(process.getAttribute("indexMap"));
		indexTransport = Boolean.parseBoolean(process.getAttribute("indexTransport"));
		indexAddress = Boolean.parseBoolean(process.getAttribute("indexAddress"));
		parseProcessAttributes(process);
		
		list = doc.getElementsByTagName("process_attributes");
		if(list.getLength() == 1){
			parseProcessAttributes((Element) list.item(0));
		}
	
		String dir = process.getAttribute("directory_for_osm_files");
		if(dir == null || !new File(dir).exists()) {
			throw new IllegalArgumentException("Please specify directory with .osm or .osm.bz2 files as directory_for_osm_files (attribute)"); //$NON-NLS-1$
		}
		osmDirFiles = new File(dir);
		dir = process.getAttribute("directory_for_index_files");
		if(dir == null || !new File(dir).exists()) {
			throw new IllegalArgumentException("Please specify directory with generated index files  as directory_for_index_files (attribute)"); //$NON-NLS-1$
		}
		indexDirFiles = new File(dir);
		InputStream regionsStream = null;
		if(downloadFiles){
			dir = process.getAttribute("list_download_regions_file");
			if(!Algoritms.isEmpty(dir)) {
				File regionsFile = new File(dir);
				if(!regionsFile.exists()){
					if (local) {
						regionsStream = IndexBatchCreator.class.getResourceAsStream("regions.xml");
					}
					if (regionsStream == null) {
						throw new IllegalArgumentException("Please specify file with regions to download as list_download_regions_file (attribute)"); //$NON-NLS-1$
					}
				} else {
					regionsStream = new FileInputStream(regionsFile);
				}
			} 
		}
		
		dir = process.getAttribute("directory_for_uploaded_files");
		if (dir != null) {
			File file = new File(dir);
			file.mkdirs();
			if (file.exists()) {
				backupUploadedFiles = file;
			}
		}
		
		
		if(uploadIndexes){
			list = doc.getElementsByTagName("authorization_info");
			if(list.getLength() != 1){
				 throw new IllegalArgumentException("You should specify exactly 1 authorization_info element to upload indexes!");
			}
			Element authorization = (Element) list.item(0);
			uploadToOsmandGooglecode = Boolean.parseBoolean(process.getAttribute("upload_osmand_googlecode"));
			if(uploadToOsmandGooglecode){
				user = authorization.getAttribute("google_code_user"); 
				password = authorization.getAttribute("google_code_password");
				
				cookieHSID = authorization.getAttribute("cookieHSID");
				cookieSID = authorization.getAttribute("cookieSID");
				pagegen = authorization.getAttribute("pagegen");
				token = authorization.getAttribute("token");
				if(googlePassword.length() > 0){
					ExtractGooglecodeAuthorization gca = new ExtractGooglecodeAuthorization();
					try {
						GooglecodeUploadTokens tokens = gca.getGooglecodeTokensForUpload(user, googlePassword);
						cookieHSID = tokens.getHsid();
						cookieSID = tokens.getSid();
						pagegen = tokens.getPagegen();
						token = tokens.getToken();
					} catch (IOException e) {
						log.error("Error retrieving google tokens", e);
					}
				}
			} else {
				user = authorization.getAttribute("osmand_download_user");
				password = authorization.getAttribute("osmand_download_password");
			}
		}
		
		List<RegionCountries> countriesToDownload = new ArrayList<RegionCountries>();
		parseCountriesToDownload(doc, countriesToDownload);
		if(regionsStream != null){
			Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(regionsStream);
			parseCountriesToDownload(innerDoc, countriesToDownload);
		}
		
		runBatch(countriesToDownload);
		
		
	}

	private void parseCountriesToDownload(Document doc, List<RegionCountries> countriesToDownload) {
		NodeList regions = doc.getElementsByTagName("regions");
		for(int i=0; i< regions.getLength(); i++){
			Element el = (Element) regions.item(i);
			if(!Boolean.parseBoolean(el.getAttribute("skip"))){
				RegionCountries countries = new RegionCountries();
				countries.siteToDownload = el.getAttribute("siteToDownload");
				if(countries.siteToDownload == null){
					continue;
				}
				countries.namePrefix = el.getAttribute("region_prefix");
				if(countries.namePrefix == null){
					countries.namePrefix = "";
				}
				countries.nameSuffix = el.getAttribute("region_suffix");
				if(countries.nameSuffix == null){
					countries.nameSuffix = "";
				}
				NodeList ncountries = el.getElementsByTagName("region");
				for(int j=0; j< ncountries.getLength(); j++){
					Element ncountry = (Element) ncountries.item(j);
					String name = ncountry.getAttribute("name");
					RegionSpecificData data = new RegionSpecificData();
					data.cityAdminLevel = ncountry.getAttribute("cityAdminLevel");
					if(name != null && !Boolean.parseBoolean(ncountry.getAttribute("skip"))){
						countries.regionNames.put(name, data);
					}
				}
				countriesToDownload.add(countries);
				
			}
		}
	}

	private void parseProcessAttributes(Element process) {
		String zooms = process.getAttribute("mapZooms");
		if(zooms == null || zooms.length() == 0){
			mapZooms = MapZooms.getDefault();
		} else {
			mapZooms = MapZooms.parseZooms(zooms);
		}
		
		String szoomWaySmoothness = process.getAttribute("zoomWaySmoothness");
		if(szoomWaySmoothness != null && !szoomWaySmoothness.equals("")){
			zoomWaySmoothness = Integer.parseInt(szoomWaySmoothness);
		}
		String f = process.getAttribute("renderingTypesFile");
		if(f == null || f.length() == 0){
			types = MapRenderingTypes.getDefault();
		} else {
			types = new MapRenderingTypes(f);
		}
		
		String osmDbDialect = process.getAttribute("osmDbDialect");
		if(osmDbDialect != null && osmDbDialect.length() > 0){
			try {
				IndexCreator.dialect = DBDialect.valueOf(osmDbDialect.toUpperCase());
			} catch (RuntimeException e) {
			}
		}
		
		String mapDbDialect = process.getAttribute("mapDbDialect");
		if (mapDbDialect != null && mapDbDialect.length() > 0) {
			try {
				IndexCreator.mapDBDialect = DBDialect.valueOf(mapDbDialect.toUpperCase());
			} catch (RuntimeException e) {
			}
		}
	}
	
	public void runBatch(List<RegionCountries> countriesToDownload ){
		Set<String> alreadyUploadedFiles = new LinkedHashSet<String>();
		Set<String> alreadyGeneratedFiles = new LinkedHashSet<String>();
		if(downloadFiles){
			downloadFilesAndGenerateIndex(countriesToDownload, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		if(generateIndexes && !downloadFiles){
			generatedIndexes(alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		if(uploadIndexes){
			uploadIndexes(alreadyUploadedFiles);
		}
	}
	

	
	protected void downloadFilesAndGenerateIndex(List<RegionCountries> countriesToDownload, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles){
		// clean before downloading
//		for(File f : osmDirFiles.listFiles()){
//			log.info("Delete old file " + f.getName());  //$NON-NLS-1$
//			f.delete();
//		}
		
		for(RegionCountries regionCountries : countriesToDownload){
			String prefix = regionCountries.namePrefix;
			String site = regionCountries.siteToDownload;
			String suffix = regionCountries.nameSuffix;
			for(String name : regionCountries.regionNames.keySet()){
				name = name.toLowerCase();
				RegionSpecificData regionSpecificData = regionCountries.regionNames.get(name);
				String url = MessageFormat.format(site, name);
				String country = prefix+name;
				File toSave = downloadFile(url, country, suffix, alreadyGeneratedFiles, alreadyUploadedFiles);
				if (toSave != null && generateIndexes) {
					generateIndex(toSave, country, regionSpecificData, alreadyGeneratedFiles, alreadyUploadedFiles);
				}
			}
		}
		System.out.println("DOWNLOADING FILES FINISHED");
	}
	
	protected File downloadFile(String url, String country, String suffix, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		String ext = ".osm";
		if(url.endsWith(".osm.bz2")){
			ext = ".osm.bz2";
		} else if(url.endsWith(".osm.pbf")){
			ext = ".osm.pbf";
		}
		File toIndex = null;
		File saveTo = new File(osmDirFiles, country + suffix + ext);
		if (wget == null || wget.trim().length() == 0) {
			toIndex = internalDownload(url, country, saveTo);
		} else {
			toIndex = wgetDownload(url, country, saveTo);
		}
		return toIndex;
	}

	private File wgetDownload(String url, String country, File toSave) 
	{
		BufferedReader wgetOutput = null;
		OutputStream wgetInput = null;
		Process wgetProc = null;
		try {
			log.info("Executing " + wget + " " + url + " -O "+ toSave.getCanonicalPath()); //$NON-NLS-1$//$NON-NLS-2$ $NON-NLS-3$
			ProcessBuilder exec = new ProcessBuilder(wget, "--read-timeout=5", "--progress=dot:binary", url, "-O", //$NON-NLS-1$//$NON-NLS-2$ $NON-NLS-3$
					toSave.getCanonicalPath());
			exec.redirectErrorStream(true);
			wgetProc = exec.start();
			wgetOutput = new BufferedReader(new InputStreamReader(wgetProc.getInputStream()));
			String line;
			while ((line = wgetOutput.readLine()) != null) {
				log.info("wget output:" + line); //$NON-NLS-1$
			}
			int exitValue = wgetProc.waitFor();
			wgetProc = null;
			if (exitValue != 0) {
				log.error("Wget exited with error code: " + exitValue); //$NON-NLS-1$
			} else {
				return toSave;
			}
		} catch (IOException e) {
			log.error("Input/output exception " + toSave.getName() + " downloading from " + url + "using wget: " + wget, e); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-3$
		} catch (InterruptedException e) {
			log.error("Interrupted exception " + toSave.getName() + " downloading from " + url + "using wget: " + wget, e); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-3$
		} finally {
			safeClose(wgetOutput, ""); //$NON-NLS-1$
			safeClose(wgetInput, ""); //$NON-NLS-1$
			if (wgetProc != null) {
				wgetProc.destroy();
			}
		}
		return null;
	}
	
	private final static int DOWNLOAD_DEBUG = 1 << 20;
	private final static int MB = 1 << 20;
	private final static int BUFFER_SIZE = 1 << 15;
	private File internalDownload(String url, String country, File toSave) {
		int count = 0;
		int downloaded = 0;
		int mbDownloaded = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		OutputStream ostream = null;
		InputStream stream = null;
		try {
			ostream = new FileOutputStream(toSave);
			stream = new URL(url).openStream();
			log.info("Downloading country " + country + " from " + url);  //$NON-NLS-1$//$NON-NLS-2$
			while ((count = stream.read(buffer)) != -1) {
				ostream.write(buffer, 0, count);
				downloaded += count;
				if(downloaded > DOWNLOAD_DEBUG){
					downloaded -= DOWNLOAD_DEBUG;
					mbDownloaded += (DOWNLOAD_DEBUG>>20);
					log.info(mbDownloaded +" megabytes downloaded of " + toSave.getName());
				}
			}
			return toSave;
		} catch (IOException e) {
			log.error("Input/output exception " + toSave.getName() + " downloading from " + url, e); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			safeClose(ostream, "Input/output exception " + toSave.getName() + " to close stream "); //$NON-NLS-1$ //$NON-NLS-2$
			safeClose(stream, "Input/output exception " + url + " to close stream "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	private static void safeClose(Closeable ostream, String message) {
		if (ostream != null) {
			try {
				ostream.close();
			} catch (Exception e) {
				log.error(message, e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	protected void generatedIndexes(Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		for (File f : getSortedFiles(osmDirFiles)) {
			if (alreadyGeneratedFiles.contains(f.getName())) {
				continue;
			}
			if (f.getName().endsWith(".osm.bz2") || f.getName().endsWith(".osm") || f.getName().endsWith(".osm.pbf")) {
				generateIndex(f, null, null, alreadyGeneratedFiles, alreadyUploadedFiles);
			}
		}
		System.out.println("GENERATING INDEXES FINISHED ");
	}
	
	
	
	protected void generateIndex(File f, String rName, RegionSpecificData regionSpecificData, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		if (!generateIndexes) {
			return;
		}
		try {
			// be independent of previous results
			RTree.clearCache();
			
			String regionName = f.getName();
			int i = f.getName().indexOf('.');
			if (i > -1) {
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i));
			}
			if(Algoritms.isEmpty(rName)){
				rName = regionName;
			} else {
				rName = Algoritms.capitalizeFirstLetterAndLowercase(rName);
			}
			
			IndexCreator indexCreator = new IndexCreator(indexDirFiles);
			indexCreator.setIndexAddress(indexAddress);
			indexCreator.setIndexPOI(indexPOI);
			indexCreator.setIndexTransport(indexTransport);
			indexCreator.setIndexMap(indexMap);
			indexCreator.setLastModifiedDate(f.lastModified());
			indexCreator.setNormalizeStreets(true);
			indexCreator.setSaveAddressWays(true);
			indexCreator.setRegionName(rName);
			if (regionSpecificData != null && regionSpecificData.cityAdminLevel != null) {
				indexCreator.setCityAdminLevel(regionSpecificData.cityAdminLevel);
			}

			String poiFileName = regionName + "_" + IndexConstants.POI_TABLE_VERSION + IndexConstants.POI_INDEX_EXT;
			indexCreator.setPoiFileName(poiFileName);
			String mapFileName = regionName + "_" + IndexConstants.BINARY_MAP_VERSION + IndexConstants.BINARY_MAP_INDEX_EXT;
			indexCreator.setMapFileName(mapFileName);
			try {
				alreadyGeneratedFiles.add(f.getName());
				indexCreator.generateIndexes(f, new ConsoleProgressImplementation(3),  null, mapZooms, types);
				if(zoomWaySmoothness != null){
					indexCreator.setZoomWaySmothness(zoomWaySmoothness);
				}
				// Do not upload poi files any more
//				if (indexPOI) {
//					uploadIndex(new File(indexDirFiles, poiFileName), alreadyUploadedFiles);
//				}
				if (indexMap || indexAddress || indexTransport || indexPOI) {
					uploadIndex(new File(indexDirFiles, mapFileName), alreadyUploadedFiles);
				}
			} catch (Exception e) {
				log.error("Exception generating indexes for " + f.getName(), e); //$NON-NLS-1$ 
			}
		} catch (OutOfMemoryError e) {
			System.gc();
			log.error("OutOfMemory", e);

		}
		System.gc();
	}
	
	protected File[] getSortedFiles(File dir){
		File[] listFiles = dir.listFiles();
		Arrays.sort(listFiles, new Comparator<File>(){
			@Override
			public int compare(File o1, File o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		return listFiles;
	}
	
	protected void uploadIndexes(Set<String> alreadyUploadedFiles){
		for(File f : getSortedFiles(indexDirFiles)){
			if(!alreadyUploadedFiles.contains(f.getName()) && !f.isDirectory()){
				uploadIndex(f, alreadyUploadedFiles);
				if(!alreadyUploadedFiles.contains(f.getName())){
					System.out.println("! NOT UPLOADED "  + f.getName());
				}
			}
		}
		System.out.println("UPLOADING INDEXES FINISHED ");
		
	}
	
	
	
	protected void uploadIndex(final File f, Set<String> alreadyUploadedFiles){
		if(!uploadIndexes){
			return;
		}
		
		String fileName = f.getName();
		log.info("Upload index " + fileName);
		String summary;
		double mbLengh = (double)f.length() / MB;
		boolean zip = true;
		if(fileName.endsWith(IndexConstants.POI_INDEX_EXT) || fileName.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			summary = "POI index for " ;
		} else if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			boolean addr = indexAddress;
			boolean trans = indexTransport;
			boolean map = indexMap;
			boolean poi = indexPOI;
			RandomAccessFile raf = null;
			if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
				try {
					raf = new RandomAccessFile(f, "r");
					BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
					trans = reader.hasTransportData();
					map = reader.containsMapData();
					addr = reader.containsAddressData();
					poi = reader.containsPoiData();
					reader.close();
				} catch (Exception e) {
					log.info("File with not be uploaded! Exception", e);
					if (raf != null) {
						try {
							raf.close();
						} catch (IOException e1) {
						}
					}
					//do not upload probably corrupted file
					return;
				}
			}
			summary = " index for ";
			boolean fir = true;
			if (addr) {
				summary = "Address" + (fir ? "" : ", ") + summary;
				fir = false;
			}
			if (trans) {
				summary = "Transport" + (fir ? "" : ", ") + summary;
				fir = false;
			}
			if (poi) {
				summary = "POI" + (fir ? "" : ", ") + summary;
				fir = false;
			}
			if (map) {
				summary = "Map" + (fir ? "" : ", ") + summary;
				fir = false;
			}
			
		} else { 
			return;
		}
		
		if(mbLengh < MIN_SIZE_TO_UPLOAD){
			// do not upload small files
			return;
		}
		File toUpload = f;
		if(mbLengh > MIN_SIZE_TO_NOT_ZIP && (fileName.endsWith(".odb") || fileName.endsWith(".obf")) && zip){
			String n = fileName;
			if(fileName.endsWith(".odb")){
				n = fileName.substring(0, fileName.length() - 4);
			}
			String zipFileName = n+".zip";
			File zFile = new File(f.getParentFile(), zipFileName);
			log.info("Zipping file " + fileName);
			try {
				ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zFile));
				zout.setLevel(9);
				ZipEntry zEntry = new ZipEntry(fileName);
				zEntry.setSize(f.length());
				zout.putNextEntry(zEntry);
				FileInputStream is = new FileInputStream(f);
				byte[] BUFFER = new byte[8192];
				int read = 0;
				while((read = is.read(BUFFER)) != -1){
					zout.write(BUFFER, 0, read);
				}
				is.close();
				zout.close();
			} catch (IOException e) {
				log.error("Exception while zipping file", e);
			}
			if(f.delete()){
				log.info("Source odb file was deleted");
			}
			toUpload = zFile;
		}
		
		String regionName  = fileName.substring(0, fileName.lastIndexOf('_', fileName.indexOf('.')));
		summary +=  regionName;
		summary = summary.replace('_', ' ');
		
		List<File> splittedFiles = Collections.emptyList(); 
		try {
			splittedFiles = splitFiles(toUpload);
			boolean uploaded = true;
			for (File fs : splittedFiles) {
				uploaded &= uploadFileToServer(fs, toUpload, summary);
			}
			// remove source file if file was splitted
			if (uploaded) {
				if (deleteFilesAfterUploading && toUpload.exists()) {
					toUpload.delete();
				} else if (backupUploadedFiles != null) {
					File toBackup = new File(backupUploadedFiles, toUpload.getName());
					if(toBackup.exists()){
						toBackup.delete();
					}
					toUpload.renameTo(toBackup);
				}
			}
			alreadyUploadedFiles.add(fileName);
		} catch (IOException e) {
			log.error("Input/output exception uploading " + fileName, e);
		} finally {
			// remove all splitted files
			for(File fs : splittedFiles){
				if(!fs.equals(toUpload)){
					fs.delete();
				}
			}
		}
	}
	
	private List<File> splitFiles(File f) throws IOException {
		double mbLengh = (double)f.length() / MB;
		if(mbLengh < MAX_SIZE_TO_NOT_SPLIT) {
			return Collections.singletonList(f);
		} else {
			ArrayList<File> arrayList = new ArrayList<File>();
			FileInputStream in = new FileInputStream(f);
			byte[] buffer = new byte[BUFFER_SIZE];
			
			int i = 1;
			int read = 0;
			while(read != -1){
				File fout = new File(f.getParent(), f.getName() + "-"+i);
				arrayList.add(fout);
				FileOutputStream fo = new FileOutputStream(fout);
				int limit = (int) (MAX_SIZE_TO_NOT_SPLIT * MB);
				while(limit > 0 && ((read = in.read(buffer)) != -1)){
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
	
	private boolean uploadFileToServer(File f, File original, String summary) throws IOException {
		if (f.length() / MB > MAX_UPLOAD_SIZE && uploadToOsmandGooglecode) {
			System.err.println("ERROR : file " + f.getName() + " exceeded 200 mb!!! Could not be uploaded.");
			return false; // restriction for google code
		}
		double originalLength = (double) original.length() / MB;
		if (uploadToOsmandGooglecode) {
			try {
				DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName(), token, pagegen, cookieHSID, cookieSID);
				if(f.getName().endsWith("obf.zip") && f.length() / MB < 5){
					// try to delete without .zip part
					DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 4), 
							token, pagegen, cookieHSID, cookieSID);
				} else if(f.getName().endsWith("poi.zip") && f.length() / MB < 5){
					// try to delete without .zip part
					DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 3) +"odb", 
							token, pagegen, cookieHSID, cookieSID);
				} else if(f.getName().endsWith(".zip-1") && f.length() / MB < 10){
					DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName().substring(0, f.getName().length() - 2),
							token, pagegen, cookieHSID, cookieSID);
				}
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// wait 5 seconds
				}
			} catch (IOException e) {
				log.warn("Deleting file from downloads" + f.getName() + " " + e.getMessage());
			}
		}
		

		MessageFormat dateFormat = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US);
		MessageFormat numberFormat = new MessageFormat("{0,number,##.#}", Locale.US);
		String size = numberFormat.format(new Object[] { originalLength });
		String date = dateFormat.format(new Object[] { new Date(original.lastModified()) });
		if (!uploadToOsmandGooglecode) {
			uploadToDownloadOsmandNet(f, summary, size, date);
		} else {
			String descriptionFile = "{" + date + " : " + size + " MB}";
			summary += " " + descriptionFile;
			GoogleCodeUploadIndex uploader = new GoogleCodeUploadIndex();
			uploader.setFileName(f.getAbsolutePath());
			uploader.setTargetFileName(f.getName());
			uploader.setProjectName("osmand");
			uploader.setUserName(user);
			uploader.setPassword(password);
			uploader.setLabels("Type-Archive, Testdata");
			uploader.setSummary(summary);
			uploader.setDescription(summary);
			uploader.upload();

		}

		return true;
	}
	
	@SuppressWarnings("deprecation")
	private void uploadToDownloadOsmandNet(File f, String description, String size, String date) throws IOException{
		log.info("Uploading file " + f.getName() + " " + size + " MB " + date + " of " + description);
		// Upload to ftp
		FTPFileUpload upload = new FTPFileUpload();
		upload.upload("download.osmand.net", user, password, "indexes/" + f.getName(), f, 1 << 15);
		
		
		String url = "http://download.osmand.net/xml_update.php?";
		url += "index="+URLEncoder.encode(f.getName());
		url += "&description="+URLEncoder.encode(description);
		url += "&date="+URLEncoder.encode(date);
		url += "&size="+URLEncoder.encode(size);
		url += "&action=update";
		log.info("Updating index " + url);  //$NON-NLS-1$//$NON-NLS-2$
		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		connection.setRequestMethod("POST");
		connection.connect();
		if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
			log.error("Error updating indexes " + connection.getResponseMessage());
		}
		InputStream is = connection.getInputStream();
		while(is.read() != -1);
		connection.disconnect();
		log.info("Finish updating index");
	}
	
}
