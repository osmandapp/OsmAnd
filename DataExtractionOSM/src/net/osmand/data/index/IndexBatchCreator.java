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
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.data.IndexConstants;
import net.osmand.data.preparation.DBDialect;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.data.preparation.MapZooms;
import net.osmand.impl.ConsoleProgressImplementation;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.swing.OsmExtractionUI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.Jdk14Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rtree.RTree;


public class IndexBatchCreator {
	
	protected static final Log log = LogUtil.getLog(IndexBatchCreator.class);
	
	public static final String GEN_LOG_EXT = ".gen.log";
	
	
	public static class RegionCountries {
		String namePrefix = ""; // for states of the country
		String nameSuffix = "";
		Map<String, RegionSpecificData> regionNames = new LinkedHashMap<String, RegionSpecificData>();
		String siteToDownload = "";
	}
	
	private static class RegionSpecificData {
		public String cityAdminLevel;
	}
	
	
	// process atributtes
	File skipExistingIndexes;
	MapZooms mapZooms = null;
	Integer zoomWaySmoothness = null; 
	MapRenderingTypes types = MapRenderingTypes.getDefault();
	
	File osmDirFiles;
	File indexDirFiles;
	File workDir;
	
	boolean indexPOI = false;
	boolean indexTransport = false;
	boolean indexAddress = false;
	boolean indexMap = false;
	
	private String wget;
	
	public static void main(String[] args) {
		IndexBatchCreator creator = new IndexBatchCreator();
		OsmExtractionUI.configLogFile();
		if(args == null || args.length == 0){
			System.out.println("Please specify -local parameter or path to batch.xml configuration file as 1 argument.");
			throw new IllegalArgumentException("Please specify -local parameter or path to batch.xml configuration file as 1 argument.");
		}
		String name = args[0];
		InputStream stream;
		InputStream regionsStream = null;
		if(name.equals("-local")){
			stream = IndexBatchCreator.class.getResourceAsStream("batch.xml");
			regionsStream = IndexBatchCreator.class.getResourceAsStream("regions.xml");
			log.info("Using local settings");
		} else {
			try {
				stream = new FileInputStream(name);
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException("XML configuration file not found : " + name, e);
			}
			if (args.length > 1) {
				try {
					File regionsFile = new File(args[1]);
					regionsStream = new FileInputStream(regionsFile);
				} catch (FileNotFoundException e) {
					throw new IllegalArgumentException("Please specify xml-file with regions to download", e); //$NON-NLS-1$
				}
			}
		}
		
		try {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
			Document regions = null;
			if(regionsStream != null) {
				name = args[1];
				regions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(regionsStream);
			}
			creator.runBatch(doc, regions);
		} catch (Exception e) {
			System.out.println("XML configuration file could not be read from " + name);
			e.printStackTrace();
			log.error("XML configuration file could not be read from " + name, e);
		} finally {
			safeClose(stream, "Error closing stream for " + name);
		}
	}
	
	public void runBatch(Document doc, Document regions) throws SAXException, IOException, ParserConfigurationException{
		NodeList list = doc.getElementsByTagName("process");
		if(list.getLength() != 1){
			 throw new IllegalArgumentException("You should specify exactly 1 process element!");
		}
		Element process = (Element) list.item(0);
		IndexCreator.REMOVE_POI_DB = true;
		String file = process.getAttribute("skipExistingIndexesAt");
		if (file != null && new File(file).exists()) {
			skipExistingIndexes = new File(file);
		}
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
			throw new IllegalArgumentException("Please specify directory with .osm or .osm.bz2 files as directory_for_osm_files (attribute)" + dir); //$NON-NLS-1$
		}
		osmDirFiles = new File(dir);
		dir = process.getAttribute("directory_for_index_files");
		if(dir == null || !new File(dir).exists()) {
			throw new IllegalArgumentException("Please specify directory with generated index files  as directory_for_index_files (attribute)"); //$NON-NLS-1$
		}
		indexDirFiles = new File(dir);
		workDir = indexDirFiles;
		dir = process.getAttribute("directory_for_generation");
		if(dir != null && new File(dir).exists()) {
			workDir = new File(dir);
		}
		
