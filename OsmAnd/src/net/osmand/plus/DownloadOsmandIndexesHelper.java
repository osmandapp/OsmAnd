package net.osmand.plus;

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
import net.osmand.access.AccessibleToast;
import net.osmand.data.IndexConstants;
import net.osmand.plus.activities.DownloadIndexActivity.AssetDownloadEntry;
import net.osmand.plus.activities.DownloadIndexActivity.DownloadEntry;

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

	public static IndexFileList getIndexesList(PackageManager pm, AssetManager amanager, String versionAsUrl) {
		IndexFileList result = downloadIndexesListFromInternet(versionAsUrl);
		if (result == null) {
			result = new IndexFileList();
		}
		//add all tts files from assets
		listVoiceAssets(result, amanager, pm);
		return result;
	}
	
	private static void listVoiceAssets(IndexFileList result, AssetManager amanager, PackageManager pm) {
		String[] list;
		try {
			String ext = IndexItem.addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION);
			File voicePath = OsmandApplication.getSettings().extendOsmandPath(ResourceManager.VOICE_PATH);
			list = amanager.list("voice");
			String date = "";
			try {
				ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
				Date installed = new Date(new File(appInfo.sourceDir).lastModified());
				date = format.format(installed);
			} catch (NameNotFoundException e) {
				//do nothing...
			}
			for (String voice : list) {
				File destFile = new File(voicePath, voice + File.separatorChar + "_ttsconfig.p");
				String key = voice + ext;
				String assetName = "voice" + File.separatorChar + voice + File.separatorChar + "ttsconfig.p";
				result.add(key, new AssetIndexItem(key, "voice", date, "0.1", "", assetName, destFile.getPath()));
			}
		} catch (IOException e) {
			log.error("Error while loading tts files from assets", e); //$NON-NLS-1$
		}
	}

	private static IndexFileList downloadIndexesListFromInternet(String versionAsUrl){
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			try {
				URL url = new URL("http://download.osmand.net/get_indexes?" + versionAsUrl ); //$NON-NLS-1$
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				parser.setInput(url.openStream(), "UTF-8"); //$NON-NLS-1$
				int next;
				while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if(next == XmlPullParser.START_TAG && ("region".equals(parser.getName()) ||
							"multiregion".equals(parser.getName()))) { //$NON-NLS-1$
						String name = parser.getAttributeValue(null, "name"); //$NON-NLS-1$
						String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
						String date = parser.getAttributeValue(null, "date"); //$NON-NLS-1$
						String description = parser.getAttributeValue(null, "description"); //$NON-NLS-1$
						String parts = parser.getAttributeValue(null, "parts"); //$NON-NLS-1$
						result.add(name, new IndexItem(name, description, date, size, parts));
					} else if (next == XmlPullParser.START_TAG && ("osmand_regions".equals(parser.getName()))) {
						String mapversion = parser.getAttributeValue(null, "mapversion");
						result.setMapVersion(mapversion);
					} 
				}
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

		public AssetIndexItem(String fileName, String description, String date,
				String size, String parts, String assetName, String destFile) {
			super(fileName, description, date, size, parts);
			this.assetName = assetName;
			this.destFile = destFile;
		}

		@Override
		public boolean isAccepted(){
			return true;
		}
		
		@Override
		public DownloadEntry createDownloadEntry(Context ctx) {
			return new AssetDownloadEntry(assetName, destFile);
		}
	}
	
	public static class IndexItem {
		private String description;
		private String date;
		private String parts;
		private String fileName;
		private String size;
		
		public IndexItem(String fileName, String description, String date, String size, String parts) {
			this.fileName = fileName;
			this.description = description;
			this.date = date;
			this.size = size;
			this.parts = parts;
		}
		
		public String getVisibleDescription(Context ctx){
			String s = ""; //$NON-NLS-1$
			if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
					|| fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
				String lowerCase = description.toLowerCase();
				if (lowerCase.contains("map")) { //$NON-NLS-1$
					if (s.length() > 0) {
						s += ", "; //$NON-NLS-1$
					}
					s += ctx.getString(R.string.map_index);
				}
				if (lowerCase.contains("poi")) { //$NON-NLS-1$
					if (s.length() > 0) {
						s += ", "; //$NON-NLS-1$
					}
					s += ctx.getString(R.string.poi);
				}
				if (lowerCase.contains("transport")) { //$NON-NLS-1$
					if (s.length() > 0) {
						s += ", "; //$NON-NLS-1$
					}
					s += ctx.getString(R.string.transport);
				}
				if (lowerCase.contains("address")) { //$NON-NLS-1$
					if (s.length() > 0 ) {
						s += ", "; //$NON-NLS-1$
					}
					s += ctx.getString(R.string.address);
				}
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
			if (fileName.endsWith(".zip")) { //$NON-NLS-1$
				name += " (zip)"; //$NON-NLS-1$
			}
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
		
		public DownloadEntry createDownloadEntry(Context ctx) {
			IndexItem item = this;
			String fileName = item.getFileName();
			File parent = null;
			String toSavePostfix = null;
			String toCheckPostfix = null;
			boolean unzipDir = false;
			boolean preventMediaIndexing = false;
			
			File externalStorageDirectory = OsmandApplication.getSettings().getExternalStorageDirectory();
			if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)){
				parent = new File(externalStorageDirectory, ResourceManager.APP_DIR);
				toSavePostfix = BINARY_MAP_INDEX_EXT;
				toCheckPostfix = BINARY_MAP_INDEX_EXT;
			} else if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
				parent = new File(externalStorageDirectory, ResourceManager.APP_DIR);
				toSavePostfix = BINARY_MAP_INDEX_EXT_ZIP;
				toCheckPostfix = BINARY_MAP_INDEX_EXT;
			} else if(fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
				parent = new File(externalStorageDirectory, ResourceManager.VOICE_PATH);
				toSavePostfix = VOICE_INDEX_EXT_ZIP;
				toCheckPostfix = ""; //$NON-NLS-1$
				unzipDir = true;
				preventMediaIndexing = true;
			} else if(fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)){
				parent = new File(externalStorageDirectory, ResourceManager.VOICE_PATH);
				toSavePostfix = TTSVOICE_INDEX_EXT_ZIP;
				toCheckPostfix = ""; //$NON-NLS-1$
				unzipDir = true;
			}
			if(parent != null) {
				parent.mkdirs();
				// ".nomedia" indicates there are no pictures and no music to list in this dir for the Gallery and Music apps
				if( preventMediaIndexing ) {				
					try {
						new File(parent, ".nomedia").createNewFile();//$NON-NLS-1$	
					} catch (IOException e) {
						// swallow io exception
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
				entry.baseName = fileName.substring(0, ls);
				entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
				entry.unzip = unzipDir;
				SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
				try {
					Date d = format.parse(item.getDate());
					entry.dateModified = d.getTime();
				} catch (ParseException e1) {
				}
				try {
					entry.sizeMB = Double.parseDouble(item.getSize());
				} catch (NumberFormatException e1) {
				}
				entry.parts = 1;
				if(item.getParts() != null){
					entry.parts = Integer.parseInt(item.getParts());
				}
				entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
			}
			return entry;
		}
	}
	
}
