package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OsmAndCollator;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DisplayPointsGroupsHelper {

	private final OsmandApplication app;
	private final Comparator<String> comparator;

	private final List<GpxDisplayGroup> groups = new ArrayList<>();
	private final Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups = new LinkedHashMap<>();

	private DisplayPointsGroupsHelper(OsmandApplication app) {
		this.app = app;
		comparator = OsmAndCollator.primaryCollator()::compare;
	}

	public static DisplayGroupsHolder getGroups(
			@NonNull OsmandApplication app,
			@NonNull List<GpxDisplayGroup> displayGroups,
			@Nullable Set<?> filteredItems
	) {
		DisplayPointsGroupsHelper helper = new DisplayPointsGroupsHelper(app);
		return helper.getGroups(displayGroups, filteredItems);
	}

	private DisplayGroupsHolder getGroups(@NonNull List<GpxDisplayGroup> displayGroups,
	                                      @Nullable Set<?> filteredItems) {
		Collections.sort(displayGroups, (g1, g2) -> {
			int i1 = g1.getType().ordinal();
			int i2 = g2.getType().ordinal();
			return Algorithms.compare(i1, i2);
		});
		List<GpxDisplayGroup> trackPointsGroups = new ArrayList<>();
		List<GpxDisplayGroup> routePointsGroups = new ArrayList<>();
		for (GpxDisplayGroup group : displayGroups) {
			if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
				trackPointsGroups.add(group);
			} else if (group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
				routePointsGroups.add(group);
			}
		}
		processDisplayGroups(trackPointsGroups, filteredItems);
		processDisplayGroups(routePointsGroups, filteredItems);
		return new DisplayGroupsHolder(groups, itemGroups);
	}

	private void processDisplayGroups(List<GpxDisplayGroup> displayGroups, Set<?> filteredItems) {
		for (int i = 0; i < displayGroups.size(); i++) {
			GpxDisplayGroup group = displayGroups.get(i);
			if (group.getDisplayItems().isEmpty()) {
				continue;
			}
			Map<String, List<GpxDisplayItem>> itemsMap = collectItemsByCategory(group, i);
			if (filteredItems != null) {
				itemsMap = filterItems(itemsMap, filteredItems);
			}
			if (!Algorithms.isEmpty(itemsMap)) {
				setCollectedItems(group, itemsMap);
			}
		}
	}

	private Map<String, List<GpxDisplayItem>> collectItemsByCategory(GpxDisplayGroup group, int index) {
		Map<String, List<GpxDisplayItem>> itemsMap = new HashMap<>();

		for (GpxDisplayItem item : group.getDisplayItems()) {
			String category;
			if (item.locationStart != null) {
				if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
					category = item.locationStart.getCategory();
					if (Algorithms.isBlank(category)) {
						category = "";
					}
				} else {
					category = app.getString(R.string.route_points) + " " + (index + 1);
				}
			} else {
				category = "";
			}
			List<GpxDisplayItem> items = itemsMap.get(category);
			if (items == null) {
				items = new ArrayList<>();
				itemsMap.put(category, items);
			}
			items.add(item);
		}
		return itemsMap;
	}

	private Map<String, List<GpxDisplayItem>> filterItems(Map<String, List<GpxDisplayItem>> itemsMap, Set<?> filteredItems) {
		Map<String, List<GpxDisplayItem>> itemsMapFiltered = new HashMap<>();
		for (Entry<String, List<GpxDisplayItem>> e : itemsMap.entrySet()) {
			String category = e.getKey();
			List<GpxDisplayItem> items = e.getValue();
			if (filteredItems.contains(category)) {
				itemsMapFiltered.put(category, items);
			} else {
				for (GpxDisplayItem i : items) {
					if (filteredItems.contains(i)) {
						List<GpxDisplayItem> itemsFiltered = itemsMapFiltered.get(category);
						if (itemsFiltered == null) {
							itemsFiltered = new ArrayList<>();
							itemsMapFiltered.put(category, itemsFiltered);
						}
						itemsFiltered.add(i);
					}
				}
			}
		}
		return itemsMapFiltered;
	}

	private void setCollectedItems(@NonNull GpxDisplayGroup group,
	                               @NonNull Map<String, List<GpxDisplayItem>> itemsMap) {
		List<String> categories = new ArrayList<>(itemsMap.keySet());
		Collections.sort(categories, comparator);
		for (String category : categories) {
			List<GpxDisplayItem> values = itemsMap.get(category);
			GpxDisplayGroup categoryGroup = group.copy();
			categoryGroup.setName(category);
			for (GpxDisplayItem i : values) {
				if (i.locationStart != null && i.locationStart.getColor() != 0) {
					categoryGroup.setColor(i.locationStart.getColor(group.getColor()));
					break;
				}
			}
			categoryGroup.clearDisplayItems();
			categoryGroup.addDisplayItems(values);
			itemGroups.put(categoryGroup, values);
			this.groups.add(categoryGroup);
		}
	}

	public static class DisplayGroupsHolder {

		public List<GpxDisplayGroup> groups;
		public Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups;

		public DisplayGroupsHolder(@NonNull List<GpxDisplayGroup> groups,
		                           @NonNull Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups) {
			this.groups = groups;
			this.itemGroups = itemGroups;
		}

		@Nullable
		public List<GpxDisplayItem> getItemsByGroupName(@NonNull String name) {
			for (GpxDisplayGroup group : itemGroups.keySet()) {
				if (Algorithms.objectEquals(name, group.getName())) {
					return itemGroups.get(group);
				}
			}
			return null;
		}

		public int getVisibleTrackGroupsNumber(@NonNull SelectedGpxFile selectedGpxFile) {
			int visibleGroupsNumber = 0;
			for (GpxDisplayGroup group : itemGroups.keySet()) {
				if (group.getType() == GpxDisplayItemType.TRACK_POINTS
						&& !selectedGpxFile.isGroupHidden(group.getName())) {
					visibleGroupsNumber++;
				}
			}
			return visibleGroupsNumber;
		}

		public int getTotalTrackGroupsNumber() {
			int total = 0;
			for (GpxDisplayGroup group : itemGroups.keySet()) {
				if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
					total++;
				}
			}
			return total;
		}
	}
}
