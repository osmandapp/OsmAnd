package net.osmand.plus;

import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.LogUtil;
import net.osmand.data.IndexConstants;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

public class DownloadOsmandIndexesHelper {
	private final static Log log = LogUtil.getLog(DownloadOsmandIndexesHelper.class);
	
	public static Map<String, IndexItem> downloadIndexesListFromInternet(){
		try {
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			TreeMap<String, IndexItem> indexFiles = new TreeMap<String, IndexItem>(new Comparator<String>(){
				private static final long serialVersionUID = 1L;

				@Override
				public int compare(String object1, String object2) {
					if(object1.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
						if(object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
							return object1.compareTo(object2);
						} else {
							return -1;
						}
					} else if(object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
						return 1;
					}
					return object1.compareTo(object2);
				}
				
			});
				try {
					URL url = new URL("http://download.osmand.net/get_indexes"); //$NON-NLS-1$
					XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
					parser.setInput(url.openStream(), "UTF-8"); //$NON-NLS-1$
					int next;
					while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
						if(next == XmlPullParser.START_TAG && (parser.getName().equals("region") ||
								parser.getName().equals("multiregion"))) { //$NON-NLS-1$
							String name = parser.getAttributeValue(null, "name"); //$NON-NLS-1$
							String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
							String date = parser.getAttributeValue(null, "date"); //$NON-NLS-1$
							String description = parser.getAttributeValue(null, "description"); //$NON-NLS-1$
							String parts = parser.getAttributeValue(null, "parts"); //$NON-NLS-1$
							IndexItem indexItem = new IndexItem(name, description, date, size, parts);
							if(indexItem.isAccepted()){
								indexFiles.put(name, indexItem);
							}
						}
					}
				} catch (IOException e) {
					log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
					return null;
				} catch (XmlPullParserException e) {
					log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
					return null;
				}
			
			if (indexFiles != null && !indexFiles.isEmpty()) {
				return indexFiles;
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			log.error("Error while loading indexes from repository", e); //$NON-NLS-1$
			return null;
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
			if (fileName.endsWith(IndexConstants.POI_INDEX_EXT) || fileName.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)) {
				s = ctx.getString(R.string.poi);
			} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
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
			if (fileName.endsWith(addVersionToExt(IndexConstants.POI_INDEX_EXT, IndexConstants.POI_TABLE_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.POI_INDEX_EXT_ZIP, IndexConstants.POI_TABLE_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT,IndexConstants.BINARY_MAP_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP,IndexConstants.BINARY_MAP_VERSION)) //
				|| fileName.endsWith(addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION))
				|| fileName.endsWith(addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION))) {
				return true;
			}
			return false;
		}

		private String addVersionToExt(String ext, int version) {
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
		
	}
	
}