		List<RegionCountries> countriesToDownload = new ArrayList<RegionCountries>();
		parseCountriesToDownload(doc, countriesToDownload);
		if (regions != null) {
			parseCountriesToDownload(regions, countriesToDownload);
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
				log.info("Region to download " +countries.siteToDownload);
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
		Set<String> alreadyGeneratedFiles = new LinkedHashSet<String>();
		if(!countriesToDownload.isEmpty()){
			downloadFilesAndGenerateIndex(countriesToDownload, alreadyGeneratedFiles);
		}
		generatedIndexes(alreadyGeneratedFiles);
	}
	

	
	protected void downloadFilesAndGenerateIndex(List<RegionCountries> countriesToDownload, Set<String> alreadyGeneratedFiles){
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
				
				String regionName = prefix + name;
				String fileName = Algoritms.capitalizeFirstLetterAndLowercase(prefix + name + suffix);
				if (skipExistingIndexes != null) {
					File bmif = new File(skipExistingIndexes, fileName + "_" + IndexConstants.BINARY_MAP_VERSION
							+ IndexConstants.BINARY_MAP_INDEX_EXT);
					File bmifz = new File(skipExistingIndexes, bmif.getName() + ".zip");
					if (bmif.exists() || bmifz.exists()) {
						continue;
					}
				}
				File toSave = downloadFile(url,  fileName);
				if (toSave != null) {
					generateIndex(toSave, regionName, regionSpecificData, alreadyGeneratedFiles);
				}
			}
		}
	}
	
	protected File downloadFile(String url, String regionName) {
		String ext = ".osm";
		if(url.endsWith(".osm.bz2")){
			ext = ".osm.bz2";
		} else if(url.endsWith(".osm.pbf")){
			ext = ".osm.pbf";
		}
		File toIndex = null;
		File saveTo = new File(osmDirFiles, regionName + ext);
		if (wget == null || wget.trim().length() == 0) {
			toIndex = internalDownload(url, saveTo);
		} else {
			toIndex = wgetDownload(url, saveTo);
		}
		return toIndex;
	}

	private File wgetDownload(String url,  File toSave) 
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
	private final static int BUFFER_SIZE = 1 << 15;
	private File internalDownload(String url, File toSave) {
		int count = 0;
		int downloaded = 0;
		int mbDownloaded = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		OutputStream ostream = null;
		InputStream stream = null;
		try {
			ostream = new FileOutputStream(toSave);
			stream = new URL(url).openStream();
			log.info("Downloading country " + toSave.getName() + " from " + url);  //$NON-NLS-1$//$NON-NLS-2$
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
	
	protected void generatedIndexes(Set<String> alreadyGeneratedFiles) {
		for (File f : getSortedFiles(osmDirFiles)) {
			if (alreadyGeneratedFiles.contains(f.getName())) {
				continue;
			}
			if (f.getName().endsWith(".osm.bz2") || f.getName().endsWith(".osm") || f.getName().endsWith(".osm.pbf")) {
				generateIndex(f, null, null, alreadyGeneratedFiles);
			}
		}
		log.info("GENERATING INDEXES FINISHED ");
	}
	
	
	
	protected void generateIndex(File f, String rName, RegionSpecificData regionSpecificData, Set<String> alreadyGeneratedFiles) {
		try {
			// be independent of previous results
			RTree.clearCache();
			
			String regionName = f.getName();
			log.warn("-------------------------------------------");
			log.warn("----------- Generate " + f.getName() + "\n\n\n");
			int i = f.getName().indexOf('.');
			if (i > -1) {
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i));
			}
			if(Algoritms.isEmpty(rName)){
				rName = regionName;
			} else {
				rName = Algoritms.capitalizeFirstLetterAndLowercase(rName);
			}
			
			IndexCreator indexCreator = new IndexCreator(workDir);
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
			if(zoomWaySmoothness != null){
				indexCreator.setZoomWaySmothness(zoomWaySmoothness);
			}

			String poiFileName = regionName + "_" + IndexConstants.POI_TABLE_VERSION + IndexConstants.POI_INDEX_EXT;
			indexCreator.setPoiFileName(poiFileName);
			String mapFileName = regionName + "_" + IndexConstants.BINARY_MAP_VERSION + IndexConstants.BINARY_MAP_INDEX_EXT;
			indexCreator.setMapFileName(mapFileName);
			try {
				alreadyGeneratedFiles.add(f.getName());
				Log warningsAboutMapData = null;
				File logFileName = new File(workDir, mapFileName + GEN_LOG_EXT);
				FileHandler fh = null;
				// configure log path
				try {

					FileOutputStream fout = new FileOutputStream(logFileName);
					fout.write((new Date() + "\n").getBytes());
					fout.write((Version.APP_MAP_CREATOR_FULL_NAME + "\n").getBytes());
					fout.close();
					fh = new FileHandler(logFileName.getAbsolutePath(), 5000000, 1, true);
					fh.setFormatter(new SimpleFormatter());
					fh.setLevel(Level.ALL);
					Jdk14Logger jdk14Logger = new Jdk14Logger("tempLogger");
					jdk14Logger.getLogger().setLevel(Level.ALL);
					jdk14Logger.getLogger().setUseParentHandlers(false);
					jdk14Logger.getLogger().addHandler(fh);
					warningsAboutMapData = jdk14Logger;
				} catch (SecurityException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				if (fh != null) {
					LogManager.getLogManager().getLogger("").addHandler(fh);
				}
				try {
					indexCreator.generateIndexes(f, new ConsoleProgressImplementation(3), null, mapZooms, types, warningsAboutMapData);
				} finally {
					if (fh != null) {
						LogManager.getLogManager().getLogger("").removeHandler(fh);
						fh.close();
					}
				}
				File generated = new File(workDir, mapFileName);
				generated.renameTo(new File(indexDirFiles, generated.getName()));

				logFileName.renameTo(new File(indexDirFiles, logFileName.getName()));
				
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
}
