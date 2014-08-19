package net.osmand.plus.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;

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
	

	public static IndexFileList getIndexesList(Context ctx) {
		PackageManager pm =ctx.getPackageManager();
		AssetManager amanager = ctx.getAssets();
		IndexFileList result = downloadIndexesListFromInternet((OsmandApplication) ctx.getApplicationContext());
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
		try {
			String ext = DownloadActivityType.addVersionToExt(IndexConstants.TTSVOICE_INDEX_EXT_ZIP, IndexConstants.TTSVOICE_VERSION);
			String extvoice = DownloadActivityType.addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION);
			File voicePath = settings.getContext().getAppPath(IndexConstants.VOICE_INDEX_DIR); 
			// list = amanager.list("voice");
			String date = "";
			long dateModified = System.currentTimeMillis();
			try {
				ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
				dateModified =  new File(appInfo.sourceDir).lastModified();
				date = AndroidUtils.formatDate((Context) settings.getContext(), dateModified);
			} catch (NameNotFoundException e) {
				//do nothing...
			}
			Map<String, String> mapping = assetMapping(amanager);
			for (String key : mapping.keySet()) {
				String target = mapping.get(key);
				if (target.endsWith("-tts/_ttsconfig.p") && target.startsWith("voice/")) {
					String voice = target.substring("voice/".length(), target.length() - "/_ttsconfig.p".length());
					File destFile = new File(voicePath, voice + File.separatorChar + "_ttsconfig.p");
					result.add(new AssetIndexItem(voice +ext, "voice", date, dateModified, 
							"0.1", 1024*100, key, destFile.getPath(), DownloadActivityType.VOICE_FILE));
				} else if (target.endsWith("/_config.p") && target.startsWith("voice/")) {
					String voice = target.substring("voice/".length(), target.length() - "/_config.p".length());
					IndexItem item = result.getIndexFilesByName(key);
					if (item != null) {
						File destFile = new File(voicePath, voice + File.separatorChar + "_config.p");
						// always use bundled config
//						if (item.getTimestamp() > dateModified) {
//							continue;
//						}
						item.timestamp = dateModified;
						item.attachedItem = new AssetIndexItem(voice +extvoice, "voice", date, dateModified, "0.1", 1024*100, key, destFile.getPath(), 
								DownloadActivityType.VOICE_FILE);
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
	

	private static IndexFileList downloadIndexesListFromInternet(OsmandApplication ctx){
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files"); //$NON-NLS-1$
			try {
				String strUrl = ctx.getAppCustomization().getIndexesUrl();
				
				log.info(strUrl);
				URL url = new URL(strUrl ); 
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				parser.setInput(new GZIPInputStream(url.openStream()), "UTF-8"); //$NON-NLS-1$
				int next;
				while((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (next == XmlPullParser.START_TAG) {
						DownloadActivityType tp = DownloadActivityType.getIndexType(parser.getAttributeValue(null, "type"));
						if (tp != null) {
							IndexItem it = tp.parseIndexItem(ctx, parser);
							if(it != null) {
								result.add(it);
							}
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
				long dateModified, String size, long sizeL, String assetName, String destFile, DownloadActivityType type) {
			super(fileName, description, dateModified, size, sizeL, sizeL, type);
			this.dateModified = dateModified;
			this.assetName = assetName;
			this.destFile = destFile;
		}
		
		public long getDateModified() {
			return dateModified;
		}

		@Override
		public List<DownloadEntry> createDownloadEntry(OsmandApplication ctx, DownloadActivityType type, List<DownloadEntry> res) {
			res.add(new DownloadEntry(this, assetName, destFile, dateModified));
			return res;
		}
	}
	
	
}
