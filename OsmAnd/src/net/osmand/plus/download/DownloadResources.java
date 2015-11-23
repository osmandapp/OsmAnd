package net.osmand.plus.download;

import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
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
	public static final String WORLD_SEAMARKS_KEY = "world_seamarks_basemap";
	
	
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
		String indexactivateddate = indexActivatedFileNames.get(sfName);
		String indexfilesdate = indexFileNames.get(sfName);
		item.setDownloaded(false);
		item.setOutdated(false);
		if(indexactivateddate == null && indexfilesdate == null) {
			return outdated;
		}
		item.setDownloaded(true);
		String date = item.getDate(format);
		boolean parsed = false;
		if(indexactivateddate != null) {
			try {
				item.setLocalTimestamp(format.parse(indexactivateddate).getTime());
				parsed = true;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (!parsed && indexfilesdate != null) {
			try {
				item.setLocalTimestamp(format.parse(indexfilesdate).getTime());
				parsed = true;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		if (date != null && !date.equals(indexactivateddate) && !date.equals(indexfilesdate)) {
			if ((item.getType() == DownloadActivityType.NORMAL_FILE && !item.extra)
					|| item.getType() == DownloadActivityType.ROADS_FILE
					|| item.getType() == DownloadActivityType.WIKIPEDIA_FILE
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

		DownloadResourceGroup voiceGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.VOICE_GROUP);
		DownloadResourceGroup voiceScreenTTS = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_TTS);
		DownloadResourceGroup voiceScreenRec = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_REC);
		DownloadResourceGroup voiceTTS = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_HEADER_TTS);
		DownloadResourceGroup voiceRec = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_HEADER_REC);

		DownloadResourceGroup worldMaps = new DownloadResourceGroup(this, DownloadResourceGroupType.WORLD_MAPS);
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
			String basename = ii.getBasename().toLowerCase();
			WorldRegion wg = regs.getRegionDataByDownloadName(basename);
			if (wg != null) {
				if (!groupByRegion.containsKey(wg)) {
					groupByRegion.put(wg, new ArrayList<IndexItem>());
				}
				groupByRegion.get(wg).add(ii);
			} else {
				if (ii.getFileName().startsWith("World_")) {
					worldMaps.addItem(ii);
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
		if (otherMaps.size() > 0) {
			addGroup(otherMapsGroup);
		}

		voiceScreenTTS.addGroup(voiceTTS);
		voiceScreenRec.addGroup(voiceRec);
		voiceGroup.addGroup(voiceScreenTTS);
		voiceGroup.addGroup(voiceScreenRec);
		addGroup(voiceGroup);

		createHillshadeSRTMGroups();
		trimEmptyGroups();
		updateLoadedFiles();
		return true;
	}


}