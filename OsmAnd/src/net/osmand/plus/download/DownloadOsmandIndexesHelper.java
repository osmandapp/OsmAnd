package net.osmand.plus.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.Version;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;

public class DownloadOsmandIndexesHelper {
	private final static Log log = PlatformUtil.getLog(DownloadOsmandIndexesHelper.class);
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
	

	public static IndexFileList getIndexesList(Context ctx) {
		PackageManager pm =ctx.getPackageManager();
		AssetManager amanager = ctx.getAssets();
		String versionUrlParam = Version.getVersionAsURLParam(((OsmandApplication) ctx.getApplicationContext()));
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
	
	private static Map<String, String>  assetMapping(AssetManager assetManager) throws XmlPullParserException, IOException {
		XmlPullParser xmlParser = XmlPullParserFactory.newInstance().newPullParser(); 
		InputStream isBundledAssetsXml = assetManager.open("bundled_assets.xml");
		xmlParser.setInput(isBundledAssetsXml, "UTF-8");
		Map<String, String> assets = new HashMap<String, String>();
		int next = 0;
		while ((next = xmlParser.next()) != XmlPullParser.END_DOCUMENT) {
			if (next == XmlPullParser.START_TAG && xmlParser.getName().equals("asset")) {
				final String source = xmlParser.getAttributeValue(null, "source");
				final String destination = xmlParser.getAttributeValue(null, "destination");
				assets.put(source, destination);
			}
		}
		isBundledAssetsXml.close();
		return assets;
	}
	
	private static void listVoiceAssets(IndexFileList result, AssetManager amanager, PackageManager pm, 
			OsmandSettings settings) {
		String[] list;
		try {
			String ext = IndexItem.addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION);
			String extvoice = IndexItem.addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION);
			File voicePath = settings.getContext().getAppPath(IndexConstants.VOICE_INDEX_DIR); 
			list = amanager.list("voice");
			String date = "";
			long dateModified = System.currentTimeMillis();
			try {
				ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
				dateModified =  new File(appInfo.sourceDir).lastModified();
				date = Algorithms.formatDate(dateModified);
			} catch (NameNotFoundException e) {
				//do nothing...
			}
			Map<String, String> mapping = assetMapping(amanager);
			for (String key : mapping.keySet()) {
				String target = mapping.get(key);
				if (target.endsWith("-tts/_ttsconfig.p") && target.startsWith("voice/")) {
					String voice = target.substring("voice/".length(), target.length() - "/_ttsconfig.p".length());
					File destFile = new File(voicePath, voice + File.separatorChar + "_ttsconfig.p");
					result.add(new AssetIndexItem(voice +ext, "voice", date, dateModified, "0.1", "", key, destFile.getPath()));
				} else if (target.endsWith("/_config.p") && target.startsWith("voice/")) {
					String voice = target.substring("voice/".length(), target.length() - "/_config.p".length());
					IndexItem item = result.getIndexFilesByName(key);
					if (item != null) {
						File destFile = new File(voicePath, voice + File.separatorChar + "_config.p");
						try {
							Date d = Algorithms.getDateFormat().parse(item.getDate());
							if (d.getTime() > dateModified) {
								continue;
							}
						} catch (Exception es) {
							log.error("Parse exception", es);
						}
						item.date = date;
						item.attachedItem = new AssetIndexItem(voice +extvoice, "voice", date, dateModified, "0.1", "", key, destFile.getPath());
					}
				}
			}
			result.sort();
		} catch (IOException e) {
			log.error("Error while loading tts files from assets", e); //$NON-NLS-1$
		} catch (XmlPullParserException e) {
			log.error("Error while loading tts files from assets", e); //$NON-NLS-1$
		}
	}
	
	private static DownloadActivityType getIndexType(String tagName){
		if("region".equals(tagName) ||
							"multiregion".equals(tagName)) {
			return DownloadActivityType.NORMAL_FILE;
		} else if("road_region".equals(tagName) ) {
			return DownloadActivityType.ROADS_FILE;
		} else if("hillshade".equals(tagName) ) {
			return DownloadActivityType.HILLSHADE_FILE;
		}
		return null;
	}

	private static IndexFileList downloadIndexesListFromInternet(String versionAsUrl){
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			try {
				String strUrl = "http://"+IndexConstants.INDEX_DOWNLOAD_DOMAIN+"/get_indexes?" + versionAsUrl; //$NON-NLS-1$
				log.info(strUrl);
				URL url = new URL(strUrl ); 
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
							date = reparseDate(date);
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

	private static String reparseDate(String date) {
		try {
			Date d = simpleDateFormat.parse(date);
			return Algorithms.formatDate(d.getTime());
		} catch (ParseException e) {
			return date;
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
		
		public long getDateModified() {
			return dateModified;
		}

		@Override
		public boolean isAccepted(){
			return true;
		}
		
		@Override
		public List<DownloadEntry> createDownloadEntry(ClientContext ctx, DownloadActivityType type, List<DownloadEntry> res) {
			res.add(new DownloadEntry(assetName, destFile, dateModified));
			return res;
		}
	}
	
	
}
