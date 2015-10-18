package net.osmand.plus.download;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;
import net.osmand.plus.download.items.DownloadIndexes;
import net.osmand.plus.download.items.ItemsListBuilder;
import net.osmand.plus.download.items.ResourceItem;
import net.osmand.plus.download.items.ResourceItemComparator;
import net.osmand.plus.download.items.ItemsListBuilder.VoicePromptsType;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.util.Algorithms;

public class DownloadResources extends DownloadResourceGroup {
	public boolean isDownloadedFromInternet = false;
	public boolean mapVersionIsIncreased = false;
	public OsmandApplication app;
	private Map<String, String> indexFileNames = new LinkedHashMap<>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<>();
	private List<IndexItem> itemsToUpdate = new ArrayList<>();

	public DownloadResources(OsmandApplication app) {
		super(null, DownloadResourceGroupType.WORLD, "", false);
		this.region = app.getWorldRegion();
		this.app = app;
	}

	public void initAlreadyLoadedFiles() {
		java.text.DateFormat dateFormat = app.getResourceManager().getDateFormat();
		Map<String, String> indexActivatedFileNames = app.getResourceManager().getIndexFileNames();
		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT, indexActivatedFileNames);
		Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT, indexFileNames);
		app.getAppCustomization().updatedLoadedFiles(indexFileNames, indexActivatedFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.TILES_INDEX_DIR), IndexConstants.SQLITE_EXT,
				indexFileNames);
		app.getResourceManager().getBackupIndexes(indexFileNames);
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
		prepareFilesToUpdate();
	}

	public boolean checkIfItemOutdated(IndexItem item) {
		boolean outdated = false;
		String sfName = item.getTargetFileName();
		java.text.DateFormat format = app.getResourceManager().getDateFormat();
		String date = item.getDate(format);
		String indexactivateddate = indexActivatedFileNames.get(sfName);
		String indexfilesdate = indexFileNames.get(sfName);
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
		return outdated;
	}

	

	protected void updateFilesToUpdate() {
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
	
	
	
	////////// FIXME ////////////
	
	private void prepareFilesToUpdate() {
		List<IndexItem> filtered = getCachedIndexFiles();
		if (filtered != null) {
			itemsToUpdate.clear();
			for (IndexItem item : filtered) {
				boolean outdated = checkIfItemOutdated(item);
				// include only activated files here
				if (outdated && indexActivatedFileNames.containsKey(item.getTargetFileName())) {
					itemsToUpdate.add(item);
				}
			}
		}
	}
	
	private void processRegion(List<IndexItem> resourcesInRepository, DownloadResources di,
			boolean processVoiceFiles, WorldRegion region) {
		String downloadsIdPrefix = region.getDownloadsIdPrefix();
		Map<String, IndexItem> regionResources = new HashMap<>();
		Set<DownloadActivityType> typesSet = new TreeSet<>(new Comparator<DownloadActivityType>() {
			@Override
			public int compare(DownloadActivityType dat1, DownloadActivityType dat2) {
				return dat1.getTag().compareTo(dat2.getTag());
			}
		});
		for (IndexItem resource : resourcesInRepository) {
			if (processVoiceFiles) {
				if (resource.getSimplifiedFileName().endsWith(".voice.zip")) {
					voiceRecItems.add(resource);
					continue;
				} else if (resource.getSimplifiedFileName().contains(".ttsvoice.zip")) {
					voiceTTSItems.add(resource);
					continue;
				}
			}
			if (!resource.getSimplifiedFileName().startsWith(downloadsIdPrefix)) {
				continue;
			}

			if (resource.type == DownloadActivityType.NORMAL_FILE
					|| resource.type == DownloadActivityType.ROADS_FILE) {
				if (resource.isAlreadyDownloaded(indexFileNames)) {
					region.processNewMapState(checkIfItemOutdated(resource)
							? WorldRegion.MapState.OUTDATED : WorldRegion.MapState.DOWNLOADED);
				} else {
					region.processNewMapState(WorldRegion.MapState.NOT_DOWNLOADED);
				}
			}
			typesSet.add(resource.getType());
			regionResources.put(resource.getSimplifiedFileName(), resource);
		}

		if (region.getSuperregion() != null && region.getSuperregion().getSuperregion() != app.getWorldRegion()) {
			if (region.getSuperregion().getResourceTypes() == null) {
				region.getSuperregion().setResourceTypes(typesSet);
			} else {
				region.getSuperregion().getResourceTypes().addAll(typesSet);
			}
		}

		region.setResourceTypes(typesSet);
		resourcesByRegions.put(region, regionResources);
	}

	protected boolean prepareData(List<IndexItem> resources) {
		for (WorldRegion region : app.getWorldRegion().getFlattenedSubregions()) {
			processRegion(resourcesInRepository, di, false, region);
		}
		processRegion(resourcesInRepository, di, true, app.getWorldRegion());

		final net.osmand.Collator collator = OsmAndCollator.primaryCollator();
		final OsmandRegions osmandRegions = app.getRegions();

		Collections.sort(di.voiceRecItems, new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem lhs, IndexItem rhs) {
				return collator.compare(lhs.getVisibleName(app.getApplicationContext(), osmandRegions),
						rhs.getVisibleName(app.getApplicationContext(), osmandRegions));
			}
		});

		Collections.sort(di.voiceTTSItems, new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem lhs, IndexItem rhs) {
				return collator.compare(lhs.getVisibleName(app.getApplicationContext(), osmandRegions),
						rhs.getVisibleName(app.getApplicationContext(), osmandRegions));
			}
		});
		initAlreadyLoadedFiles();
		return true;
	}

	
	public class ItemsListBuilder {

		//public static final String WORLD_BASEMAP_KEY = "world_basemap.obf.zip";
		public static final String WORLD_SEAMARKS_KEY = "world_seamarks_basemap.obf.zip";
		private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ItemsListBuilder.class);

		private DownloadIndexes downloadIndexes;
		
		private List<ResourceItem> regionMapItems;
		private List<Object> allResourceItems;
		private List<WorldRegion> allSubregionItems;

		private OsmandApplication app;
		private WorldRegion region;

		private boolean srtmDisabled;
		private boolean hasSrtm;
		private boolean hasHillshade;

		public List<ResourceItem> getRegionMapItems() {
			return regionMapItems;
		}

		public List<Object> getAllResourceItems() {
			return allResourceItems;
		}

		public List<WorldRegion> getRegionsFromAllItems() {
			List<WorldRegion> list = new LinkedList<>();
			for (Object obj : allResourceItems) {
				if (obj instanceof WorldRegion) {
					list.add((WorldRegion) obj);
				}
			}
			return list;
		}

		public static String getVoicePromtName(Context ctx, VoicePromptsType type) {
			switch (type) {
				case RECORDED:
					return ctx.getResources().getString(R.string.index_name_voice);
				case TTS:
					return ctx.getResources().getString(R.string.index_name_tts_voice);
				default:
					return "";
			}
		}

		public List<IndexItem> getVoicePromptsItems(VoicePromptsType type) {
			switch (type) {
				case RECORDED:
					return downloadIndexes.voiceRecItems;
				case TTS:
					return downloadIndexes.voiceTTSItems;
				default:
					return new LinkedList<>();
			}
		}

		public boolean isVoicePromptsItemsEmpty(VoicePromptsType type) {
			switch (type) {
				case RECORDED:
					return downloadIndexes.voiceRecItems.isEmpty();
				case TTS:
					return downloadIndexes.voiceTTSItems.isEmpty();
				default:
					return true;
			}
		}

		// FIXME
		public ItemsListBuilder(OsmandApplication app, String regionId, DownloadIndexes di) {
			this.app = app;
			this.downloadIndexes = di;

			regionMapItems = new LinkedList<>();
			allResourceItems = new LinkedList<>();
			allSubregionItems = new LinkedList<>();

			region = app.getWorldRegion().getRegionById(regionId);
		}

		public ItemsListBuilder build() {
			if (obtainDataAndItems()) {
				return this;
			} else {
				return null;
			}
		}

		private boolean obtainDataAndItems() {
			if (downloadIndexes.resourcesByRegions.isEmpty() || region == null) {
				return false;
			}

			collectSubregionsDataAndItems();
			collectResourcesDataAndItems();

			return true;
		}

		private void collectSubregionsDataAndItems() {
			srtmDisabled = OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) == null;
			hasSrtm = false;
			hasHillshade = false;

			// Collect all regions (and their parents) that have at least one
			// resource available in repository or locally.

			allResourceItems.clear();
			allSubregionItems.clear();
			regionMapItems.clear();

			for (WorldRegion subregion : region.getFlattenedSubregions()) {
				if (subregion.getSuperregion() == region) {
					if (subregion.getFlattenedSubregions().size() > 0) {
						allSubregionItems.add(subregion);
					} else {
						collectSubregionItems(subregion);
					}
				}
			}
		}

		private void collectSubregionItems(WorldRegion region) {
			Map<String, IndexItem> regionResources = downloadIndexes.resourcesByRegions.get(region);

			if (regionResources == null) {
				return;
			}

			List<ResourceItem> regionMapArray = new LinkedList<>();
			List<Object> allResourcesArray = new LinkedList<>();

			Context context = app.getApplicationContext();
			OsmandRegions osmandRegions = app.getRegions();

			for (IndexItem indexItem : regionResources.values()) {

				String name = indexItem.getVisibleName(context, osmandRegions, false);
				if (Algorithms.isEmpty(name)) {
					continue;
				}

				ResourceItem resItem = new ResourceItem(indexItem, region);
				resItem.setResourceId(indexItem.getSimplifiedFileName());
				resItem.setTitle(name);

				if (region != this.region && srtmDisabled) {
					if (indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
						if (hasSrtm) {
							continue;
						} else {
							hasSrtm = true;
						}
					} else if (indexItem.getType() == DownloadActivityType.HILLSHADE_FILE) {
						if (hasHillshade) {
							continue;
						} else {
							hasHillshade = true;
						}
					}
				}


				if (region == this.region) {
					regionMapArray.add(resItem);
				} else {
					allResourcesArray.add(resItem);
				}

			}

			regionMapItems.addAll(regionMapArray);

			if (allResourcesArray.size() > 1) {
				allSubregionItems.add(region);
			} else {
				allResourceItems.addAll(allResourcesArray);
			}
		}

		private void collectResourcesDataAndItems() {
			collectSubregionItems(region);

			allResourceItems.addAll(allSubregionItems);

			Collections.sort(allResourceItems, new ResourceItemComparator());
			Collections.sort(regionMapItems, new ResourceItemComparator());
		}
		
		public enum MapState {
			NOT_DOWNLOADED,
			DOWNLOADED,
			OUTDATED
		}
		
		
		public enum VoicePromptsType {
			NONE,
			RECORDED,
			TTS
		}
	}

}