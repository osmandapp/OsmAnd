package net.osmand.plus.download;

import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.TTSVOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.VOICE_INDEX_EXT_ZIP;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.access.AccessibleToast;
import net.osmand.data.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.DownloadIndexActivity.DownloadActivityType;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.widget.Toast;

public class DownloadOsmandIndexesHelper {
	private final static Log log = LogUtil.getLog(DownloadOsmandIndexesHelper.class);
	public static final String INDEX_DOWNLOAD_DOMAIN = "download.osmand.net";

	public static IndexFileList getIndexesList(Context ctx) {
		PackageManager pm =ctx.getPackageManager();
		AssetManager amanager = ctx.getAssets();
		String versionUrlParam = Version.getVersionAsURLParam(ctx);
		IndexFileList result = downloadIndexesListFromInternet(versionUrlParam);
		if (result == null) {
			result = new IndexFileList();
		} else {
			result.setDownloadedFromInternet(true);
		}
		//add all tts files from assets
		listVoiceAssets(result, amanager, pm, ((OsmandApplication) ctx.getApplicationContext()).getSettings());
		return result;
	}
	
	private static void listVoiceAssets(IndexFileList result, AssetManager amanager, PackageManager pm, 
			OsmandSettings settings) {
		String[] list;
		try {
			String ext = IndexItem.addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION);
			String extvoice = IndexItem.addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION);
			File voicePath = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
			list = amanager.list("voice");
			String date = "";
			long dateModified = System.currentTimeMillis();
			try {
				ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
				dateModified =  new File(appInfo.sourceDir).lastModified();
				Date installed = new Date(dateModified);
				date = format.format(installed);
			} catch (NameNotFoundException e) {
				//do nothing...
			}
			for (String voice : list) {
				if (voice.endsWith("tts")) {
					File destFile = new File(voicePath, voice + File.separatorChar + "_ttsconfig.p");
					String key = voice + ext;
					String assetName = "voice" + File.separatorChar + voice + File.separatorChar + "ttsconfig.p";
					result.add(new AssetIndexItem(key, "voice", date, dateModified, "0.1", "", assetName, destFile.getPath()));
				} else {
					String key = voice + extvoice;
					IndexItem item = result.getIndexFilesByName(key);
					if (item != null) {
						File destFile = new File(voicePath, voice + File.separatorChar + "_config.p");
						SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
						try {
							Date d = format.parse(item.getDate());
							if (d.getTime() > dateModified) {
								continue;
							}
						} catch (Exception es) {
							log.error("Parse exception", es);
						}
						item.date = date;
						String assetName = "voice" + File.separatorChar + voice + File.separatorChar + "config.p";
						item.attachedItem = new AssetIndexItem(key, "voice", date, dateModified, "0.1", "", assetName, destFile.getPath());
					}
				}
			}
			result.sort();
		} catch (IOException e) {
			log.error("Error while loading tts files from assets", e); //$NON-NLS-1$
		}
	}
	
	private static DownloadActivityType getIndexType(String tagName){
		if("region".equals(tagName) ||
							"multiregion".equals(tagName)) {
			return DownloadActivityType.NORMAL_FILE;
		} else if("road_region".equals(tagName) ) {
			return DownloadActivityType.ROADS_FILE;
		}
		return null;
	}

	private static IndexFileList downloadIndexesListFromInternet(String versionAsUrl){
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			try {
				log.info("http://"+INDEX_DOWNLOAD_DOMAIN+"/get_indexes?" + versionAsUrl);
				URL url = new URL("http://"+INDEX_DOWNLOAD_DOMAIN+"/get_indexes?" + versionAsUrl ); //$NON-NLS-1$
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				parser.setInput(url.openStream(), "UTF-8"); //$NON-NLS-1$
				int next;
				while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (next == XmlPullParser.START_TAG) {
						DownloadActivityType tp = getIndexType(parser.getName());
						if (tp != null) {
							String name = parser.getAttributeValue(null, "name"); //$NON-NLS-1$
							String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
							String date = parser.getAttributeValue(null, "date"); //$NON-NLS-1$
							String description = parser.getAttributeValue(null, "description"); //$NON-NLS-1$
							String parts = parser.getAttributeValue(null, "parts"); //$NON-NLS-1$
							IndexItem it = new IndexItem(name, description, date, size, parts);
							it.setType(tp);
							result.add(it);
						} else if ("osmand_regions".equals(parser.getName())) {
							String mapversion = parser.getAttributeValue(null, "mapversion");
							result.setMapVersion(mapversion);
						}
					}
				}
				result.sort();
			} catch (IOException e) {
				log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
				return null;
			} catch (XmlPullParserException e) {
				log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
				return null;
			}
			
			if (result.isAcceptable()) {
				return result;
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
			return null;
		}
	}

	public static class AssetIndexItem extends IndexItem {
		
		private final String assetName;
		private final String destFile;
		private final long dateModified;

		public AssetIndexItem(String fileName, String description, String date,
				long dateModified, String size, String parts, String assetName, String destFile) {
			super(fileName, description, date, size, parts);
			this.dateModified = dateModified;
			this.assetName = assetName;
			this.destFile = destFile;
		}

		@Override
		public boolean isAccepted(){
			return true;
		}
		
		@Override
		public DownloadEntry createDownloadEntry(Context ctx, DownloadActivityType type) {
			return new DownloadEntry(assetName, destFile, dateModified);
		}
	}
	
	public static class IndexItem {
		private String description;
		private String date;
		private String parts;
		private String fileName;
		private String size;
		private IndexItem attachedItem;
		private DownloadActivityType type;
		
		public IndexItem(String fileName, String description, String date, String size, String parts) {
			this.fileName = fileName;
			this.description = description;
			this.date = date;
			this.size = size;
			this.parts = parts;
			this.type = DownloadActivityType.NORMAL_FILE;
		}
		
		public DownloadActivityType getType() {
			return type;
		}
		
		public void setType(DownloadActivityType type) {
			this.type = type;
		}
		
		
		public IndexItem getAttachedItem() {
			return attachedItem;
		}
		
		public String getVisibleDescription(Context ctx, DownloadActivityType type){
			String s = ""; //$NON-NLS-1$
			if(type == DownloadActivityType.ROADS_FILE){
				return ctx.getString(R.string.download_roads_only_item);
			}
			if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
					|| fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			} else if (fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)) {
				s = ctx.getString(R.string.voice);
			} else if (fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
				s = ctx.getString(R.string.ttsvoice);
			}
			return s;
		}
		
		public String getVisibleName(){
			int l = fileName.lastIndexOf('_');
			String name = fileName.substring(0, l < 0 ? fileName.length() : l).replace('_', ' ');
			return name;
		}
		
		public boolean isAccepted(){
			// POI index download is not supported any longer
			if (fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT,IndexConstants.BINARY_MAP_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP,IndexConstants.BINARY_MAP_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION))
				//|| fileName.endsWith(addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION)) drop support for downloading tts files from inet
				) {
				return true;
			}
			return false;
		}

		protected static String addVersionToExt(String ext, int version) {
			return "_" + version + ext;
		}
		
		public String getFileName() {
			return fileName;
		}
		public String getDescription() {
			return description;
		}
		public String getDate() {
			return date;
		}
		
		public String getSize() {
			return size;
		}
		
		public String getParts() {
			return parts;
		}
		
		public DownloadEntry createDownloadEntry(Context ctx, DownloadActivityType type) {
			IndexItem item = this;
			String fileName = item.getFileName();
			File parent = null;
			String toSavePostfix = null;
			String toCheckPostfix = null;
			boolean unzipDir = false;
			boolean preventMediaIndexing = false;
			OsmandSettings settings = ((OsmandApplication) ctx.getApplicationContext()).getSettings();
			if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)){
				parent = settings.extendOsmandPath(ResourceManager.APP_DIR);
				toSavePostfix = BINARY_MAP_INDEX_EXT;
				toCheckPostfix = BINARY_MAP_INDEX_EXT;
			} else if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
				parent = settings.extendOsmandPath(ResourceManager.APP_DIR);
				toSavePostfix = BINARY_MAP_INDEX_EXT_ZIP;
				toCheckPostfix = BINARY_MAP_INDEX_EXT;
			} else if(fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
				parent = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
				toSavePostfix = VOICE_INDEX_EXT_ZIP;
				toCheckPostfix = ""; //$NON-NLS-1$
				unzipDir = true;
				preventMediaIndexing = true;
			} else if(fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)){
				parent = settings.extendOsmandPath(ResourceManager.VOICE_PATH);
				toSavePostfix = TTSVOICE_INDEX_EXT_ZIP;
				toCheckPostfix = ""; //$NON-NLS-1$
				unzipDir = true;
			}
			if(type == DownloadActivityType.ROADS_FILE) {
				toSavePostfix = "-roads" + toSavePostfix;
				toCheckPostfix = "-roads" + toCheckPostfix;
			}
			if(parent != null) {
				parent.mkdirs();
				// ".nomedia" indicates there are no pictures and no music to list in this dir for the Gallery and Music apps
				if( preventMediaIndexing ) {				
					try {
						new File(parent, ".nomedia").createNewFile();//$NON-NLS-1$	
					} catch (IOException e) {
						// swallow io exception
						log.error("IOException" ,e);
					} 
				}
			}
			final DownloadEntry entry;
			if(parent == null || !parent.exists()){
				AccessibleToast.makeText(ctx, ctx.getString(R.string.sd_dir_not_accessible), Toast.LENGTH_LONG).show();
				entry = null;
			} else {
				entry = new DownloadEntry();
				int ls = fileName.lastIndexOf('_');
				entry.isRoadMap = type == DownloadActivityType.ROADS_FILE;
				entry.baseName = fileName.substring(0, ls);
				entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
				entry.unzip = unzipDir;
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
				try {
					Date d = format.parse(item.getDate());
					entry.dateModified = d.getTime();
				} catch (ParseException e1) {
					log.error("ParseException" ,e1);
				}
				try {
					entry.sizeMB = Double.parseDouble(item.getSize());
				} catch (NumberFormatException e1) {
					log.error("ParseException" ,e1);
				}
				entry.parts = 1;
				if (item.getParts() != null) {
					entry.parts = Integer.parseInt(item.getParts());
				}
				entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
				File backup = settings.extendOsmandPath(ResourceManager.BACKUP_PATH + entry.fileToUnzip.getName());
				if (backup.exists()) {
					entry.existingBackupFile = backup;
				}
			}
			if(attachedItem != null) {
				entry.attachedEntry = attachedItem.createDownloadEntry(ctx, type);
			}
			return entry;
		}
		
		public String convertServerFileNameToLocal(){
			String e = getFileName();
			int l = e.lastIndexOf('_');
			String s;
			if(e.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || e.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
				s = IndexConstants.BINARY_MAP_INDEX_EXT;
			} else {
				s = ""; //$NON-NLS-1$
			}
			if(getType() == DownloadActivityType.ROADS_FILE ) {
				s = "-roads"+s;
			}
			return e.substring(0, l) + s;
		}
	}
	
}
