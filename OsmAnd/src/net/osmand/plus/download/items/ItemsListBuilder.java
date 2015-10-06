package net.osmand.plus.download.items;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.util.Algorithms;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ItemsListBuilder {

	public static final String WORLD_BASEMAP_KEY = "world_basemap.obf.zip";
	public static final String WORLD_SEAMARKS_KEY = "world_seamarks_basemap.obf.zip";

	private Map<WorldRegion, Map<String, IndexItem>> resourcesByRegions;
	private List<IndexItem> voiceRecItems;
	private List<IndexItem> voiceTTSItems;

	public class ResourceItem {

		private String resourceId;
		private String title;

		private IndexItem indexItem;
		private WorldRegion worldRegion;

		public IndexItem getIndexItem() {
			return indexItem;
		}

		public WorldRegion getWorldRegion() {
			return worldRegion;
		}

		public String getResourceId() {
			return resourceId;
		}

		public void setResourceId(String resourceId) {
			this.resourceId = resourceId;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public ResourceItem(IndexItem indexItem, WorldRegion worldRegion) {
			this.indexItem = indexItem;
			this.worldRegion = worldRegion;
		}
	}

	class ResourceItemComparator implements Comparator<Object> {
		@Override
		public int compare(Object obj1, Object obj2) {
			String str1;
			String str2;

			if (obj1 instanceof WorldRegion) {
				str1 = ((WorldRegion) obj1).getName();
			} else {
				ResourceItem item = (ResourceItem) obj1;
				str1 = item.title + item.getIndexItem().getType().getOrderIndex();
			}

			if (obj2 instanceof WorldRegion) {
				str2 = ((WorldRegion) obj2).getName();
			} else {
				ResourceItem item = (ResourceItem) obj2;
				str2 = item.title + item.getIndexItem().getType().getOrderIndex();
			}

			return str1.compareTo(str2);
		}
	}

	public enum VoicePromptsType {
		NONE,
		RECORDED,
		TTS;
	}

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ItemsListBuilder.class);

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

	public String getVoicePromtName(VoicePromptsType type) {
		switch (type) {
			case RECORDED:
				return app.getResources().getString(R.string.index_name_voice);
			case TTS:
				return app.getResources().getString(R.string.index_name_tts_voice);
			default:
				return "";
		}
	}

	public List<IndexItem> getVoicePromptsItems(VoicePromptsType type) {
		switch (type) {
			case RECORDED:
				return voiceRecItems;
			case TTS:
				return voiceTTSItems;
			default:
				return new LinkedList<>();
		}
	}

	public boolean isVoicePromptsItemsEmpty(VoicePromptsType type) {
		switch (type) {
			case RECORDED:
				return voiceRecItems.isEmpty();
			case TTS:
				return voiceTTSItems.isEmpty();
			default:
				return true;
		}
	}

	public ItemsListBuilder(OsmandApplication app, String regionId, Map<WorldRegion, Map<String, IndexItem>> resourcesByRegions,
							List<IndexItem> voiceRecItems, List<IndexItem> voiceTTSItems) {
		this.app = app;
		this.resourcesByRegions = resourcesByRegions;
		this.voiceRecItems = voiceRecItems;
		this.voiceTTSItems = voiceTTSItems;

		regionMapItems = new LinkedList<>();
		allResourceItems = new LinkedList<>();
		allSubregionItems = new LinkedList<>();

		region = app.getWorldRegion().getRegionById(regionId);
	}

	public boolean build() {
		return obtainDataAndItems();
	}

	private boolean obtainDataAndItems() {
		if (resourcesByRegions.isEmpty() || region == null) {
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
		Map<String, IndexItem> regionResources = resourcesByRegions.get(region);

		List<ResourceItem> regionMapArray = new LinkedList<>();
		List<Object> allResourcesArray = new LinkedList<Object>();

		Context context = app.getApplicationContext();
		OsmandRegions osmandRegions = app.getRegions();

		for (IndexItem indexItem : regionResources.values()) {

			String name = indexItem.getVisibleName(context, osmandRegions);
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
}
