package net.osmand.plus.download;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@SuppressLint("DefaultLocale")
public class DownloadResourceGroup {

	private final DownloadResourceGroupType type;
	private final DownloadResourceGroup parentGroup;
	// ASSERT: individualDownloadItems are not empty if and only if groups are empty
	private final List<DownloadItem> individualDownloadItems;
	private final List<DownloadResourceGroup> groups;
	protected final String id;

	protected WorldRegion region;

	public DownloadResourceGroup(@Nullable DownloadResourceGroup parentGroup, @NonNull DownloadResourceGroupType type) {
		this(parentGroup, type, type.getDefaultId());
	}

	public DownloadResourceGroup(@Nullable DownloadResourceGroup parentGroup, @NonNull DownloadResourceGroupType type, @NonNull String id) {
		boolean flat = type.containsIndexItem();
		if (flat) {
			this.individualDownloadItems = new ArrayList<DownloadItem>();
			this.groups = null;
		} else {
			this.individualDownloadItems = null;
			this.groups = new ArrayList<DownloadResourceGroup>();
		}
		this.id = id;
		this.type = type;
		this.parentGroup = parentGroup;
	}

	public static WorldRegion getRegion(DownloadResourceGroup group) {
		if (group != null) {
			if (group.getRegion() != null) {
				return group.getRegion();
			} else if (group.getParentGroup() != null) {
				return getRegion(group.getParentGroup());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public DownloadResourceGroup getRegionGroup(WorldRegion region) {
		DownloadResourceGroup res = null;
		if (this.region == region) {
			res = this;
		} else if (groups != null) {
			for (DownloadResourceGroup group : groups) {
				if (group.region == region) {
					res = group;
					break;
				} else {
					res = group.getRegionGroup(region);
					if (res != null) {
						break;
					}
				}
			}
		}
		return res;
	}

	public void trimEmptyGroups() {
		if (groups != null) {
			for (DownloadResourceGroup gr : groups) {
				gr.trimEmptyGroups();
			}
			Iterator<DownloadResourceGroup> gr = groups.iterator();
			while (gr.hasNext()) {
				DownloadResourceGroup group = gr.next();
				if (group.isEmpty()) {
					gr.remove();
				}
			}
		}
	}
	
	public void createHillshadeSRTMGroups() {
		if (getType().isScreen()) {
			DownloadResourceGroup regionMaps = getSubGroupById(DownloadResourceGroupType.REGION_MAPS.getDefaultId());
			if (regionMaps != null && regionMaps.size() == 1 && parentGroup != null && parentGroup.getParentGroup() != null &&
					isEmpty(getSubGroupById(DownloadResourceGroupType.SUBREGIONS.getDefaultId()))) {
				IndexItem item = regionMaps.getIndividualResources().get(0);
				DownloadResourceGroup screenParent = parentGroup.getParentGroup();
				if (item.getType() == DownloadActivityType.HILLSHADE_FILE) {
					DownloadResourceGroup hillshades =
							screenParent.getSubGroupById(DownloadResourceGroupType.HILLSHADE_HEADER.getDefaultId());
					if (hillshades == null) {
						hillshades = new DownloadResourceGroup(screenParent, DownloadResourceGroupType.HILLSHADE_HEADER);
						screenParent.addGroup(hillshades);
					}
					hillshades.addItem(item);
					regionMaps.individualDownloadItems.remove(0);
				} else if (item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
					DownloadResourceGroup hillshades = screenParent
							.getSubGroupById(DownloadResourceGroupType.SRTM_HEADER.getDefaultId());
					if (hillshades == null) {
						hillshades = new DownloadResourceGroup(screenParent, DownloadResourceGroupType.SRTM_HEADER);
						screenParent.addGroup(hillshades);
					}
					hillshades.addItem(item);
					regionMaps.individualDownloadItems.remove(0);
				}
			}
			DownloadResourceGroup subregs = getSubGroupById(DownloadResourceGroupType.SUBREGIONS.getDefaultId());
			if (subregs != null) {
				for (DownloadResourceGroup g : subregs.getGroups()) {
					g.createHillshadeSRTMGroups();
				}
			}
		}
	}
	
	private boolean isEmpty(DownloadResourceGroup subGroupById) {
		return subGroupById == null || subGroupById.isEmpty();
	}

	public void addGroup(DownloadResourceGroup g) {
		if (type.isScreen()) {
			if (!g.type.isHeader()) {
				throw new UnsupportedOperationException("Trying to add " + g.getUniqueId() + " to " + getUniqueId());
			}
		}
		if (type.isHeader()) {
			if (!g.type.isScreen()) {
				throw new UnsupportedOperationException("Trying to add " + g.getUniqueId() + " to " + getUniqueId());
			}
		}
		groups.add(g);
		sortDownloadItems(g.individualDownloadItems);
	}

	protected void sortDownloadItems(List<DownloadItem> items) {
		if (Algorithms.isEmpty(items)) {
			return;
		}
		Collections.sort(items, getComparator(getRoot().app));
	}
	
	public void addItem(DownloadItem i) {
		i.setRelatedGroup(this);
		individualDownloadItems.add(i);
	}
	
	public boolean isEmpty() {
		return isEmpty(individualDownloadItems) && isEmpty(groups);
	}

	private boolean isEmpty(List<?> l) {
		return l == null || l.isEmpty();
	}

	public DownloadResourceGroup getParentGroup() {
		return parentGroup;
	}
	
	public List<DownloadResourceGroup> getGroups() {
		return groups;
	}
	
	public int size() {
		return groups != null ? groups.size() : individualDownloadItems.size();
	}
	
	public DownloadResourceGroup getGroupByIndex(int ind) {
		if (groups != null && ind < groups.size()) {
			return groups.get(ind);
		}
		return null;
	}
	
	public DownloadItem getItemByIndex(int ind) {
		if (individualDownloadItems != null && ind >= 0 && ind < individualDownloadItems.size()) {
			return individualDownloadItems.get(ind);
		}
		return null;
	}

	public DownloadResources getRoot() {
		if (this instanceof DownloadResources) {
			return (DownloadResources) this;
		} else if (parentGroup != null) {
			return parentGroup.getRoot();
		}
		return null;
	}

	public DownloadResourceGroupType getType() {
		return type;
	}

	public DownloadResourceGroup getGroupById(String uid) {
		String[] lst = uid.split("\\#");
		return getGroupById(lst, 0);
	}
	
	public DownloadResourceGroup getSubGroupById(String uid) {
		String[] lst = uid.split("\\#");
		return getSubGroupById(lst, 0);
	}
	
	public List<IndexItem> getIndividualResources() {
		List<IndexItem> individualResources = new ArrayList<>();
		if (individualDownloadItems != null) {
			for (DownloadItem item : individualDownloadItems) {
				if (item instanceof IndexItem) {
					individualResources.add((IndexItem) item);
				}
			}
		}
		return individualResources;
	}

	public List<DownloadItem> getIndividualDownloadItems() {
		return individualDownloadItems;
	}
	
	public WorldRegion getRegion() {
		return region;
	}

	private DownloadResourceGroup getGroupById(String[] lst, int subInd) {
		if (lst.length > subInd && lst[subInd].equals(id)) {
			if (lst.length == subInd + 1) {
				return this;
			} else if (groups != null) {
				return getSubGroupById(lst, subInd + 1);
			}
		}
		return null;
	}

	private DownloadResourceGroup getSubGroupById(String[] lst, int subInd) {
		for (DownloadResourceGroup rg : groups) {
			DownloadResourceGroup r = rg.getGroupById(lst, subInd);
			if (r != null) {
				return r;
			}
		}
		return null;
	}
	
	public String getName(Context ctx) {
		if (region != null) {
			return region.getLocaleName();
		} else if (type != null && type.resId != -1) {
			return ctx.getString(type.resId);
		} else {
			return id;
		}
	}

	public String getUniqueId() {
		if (parentGroup == null) {
			return id;
		}
		return parentGroup.getUniqueId() + "#" + id;
	}

	public String getId() {
		return id;
	}

	@NonNull
	public List<DownloadItem> getAllDownloadItems() {
		List<DownloadItem> downloadItems = new ArrayList<>();
		if (individualDownloadItems != null) {
			downloadItems.addAll(individualDownloadItems);
		}
		if (groups != null) {
			for (DownloadResourceGroup resourceGroup : groups) {
				List<DownloadItem> items = resourceGroup.getIndividualDownloadItems();
				if (items != null) {
					downloadItems.addAll(items);
				}
			}
		}
		return downloadItems;
	}

	public static Comparator<DownloadItem> getComparator(OsmandApplication app) {
		OsmandRegions osmandRegions = app.getRegions();
		Collator collator = OsmAndCollator.primaryCollator();
		return (firstItem, secondItem) -> {
			int firstOrder = firstItem.getType().getOrderIndex();
			int secondOrder = secondItem.getType().getOrderIndex();
			if (firstOrder < secondOrder) {
				return -1;
			} else if (firstOrder > secondOrder) {
				return 1;
			}
			String firstName = firstItem.getVisibleName(app, osmandRegions);
			String secondName = secondItem.getVisibleName(app, osmandRegions);
			return collator.compare(firstName, secondName);
		};
	}
}