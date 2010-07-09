package com.osmand.data.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;

import com.osmand.LogUtil;
import com.osmand.data.Region;
import com.osmand.data.preparation.DataExtraction;
import com.osmand.impl.ConsoleProgressImplementation;

public class IndexBatchCreator {
	// config params
	private static final boolean indexPOI = true;
	private static final boolean indexAddress = true;
	private static final boolean indexTransport = true;
	private static final boolean writeWayNodes = true;
	
	protected static final Log log = LogUtil.getLog(IndexBatchCreator.class);
	protected static final String SITE_TO_DOWNLOAD1 = "http://download.geofabrik.de/osm/europe/"; //$NON-NLS-1$
	
	protected static final String[] countriesToDownload1 = new String[] {
//		"albania", "andorra", "austria", // 5.3, 0.4, 100 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"belarus", "belgium", "bosnia-herzegovina", // 39, 43, 4.1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"bulgaria", "croatia", "cyprus",  // 13, 12, 5 //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
//		"denmark", "estonia", "faroe_islands", // 75, 38, 1.5 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"finland", "greece", "hungary", //80, 25, 14 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"iceland", "ireland", "isle_of_man", // 5.9, 27, 1.1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"kosovo", "latvia", "liechtenstein", // 8.2, 6.5, 0.2 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"lithuania", "luxembourg", "macedonia", // 5, 5, 4 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"malta", "moldova", "monaco", //0.8, 5, 0.6 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"montenegro", "norway", "poland", // 1.2, 56, 87 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"portugal", "romania", "serbia", // 10, 25, 10 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"slovakia", "slovenia", "spain", // 69, 10, 123 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"sweden", "switzerland", "turkey", // 88, 83, 17 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		"ukraine", // 19 //$NON-NLS-1$
//		 TOTAL : 1129 MB 
//		 "czech_republic", "netherlands", // 168, 375,
//		 "great_britain",  "italy", // 281, 246, 
		// ADD TO TOTAL : 2449 MB
		// TODO transport, poi : "czech_republic", "netherlands", "great_britain",  "italy" 
		//      address : "great_britain",  "italy" - out of memory

	};

	// TODO all
	protected static final String[] franceProvinces = new String[] {
		"alsace","aquitaine", "auvergne", "basse-normandie", "bourgogne", "bretagne", "centre",
		"champagne-ardenne", "corse", "franche-comte", "haute-normandie", "ile-de-france",
		"languedoc-roussillon", "limousin", "lorraine", "midi-pyrenees", "nord-pas-de-calais",
		"pays-de-la-loire", "picardie","poitou-charentes", "provence-alpes-cote-d-azur", "rhone-alpes"
	};
	
	// TODO all
	protected static final String[] germanyLands = new String[] {
		"baden-wuerttemberg","bayern", "berlin", "brandenburg", "bremen", "hamburg", "hessen",
		"mecklenburg-vorpommern", "niedersachsen", "nordrhein-westfalen", "rheinland-pfalz", "saarland",
		"sachsen-anhalt", "sachsen", "schleswig-holstein", "thueringen",
	};

	
	protected static final String SITE_TO_DOWNLOAD2 = "http://downloads.cloudmade.com/"; //$NON-NLS-1$
	// us states
	// TODO address
	protected static final String[] usStates = new String[] {
//		"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", 
//		"Delaware",	"District_of_Columbia", "Florida", "Georgia", "Guantanamo_Bay",	"Hawaii",
//		"Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine",
//		"Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", 
//		"Montana", "Nebraska", "Nevada", "New_Hampshire", "New_Jersey", "New_Mexico",
//		"New_York",	"North_Carolina", "North_Dakota", "Ohio", "Oklahoma", "Oregon",
//		"Pennsylvania", "Rhode Island",	"South Carolina", "South Dakota", "Tennessee",
//		"Texas", "Utah", "Vermont", "Virginia", "Washington", "West_Virginia", "Wisconsin", "Wyoming",
	};
	
