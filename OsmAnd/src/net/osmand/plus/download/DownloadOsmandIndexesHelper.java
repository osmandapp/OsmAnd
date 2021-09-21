package net.osmand.plus.download;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.provider.Settings.Secure;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import androidx.annotation.NonNull;

public class DownloadOsmandIndexesHelper {

	private final static Log log = PlatformUtil.getLog(DownloadOsmandIndexesHelper.class);

	public static class IndexFileList implements Serializable {
		private static final long serialVersionUID = 1L;

		private boolean downloadedFromInternet = false;
		IndexItem basemap;
		ArrayList<IndexItem> indexFiles = new ArrayList<IndexItem>();
		private String mapversion;

		private Comparator<IndexItem> comparator = new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem o1, IndexItem o2) {
				String object1 = o1.getFileName();
				String object2 = o2.getFileName();
				if (object1.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)) {
					if (object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)) {
						return object1.compareTo(object2);
					} else {
						return -1;
					}
				} else if (object2.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)) {
					return 1;
				}
				return object1.compareTo(object2);
			}
		};

		public void setDownloadedFromInternet(boolean downloadedFromInternet) {
			this.downloadedFromInternet = downloadedFromInternet;
		}

		public boolean isDownloadedFromInternet() {
			return downloadedFromInternet;
		}

		public void setMapVersion(String mapversion) {
			this.mapversion = mapversion;
		}

		@SuppressLint("DefaultLocale")
		public void add(IndexItem indexItem) {
			indexFiles.add(indexItem);
			if (indexItem.getFileName().toLowerCase().startsWith("world_basemap")) {
				basemap = indexItem;
			}
		}

		public void sort() {
			Collections.sort(indexFiles, comparator);
		}

		public boolean isAcceptable() {
			return (indexFiles != null && !indexFiles.isEmpty()) || (mapversion != null);
		}

		public List<IndexItem> getIndexFiles() {
			return indexFiles;
		}

		public IndexItem getBasemap() {
			return basemap;
		}

		public boolean isIncreasedMapVersion() {
			try {
				int mapVersionInList = Integer.parseInt(mapversion);
				return IndexConstants.BINARY_MAP_VERSION < mapVersionInList;
			} catch (NumberFormatException e) {
				//ignore this...
			}
			return false;
		}

	}

	public static IndexFileList getIndexesList(OsmandApplication app) {
		IndexFileList indexes = downloadIndexesListFromInternet(app);
		if (indexes == null) {
			indexes = new IndexFileList();
		} else {
			indexes.setDownloadedFromInternet(true);
		}
		// Add all tts files from assets and data folder
		addTtsVoiceIndexes(app, indexes);

		indexes.sort();
		return indexes;
	}

	private static void addTtsVoiceIndexes(OsmandApplication app, IndexFileList indexes) {
		List<IndexItem> ttsIndexes = listTtsVoiceIndexes(app, false);
		for (IndexItem index : ttsIndexes) {
			indexes.add(index);
		}
	}

	@NonNull
	public static List<IndexItem> listTtsVoiceIndexes(OsmandApplication app) {
		return listTtsVoiceIndexes(app, true);
	}

	@NonNull
	private static List<IndexItem> listTtsVoiceIndexes(OsmandApplication app, boolean sort) {
		List<IndexItem> ttsList = new ArrayList<>();

		try {
			List<AssetEntry> bundledAssets = getBundledAssets(app.getAssets());
			ttsList.addAll(listDefaultTtsVoiceIndexes(app, bundledAssets));
			ttsList.addAll(listCustomTtsVoiceIndexes(app, bundledAssets));
		} catch (IOException | XmlPullParserException e) {
			log.error("Error while loading tts files from assets", e);
		}

		if (sort) {
			Collections.sort(ttsList, DownloadResourceGroup.getComparator(app));
		}

		return ttsList;
	}

	@NonNull
	public static List<AssetEntry> getBundledAssets(AssetManager assetManager) throws XmlPullParserException, IOException {
		XmlPullParser xmlParser = XmlPullParserFactory.newInstance().newPullParser();
		InputStream isBundledAssetsXml = assetManager.open("bundled_assets.xml");
		xmlParser.setInput(isBundledAssetsXml, "UTF-8");
		List<AssetEntry> assets = new ArrayList<>();
		int next;
		while ((next = xmlParser.next()) != XmlPullParser.END_DOCUMENT) {
			if (next == XmlPullParser.START_TAG && xmlParser.getName().equals("asset")) {
				final String source = xmlParser.getAttributeValue(null, "source");
				final String destination = xmlParser.getAttributeValue(null, "destination");
				final String combinedMode = xmlParser.getAttributeValue(null, "mode");
				assets.add(new AssetEntry(source, destination, combinedMode));
			}
		}
		isBundledAssetsXml.close();
		return assets;
	}

	@NonNull
	private static List<IndexItem> listDefaultTtsVoiceIndexes(OsmandApplication app, List<AssetEntry> bundledAssets) {
		List<IndexItem> defaultTTS = new ArrayList<>();
		File voiceDirPath = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		long installDate = getInstallDate(app);

		for (AssetEntry asset : bundledAssets) {
			String target = asset.destination;
			boolean isTTS = target.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS)
					&& target.startsWith(IndexConstants.VOICE_INDEX_DIR)
					&& target.contains(IndexConstants.VOICE_PROVIDER_SUFFIX);
			if (!isTTS) {
				continue;
			}

			String lang = target.substring(IndexConstants.VOICE_INDEX_DIR.length(),
					target.indexOf(IndexConstants.VOICE_PROVIDER_SUFFIX));
			File destFile = new File(voiceDirPath, target.substring(IndexConstants.VOICE_INDEX_DIR.length(),
					target.indexOf("/", IndexConstants.VOICE_INDEX_DIR.length())) + "/" + lang + "_tts.js");
			defaultTTS.add(new AssetIndexItem(lang + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS,
					"voice", installDate, "0.1", destFile.length(), asset.source,
					destFile.getPath(), DownloadActivityType.VOICE_FILE));
		}

		return defaultTTS;
	}

	@NonNull
	private static List<IndexItem> listCustomTtsVoiceIndexes(OsmandApplication app, List<AssetEntry> bundledAssets) {
		File voiceDirPath = app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		LocalIndexHelper localIndexHelper = new LocalIndexHelper(app);
		List<LocalIndexInfo> localIndexes = new ArrayList<>();
		List<IndexItem> customTTS = new ArrayList<>();
		long installDate = getInstallDate(app);

		localIndexHelper.loadVoiceData(voiceDirPath, localIndexes, false, true, true,
				app.getResourceManager().getIndexFiles(), null);
		for (LocalIndexInfo indexInfo : localIndexes) {
			if (!indexInfo.getFileName().contains("tts")) {
				continue;
			}
			boolean isCustomVoice = true;
			for (AssetEntry assetEntry : bundledAssets) {
				if (assetEntry.destination.contains("/" + indexInfo.getFileName() + "/")) {
					isCustomVoice = false;
					break;
				}
			}
			if (isCustomVoice) {
				String fileName = indexInfo.getFileName().replace("-", "_") + ".js";
				File file = new File(voiceDirPath, indexInfo.getFileName() + "/" + fileName);
				customTTS.add(new AssetIndexItem(fileName, "voice", installDate, "0.1",
						file.length(), "", file.getPath(), DownloadActivityType.VOICE_FILE));
			}
		}

		return customTTS;
	}

	private static IndexFileList downloadIndexesListFromInternet(OsmandApplication ctx) {
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files");
			try {
				String strUrl = ctx.getAppCustomization().getIndexesUrl();
				long nd = ctx.getAppInitializer().getFirstInstalledDays();
				if (nd > 0) {
					strUrl += "&nd=" + nd;
				}
				strUrl += "&ns=" + ctx.getAppInitializer().getNumberOfStarts();
				try {
					strUrl += "&aid=" + Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
				} catch (Exception e) {
					e.printStackTrace();
				}
				log.info(strUrl);
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				URLConnection connection = NetworkUtils.getHttpURLConnection(strUrl);
				InputStream in = connection.getInputStream();
				GZIPInputStream gzin = new GZIPInputStream(in);
				parser.setInput(gzin, "UTF-8");
				int next;
				while ((next = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (next == XmlPullParser.START_TAG) {
						String attrValue = parser.getAttributeValue(null, "type");
						DownloadActivityType tp = DownloadActivityType.getIndexType(attrValue);
						if (tp != null) {
							IndexItem it = tp.parseIndexItem(ctx, parser);
							if (it != null) {
								result.add(it);
							}
						} else if ("osmand_regions".equals(parser.getName())) {
							String mapVersion = parser.getAttributeValue(null, "mapversion");
							result.setMapVersion(mapVersion);
						}
					}
				}
				result.sort();
				gzin.close();
				in.close();
			} catch (IOException | XmlPullParserException e) {
				log.error("Error while loading indexes from repository", e);
				return null;
			}

			if (result.isAcceptable()) {
				return result;
			} else {
				return null;
			}
		} catch (RuntimeException e) {
			log.error("Error while loading indexes from repository", e);
			return null;
		}
	}

	private static long getInstallDate(OsmandApplication app) {
		PackageManager packageManager = app.getPackageManager();
		long appInstallDate = System.currentTimeMillis();
		try {
			ApplicationInfo appInfo = packageManager.getApplicationInfo(app.getPackageName(), 0);
			appInstallDate = new File(appInfo.sourceDir).lastModified();
		} catch (NameNotFoundException e) {
			log.error(e);
		}
		return appInstallDate;
	}

	public static class AssetIndexItem extends IndexItem {

		private final String assetName;
		private final String destFile;
		private final long dateModified;

		public AssetIndexItem(String fileName, String description, long dateModified, String size,
		                      long sizeL, String assetName, String destFile, DownloadActivityType type) {
			super(fileName, description, dateModified, size, sizeL, sizeL, type);
			this.dateModified = dateModified;
			this.assetName = assetName;
			this.destFile = destFile;
		}

		public long getDateModified() {
			return dateModified;
		}

		@Override
		public DownloadEntry createDownloadEntry(OsmandApplication ctx) {
			return new DownloadEntry(assetName, destFile, dateModified);
		}

		public String getDestFile() {
			return destFile;
		}
	}

	public static class AssetEntry {
		public final String source;
		public final String destination;
		public final String combinedMode;

		public AssetEntry(String source, String destination, String combinedMode) {
			this.source = source;
			this.destination = destination;
			this.combinedMode = combinedMode;
		}
	}
}