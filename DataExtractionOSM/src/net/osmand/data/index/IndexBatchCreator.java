package net.osmand.data.index;

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

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.data.Region;
import net.osmand.data.preparation.DataExtraction;
import net.osmand.data.preparation.IndexCreator;
import net.osmand.impl.ConsoleProgressImplementation;

import org.apache.commons.logging.Log;


public class IndexBatchCreator {
	// config params
	private static final boolean indexPOI = true;
	private static final boolean indexAddress = true;
	private static final boolean indexTransport = true;
	private static final boolean writeWayNodes = true;
	
	protected static final Log log = LogUtil.getLog(IndexBatchCreator.class);
	protected static final String SITE_TO_DOWNLOAD1 = "http://download.geofabrik.de/osm/europe/"; //$NON-NLS-1$
	
	protected static final String[] europeCountries = new String[] {
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
		//TOTAL : 1129 MB 
//		 "czech_republic", "netherlands", // 168, 375,
//		 "great_britain",  "italy", // 310, 246, 
		// ADD TO TOTAL : 2449 MB

	};

	protected static final String[] franceProvinces = new String[] {
//		"alsace","aquitaine", "auvergne", "basse-normandie", "bourgogne", "bretagne", "centre",
//		"champagne-ardenne", "corse", "franche-comte", "haute-normandie", "ile-de-france",
//		"languedoc-roussillon", "limousin", "lorraine", "midi-pyrenees", "nord-pas-de-calais",
//		"pays-de-la-loire", "picardie","poitou-charentes", "provence-alpes-cote-d-azur", "rhone-alpes"
	};
	
	protected static final String[] germanyLands = new String[] {
//		"baden-wuerttemberg","bayern", "berlin", "brandenburg", "bremen", "hamburg", "hessen",
//		"mecklenburg-vorpommern", "niedersachsen", "nordrhein-westfalen", "rheinland-pfalz", "saarland",
//		"sachsen-anhalt", "sachsen", "schleswig-holstein", "thueringen",
	};

	// TODO send address index : 
	// Great-britain, alabama, california, florida, georgia
	
	protected static final String SITE_TO_DOWNLOAD2 = "http://downloads.cloudmade.com/"; //$NON-NLS-1$
	// us states
	// TODO Address (out of memory) : west-virginia, virginia, vermont, utas, texas, tennesse, pensilvania, oregon,..
	protected static final String[] usStates = new String[] {
//		"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", 
//		"Delaware",	"District_of_Columbia", "Florida", "Georgia", "Guantanamo_Bay",	"Hawaii",
//		"Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine",
//		"Maryland", "Massachusetts", "Michigan", 
		// TODO
//		"Minnesota", "Mississippi", "Missouri", 
//		"Montana", 
//		"Nebraska", "Nevada", "New_Hampshire", "New_Jersey", "New_Mexico",
//		"New_York",	"North_Carolina", "North_Dakota", "Ohio", "Oklahoma", "Oregon",
//		"Pennsylvania", "Rhode Island",	"South Carolina", "South Dakota", "Tennessee",
//		"Texas", "Utah", "Vermont", "Virginia", "Washington", "West_Virginia", "Wisconsin", "Wyoming",
	};
	
	protected static final String[] canadaStates = new String[] {
//		"Alberta","British_Columbia","Manitoba","New_Brunswick","Newfoundland",
//		"Nova_Scotia","Nunavut", "Nw_Territories","Ontario","Pr_Edwrd_Island",
//		"Quebec","Saskatchewan","Yukon",
	};
	
	protected static final String[] southAmerica = new String[] {
//		"Argentina","Bolivia","Brazil","Chile","Colombia",
//		"Ecuador","Falkland_Islands", "French_Guiana","Guyana","Paraguay",
//		"Peru","Suriname","Uruguay","Venezuela"
	};
	
	// TODO only australia, new zealand created
	protected static final String[] oceania = new String[] {
//		"Australia", "New_Zealand",
//		"American_Samoa","Baker_Island","Cocos_Keeling_Islands","Cook_Islands",
//		"Federated_States_of_Micronesia","Fiji", "French_Polynesia","Guam","Howland_Island",
//		"Independent_State_of_Samoa","Jarvis_Island","Johnston_Atoll","Kiribati",
//		"Marshall_Islands","Midway_Islands","Nauru","New_Caledonia",
//		"Niue","Norfolk_Island","Northern_Mariana_Islands","Papua_New_Guinea","Pitcairn_Islands",
//		"Republic_of_Palau","Solomon_Islands","Tokelau","Tonga","Tuvalu","Vanuatu","Wake_Island","Wallis_and_Futuna",
	};
	
	
	protected static final String SITE_TO_DOWNLOAD3 = "http://gis-lab.info/data/osm/"; //$NON-NLS-1$
	protected static final String[] russiaStates = new String[] {
//		"adygeya", "altay", "altayskiy","amur","arkhan","astrakhan",
//		"bashkir", "belgorod","bryansk","buryat","chechen", "chel",
//		"chukot", "chuvash", "dagestan","evrey", "ingush", "irkutsk",
//	    "ivanov","kabardin","kalinin","kalmyk","kaluzh","kamch","karach","karel",
//	    "kemerovo","khabar","khakas","khanty","kirov","komi","kostrom","krasnodar",
//	    "krasnoyarsk","kurgan","kursk","leningrad","lipetsk","magadan","mariyel","mordov","moscow","mosobl","murmansk",
//	    "nenec","nizhegorod","novgorod","novosib","omsk","orenburg","orlovsk","osetiya",
//	    "penz","perm","prim","pskov","rostov","ryazan","sakhalin","samar","saratov","smol",
//	    "stavrop","stpeter","sverdl","tambov","tatar","tomsk","tul","tumen","tver","tyva","udmurt",
//	    "ulyan","vladimir","volgograd","vologda","voronezh","yakut","yamal","yarosl","zabaikal",
	};
	
