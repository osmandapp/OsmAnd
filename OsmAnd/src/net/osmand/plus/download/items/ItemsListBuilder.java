package net.osmand.plus.download.items;

import android.content.Context;

import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ItemsListBuilder {

	public class ResourceItem {

		private String resourceId;
		private String title;
		private long contentSize;
		private long containerSize;
		private boolean disabled;

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

		public long getContentSize() {
			return contentSize;
		}

		public void setContentSize(long contentSize) {
			this.contentSize = contentSize;
		}

		public long getContainerSize() {
			return containerSize;
		}

		public void setContainerSize(long containerSize) {
			this.containerSize = containerSize;
		}

		public boolean isDisabled() {
			return disabled;
		}

		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}

		public ResourceItem(IndexItem indexItem, WorldRegion worldRegion) {
			this.indexItem = indexItem;
			this.worldRegion = worldRegion;
		}
	}

	class ResourceItemComparator implements Comparator {
		@Override
		public int compare(Object obj1, Object obj2) {
			String str1;
			String str2;

			if (obj1 instanceof WorldRegion) {
				str1 = ((WorldRegion)obj1).getName();
			} else {
				ResourceItem item = (ResourceItem)obj1;
				str1 = item.title + item.getIndexItem().getType().getTag();
			}

			if (obj2 instanceof WorldRegion) {
				str2 = ((WorldRegion)obj2).getName();
			} else {
				ResourceItem item = (ResourceItem)obj2;
				str2 = item.title + item.getIndexItem().getType().getTag();
			}

			return str1.compareTo(str2);
		}
	}

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(ItemsListBuilder.class);

	private static Map<WorldRegion, Map<String, IndexItem>> resourcesByRegions =
			new HashMap<>();
	private static List<WorldRegion> searchableWorldwideRegionItems = new LinkedList<>();

	private List<ResourceItem> regionMapItems;
	private List allResourceItems;
	private List<WorldRegion> allSubregionItems;

	private OsmandApplication app;
	private WorldRegion region;

	private boolean invalidated;
	private boolean srtmDisabled;
	private boolean hasSrtm;

	public List<ResourceItem> getRegionMapItems() {
		return regionMapItems;
	}

	public List getAllResourceItems() {
		return allResourceItems;
	}

	public List<WorldRegion> getRegionsFromAllItems() {
		List<WorldRegion> list = new LinkedList<>();
		for (Object obj : allResourceItems) {
			if (obj instanceof WorldRegion) {
				list.add((WorldRegion)obj);
			}
		}
		return list;
	}

	public ItemsListBuilder(OsmandApplication app) {
		this.app = app;
		regionMapItems = new LinkedList();
		allResourceItems = new LinkedList();
		allSubregionItems = new LinkedList<>();
	}

	public ItemsListBuilder(OsmandApplication app, WorldRegion region) {
		this(app);
		this.region = region;
	}

	public boolean build() {
		return obtainDataAndItems();
	}

	private boolean obtainDataAndItems() {
		if (invalidated) {
			resourcesByRegions.clear();
		}

		if (resourcesByRegions.isEmpty()) {
			if (!prepareData()) {
				return false;
			}
		}

		collectSubregionsDataAndItems();
		collectResourcesDataAndItems();

		LOG.warn("getRegionMapItems >>>");
		for (ResourceItem resourceItem : getRegionMapItems()) {
			LOG.warn("resId=" + resourceItem.getIndexItem().getFileName() + " title=" + resourceItem.getTitle());
		}

		LOG.warn("getAllResourceItems >>>");
		for (Object obj : getAllResourceItems()) {
			if (obj instanceof WorldRegion) {
				WorldRegion item = (WorldRegion)obj;
				LOG.warn("W resId=" + item.getRegionId() + " title=" + item.getName());
			} else if (obj instanceof ResourceItem) {
				ResourceItem resourceItem = (ResourceItem)obj;
				LOG.warn("R resId=" + resourceItem.getIndexItem().getFileName() + " title=" + resourceItem.getTitle());
			}
		}

		invalidated = false;

		return true;
	}

	public void invalidate() {
		invalidated = true;
	}

	private boolean prepareData() {
		List<IndexItem> resourcesInRepository = DownloadActivity.downloadListIndexThread.getCachedIndexFiles();
		if (resourcesInRepository == null) {
			return false;
		}

		boolean doInit = resourcesByRegions.isEmpty();
		boolean initSearchableRegions = searchableWorldwideRegionItems.isEmpty() || doInit;

		if (initSearchableRegions) {
			searchableWorldwideRegionItems.clear();
		}

		List<WorldRegion> mergedRegions = app.getWorldRegion().getFlattenedSubregions();
		mergedRegions.add(app.getWorldRegion());
		for(WorldRegion region : mergedRegions)
		{
			if (initSearchableRegions) {
				searchableWorldwideRegionItems.add(region);
			}

			String downloadsIdPrefix = region.getDownloadsIdPrefix().toLowerCase();

			Map<String, IndexItem> regionResources = new HashMap<>();

			if (!doInit)
			{
				regionResources.putAll(resourcesByRegions.get(region));
			}

			if (doInit)
			{
				List<DownloadActivityType> typesArray = new LinkedList<>();
				boolean hasSrtm = false;
				for (IndexItem resource : resourcesInRepository)
				{
					if (!resource.getFileName().startsWith(downloadsIdPrefix))
						continue;

					if (resource.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
						hasSrtm = true;
					}

					typesArray.add(resource.getType());

					regionResources.put(resource.getFileName(), resource);
				}

				if (region.getSuperregion() != null && hasSrtm && region.getSuperregion().getSuperregion() != app.getWorldRegion())
				{
					if (!region.getSuperregion().getResourceTypes().contains(DownloadActivityType.SRTM_COUNTRY_FILE))
					{
						region.getSuperregion().getResourceTypes().add(DownloadActivityType.SRTM_COUNTRY_FILE);
						Collections.sort(region.getSuperregion().getResourceTypes(), new Comparator<DownloadActivityType>() {
							@Override
							public int compare(DownloadActivityType dat1, DownloadActivityType dat2) {
								return dat1.getTag().compareTo(dat2.getTag());
							}
						});
					}
				}

				Collections.sort(typesArray, new Comparator<DownloadActivityType>() {
					@Override
					public int compare(DownloadActivityType dat1, DownloadActivityType dat2) {
						return dat1.getTag().compareTo(dat2.getTag());
					}
				});
				region.setResourceTypes(typesArray);
			}

			resourcesByRegions.put(region, regionResources);
		}
		return true;
	}

	private void collectSubregionsDataAndItems() {
		srtmDisabled = false; //todo: check if srtm plugin disabled
		hasSrtm = false;

		// Collect all regions (and their parents) that have at least one
		// resource available in repository or locally.

		allResourceItems.clear();
		allSubregionItems.clear();
		regionMapItems.clear();

		for (WorldRegion subregion : region.getFlattenedSubregions())
		{
			if (subregion.getSuperregion() == region)
			{
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
		List allResourcesArray = new LinkedList();

		Context context = app.getApplicationContext();
		OsmandRegions osmandRegions = app.getRegions();

		for (IndexItem indexItem : regionResources.values()) {

			String name = indexItem.getVisibleName(context, osmandRegions);
			if (Algorithms.isEmpty(name)) {
				continue;
			}

			ResourceItem resItem = new ResourceItem(indexItem, region);
			resItem.setResourceId(indexItem.getFileName());
			resItem.setTitle(name);
			resItem.setContentSize(indexItem.getContentSize());
			resItem.setContainerSize(indexItem.getSize());

				if (region != this.region && srtmDisabled)
				{
					if (hasSrtm && indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE)
						continue;

					if (indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE)
					{
						resItem.setTitle("srtm_disabled"); // todo: constant
						resItem.setContentSize(0);
						resItem.setContainerSize(0);
					}

					if (!hasSrtm && indexItem.getType() == DownloadActivityType.SRTM_COUNTRY_FILE)
						hasSrtm = true;
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

		/*
		 * todo: remove seamarks if plugin is off
		if (![[OAIAPHelper sharedInstance] productPurchased:kInAppId_Addon_Nautical]) {
			for (ResourceItem *item in _regionMapItems)
			if (item.resourceId.compare(QString(kWorldSeamarksKey)) == 0) {
				[_regionMapItems removeObject:item];
				break;
			}
		}
		*/
	}
}