	protected static final String[] canadaStates = new String[] {
//		"Alberta","British_Columbia","Manitoba","New_Brunswick","Newfoundland",
//		"Nova_Scotia","Nunavut", "Nw_Territories","Ontario","Pr_Edwrd_Island",
//		"Quebec","Saskatchewan","Yukon",
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
		Set<String> alreadyUploadedFiles = new LinkedHashSet<String>();
		Set<String> alreadyGeneratedFiles = new LinkedHashSet<String>();
		if(downloadFiles){
			downloadFiles(alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		if(generateIndexes){
			generatedIndexes(alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		if(uploadIndexes){
			uploadIndexes(alreadyUploadedFiles);
		}
	}
	

	
	protected void downloadFiles(Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles){
		// clean before downloading
//		for(File f : osmDirFiles.listFiles()){
//			log.info("Delete old file " + f.getName());  //$NON-NLS-1$
//			f.delete();
//		}
		
		// europe
		for(String country : countriesToDownload1){
			String url = SITE_TO_DOWNLOAD1 + country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		
		// france
		for(String country : franceProvinces){
			String url = SITE_TO_DOWNLOAD1 +"france/" + country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		
		// germany
		for(String country : germanyLands){
			String url = SITE_TO_DOWNLOAD1 +"germany/" + country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		
		// usa
		for(String country : usStates){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "north_america/united_states/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, "US_"+country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}

		// canada
		for(String country : canadaStates){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "north_america/canada/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, "Canada_"+country, alreadyGeneratedFiles, alreadyUploadedFiles); 
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
			DataExtraction extr = new DataExtraction(indexAddress, indexPOI, indexTransport, indexAddress, false, false, indexDirFiles);
			try {
				alreadyGeneratedFiles.add(f.getName());
				Region country = extr.readCountry(f.getAbsolutePath(), new ConsoleProgressImplementation(9), null);
				DataIndexWriter dataIndexWriter = new DataIndexWriter(indexDirFiles, country);
				String name = country.getName();
				if (indexAddress) {
					String fName = name + "_" + IndexConstants.ADDRESS_TABLE_VERSION + IndexConstants.ADDRESS_INDEX_EXT;
					dataIndexWriter.writeAddress(fName, f.lastModified(), writeWayNodes);
					uploadIndex(new File(indexDirFiles, fName), alreadyUploadedFiles);
				}
				if (indexPOI) {
					String fName = name + "_" + IndexConstants.POI_TABLE_VERSION + IndexConstants.POI_INDEX_EXT;
					dataIndexWriter.writePOI(fName, f.lastModified());
					uploadIndex(new File(indexDirFiles, fName), alreadyUploadedFiles);
				}
				if (indexTransport) {
					String fName = name + "_" + IndexConstants.TRANSPORT_TABLE_VERSION + IndexConstants.TRANSPORT_INDEX_EXT;
					dataIndexWriter.writeTransport(fName, f.lastModified());
					uploadIndex(new File(indexDirFiles, fName), alreadyUploadedFiles);
				}
			} catch (Exception e) {
				log.error("Exception generating indexes for " + f.getName()); //$NON-NLS-1$ 
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
		String regionName;
		if(f.getName().endsWith(IndexConstants.POI_INDEX_EXT) || f.getName().endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.POI_INDEX_EXT.length() - 2);
			summary = "POI index for " ;
		} else if(f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT) || f.getName().endsWith(IndexConstants.ADDRESS_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.ADDRESS_INDEX_EXT.length() - 2);
			summary = "Adress index for " ;
		} else if(f.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT) || f.getName().endsWith(IndexConstants.TRANSPORT_INDEX_EXT_ZIP)){
			regionName = f.getName().substring(0, f.getName().length() - IndexConstants.TRANSPORT_INDEX_EXT.length() - 2);
			summary = "Transport index for ";
		} else { 
			return;
		}
		if(mbLengh > 5 && f.getName().endsWith(".odb")){
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
				log.error("Exception while zipping file");
			}
			if(f.delete()){
				log.info("Source odb file was deleted");
			}
			f = zFile;
			
		}
		mbLengh = (double)f.length() / MB;
		if(mbLengh > 100){
			System.err.println("ERROR : file " + f.getName() + " exceeded 100 mb!!! Could not be uploaded.");
			return; // restriction for google code
		}
		String descriptionFile = "{"+format.format(new Object[]{new Date(f.lastModified()), mbLengh})+"}";
		summary +=  regionName + " " + descriptionFile;
		
		alreadyUploadedFiles.add(f.getName());
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
	
	

}
