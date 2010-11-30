package net.osmand.data.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.data.preparation.MapZooms;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.MapRenderingTypes;

import org.apache.commons.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class IndexBatchCreator {
	
	
	protected static final Log log = LogUtil.getLog(IndexBatchCreator.class);
	
	public static class RegionCountries {
		String namePrefix = ""; // for states of the country
		String nameSuffix = "";
		Set<String> regionNames = new LinkedHashSet<String>();
		String siteToDownload = "";
	}
	
	
	// process atributtes
	boolean downloadFiles = false;
	boolean generateIndexes = false;
	boolean uploadIndexes = false;
	MapZooms mapZooms = null;
	MapRenderingTypes types = MapRenderingTypes.getDefault();
	boolean deleteFilesAfterUploading = true;
	
	File osmDirFiles;
	File indexDirFiles;
	
	boolean indexPOI = false;
	boolean indexTransport = false;
	boolean indexAddress = false;
	boolean writeWayNodes = false; 
	boolean indexMap = false;
	
	
	String user;
	String password;
	String cookieHSID = "";
	String cookieSID = "";
	String pagegen = "";
	String token = "";
	
	
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
		} else {
			try {
				stream = new FileInputStream(name);
			} catch (FileNotFoundException e) {
				System.out.println("XML configuration file not found : " + name);
				return;
			}
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
		}
		
	}
	
	public void runBatch(Document doc){
		NodeList list = doc.getElementsByTagName("process");
		if(list.getLength() != 1){
			 throw new IllegalArgumentException("You should specify exactly 1 process element!");
		}
		Element process = (Element) list.item(0);
		downloadFiles = Boolean.parseBoolean(process.getAttribute("downloadOsmFiles"));
		generateIndexes = Boolean.parseBoolean(process.getAttribute("generateIndexes"));
		uploadIndexes = Boolean.parseBoolean(process.getAttribute("uploadIndexes"));
		deleteFilesAfterUploading = Boolean.parseBoolean(process.getAttribute("deleteFilesAfterUploading"));
		
		indexPOI = Boolean.parseBoolean(process.getAttribute("indexPOI"));
		indexMap = Boolean.parseBoolean(process.getAttribute("indexMap"));
		indexTransport = Boolean.parseBoolean(process.getAttribute("indexTransport"));
		indexAddress = Boolean.parseBoolean(process.getAttribute("indexAddress"));
		String zooms = process.getAttribute("mapZooms");
		if(zooms == null || zooms.length() == 0){
			mapZooms = MapZooms.getDefault();
		} else {
			mapZooms = MapZooms.parseZooms(zooms);
		}
		
		String f = process.getAttribute("renderingTypesFile");
		if(f == null || f.length() == 0){
			types = MapRenderingTypes.getDefault();
		} else {
			types = new MapRenderingTypes(f);
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
		
		if(uploadIndexes){
			list = doc.getElementsByTagName("authorization_info");
			if(list.getLength() != 1){
				 throw new IllegalArgumentException("You should specify exactly 1 authorization_info element to upload indexes!");
			}
			Element authorization = (Element) list.item(0);
			cookieHSID = authorization.getAttribute("cookieHSID");
			cookieSID = authorization.getAttribute("cookieSID");
			pagegen = authorization.getAttribute("pagegen");
			token = authorization.getAttribute("token");
			user = authorization.getAttribute("google_code_user");
			password = authorization.getAttribute("google_code_password");
		}
		
		List<RegionCountries> countriesToDownload = new ArrayList<RegionCountries>();
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
					if(name != null && !Boolean.parseBoolean(ncountry.getAttribute("skip"))){
						countries.regionNames.add(name);
					}
				}
				countriesToDownload.add(countries);
				
			}
		}
		
		runBatch(countriesToDownload);
		
		
	}
	
	public void runBatch(List<RegionCountries> countriesToDownload ){
		Set<String> alreadyUploadedFiles = new LinkedHashSet<String>();
		Set<String> alreadyGeneratedFiles = new LinkedHashSet<String>();
		if(downloadFiles){
			downloadFiles(countriesToDownload, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		if(generateIndexes){
			generatedIndexes(alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		if(uploadIndexes){
			uploadIndexes(alreadyUploadedFiles);
		}
	}
	

	
	protected void downloadFiles(List<RegionCountries> countriesToDownload, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles){
		// clean before downloading
//		for(File f : osmDirFiles.listFiles()){
//			log.info("Delete old file " + f.getName());  //$NON-NLS-1$
//			f.delete();
//		}
		
		for(RegionCountries regionCountries : countriesToDownload){
			String prefix = regionCountries.namePrefix;
			String site = regionCountries.siteToDownload;
			String suffix = regionCountries.nameSuffix;
			for(String name : regionCountries.regionNames){
				name = name.toLowerCase();
				String url = MessageFormat.format(site, name);
				downloadFile(url, prefix+name+suffix, alreadyGeneratedFiles, alreadyUploadedFiles);
			}
		}
		
		System.out.println("DOWNLOADING FILES FINISHED");
	}
	
	private final static int DOWNLOAD_DEBUG = 1 << 20;
	private final static int MB = 1 << 20;
	private final static int BUFFER_SIZE = 1 << 15;
	protected void downloadFile(String url, String country, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		byte[] buffer = new byte[BUFFER_SIZE];
		int count = 0;
		int downloaded = 0;
		int mbDownloaded = 0;
		File toSave = new File(osmDirFiles, country+".osm.bz2");
		try {
			log.info("Downloading country " + country + " from " + url);  //$NON-NLS-1$//$NON-NLS-2$
			FileOutputStream ostream = new FileOutputStream(toSave);
			InputStream stream = new URL(url).openStream();
			while ((count = stream.read(buffer)) != -1) {
				ostream.write(buffer, 0, count);
				downloaded += count;
				if(downloaded > DOWNLOAD_DEBUG){
					downloaded -= DOWNLOAD_DEBUG;
					mbDownloaded += (DOWNLOAD_DEBUG>>20);
					log.info(mbDownloaded +" megabytes downloaded of " + toSave.getName());
				}
			}
			ostream.close();
			stream.close();
			generateIndex(toSave, alreadyGeneratedFiles, alreadyUploadedFiles);
		} catch (IOException e) {
			log.error("Input/output exception " + toSave.getName() + " downloading from " + url, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
	}
	
	protected void generatedIndexes(Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		for (File f : getSortedFiles(osmDirFiles)) {
			if (alreadyGeneratedFiles.contains(f.getName())) {
				continue;
			}
			if (f.getName().endsWith(".osm.bz2") || f.getName().endsWith(".osm")) {
				generateIndex(f, alreadyGeneratedFiles, alreadyUploadedFiles);
			}
		}
		System.out.println("GENERATING INDEXES FINISHED ");
	}
	
	
	
	protected void generateIndex(File f, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		if (!generateIndexes) {
			return;
		}
		try {
			String regionName = f.getName();
			int i = f.getName().indexOf('.');
			if (i > -1) {
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i));
			}
			
			IndexCreator indexCreator = new IndexCreator(indexDirFiles);
			indexCreator.setIndexAddress(indexAddress);
			indexCreator.setIndexPOI(indexPOI);
			indexCreator.setIndexTransport(indexTransport);
			indexCreator.setIndexMap(indexMap);
			indexCreator.setLastModifiedDate(f.lastModified());
			indexCreator.setNormalizeStreets(true);
			indexCreator.setSaveAddressWays(writeWayNodes);

			String poiFileName = regionName + "_" + IndexConstants.POI_TABLE_VERSION + IndexConstants.POI_INDEX_EXT;
			indexCreator.setPoiFileName(poiFileName);
			String mapFileName = regionName + "_" + IndexConstants.BINARY_MAP_VERSION + IndexConstants.BINARY_MAP_INDEX_EXT;
			indexCreator.setMapFileName(mapFileName);
			try {
				alreadyGeneratedFiles.add(f.getName());
				indexCreator.generateIndexes(f, new ConsoleProgressImplementation(3),  null, mapZooms, types);
				if (indexPOI) {
					uploadIndex(new File(indexDirFiles, poiFileName), alreadyUploadedFiles);
				}
				if (indexMap || indexAddress || indexTransport) {
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
			if(!alreadyUploadedFiles.contains(f.getName())){
				uploadIndex(f, alreadyUploadedFiles);
				if(!alreadyUploadedFiles.contains(f.getName())){
					System.out.println("! NOT UPLOADED "  + f.getName());
				}
			}
		}
		System.out.println("UPLOADING INDEXES FINISHED ");
		
	}
	
	protected void uploadIndex(File f, Set<String> alreadyUploadedFiles){
		if(!uploadIndexes){
			return;
		}
		MessageFormat format = new MessageFormat("{0,date,dd.MM.yyyy} : {1, number,##.#} MB", Locale.US);
		String summary;
		double mbLengh = (double)f.length() / MB;
		boolean zip = true;
		String regionName;
		if(f.getName().endsWith(IndexConstants.POI_INDEX_EXT) || f.getName().endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.POI_INDEX_EXT.length() - 2);
			summary = "POI index for " ;
		} else if(f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT) || f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.ADDRESS_INDEX_EXT.length() - 2);
			summary = "Address index for " ;
		} else if(f.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT) || f.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.TRANSPORT_INDEX_EXT.length() - 2);
			summary = "Transport index for ";
		} else if(f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.BINARY_MAP_INDEX_EXT.length() - 2);
			boolean addr = indexAddress;
			boolean trans = indexTransport;
			boolean map = indexMap;
			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(f, "r");
				BinaryMapIndexReader reader = new BinaryMapIndexReader(raf);
				trans = reader.hasTransportData();
				map = reader.containsMapData();
				addr = reader.containsAddressData();
				reader.close();
			} catch (Exception e) {
				log.info("Exception", e);
				if (raf != null) {
					try {
						raf.close();
					} catch (IOException e1) {
					}
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
			if (map) {
				summary = "Map" + (fir ? "" : ", ") + summary;
				fir = false;
			}
			
		} else { 
			return;
		}
		if(mbLengh < 0.015){
			// do not upload small files
			return;
		}
		if(mbLengh > 3 && f.getName().endsWith(".odb") && zip){
			String zipFileName = f.getName().subSequence(0, f.getName().length() - 4)+".zip";
			File zFile = new File(f.getParentFile(), zipFileName);
			log.info("Zipping file " + f.getName());
			try {
				ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zFile));
				zout.setLevel(9);
				zout.putNextEntry(new ZipEntry(f.getName()));
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
			f = zFile;
			
		}
		try {
			DownloaderIndexFromGoogleCode.deleteFileFromGoogleDownloads(f.getName(), token, pagegen, cookieHSID, cookieSID);
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// wait 5 seconds
			}
		} catch (IOException e) {
			log.warn("Deleting file from downloads" + f.getName() +  " " + e.getMessage());
		}
		
		mbLengh = (double)f.length() / MB;
		if(mbLengh > 100){
			System.err.println("ERROR : file " + f.getName() + " exceeded 100 mb!!! Could not be uploaded.");
			return; // restriction for google code
		}
		String descriptionFile = "{"+format.format(new Object[]{new Date(f.lastModified()), mbLengh})+"}";
		summary +=  regionName + " " + descriptionFile;
		
		
		GoogleCodeUploadIndex uploader = new GoogleCodeUploadIndex();
		uploader.setFileName(f.getAbsolutePath());
		uploader.setTargetFileName(f.getName());
		uploader.setProjectName("osmand");
		uploader.setUserName(user);
		uploader.setPassword(password);
		uploader.setLabels("Type-Archive, Testdata");
		uploader.setSummary(summary.replace('_', ' '));
		try {
			uploader.upload();
			if(deleteFilesAfterUploading){
				f.delete();
			}
			alreadyUploadedFiles.add(f.getName());
		} catch (IOException e) {
			log.error("Input/output exception uploading " + f.getName(), e);
		}
	}
	
	

}
