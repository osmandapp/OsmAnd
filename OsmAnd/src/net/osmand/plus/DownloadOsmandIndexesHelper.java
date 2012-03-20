package net.osmand.plus;

import java.io.IOException;
import java.net.URL;

import net.osmand.LogUtil;
import net.osmand.data.IndexConstants;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

public class DownloadOsmandIndexesHelper {
	private final static Log log = LogUtil.getLog(DownloadOsmandIndexesHelper.class);
	
	public static IndexFileList downloadIndexesListFromInternet(String versionAsUrl){
		try {
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			IndexFileList result = new IndexFileList();
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
