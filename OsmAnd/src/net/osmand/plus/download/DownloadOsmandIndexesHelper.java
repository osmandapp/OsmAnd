package net.osmand.plus.download;

import static net.osmand.IndexConstants.TTSVOICE_INDEX_EXT_JS;
import static net.osmand.IndexConstants.VOICE_INDEX_DIR;
import static net.osmand.IndexConstants.VOICE_INDEX_EXT_ZIP;
import static net.osmand.plus.download.DownloadActivityType.VOICE_FILE;
import static net.osmand.plus.download.local.LocalItemType.VOICE_DATA;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class DownloadOsmandIndexesHelper {

	private static final Log log = PlatformUtil.getLog(DownloadOsmandIndexesHelper.class);

	public static class IndexFileList implements Serializable {
		private static final long serialVersionUID = 1L;

		private boolean downloadedFromInternet;
		IndexItem basemap;
		ArrayList<IndexItem> indexFiles = new ArrayList<IndexItem>();
		private String mapversion;

		private final Comparator<IndexItem> comparator = new Comparator<IndexItem>() {
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

	@NonNull
	public static Map<String, IndexItem> getSupportedTtsByLanguages(@NonNull OsmandApplication app) {
		Map<String, IndexItem> byLanguages = new HashMap<>();
		for (IndexItem indexItem : listTtsVoiceIndexes(app, false)) {
			String baseName = indexItem.getBasename();
			String langCode = baseName.replaceAll("-tts", "");
			byLanguages.put(langCode, indexItem);
		}
		return byLanguages;
	}

	public static void downloadTtsWithoutInternet(@NonNull OsmandApplication app, @NonNull IndexItem item) {
		try {
			IndexItem.DownloadEntry de = item.createDownloadEntry(app);
			ResourceManager.copyAssets(app.getAssets(), de.assetName, de.targetFile);
			boolean changedDate = de.targetFile.setLastModified(de.dateModified);
			if (!changedDate) {
				log.error("Set last timestamp is not supported");
			}
		} catch (IOException e) {
			log.error("Copy exception", e);
		}
	}

	private static void addTtsVoiceIndexes(@NonNull OsmandApplication app, @NonNull IndexFileList indexes) {
		List<IndexItem> items = listTtsVoiceIndexes(app, false);
		for (IndexItem item : items) {
			indexes.add(item);
		}
	}

	@NonNull
	public static List<IndexItem> listTtsVoiceIndexes(@NonNull OsmandApplication app) {
		return listTtsVoiceIndexes(app, true);
	}

	@NonNull
	private static List<IndexItem> listTtsVoiceIndexes(@NonNull OsmandApplication app, boolean sort) {
		List<IndexItem> items = new ArrayList<>();
		try {
			List<AssetEntry> bundledAssets = getBundledAssets(app.getAssets());
			items.addAll(listDefaultTtsVoiceIndexes(app, bundledAssets));
			items.addAll(listCustomTtsVoiceIndexes(app, bundledAssets));
		} catch (Exception e) {
			log.error("Error while loading tts files from assets", e);
		}
		if (sort) {
			items.sort(DownloadResourceGroup.getComparator(app));
		}
		return items;
	}

	@NonNull
	public static List<AssetEntry> getBundledAssets(@NonNull AssetManager assetManager) throws XmlPullParserException, IOException {
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
		XmlPullParser xmlParser = XmlPullParserFactory.newInstance().newPullParser();
		InputStream isBundledAssetsXml = assetManager.open("bundled_assets.xml");
		xmlParser.setInput(isBundledAssetsXml, "UTF-8");
		List<AssetEntry> assets = new ArrayList<>();
		int next;
		while ((next = xmlParser.next()) != XmlPullParser.END_DOCUMENT) {
			if (next == XmlPullParser.START_TAG && xmlParser.getName().equals("asset")) {
				String source = xmlParser.getAttributeValue(null, "source");
				String destination = xmlParser.getAttributeValue(null, "destination");
				String combinedMode = xmlParser.getAttributeValue(null, "mode");
				AssetEntry ae = new AssetEntry(source, destination, combinedMode);
				String version = xmlParser.getAttributeValue(null, "version");
				if (!Algorithms.isEmpty(version)) {
					try {
						ae.version = DATE_FORMAT.parse(version);
					} catch (ParseException e) {
						log.error(e.getMessage(), e);
					}
				}
				assets.add(ae);
			}
		}
		isBundledAssetsXml.close();
		return assets;
	}

	@NonNull
	private static List<IndexItem> listDefaultTtsVoiceIndexes(@NonNull OsmandApplication app, @NonNull List<AssetEntry> bundledAssets) {
		List<IndexItem> defaultTTS = new ArrayList<>();
		File voiceDirPath = app.getAppPath(VOICE_INDEX_DIR);
		long installDate = getInstallDate(app);

		for (AssetEntry asset : bundledAssets) {
			String target = asset.destination;
			boolean isTTS = target.endsWith(TTSVOICE_INDEX_EXT_JS)
					&& target.startsWith(VOICE_INDEX_DIR)
					&& target.contains(IndexConstants.VOICE_PROVIDER_SUFFIX);
			if (!isTTS) {
				continue;
			}

			String lang = target.substring(VOICE_INDEX_DIR.length(),
					target.indexOf(IndexConstants.VOICE_PROVIDER_SUFFIX));
			String ttsIndexFolder = target.substring(VOICE_INDEX_DIR.length(),
					target.indexOf("/", VOICE_INDEX_DIR.length()));
			File destFile = new File(voiceDirPath, ttsIndexFolder + "/" + lang + "_tts.js");
			IndexItem ttsIndex = new AssetIndexItem(lang + "_" + TTSVOICE_INDEX_EXT_JS,
					"voice", installDate, "0.1", destFile.length(), asset.source,
					destFile.getPath(), VOICE_FILE);
			ttsIndex.setDownloaded(destFile.exists());
			defaultTTS.add(ttsIndex);
		}

		return defaultTTS;
	}

	@NonNull
	private static List<IndexItem> listCustomTtsVoiceIndexes(OsmandApplication app, List<AssetEntry> bundledAssets) {
		File voiceDirPath = app.getAppPath(VOICE_INDEX_DIR);
		LocalIndexHelper localIndexHelper = new LocalIndexHelper(app);
		List<LocalItem> localItems = new ArrayList<>();
		List<IndexItem> customTTS = new ArrayList<>();
		long installDate = getInstallDate(app);

		ResourceManager resourceManager = app.getResourceManager();
		if (resourceManager != null) {
			localIndexHelper.loadVoiceData(voiceDirPath, localItems,
					true, true, resourceManager.getIndexFiles(), null);
		}
		for (LocalItem item : localItems) {
			if (!item.getFileName().contains("tts")) {
				continue;
			}
			boolean isCustomVoice = true;
			for (AssetEntry assetEntry : bundledAssets) {
				if (assetEntry.destination.contains("/" + item.getFileName() + "/")) {
					isCustomVoice = false;
					break;
				}
			}
			if (isCustomVoice) {
				String fileName = item.getFileName().replace("-", "_") + ".js";
				File file = new File(voiceDirPath, item.getFileName() + "/" + fileName);
				IndexItem customVoiceIndex = new AssetIndexItem(fileName, "voice", installDate, "0.1",
						file.length(), "", file.getPath(), VOICE_FILE);
				customVoiceIndex.setDownloaded(true);
				customTTS.add(customVoiceIndex);
			}
		}

		return customTTS;
	}

	@NonNull
	public static List<IndexItem> listLocalRecordedVoiceIndexes(OsmandApplication app) {
		File voiceDirPath = app.getAppPath(VOICE_INDEX_DIR);
		LocalIndexHelper localIndexHelper = new LocalIndexHelper(app);
		List<LocalItem> localItems = new ArrayList<>();
		List<IndexItem> recordedVoiceList = new ArrayList<>();

		localIndexHelper.loadVoiceData(voiceDirPath, localItems, true, true,
				app.getResourceManager().getIndexFiles(), null);
		for (LocalItem item : localItems) {
			if (item.getType() != VOICE_DATA || item.getFileName().contains("tts")) {
				continue;
			}

			String recordedZipName = item.getFileName() + "_0" + VOICE_INDEX_EXT_ZIP;
			String ttsFileName = item.getFileName() + "_" + TTSVOICE_INDEX_EXT_JS;
			File ttsFile = new File(voiceDirPath, item.getFileName() + "/" + ttsFileName);
			long installDate = ttsFile.lastModified();
			IndexItem localRecordedVoiceIndex = new IndexItem(recordedZipName, "", installDate,
					"", 0, 0, VOICE_FILE, false, null, false);
			localRecordedVoiceIndex.setDownloaded(true);
			recordedVoiceList.add(localRecordedVoiceIndex);
		}

		return recordedVoiceList;
	}

	private static IndexFileList downloadIndexesListFromInternet(@NonNull OsmandApplication app) {
		try {
			IndexFileList result = new IndexFileList();
			log.debug("Start loading list of index files");
			try {
				String strUrl = app.getAppCustomization().getIndexesUrl();
				long nd = app.getAppInitializer().getFirstInstalledDays();
				if (nd > 0) {
					strUrl += "&nd=" + nd;
				}
				strUrl += "&ns=" + app.getAppInitializer().getNumberOfStarts();
				try {
					if (app.isUserAndroidIdAllowed()) {
						strUrl += "&aid=" + app.getUserAndroidId();
					}
				} catch (Exception e) {
					log.error(e);
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
						DownloadActivityType type = DownloadActivityType.getIndexType(attrValue);
						// ignore old DEPTH_CONTOUR_FILE
						if (type != null && type != DownloadActivityType.DEPTH_CONTOUR_FILE) {
							IndexItem item = type.parseIndexItem(app, parser);
							if (item != null) {
								result.add(item);
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
			super(fileName, description, dateModified, size, sizeL, sizeL, type, false, null);
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
		public Date version = null;

		public AssetEntry(String source, String destination, String combinedMode) {
			this.source = source;
			this.destination = destination;
			this.combinedMode = combinedMode;
		}
	}
}