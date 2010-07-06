package com.osmand.data.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;

import com.osmand.LogUtil;
import com.osmand.data.Region;
import com.osmand.data.preparation.DataExtraction;
import com.osmand.impl.ConsoleProgressImplementation;

public class IndexBatchCreator {
	// config params
	private static final boolean indexPOI = true;
	private static final boolean indexAddress = false;
	private static final boolean indexTransport = true;
	private static final boolean writeWayNodes = true;
	
	protected static final Log log = LogUtil.getLog(IndexBatchCreator.class);
	protected static final String SITE_TO_DOWNLOAD1 = "http://download.geofabrik.de/osm/europe/"; //$NON-NLS-1$
	protected static final String[] countriesToDownload1 = new String[] {
		"albania", "andorra", "austria", // 5.3, 0.4, 100 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"belarus", "belgium", "bosnia-herzegovina", // 39, 43, 4.1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"bulgaria", "croatia", "cyprus",  // 13, 12, 5 //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		"denmark", "estonia", "faroe_islands", // 75, 38, 1.5 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"finland", "greece", "hungary", //80, 25, 14 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"iceland", "ireland", "isle_of_man", // 5.9, 27, 1.1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"kosovo", "latvia", "liechtenstein", // 8.2, 6.5, 0.2 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"lithuania", "luxembourg", "macedonia", // 5, 5, 4 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"malta", "moldova", "monaco", //0.8, 5, 0.6 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"montenegro", "norway", "poland", // 1.2, 56, 87 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"portugal", "romania", "serbia", // 10, 25, 10 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"slovakia", "slovenia", "spain", // 69, 10, 123 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"sweden", "switzerland", "turkey", // 88, 83, 17 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		"ukraine", // 19 //$NON-NLS-1$
//		 TOTAL : 1129 MB
		 "great_britain",  "italy", // 281, 246, 
		 "czech_republic", "netherlands", // 168, 375,  
//		 "france",  "germany", //519, 860
		// ADD TO TOTAL : 2449 MB
		
		// currently : austria, spain address > 100 MB
		// address : "great_britain",  "italy", "france",  "germany" - out of memory
		// poi : "france",  "germany" - out of memory

	};
	
	protected static final String SITE_TO_DOWNLOAD2 = "http://downloads.cloudmade.com/"; //$NON-NLS-1$
	// us states
	protected static final String[] usStates = new String[] {
		"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
		"Delaware",	"District_of_Columbia", "Florida", "Georgia", "Guantanamo_Bay",	"Hawaii",
		"Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine",
		"Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", 
		"Montana", "Nebraska", "Nevada", "New_Hampshire", "New_Jersey", "New_Mexico",
		"New_York",	"North_Carolina", "North_Dakota", "Ohio", "Oklahoma", "Oregon",
		"Pennsylvania", "Rhode Island",	"South Carolina", "South Dakota", "Tennessee",
		"Texas", "Utah", "Vermont", "Virginia", "Washington", "West_Virginia", "Wisconsin", "Wyoming",
	};
	protected static final String[] canadaStates = new String[] {
		"Alberta","British_Columbia","Manitoba","New_Brunswick","Newfoundland",
		"Nova_Scotia","Nunavut", "Nw_Territories","Ontario","Pr_Edwrd_Island",
		"Quebec","Saskatchewan","Yukon",
	};
	
	
	
	boolean downloadFiles = false;
	boolean generateIndexes = false;
	boolean uploadIndexes = false;
	File osmDirFiles;
	File indexDirFiles;
	String user;
	String password;
	
	
	public static void main(String[] args) {
		IndexBatchCreator creator = new IndexBatchCreator();
		if(args.length < 1 || !new File(args[0]).exists()) {
			throw new IllegalArgumentException("Please specify directory with .osm or .osm.bz2 files as first arg"); //$NON-NLS-1$
		}
		creator.osmDirFiles = new File(args[0]); 
		if(args.length < 2 || !new File(args[1]).exists()) {
			throw new IllegalArgumentException("Please specify directory with generated index files as second arg"); //$NON-NLS-1$
		}
		creator.indexDirFiles = new File(args[1]);
		for(int i=2; i<args.length; i++){
			if("-downloadOsm".equals(args[i])){ //$NON-NLS-1$
				creator.downloadFiles = true;
			} else if("-genIndexes".equals(args[i])){ //$NON-NLS-1$
				creator.generateIndexes = true;
			} else if("-upload".equals(args[i])){ //$NON-NLS-1$
				creator.uploadIndexes = true;
			} else if(args[i].startsWith("-guser=")){ //$NON-NLS-1$
				creator.user = args[i].substring("-guser=".length()); //$NON-NLS-1$
			} else if(args[i].startsWith("-gpassword=")){ //$NON-NLS-1$
				creator.password = args[i].substring("-gpassword=".length()); //$NON-NLS-1$
			}
		}
		creator.runBatch();
	}
	
	public void runBatch(){
		if(downloadFiles){
			downloadFiles();
		}
		if(generateIndexes){
			generatedIndexes();
		}
		if(uploadIndexes){
			uploadIndexes();
		}
	}
	
