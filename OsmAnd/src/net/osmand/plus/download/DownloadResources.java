package net.osmand.plus.download;

import static net.osmand.binary.BinaryMapIndexReader.DETAILED_MAP_MIN_ZOOM;
import static net.osmand.plus.download.DownloadResourceGroupType.REGION_MAPS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.ResourceManager.BinaryMapReaderResource;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DownloadResources extends DownloadResourceGroup {

	public boolean isDownloadedFromInternet;
	public boolean downloadFromInternetFailed;
	public boolean mapVersionIsIncreased;
	public OsmandApplication app;
	private Map<String, String> indexFileNames = new LinkedHashMap<>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<>();
	private List<IndexItem> rawResources;
	private Map<WorldRegion, List<IndexItem>> groupByRegion;
	private List<IndexItem> itemsToUpdate = new ArrayList<>();
	public static final String WORLD_SEAMARKS_KEY = "world_seamarks";
	public static final String WORLD_SEAMARKS_NAME = "World_seamarks";
	public static final String WORLD_SEAMARKS_OLD_KEY = "world_seamarks_basemap";
	public static final String WORLD_SEAMARKS_OLD_NAME = "World_seamarks_basemap";
	public static final String WORLD_CONTOURS_SUFFIX = "world_contours";
	public static final String NAUTICAL_DEPTH_POINTS_SUFFIX = "points";
	public static final String WIKIVOYAGE_FILE_FILTER = "wikivoyage";
	private static final Log LOG = PlatformUtil.getLog(DownloadResources.class);


	public DownloadResources(OsmandApplication app) {
		super(null, DownloadResourceGroupType.WORLD, "");
		this.region = app.getRegions().getWorldRegion();
		this.app = app;
	}

	public List<IndexItem> getItemsToUpdate() {
		return itemsToUpdate;
	}

	@Nullable
	public IndexItem getWorldBaseMapItem() {
		DownloadResourceGroup worldMaps = getSubGroupById(DownloadResourceGroupType.WORLD_MAPS.getDefaultId());
		if (worldMaps != null) {
			List<IndexItem> list = worldMaps.getIndividualResources();
			if (list != null) {
				for (IndexItem item : list) {
					if (item.getBasename().equalsIgnoreCase(WorldRegion.WORLD_BASEMAP)) {
						return item;
					}
				}
			}
		}
		return null;
	}

	@Nullable
	public IndexItem getWeatherWorldItem() {
		DownloadResourceGroup worldMaps = getSubGroupById(DownloadResourceGroupType.WORLD_MAPS.getDefaultId());
		if (worldMaps != null) {
			List<IndexItem> list = worldMaps.getIndividualResources();
			if (list != null) {
				for (IndexItem item : list) {
					if (item.type == DownloadActivityType.WEATHER_FORECAST) {
						return item;
					}
				}
			}
		}
		return null;
	}

	@Nullable
	public IndexItem getWikivoyageItem(@NonNull String fileName) {
		List<IndexItem> items = getWikivoyageItems();
		if (items != null) {
			for (IndexItem ii : items) {
				if (ii.getTargetFile(app).getName().equals(fileName)) {
					return ii;
				}
			}
		}
		return null;
	}

	@Nullable
	public List<IndexItem> getWikivoyageItems() {
		String groupId = DownloadResourceGroupType.TRAVEL_GROUP.getDefaultId() + "#" +
				DownloadResourceGroupType.WIKIVOYAGE_MAPS.getDefaultId() + "#" +
				DownloadResourceGroupType.WIKIVOYAGE_HEADER.getDefaultId();
		DownloadResourceGroup header = getSubGroupById(groupId);
		return header == null ? null : header.getIndividualResources();
	}

	@Nullable
	public IndexItem getIndexItem(String fileName) {
		IndexItem res = null;
		if (rawResources == null) {
			return null;
		}
		for (IndexItem item : rawResources) {
			if (fileName.equals(item.getFileName())) {
				res = item;
				break;
			}
		}
		return res;
	}

	@NonNull
	public List<DownloadItem> getDownloadItems(WorldRegion region) {
		DownloadResourceGroup group = getRegionMapsGroup(region);
		if (group != null) {
			return group.getIndividualDownloadItems();
		}
		return Collections.emptyList();
	}

	@NonNull
	public List<IndexItem> getIndexItems(WorldRegion region) {
		if (groupByRegion != null) {
			List<IndexItem> res = groupByRegion.get(region);
			if (res != null) {
				return res;
			}
		}
		return Collections.emptyList();
	}

	public void updateLoadedFiles() {
		initAlreadyLoadedFiles();
		prepareFilesToUpdate();
	}

	private void initAlreadyLoadedFiles() {
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
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.HEIGHTMAP_INDEX_DIR),
				IndexConstants.HEIGHTMAP_SQLITE_EXT, indexFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.GEOTIFF_DIR),
				IndexConstants.TIF_EXT, indexFileNames);

		app.getResourceManager().getBackupIndexes(indexFileNames);
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
	}

	public boolean checkIfItemOutdated(IndexItem item, java.text.DateFormat format) {
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

	private void logItemUpdateInfo(IndexItem item, DateFormat format, long itemSize, long oldItemSize) {
		String date = item.getDate(format);
		String sfName = item.getTargetFileName();
		String indexActivatedDate = indexActivatedFileNames.get(sfName);
		String indexFilesDate = indexFileNames.get(sfName);
		LOG.info("name " + item.getFileName() + " timestamp " + item.timestamp + " localTimestamp " + item.localTimestamp + " date " + date
				+ " indexActivatedDate " + indexActivatedDate + " indexFilesDate " + indexFilesDate
				+ " itemSize " + itemSize + " oldItemSize " + oldItemSize);
	}

	protected void updateFilesToUpdate() {
		initAlreadyLoadedFiles();
		recalculateFilesToUpdate();
	}

	private void recalculateFilesToUpdate() {
		List<IndexItem> stillUpdate = new ArrayList<IndexItem>();
		for (IndexItem item : itemsToUpdate) {
			DateFormat format = app.getResourceManager().getDateFormat();
			checkIfItemOutdated(item, format);
			if (item.isOutdated()) {
				stillUpdate.add(item);
			}
		}
		itemsToUpdate = stillUpdate;
	}

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

	private File findFileInDir(File file) {
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

	private void prepareFilesToUpdate() {
		List<IndexItem> filtered = rawResources;
		if (filtered != null) {
			itemsToUpdate.clear();
			DateFormat format = app.getResourceManager().getDateFormat();
			for (IndexItem item : filtered) {
				boolean outdated = checkIfItemOutdated(item, format);
				// include only activated files here
				if (outdated && indexActivatedFileNames.containsKey(item.getTargetFileName())) {
					itemsToUpdate.add(item);
				}
			}
		}
	}

	protected boolean prepareData(List<IndexItem> resources) {
		this.rawResources = resources;

		DownloadResourceGroup extraMapsGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.EXTRA_MAPS);

		DownloadResourceGroup otherMapsGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.OTHER_MAPS_GROUP);
		DownloadResourceGroup otherMapsScreen = new DownloadResourceGroup(otherMapsGroup, DownloadResourceGroupType.OTHER_MAPS);
		DownloadResourceGroup otherMaps = new DownloadResourceGroup(otherMapsGroup, DownloadResourceGroupType.OTHER_MAPS_HEADER);
		otherMapsScreen.addGroup(otherMaps);
		otherMapsGroup.addGroup(otherMapsScreen);

		DownloadResourceGroup otherGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.OTHER_GROUP);
		DownloadResourceGroup voiceScreenTTS = new DownloadResourceGroup(otherGroup, DownloadResourceGroupType.VOICE_TTS);
		DownloadResourceGroup voiceScreenRec = new DownloadResourceGroup(otherGroup, DownloadResourceGroupType.VOICE_REC);
		DownloadResourceGroup fontScreen = new DownloadResourceGroup(otherGroup, DownloadResourceGroupType.FONTS);
		DownloadResourceGroup voiceTTS = new DownloadResourceGroup(otherGroup, DownloadResourceGroupType.VOICE_HEADER_TTS);
		DownloadResourceGroup voiceRec = new DownloadResourceGroup(otherGroup, DownloadResourceGroupType.VOICE_HEADER_REC);
		DownloadResourceGroup fonts = new DownloadResourceGroup(otherGroup, DownloadResourceGroupType.FONTS_HEADER);

		DownloadResourceGroup worldMaps = new DownloadResourceGroup(this, DownloadResourceGroupType.WORLD_MAPS);

		DownloadResourceGroup nauticalMapsGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.NAUTICAL_MAPS_GROUP);
		DownloadResourceGroup nauticalMapsScreen = new DownloadResourceGroup(nauticalMapsGroup, DownloadResourceGroupType.NAUTICAL_MAPS);
		DownloadResourceGroup nauticalWorldwideMaps = new DownloadResourceGroup(nauticalMapsGroup, DownloadResourceGroupType.NAUTICAL_WORLDWIDE_HEADER);
		DownloadResourceGroup nauticalDepthContoursMaps = new DownloadResourceGroup(nauticalMapsGroup, DownloadResourceGroupType.NAUTICAL_DEPTH_HEADER);
		DownloadResourceGroup nauticalDepthPointsMaps = new DownloadResourceGroup(nauticalMapsGroup, DownloadResourceGroupType.NAUTICAL_POINTS_HEADER);

		DownloadResourceGroup wikivoyageMapsGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.TRAVEL_GROUP);
		DownloadResourceGroup wikivoyageMapsScreen = new DownloadResourceGroup(wikivoyageMapsGroup, DownloadResourceGroupType.WIKIVOYAGE_MAPS);
		DownloadResourceGroup wikivoyageMaps = new DownloadResourceGroup(wikivoyageMapsGroup, DownloadResourceGroupType.WIKIVOYAGE_HEADER);

		Map<WorldRegion, List<IndexItem>> groupByRegion = new LinkedHashMap<>();
		OsmandRegions regs = app.getRegions();
		for (IndexItem item : resources) {
			DownloadActivityType type = item.getType();
			if (type == DownloadActivityType.VOICE_FILE) {
				if (DownloadActivityType.isVoiceTTS(item)) {
					voiceTTS.addItem(item);
				} else if (DownloadActivityType.isVoiceRec(item)) {
					voiceRec.addItem(item);
				}
				continue;
			}
			if (type == DownloadActivityType.FONT_FILE) {
				fonts.addItem(item);
				continue;
			}
			if (type == DownloadActivityType.DEPTH_MAP_FILE) {
				String fileName = item.getFileName().toLowerCase();
				if (fileName.startsWith(WORLD_CONTOURS_SUFFIX)) {
					nauticalWorldwideMaps.addItem(item);
				} else if (InAppPurchaseUtils.isDepthContoursAvailable(app)) {
					if (fileName.contains(NAUTICAL_DEPTH_POINTS_SUFFIX)) {
						nauticalDepthPointsMaps.addItem(item);
					} else {
						nauticalDepthContoursMaps.addItem(item);
					}
				}
				continue;
			}
			if (type == DownloadActivityType.WIKIVOYAGE_FILE) {
				if (app.getTravelHelper() instanceof TravelDbHelper) {
					wikivoyageMaps.addItem(item);
				}
				continue;
			}
			if (type == DownloadActivityType.TRAVEL_FILE) {
				if (item.getFileName().contains(WIKIVOYAGE_FILE_FILTER)) {
					wikivoyageMaps.addItem(item);
				}
				continue;
			}
			if (type == DownloadActivityType.HEIGHTMAP_FILE_LEGACY) {
				// Hide heightmaps of sqlite format
				continue;
			}
			if (type == DownloadActivityType.HILLSHADE_FILE || type == DownloadActivityType.SLOPE_FILE) {
				OsmandDevelopmentPlugin plugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin.class);
				if (app.useOpenGlRenderer() && plugin != null && !plugin.USE_RASTER_SQLITEDB.get()) {
					continue;
				}
			}

			String basename = item.getBasename();
			WorldRegion region = regs.getRegionDataByDownloadName(basename.toLowerCase());
			if (region != null) {
				if (!groupByRegion.containsKey(region)) {
					groupByRegion.put(region, new ArrayList<>());
				}
				groupByRegion.get(region).add(item);
			} else {
				String fileName = item.getFileName();
				if (fileName.contains("World")) {
					if (CollectionUtils.startsWithAny(fileName.toLowerCase(), WORLD_SEAMARKS_KEY, WORLD_SEAMARKS_OLD_KEY)) {
						nauticalWorldwideMaps.addItem(item);
					} else {
						worldMaps.addItem(item);
					}
				} else {
					otherMaps.addItem(item);
				}
			}
		}
		this.groupByRegion = groupByRegion;

		List<WorldRegion> customRegions = PluginsHelper.getCustomDownloadRegions();
		if (!Algorithms.isEmpty(customRegions)) {
			addGroup(extraMapsGroup);
			for (WorldRegion region : customRegions) {
				buildRegionsGroups(region, extraMapsGroup);
			}
		}

		LinkedList<WorldRegion> queue = new LinkedList<>();
		LinkedList<DownloadResourceGroup> parent = new LinkedList<>();
		DownloadResourceGroup worldSubregions = new DownloadResourceGroup(this, DownloadResourceGroupType.SUBREGIONS);
		addGroup(worldSubregions);
		for (WorldRegion rg : region.getSubregions()) {
			queue.add(rg);
			parent.add(worldSubregions);
		}
		while (!queue.isEmpty()) {
			WorldRegion reg = queue.pollFirst();
			DownloadResourceGroup parentGroup = parent.pollFirst();
			List<WorldRegion> subregions = reg.getSubregions();
			DownloadResourceGroup mainGrp = new DownloadResourceGroup(parentGroup, DownloadResourceGroupType.REGION, reg.getRegionId());
			mainGrp.region = reg;
			parentGroup.addGroup(mainGrp);

			DownloadResourceGroup flatFiles = new DownloadResourceGroup(mainGrp, REGION_MAPS);
			List<IndexItem> list = groupByRegion.get(reg);
			if (list != null) {
				for (IndexItem ii : list) {
					flatFiles.addItem(ii);
				}
			}
			if (list != null || !reg.isContinent()) {
				mainGrp.addGroup(flatFiles);
			}
			DownloadResourceGroup subRegions = new DownloadResourceGroup(mainGrp, DownloadResourceGroupType.SUBREGIONS);
			mainGrp.addGroup(subRegions);
			// add to processing queue
			for (WorldRegion rg : subregions) {
				queue.add(rg);
				parent.add(subRegions);
			}
		}
		// Possible improvements
		// 1. if there is no subregions no need to create resource group REGIONS_MAPS - objection raise diversity and there is no value
		// 2. if there is no subregions and there only 1 index item it could be merged to the level up - objection there is no such maps
		// 3. if hillshade/srtm is disabled, all maps from inner level could be combined into 1 
		addGroup(worldMaps);

		nauticalMapsScreen.addGroup(nauticalWorldwideMaps);
		nauticalMapsScreen.addGroup(nauticalDepthContoursMaps);
		nauticalMapsScreen.addGroup(nauticalDepthPointsMaps);
		nauticalMapsGroup.addGroup(nauticalMapsScreen);
		addGroup(nauticalMapsGroup);

		wikivoyageMapsScreen.addGroup(wikivoyageMaps);
		wikivoyageMapsGroup.addGroup(wikivoyageMapsScreen);
		addGroup(wikivoyageMapsGroup);

		if (otherMaps.size() > 0) {
			addGroup(otherMapsGroup);
		}

		voiceScreenTTS.addGroup(voiceTTS);
		voiceScreenRec.addGroup(voiceRec);
		if (fonts.getIndividualResources() != null) {
			fontScreen.addGroup(fonts);
		}
		otherGroup.addGroup(voiceScreenTTS);
		otherGroup.addGroup(voiceScreenRec);


		if (fonts.getIndividualResources() != null) {
			otherGroup.addGroup(fontScreen);
		}
		addGroup(otherGroup);

		createHillshadeSRTMGroups();
		replaceIndividualSrtmWithGroups(region);
		createMultipleDownloadItems(region);
		trimEmptyGroups();
		updateLoadedFiles();
		return true;
	}

	private void replaceIndividualSrtmWithGroups(@NonNull WorldRegion region) {
		DownloadResourceGroup group = getRegionMapsGroup(region);
		if (group != null) {
			boolean useMetersByDefault = SrtmDownloadItem.isUseMetricByDefault(app);
			boolean listModified = false;
			DownloadActivityType srtmType = DownloadActivityType.SRTM_COUNTRY_FILE;
			List<DownloadItem> individualItems = group.getIndividualDownloadItems();
			if (isListContainsType(individualItems, srtmType)) {
				List<IndexItem> srtmIndexes = new ArrayList<>();
				for (DownloadItem item : individualItems) {
					if (item.getType() == srtmType && item instanceof IndexItem) {
						srtmIndexes.add((IndexItem) item);
					}
				}
				if (srtmIndexes.size() > 0) {
					individualItems.removeAll(srtmIndexes);
					group.addItem(new SrtmDownloadItem(srtmIndexes, useMetersByDefault));
				}
				listModified = true;
			}
			if (listModified) {
				sortDownloadItems(individualItems);
			}
		}

		List<WorldRegion> subRegions = region.getSubregions();
		if (!Algorithms.isEmpty(subRegions)) {
			for (WorldRegion subRegion : subRegions) {
				replaceIndividualSrtmWithGroups(subRegion);
			}
		}
	}

	private void createMultipleDownloadItems(@NonNull WorldRegion region) {
		List<WorldRegion> subRegions = region.getSubregions();
		if (Algorithms.isEmpty(subRegions)) return;

		DownloadResourceGroup group = getRegionMapsGroup(region);
		if (group != null) {
			boolean listModified = false;
			List<DownloadItem> downloadItems = group.getIndividualDownloadItems();
			List<WorldRegion> uniqueSubRegions = WorldRegion.removeDuplicates(subRegions);
			for (DownloadActivityType type : DownloadActivityType.values()) {
				if (!isListContainsType(downloadItems, type)) {
					List<DownloadItem> itemsFromSubRegions = collectItemsOfType(uniqueSubRegions, type);
					if (itemsFromSubRegions != null) {
						group.addItem(new MultipleDownloadItem(region, itemsFromSubRegions, type));
						listModified = true;
					}
				}
			}
			if (listModified) {
				sortDownloadItems(group.getIndividualDownloadItems());
			}
		}
		for (WorldRegion subRegion : subRegions) {
			createMultipleDownloadItems(subRegion);
		}
	}

	private DownloadResourceGroup getRegionMapsGroup(WorldRegion region) {
		DownloadResourceGroup group = getRegionGroup(region);
		if (group != null) {
			return group.getSubGroupById(REGION_MAPS.getDefaultId());
		}
		return null;
	}

	@Nullable
	private List<DownloadItem> collectItemsOfType(@NonNull List<WorldRegion> regions,
	                                              @NonNull DownloadActivityType type) {
		List<DownloadItem> collectedItems = new ArrayList<>();
		for (WorldRegion region : regions) {
			boolean found = false;
			for (DownloadItem item : getDownloadItems(region)) {
				if (item.getType() == type) {
					found = true;
					collectedItems.add(item);
					break;
				}
			}
			if (!found) return null;
		}
		return collectedItems;
	}

	private void buildRegionsGroups(WorldRegion region, DownloadResourceGroup group) {
		LinkedList<WorldRegion> queue = new LinkedList<WorldRegion>();
		LinkedList<DownloadResourceGroup> parent = new LinkedList<DownloadResourceGroup>();
		queue.add(region);
		parent.add(group);
		while (!queue.isEmpty()) {
			WorldRegion reg = queue.pollFirst();
			DownloadResourceGroup parentGroup = parent.pollFirst();
			List<WorldRegion> subregions = reg.getSubregions();
			DownloadResourceGroup mainGrp = new DownloadResourceGroup(parentGroup, DownloadResourceGroupType.REGION, reg.getRegionId());
			mainGrp.region = reg;
			parentGroup.addGroup(mainGrp);

			if (reg instanceof CustomRegion) {
				CustomRegion customRegion = (CustomRegion) reg;
				List<IndexItem> indexItems = customRegion.loadIndexItems();
				if (!Algorithms.isEmpty(indexItems)) {
					DownloadResourceGroup flatFiles = new DownloadResourceGroup(mainGrp, REGION_MAPS);
					for (IndexItem ii : indexItems) {
						flatFiles.addItem(ii);
					}
					mainGrp.addGroup(flatFiles);
				}
			}
			DownloadResourceGroup subRegions = new DownloadResourceGroup(mainGrp, DownloadResourceGroupType.EXTRA_MAPS);
			mainGrp.addGroup(subRegions);
			// add to processing queue
			for (WorldRegion rg : subregions) {
				queue.add(rg);
				parent.add(subRegions);
			}
		}
	}

	/**
	 * @return smallest index item, if there are no downloaded index items; Downloaded item otherwise.
	 */
	@Nullable
	public static IndexItem findSmallestIndexItemAt(OsmandApplication app, LatLon latLon, DownloadActivityType type) throws IOException {
		IndexItem res = null;
		List<IndexItem> items = findIndexItemsAt(app, latLon, type, true);
		for (IndexItem item : items) {
			if (item.isDownloaded()) {
				return item;
			}
			if (res == null) {
				res = item;
			} else {
				res = getSmallestIndexItem(res, item);
			}
		}
		return res;
	}

	public boolean hasDownloadedMapsAt(@NonNull LatLon latLon, boolean routeData) {
		int x31 = MapUtils.get31TileNumberX(latLon.getLongitude());
		int y31 = MapUtils.get31TileNumberY(latLon.getLatitude());
		return !getMapFileNamesAt(x31, y31, routeData, false).isEmpty();
	}

	@NonNull
	public List<String> getExternalMapFileNamesAt(int x31, int y31, boolean routeData) {
		return getMapFileNamesAt(x31, y31, routeData, true);
	}

	@NonNull
	public List<String> getMapFileNamesAt(int x31, int y31, boolean routeData, boolean onlyExternal) {
		List<String> res = new ArrayList<>();
		for (BinaryMapReaderResource reader : app.getResourceManager().getFileReaders()) {
			String fileName = reader.getFileName();
			if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
					&& !fileName.startsWith("World_")
					&& !fileName.endsWith(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)
					&& !fileName.endsWith(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT)
					&& !fileName.endsWith(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
				BinaryMapIndexReader shallowReader = reader.getShallowReader();
				if (shallowReader != null && !shallowReader.isBasemap()) {
					if (routeData && !shallowReader.containsRouteData()) {
						continue;
					}
					if (shallowReader.containsMapData() && (!onlyExternal || !isOsmandMapRegion(fileName))) {
						if (routeData) {
							if (shallowReader.containsRouteData(x31, y31, x31, y31, DETAILED_MAP_MIN_ZOOM)) {
								res.add(fileName);
							}
						} else if (shallowReader.containsMapData(x31, y31, x31, y31, DETAILED_MAP_MIN_ZOOM)) {
							res.add(fileName);
						}
					}
				}
			}
		}
		return res;
	}

	public boolean isOsmandMapRegion(@NonNull String mapFileName) {
		OsmandRegions osmandRegions = app.getRegions();
		String downloadName = WorldRegion.getRegionDownloadName(mapFileName);
		WorldRegion region = osmandRegions.getRegionDataByDownloadName(downloadName);
		return region != null && (region.isRegionRoadsDownload() || region.isRegionMapDownload());
	}

	private static IndexItem getSmallestIndexItem(@NonNull IndexItem item1, @NonNull IndexItem item2) {
		if (item1.contentSize > item2.contentSize) {
			return item2;
		}
		return item1;
	}

	public static List<IndexItem> findIndexItemsAt(OsmandApplication app, LatLon latLon, DownloadActivityType type) throws IOException {
		return findIndexItemsAt(app, latLon, type, false);
	}

	public static List<IndexItem> findIndexItemsAt(OsmandApplication app, LatLon latLon, DownloadActivityType type, boolean includeDownloaded) throws IOException {
		return findIndexItemsAt(app, latLon, type, includeDownloaded, -1, false);
	}

	public static List<IndexItem> findIndexItemsAt(OsmandApplication app, LatLon latLon, DownloadActivityType type, boolean includeDownloaded, int limit, boolean skipIfOneDownloaded) throws IOException {
		List<IndexItem> res = new ArrayList<>();
		OsmandRegions regions = app.getRegions();
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		List<WorldRegion> downloadRegions = regions.getWorldRegionsAt(latLon);
		for (WorldRegion downloadRegion : downloadRegions) {
			boolean itemDownloaded = isIndexItemDownloaded(downloadThread, type, downloadRegion, res);
			if (skipIfOneDownloaded && itemDownloaded) {
				return new ArrayList<>();
			}
			if (includeDownloaded || !itemDownloaded) {
				addIndexItem(downloadThread, type, downloadRegion, res);
			}
			if (limit != -1 && res.size() == limit) {
				break;
			}
		}
		return res;
	}

	public List<DownloadItem> getDownloadItemsForGroup(String groupId) {
		DownloadResourceGroup group = getSubGroupById(groupId);
		if (group != null) {
			return group.getIndividualDownloadItems();
		}
		return Collections.emptyList();
	}

	public static List<IndexItem> findIndexItemsAt(OsmandApplication app,
	                                               List<String> names,
	                                               DownloadActivityType type,
	                                               boolean includeDownloaded,
	                                               int limit) {
		List<IndexItem> res = new ArrayList<>();
		OsmandRegions regions = app.getRegions();
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		for (String name : names) {
			WorldRegion downloadRegion = regions.getRegionDataByDownloadName(name);
			if (downloadRegion != null && (includeDownloaded || !isIndexItemDownloaded(downloadThread, type, downloadRegion, res))) {
				addIndexItem(downloadThread, type, downloadRegion, res);
			}
			if (limit != -1 && res.size() == limit) {
				break;
			}
		}
		return res;
	}

	private static boolean isIndexItemDownloaded(DownloadIndexesThread downloadThread,
	                                             DownloadActivityType type,
	                                             WorldRegion downloadRegion,
	                                             List<IndexItem> res) {
		List<IndexItem> otherIndexItems =
				new ArrayList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
		for (IndexItem indexItem : otherIndexItems) {
			if (indexItem.getType() == type && indexItem.isDownloaded()) {
				return true;
			}
		}
		return downloadRegion.getSuperregion() != null
				&& isIndexItemDownloaded(downloadThread, type, downloadRegion.getSuperregion(), res);
	}

	private boolean isListContainsType(List<DownloadItem> items,
	                                   DownloadActivityType type) {
		if (items != null) {
			for (DownloadItem item : items) {
				if (item.getType() == type) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean addIndexItem(DownloadIndexesThread downloadThread,
	                                    DownloadActivityType type,
	                                    WorldRegion downloadRegion,
	                                    List<IndexItem> res) {
		List<IndexItem> otherIndexItems =
				new ArrayList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
		for (IndexItem indexItem : otherIndexItems) {
			if (indexItem.getType() == type
					&& !res.contains(indexItem)) {
				res.add(indexItem);
				return true;
			}
		}
		return downloadRegion.getSuperregion() != null
				&& addIndexItem(downloadThread, type, downloadRegion.getSuperregion(), res);
	}
}
