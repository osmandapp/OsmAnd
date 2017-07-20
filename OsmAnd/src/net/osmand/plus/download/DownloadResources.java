package net.osmand.plus.download;

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DownloadResources extends DownloadResourceGroup {
	public boolean isDownloadedFromInternet = false;
	public boolean downloadFromInternetFailed = false;
	public boolean mapVersionIsIncreased = false;
	public OsmandApplication app;
	private Map<String, String> indexFileNames = new LinkedHashMap<>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<>();
	private List<IndexItem> rawResources;
	private Map<WorldRegion, List<IndexItem> > groupByRegion;
	private List<IndexItem> itemsToUpdate = new ArrayList<>();
	public static final String WORLD_SEAMARKS_KEY = "world_seamarks";
	public static final String WORLD_SEAMARKS_NAME = "World_seamarks";
	public static final String WORLD_SEAMARKS_OLD_KEY = "world_seamarks_basemap";
	public static final String WORLD_SEAMARKS_OLD_NAME = "World_seamarks_basemap";
	
	
	public DownloadResources(OsmandApplication app) {
		super(null, DownloadResourceGroupType.WORLD, "");
		this.region = app.getRegions().getWorldRegion();
		this.app = app;
	}
	
	public List<IndexItem> getItemsToUpdate() {
		return itemsToUpdate;
	}

	public IndexItem getWorldBaseMapItem() {
		DownloadResourceGroup worldMaps = getSubGroupById(DownloadResourceGroupType.WORLD_MAPS.getDefaultId());
		IndexItem worldMap = null;
		if (worldMaps != null) {
			List<IndexItem> list = worldMaps.getIndividualResources();
			if (list != null) {
				for (IndexItem ii : list) {
					if (ii.getBasename().equalsIgnoreCase(WorldRegion.WORLD_BASEMAP)) {
						worldMap = ii;
						break;
					}
				}
			}
		}
		return worldMap;
	}

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

	public List<IndexItem> getIndexItems(WorldRegion region) {
		if (groupByRegion != null) {
			List<IndexItem> res = groupByRegion.get(region);
			if (res != null) {
				return res;
			}
		}
		return new LinkedList<>();
	}

	public void updateLoadedFiles() {
		initAlreadyLoadedFiles();
		prepareFilesToUpdate();
	}

	private void initAlreadyLoadedFiles() {
		java.text.DateFormat dateFormat = app.getResourceManager().getDateFormat();
		Map<String, String> indexActivatedFileNames = app.getResourceManager().getIndexFileNames();
		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT, indexActivatedFileNames);
		Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT, indexFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.TILES_INDEX_DIR), IndexConstants.SQLITE_EXT,
				indexFileNames);
		app.getResourceManager().getBackupIndexes(indexFileNames);
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
	}

	public boolean checkIfItemOutdated(IndexItem item, java.text.DateFormat format) {
		boolean outdated = false;
		String sfName = item.getTargetFileName();
		String indexActivatedDate = indexActivatedFileNames.get(sfName);
		String indexFilesDate = indexFileNames.get(sfName);
		item.setDownloaded(false);
		item.setOutdated(false);
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
				e.printStackTrace();
			}
		}
		if (date != null && !date.equals(indexActivatedDate) && !date.equals(indexFilesDate)) {
			if ((item.getType() == DownloadActivityType.NORMAL_FILE && !item.extra)
					|| item.getType() == DownloadActivityType.ROADS_FILE
					|| item.getType() == DownloadActivityType.WIKIPEDIA_FILE
					|| item.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE
					|| item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
				outdated = true;
			} else {
				long itemSize = item.getContentSize();
				long oldItemSize = 0;
				if (item.getType() == DownloadActivityType.VOICE_FILE) {
					if (item instanceof AssetIndexItem) {
						File file = new File(((AssetIndexItem) item).getDestFile());
						oldItemSize = file.length();
					} else {
						File fl = new File(item.getType().getDownloadFolder(app, item), sfName + "/_config.p");
						if (fl.exists()) {
							oldItemSize = fl.length();
							try {
								InputStream is = app.getAssets().open("voice/" + sfName + "/config.p");
								if (is != null) {
									itemSize = is.available();
									is.close();
								}
							} catch (IOException e) {
							}
						}
					}
				} else if (item.getType() == DownloadActivityType.FONT_FILE) {
					oldItemSize = new File(app.getAppPath(IndexConstants.FONT_INDEX_DIR), item.getTargetFileName()).length();
				} else {
					oldItemSize = app.getAppPath(item.getTargetFileName()).length();
				}
				if (itemSize != oldItemSize) {
					outdated = true;
				}
			}
		}
		item.setOutdated(outdated);
		return outdated;
	}

	

	protected void updateFilesToUpdate() {
		initAlreadyLoadedFiles();;
		recalculateFilesToUpdate();
	}

	private void recalculateFilesToUpdate() {
		List<IndexItem> stillUpdate = new ArrayList<IndexItem>();
		for (IndexItem item : itemsToUpdate) {
			java.text.DateFormat format = app.getResourceManager().getDateFormat();
			checkIfItemOutdated(item, format);
			if (item.isOutdated()) {
				stillUpdate.add(item);
			}
		}
		itemsToUpdate = stillUpdate;
	}

	private Map<String, String> listWithAlternatives(final java.text.DateFormat dateFormat, File file,
			final String ext, final Map<String, String> files) {
		if (file.isDirectory()) {
			file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(ext)) {
						String date = dateFormat.format(findFileInDir(new File(dir, filename)).lastModified());
						files.put(filename, date);
						return true;
					} else {
						return false;
					}
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
			java.text.DateFormat format = app.getResourceManager().getDateFormat();
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
		DownloadResourceGroup nauticalMaps = new DownloadResourceGroup(nauticalMapsGroup, DownloadResourceGroupType.NAUTICAL_MAPS_HEADER);

		Map<WorldRegion, List<IndexItem> > groupByRegion = new LinkedHashMap<WorldRegion, List<IndexItem>>();
		OsmandRegions regs = app.getRegions();
		for (IndexItem ii : resources) {
			if (ii.getType() == DownloadActivityType.VOICE_FILE) {
				if (ii.getFileName().endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
					voiceTTS.addItem(ii);
				} else {
					voiceRec.addItem(ii);
				}
				continue;
			}
			if (ii.getType() == DownloadActivityType.FONT_FILE) {
				fonts.addItem(ii);
				continue;
			}
			if (ii.getType() == DownloadActivityType.DEPTH_CONTOUR_FILE) {
				if (app.getSettings().DEPTH_CONTOURS_PURCHASED.get() || nauticalMaps.size() == 0) {
					nauticalMaps.addItem(ii);
				}
				continue;
			}
			String basename = ii.getBasename().toLowerCase();
			WorldRegion wg = regs.getRegionDataByDownloadName(basename);
			if (wg != null) {
				if (!groupByRegion.containsKey(wg)) {
					groupByRegion.put(wg, new ArrayList<IndexItem>());
				}
				groupByRegion.get(wg).add(ii);
			} else {
				if (ii.getFileName().startsWith("World_")) {
					if (ii.getFileName().toLowerCase().startsWith(WORLD_SEAMARKS_KEY) || 
							ii.getFileName().toLowerCase().startsWith(WORLD_SEAMARKS_OLD_KEY)) {
						nauticalMaps.addItem(ii);
					} else {
						worldMaps.addItem(ii);
					}
				} else {
					otherMaps.addItem(ii);
				}
			}
		}
		this.groupByRegion = groupByRegion;

		LinkedList<WorldRegion> queue = new LinkedList<WorldRegion>();
		LinkedList<DownloadResourceGroup> parent = new LinkedList<DownloadResourceGroup>();
		DownloadResourceGroup worldSubregions = new DownloadResourceGroup(this, DownloadResourceGroupType.SUBREGIONS);
		addGroup(worldSubregions);
		for(WorldRegion rg : region.getSubregions()) {
			queue.add(rg);
			parent.add(worldSubregions);
		}
		while(!queue.isEmpty()) {
			WorldRegion reg = queue.pollFirst();
			DownloadResourceGroup parentGroup = parent.pollFirst();
			List<WorldRegion> subregions = reg.getSubregions();
			DownloadResourceGroup mainGrp = new DownloadResourceGroup(parentGroup, DownloadResourceGroupType.REGION, reg.getRegionId());
			mainGrp.region = reg;
			parentGroup.addGroup(mainGrp);
			
			List<IndexItem> list = groupByRegion.get(reg);
			if(list != null) {
				DownloadResourceGroup flatFiles = new DownloadResourceGroup(mainGrp, DownloadResourceGroupType.REGION_MAPS);
				for(IndexItem ii : list) {
					flatFiles.addItem(ii);
				}
				mainGrp.addGroup(flatFiles);
			}
			DownloadResourceGroup subRegions = new DownloadResourceGroup(mainGrp, DownloadResourceGroupType.SUBREGIONS);
			mainGrp.addGroup(subRegions);
			// add to processing queue
			for(WorldRegion rg : subregions) {
				queue.add(rg);
				parent.add(subRegions);
			}	
		}
		// Possible improvements
		// 1. if there is no subregions no need to create resource group REGIONS_MAPS - objection raise diversity and there is no value
		// 2. if there is no subregions and there only 1 index item it could be merged to the level up - objection there is no such maps
		// 3. if hillshade/srtm is disabled, all maps from inner level could be combined into 1 
		addGroup(worldMaps);

		nauticalMapsScreen.addGroup(nauticalMaps);
		nauticalMapsGroup.addGroup(nauticalMapsScreen);
		addGroup(nauticalMapsGroup);

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
		trimEmptyGroups();
		updateLoadedFiles();
		return true;
	}

	public static List<IndexItem> findIndexItemsAt(OsmandApplication app, LatLon latLon, DownloadActivityType type) throws IOException {

		List<IndexItem> res = new ArrayList<>();
		OsmandRegions regions = app.getRegions();
		DownloadIndexesThread downloadThread = app.getDownloadThread();

		int point31x = MapUtils.get31TileNumberX(latLon.getLongitude());
		int point31y = MapUtils.get31TileNumberY(latLon.getLatitude());

		List<BinaryMapDataObject> mapDataObjects;
		try {
			mapDataObjects = regions.queryBbox(point31x, point31x, point31y, point31y);
		} catch (IOException e) {
			throw new IOException("Error while calling queryBbox");
		}
		if (mapDataObjects != null) {
			Iterator<BinaryMapDataObject> it = mapDataObjects.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (o.getTypes() != null) {
					boolean isRegion = true;
					for (int i = 0; i < o.getTypes().length; i++) {
						BinaryMapIndexReader.TagValuePair tp = o.getMapIndex().decodeType(o.getTypes()[i]);
						if ("boundary".equals(tp.value)) {
							isRegion = false;
							break;
						}
					}
					WorldRegion downloadRegion = regions.getRegionData(regions.getFullName(o));
					if (downloadRegion != null && isRegion && regions.contain(o, point31x, point31y)) {
						if (!isIndexItemDownloaded(downloadThread, type, downloadRegion, res)) {
							addIndexItem(downloadThread, type, downloadRegion, res);
						}
					}
				}
			}
		}
		return res;
	}

	private static boolean isIndexItemDownloaded(DownloadIndexesThread downloadThread, DownloadActivityType type, WorldRegion downloadRegion, List<IndexItem> res) {
		List<IndexItem> otherIndexItems = new ArrayList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
		for (IndexItem indexItem : otherIndexItems) {
			if (indexItem.getType() == type && indexItem.isDownloaded()) {
				return true;
			}
		}
		return downloadRegion.getSuperregion() != null
				&& isIndexItemDownloaded(downloadThread, type, downloadRegion.getSuperregion(), res);
	}

	private static boolean addIndexItem(DownloadIndexesThread downloadThread, DownloadActivityType type, WorldRegion downloadRegion, List<IndexItem> res) {
		List<IndexItem> otherIndexItems = new ArrayList<>(downloadThread.getIndexes().getIndexItems(downloadRegion));
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