	protected void downloadFiles(){
		// clean before downloading
//		for(File f : osmDirFiles.listFiles()){
//			log.info("Delete old file " + f.getName());  //$NON-NLS-1$
//			f.delete();
//		}
		for(String country : countriesToDownload1){
			String url = SITE_TO_DOWNLOAD1 + country +".osm.bz2"; //$NON-NLS-1$
			log.info("Downloading country " + country + " from " + url);  //$NON-NLS-1$//$NON-NLS-2$
			downloadFile(url, new File(osmDirFiles, country +".osm.bz2")); //$NON-NLS-1$
		}
		
		for(String country : usStates){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "north_america/united_states/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			log.info("Downloading country " + country + " from " + url);  //$NON-NLS-1$//$NON-NLS-2$
			downloadFile(url, new File(osmDirFiles, "US_"+country +".osm.bz2")); //$NON-NLS-1$
		}
		
		for(String country : canadaStates){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "north_america/canada/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			log.info("Downloading country " + country + " from " + url);  //$NON-NLS-1$//$NON-NLS-2$
			downloadFile(url, new File(osmDirFiles, "Canada_"+country +".osm.bz2")); //$NON-NLS-1$
		}
		System.out.println("DOWNLOADING FILES FINISHED");
	}
	
	private final static int DOWNLOAD_DEBUG = 1 << 20;
	private final static int MB = 1 << 20;
	private final static int BUFFER_SIZE = 1 << 15;
	protected void downloadFile(String url, File toSave) {
		byte[] buffer = new byte[BUFFER_SIZE];
		int count = 0;
		int downloaded = 0;
		int mbDownloaded = 0;
		try {
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
		} catch (IOException e) {
			log.error("Input/output exception " + toSave.getName() + " downloading from " + url, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	protected void generatedIndexes() {
		for (File f : getSortedFiles(osmDirFiles)) {
			if (f.getName().endsWith(".osm.bz2") || f.getName().endsWith(".osm")) {
				System.gc();
				try {
					generateIndex(f);
				} catch (OutOfMemoryError e) {
					log.error("OutOfMemory", e);
					System.gc();
				}
			}
		}
		System.out.println("GENERATING INDEXES FINISHED ");
	}
	protected void generateIndex(File f){
		DataExtraction extr = new DataExtraction(indexAddress, indexPOI, indexTransport, indexAddress, false, false, indexDirFiles);
		try {
			Region country = extr.readCountry(f.getAbsolutePath(), new ConsoleProgressImplementation(9), null);
			DataIndexWriter dataIndexWriter = new DataIndexWriter(indexDirFiles, country);
			String name = country.getName();
			if(indexAddress){
				dataIndexWriter.writeAddress(name + "_" + IndexConstants.ADDRESS_TABLE_VERSION + IndexConstants.ADDRESS_INDEX_EXT, f.lastModified(), writeWayNodes);
			}
			if(indexPOI){
				dataIndexWriter.writePOI(name + "_" + IndexConstants.POI_TABLE_VERSION + IndexConstants.POI_INDEX_EXT, f.lastModified());
			}
			if(indexTransport){
				dataIndexWriter.writeTransport(name + "_" + IndexConstants.TRANSPORT_TABLE_VERSION + IndexConstants.TRANSPORT_INDEX_EXT, f.lastModified());
			}
		} catch (Exception e) { 
			log.error("Exception generating indexes for " + f.getName()); //$NON-NLS-1$ 
		}
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
	
	protected void uploadIndexes(){
		MessageFormat format = new MessageFormat("{0,date,dd.MM.yyyy} : {1, number,##.#} MB", Locale.US);
		for(File f : getSortedFiles(indexDirFiles)){
			String summary;
			double mbLengh = (double)f.length() / MB;
			
			String descriptionFile = "{"+format.format(new Object[]{new Date(f.lastModified()), mbLengh})+"}";
			if(f.getName().endsWith(IndexConstants.POI_INDEX_EXT)){
				String regionName = f.getName().substring(0, f.getName().length() - IndexConstants.POI_INDEX_EXT.length() - 2);
				summary = "POI index for " + regionName + " " + descriptionFile;
			} else if(f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT)){
				String regionName = f.getName().substring(0, f.getName().length() - IndexConstants.ADDRESS_INDEX_EXT.length() - 2);
				summary = "Adress index for " + regionName + " " + descriptionFile;
			} else if(f.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT)){
				String regionName = f.getName().substring(0, f.getName().length() - IndexConstants.TRANSPORT_INDEX_EXT.length() - 2);
				summary = "Transport index for " + regionName + " " + descriptionFile;
			} else { 
				continue;
			}
			if(mbLengh > 90){
				System.err.println("ERROR : file " + f.getName() + " exceeded 90 mb!!! Could not be uploaded.");
			}
			GoogleCodeUploadIndex uploader = new GoogleCodeUploadIndex();
			uploader.setFileName(f.getAbsolutePath());
			uploader.setTargetFileName(f.getName());
			uploader.setProjectName("osmand");
			uploader.setUserName(user);
			uploader.setPassword(password);
			uploader.setLabels("Type-Archive, Testdata");
			uploader.setSummary(summary);
			try {
				uploader.upload();
			} catch (IOException e) {
				log.error("Input/output exception uploading " + f.getName(), e);
			}
		}
		System.out.println("UPLOADING INDEXES FINISHED ");
		
	}
	

}