	// TODO Japan
	protected static final String[] asia = new String[] { 
//			"Afghanistan", "Bahrain", "Bangladesh", "Bhutan", "British_Indian_Ocean_Territory", "Brunei", "Cambodia", 
//			"China", "Christmas_Island", "Democratic_Republic_of_Timor-Leste", "Gaza_Strip", "India", "Indonesia", "Iran", 
//			"Iraq", "Israel", "Jordan", "Kazakhstan", "Kuwait", "Kyrgyzstan", "Laos", "Lebanon", "Macau", "Malaysia", 
//			"Maldives", "Mongolia", "Nepal", "North_Korea", "Oman", "Pakistan", "Paracel_Islands", "Philippines", "Qatar", 
//			"Saudi_Arabia", "Singapore", "South_Korea", "Spratly_Islands", "Sri_Lanka", "Syria", "Taiwan", "Tajikistan", 
//			"Thailand", "Turkmenistan", "Union_of_Myanmar", "United_Arab_Emirates", "Uzbekistan", "Vietnam", "Yemen", 
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
		// EUROPE //
		// europe
		for(String country : europeCountries){
			String url = SITE_TO_DOWNLOAD1 + country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		
		// france
		for(String country : franceProvinces){
			String url = SITE_TO_DOWNLOAD1 +"france/" + country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, "France_"+country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}
		
		// germany
		for(String country : germanyLands){
			String url = SITE_TO_DOWNLOAD1 +"germany/" + country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, "Germany_"+country, alreadyGeneratedFiles, alreadyUploadedFiles);
		}

		// TODO NORTH AMERICA partially //
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
		
		// SOUTH AMERICA//
		for(String country : southAmerica){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "south_america/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles); 
		}
		
		// ASIA //
		for(String country : asia){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "asia/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles); 
		}
		
		// russia
		for(String country : russiaStates){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD3 + country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, "Russia_"+country, alreadyGeneratedFiles, alreadyUploadedFiles); 
		}

		// OCEANIA //
		for(String country : oceania){
			country = country.toLowerCase();
			String url = SITE_TO_DOWNLOAD2 + "oceania/"+country+"/"+country +".osm.bz2"; //$NON-NLS-1$
			downloadFile(url, country, alreadyGeneratedFiles, alreadyUploadedFiles); 
		}

		// TODO AFRICA not done //
		// TODO Antarctica not done //
		
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
	protected void generateIndexOld(File f, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
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
				log.error("Exception generating indexes for " + f.getName(), e); //$NON-NLS-1$ 
			}
		} catch (OutOfMemoryError e) {
			System.gc();
			log.error("OutOfMemory", e);

		}
		System.gc();
	}
	
	protected void generateIndex(File f, Set<String> alreadyGeneratedFiles, Set<String> alreadyUploadedFiles) {
		if (!generateIndexes) {
			return;
		}
		try {
			IndexCreator indexCreator = new IndexCreator(indexDirFiles);
			indexCreator.setIndexAddress(indexAddress);
			indexCreator.setIndexPOI(indexPOI);
			indexCreator.setIndexTransport(indexTransport);
			indexCreator.setNormalizeStreets(true);
			indexCreator.setSaveAddressWays(writeWayNodes);

			String regionName = f.getName();
			int i = f.getName().indexOf('.');
			if (i > -1) {
				regionName = Algoritms.capitalizeFirstLetterAndLowercase(f.getName().substring(0, i));
			}
			String addressFileName = regionName + "_" + IndexConstants.ADDRESS_TABLE_VERSION + IndexConstants.ADDRESS_INDEX_EXT;
			indexCreator.setAddressFileName(addressFileName);
			String transportFileName = regionName + "_" + IndexConstants.TRANSPORT_TABLE_VERSION + IndexConstants.TRANSPORT_INDEX_EXT;
			indexCreator.setTransportFileName(transportFileName);
			String poiFileName = regionName + "_" + IndexConstants.POI_TABLE_VERSION + IndexConstants.POI_INDEX_EXT;
			indexCreator.setPoiFileName(poiFileName);
			try {
				alreadyGeneratedFiles.add(f.getName());
				indexCreator.generateIndexes(f, new ConsoleProgressImplementation(3),  null);
				if (indexAddress) {
					uploadIndex(new File(indexDirFiles, addressFileName), alreadyUploadedFiles);
				}
				if (indexPOI) {
					uploadIndex(new File(indexDirFiles, poiFileName), alreadyUploadedFiles);
				}
				if (indexTransport) {
					uploadIndex(new File(indexDirFiles, transportFileName), alreadyUploadedFiles);
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
				log.error("Exception while zipping file", e);
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
		uploader.setSummary(summary.replace('_', ' '));
		try {
			uploader.upload();
		} catch (IOException e) {
			log.error("Input/output exception uploading " + f.getName(), e);
		}
	}
	
	

}
