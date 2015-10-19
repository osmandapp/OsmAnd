package net.osmand.plus.download;

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

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;

public class DownloadResources extends DownloadResourceGroup {
	public boolean isDownloadedFromInternet = false;
	public boolean mapVersionIsIncreased = false;
	public OsmandApplication app;
	private Map<String, String> indexFileNames = new LinkedHashMap<>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<>();
	private List<IndexItem> rawResources;
	private List<IndexItem> itemsToUpdate = new ArrayList<>();
	//public static final String WORLD_BASEMAP_KEY = "world_basemap.obf.zip";
	public static final String WORLD_SEAMARKS_KEY = "world_seamarks_basemap";
	
	
	public DownloadResources(OsmandApplication app) {
		super(null, DownloadResourceGroupType.WORLD, "");
		this.region = app.getWorldRegion();
		this.app = app;
	}
	
	public List<IndexItem> getItemsToUpdate() {
		return itemsToUpdate;
	}

	public void initAlreadyLoadedFiles() {
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
		prepareFilesToUpdate();
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
		initAlreadyLoadedFiles();
		List<IndexItem> stillUpdate = new ArrayList<IndexItem>();
		for (IndexItem item : itemsToUpdate) {
			String sfName = item.getTargetFileName();
			java.text.DateFormat format = app.getResourceManager().getDateFormat();
			String date = item.getDate(format);
			String indexactivateddate = indexActivatedFileNames.get(sfName);
			String indexfilesdate = indexFileNames.get(sfName);
			if (date != null && !date.equals(indexactivateddate) && !date.equals(indexfilesdate)
					&& indexActivatedFileNames.containsKey(sfName)) {
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
		
		DownloadResourceGroup voiceGroup = new DownloadResourceGroup(this, DownloadResourceGroupType.VOICE_GROUP);
		DownloadResourceGroup voiceScreenRec = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_REC);
		DownloadResourceGroup voiceScreenTTS = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_TTS);
		DownloadResourceGroup voiceRec = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_HEADER_REC);
		DownloadResourceGroup voiceTTS = new DownloadResourceGroup(voiceGroup, DownloadResourceGroupType.VOICE_HEADER_TTS);
		voiceScreenTTS.addGroup(voiceTTS);
		voiceScreenRec.addGroup(voiceRec);
		voiceGroup.addGroup(voiceScreenRec);
		voiceGroup.addGroup(voiceScreenTTS);
		
		DownloadResourceGroup worldMaps = new DownloadResourceGroup(this, DownloadResourceGroupType.WORLD_MAPS);
		Map<WorldRegion, List<IndexItem> > groupByRegion = new LinkedHashMap<WorldRegion, List<IndexItem>>();
		
		Map<String, WorldRegion> downloadIdForRegion = new LinkedHashMap<String, WorldRegion>();
		for(WorldRegion wg : region.getFlattenedSubregions()) {
			downloadIdForRegion.put(wg.getDownloadsId(), wg);
		}
		
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
			WorldRegion wg = downloadIdForRegion.get(basename);
			if (wg != null) {
				if (!groupByRegion.containsKey(wg)) {
					groupByRegion.put(wg, new ArrayList<IndexItem>());
				}
				groupByRegion.get(wg).add(ii);
			} else {
				worldMaps.addItem(ii);
			}
		}
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
		addGroup(voiceGroup);
		
		trimEmptyGroups();
		initAlreadyLoadedFiles();
		return true;
	}

	

}