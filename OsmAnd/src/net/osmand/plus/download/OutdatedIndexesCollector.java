package net.osmand.plus.download;

import androidx.annotation.NonNull;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;
import net.osmand.plus.resources.ResourceManager;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OutdatedIndexesCollector {

	private static final Log LOG = PlatformUtil.getLog(OutdatedIndexesCollector.class);

	private final OsmandApplication app;
	private Map<String, String> indexFileNames = new LinkedHashMap<>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<>();

	@NonNull
	public static OutdatedIndexesCollection collect(@NonNull OsmandApplication app,
	                                                @NonNull OutdatedIndexesCollection outdatedIndexes) {
		return collect(app, outdatedIndexes.all(), outdatedIndexes.deprecated());
	}

	@NonNull
	public static OutdatedIndexesCollection collect(@NonNull OsmandApplication app,
	                                                @NonNull List<IndexItem> indexItems,
	                                                @NonNull List<IndexItem> deprecatedItems) {
		return new OutdatedIndexesCollector(app).collect(indexItems, deprecatedItems);
	}

	public OutdatedIndexesCollector(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	private OutdatedIndexesCollection collect(@NonNull List<IndexItem> indexItems,
	                                          @NonNull List<IndexItem> deprecatedItems) {
		initAlreadyLoadedFiles();
		List<IndexItem> outdatedIndexes = new ArrayList<>();
		List<IndexItem> activatedOutdatedIndexes = new ArrayList<>();

		DateFormat format = app.getResourceManager().getDateFormat();
		for (IndexItem item : indexItems) {
			if (checkIfItemOutdated(item, format)) {
				outdatedIndexes.add(item);
				if (checkIfItemActivated(item)) {
					activatedOutdatedIndexes.add(item);
				}
			}
		}
		List<DownloadItem> groupedIndexes = groupItemsByRegion(outdatedIndexes);
		List<DownloadItem> groupedActivatedIndexes = groupItemsByRegion(activatedOutdatedIndexes);

		List<IndexItem> deprecatedActivatedIndexes = new ArrayList<>();
		for (IndexItem item : deprecatedItems) {
			if (checkIfItemActivated(item)) {
				deprecatedActivatedIndexes.add(item);
			}
		}
		return new OutdatedIndexesCollection(outdatedIndexes,
				activatedOutdatedIndexes, groupedIndexes,
				groupedActivatedIndexes, deprecatedActivatedIndexes);
	}

	public void initAlreadyLoadedFiles() {
		ResourceManager resourceManager = app.getResourceManager();
		DateFormat dateFormat = resourceManager.getDateFormat();
		Map<String, String> indexFileNames = resourceManager.getIndexFileNames();
		Map<String, String> indexActivatedFileNames = resourceManager.getIndexFileNames();

		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT, indexActivatedFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR),
				IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT, indexActivatedFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR),
				IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT, indexActivatedFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.WEATHER_FORECAST_DIR),
				IndexConstants.WEATHER_EXT, indexActivatedFileNames);

		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT, indexFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.TILES_INDEX_DIR), IndexConstants.SQLITE_EXT,
				indexFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR),
				IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT, indexFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR),
				IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT, indexFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.GEOTIFF_DIR),
				IndexConstants.TIF_EXT, indexFileNames);

		app.getResourceManager().getBackupIndexes(indexFileNames);
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
	}

	@NonNull
	private Map<String, String> listWithAlternatives(java.text.DateFormat dateFormat, File file,
	                                                 String ext, Map<String, String> files) {
		if (file.isDirectory()) {
			file.list((dir, filename) -> {
				if (filename.endsWith(ext)) {
					String date = dateFormat.format(findFileInDir(new File(dir, filename)).lastModified());
					files.put(filename, date);
					return true;
				} else {
					return false;
				}
			});

		}
		return files;
	}

	@NonNull
	private File findFileInDir(@NonNull File file) {
		if (file.isDirectory()) {
			File[] lf = file.listFiles();
			if (lf != null) {
				for (File f : lf) {
					if (f.isFile()) {
						return f;
					}
				}
			}
		}
		return file;
	}

	@NonNull
	private List<DownloadItem> groupItemsByRegion(@NonNull List<IndexItem> items) {
		OsmandRegions regions = app.getRegions();
		List<DownloadItem> result = new ArrayList<>();
		Map<UpdateKey, List<IndexItem>> map = new LinkedHashMap<>();
		for (IndexItem item : items) {
			String baseName = item.getBasename();
			WorldRegion countryRegion = regions.getCountryRegionDataByDownloadName(baseName);
			if (countryRegion != null) {
				UpdateKey key = new UpdateKey(item.getType(), countryRegion);
				map.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
			} else {
				result.add(item);
			}
		}
		for (Map.Entry<UpdateKey, List<IndexItem>> entry : map.entrySet()) {
			UpdateKey key = entry.getKey();
			List<IndexItem> indexItems = entry.getValue();
			if (indexItems != null && !indexItems.isEmpty()) {
				if (indexItems.size() > 1) {
					result.add(new MultipleDownloadItem(key.region(), new ArrayList<>(indexItems), key.type()));
				} else {
					result.add(indexItems.get(0));
				}
			}
		}
		return result;
	}

	public boolean checkIfItemOutdated(@NonNull IndexItem item,
	                                   @NonNull java.text.DateFormat format) {
		boolean outdated = false;
		item.setDownloaded(false);
		item.setOutdated(false);

		String sfName = item.getTargetFileName();
		String indexActivatedDate = indexActivatedFileNames.get(sfName);
		String indexFilesDate = indexFileNames.get(sfName);
		if (indexActivatedDate == null && indexFilesDate == null) {
			return false;
		}
		item.setDownloaded(true);
		String date = item.getDate(format);
		boolean parsed = false;
		if (indexActivatedDate != null) {
			try {
				item.setLocalTimestamp(format.parse(indexActivatedDate).getTime());
				parsed = true;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (!parsed && indexFilesDate != null) {
			try {
				item.setLocalTimestamp(format.parse(indexFilesDate).getTime());
				parsed = true;
			} catch (ParseException e) {
				LOG.error(e);
			}
		}
		if (date != null && !date.equals(indexActivatedDate) && !date.equals(indexFilesDate)) {
			long oldItemSize = 0;
			long itemSize = item.getContentSize();
			if ((item.getType() == DownloadActivityType.NORMAL_FILE && !item.extra)
					|| item.getType() == DownloadActivityType.ROADS_FILE
					|| item.getType() == DownloadActivityType.WIKIPEDIA_FILE
					|| item.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE
					|| item.getType() == DownloadActivityType.DEPTH_MAP_FILE
					|| item.getType() == DownloadActivityType.WEATHER_FORECAST
					|| item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
				outdated = true;
			} else if (item.getType() == DownloadActivityType.WIKIVOYAGE_FILE
					|| item.getType() == DownloadActivityType.TRAVEL_FILE) {
				oldItemSize = app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR +
						item.getTargetFileName()).length();
				if (itemSize != oldItemSize) {
					outdated = true;
				}
			} else {
				if (parsed && item.getTimestamp() > item.getLocalTimestamp()) {
					outdated = true;
				} else if (item.getType() == DownloadActivityType.VOICE_FILE) {
					if (item instanceof AssetIndexItem) {
						File file = new File(((AssetIndexItem) item).getDestFile());
						oldItemSize = file.length();
					}
				} else if (item.getType() == DownloadActivityType.FONT_FILE) {
					oldItemSize = new File(app.getAppPath(IndexConstants.FONT_INDEX_DIR), item.getTargetFileName()).length();
				} else {
					oldItemSize = app.getAppPath(item.getTargetFileName()).length();
				}
				if (!parsed && itemSize != oldItemSize) {
					outdated = true;
				}
			}
			if (outdated) {
				logItemUpdateInfo(item, format, itemSize, oldItemSize);
			}
		}
		item.setOutdated(outdated);
		return outdated;
	}

	private boolean checkIfItemActivated(@NonNull IndexItem item) {
		return indexActivatedFileNames.containsKey(item.getTargetFileName());
	}

	private void logItemUpdateInfo(@NonNull IndexItem item, @NonNull DateFormat format,
	                               long itemSize, long oldItemSize) {
		String date = item.getDate(format);
		String sfName = item.getTargetFileName();
		String indexActivatedDate = indexActivatedFileNames.get(sfName);
		String indexFilesDate = indexFileNames.get(sfName);
		LOG.info("name " + item.getFileName() + " timestamp " + item.timestamp + " localTimestamp " + item.localTimestamp + " date " + date
				+ " indexActivatedDate " + indexActivatedDate + " indexFilesDate " + indexFilesDate
				+ " itemSize " + itemSize + " oldItemSize " + oldItemSize);
	}

	private record UpdateKey(DownloadActivityType type, WorldRegion region) {}
}
