package net.osmand.plus.download.local.dialogs.controllers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalGroup;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalItemUtils;
import net.osmand.plus.download.local.MultipleLocalItem;
import net.osmand.plus.download.local.dialogs.HeaderGroup;
import net.osmand.plus.download.local.dialogs.LocalItemsComparator;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.settings.enums.LocalSortMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LocalItemsCollector {

	private final OsmandApplication app;

	public LocalItemsCollector(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@NonNull
	public LocalItemsCollection collect(@NonNull LocalGroup localGroup,
	                                    @Nullable String currentFolderId) {
		List<Object> rootItems = collectRootItems(localGroup);

		if (currentFolderId == null) {
			return new LocalItemsCollection(rootItems, null);
		}

		// Try to find the currently selected folder in the newly generated list
		MultipleLocalItem folder = findFolderById(rootItems, currentFolderId);

		if (folder != null) {
			List<BaseLocalItem> folderItems = new ArrayList<>(folder.getItems());
			sortItems(localGroup.getType(), folderItems);
			return new LocalItemsCollection(new ArrayList<>(folderItems), folder);
		} else {
			// Folder no longer exists (e.g. empty or merged), fallback to root
			return new LocalItemsCollection(rootItems, null);
		}
	}

	@NonNull
	private List<Object> collectRootItems(@NonNull LocalGroup group) {
		List<BaseLocalItem> activeItems = new ArrayList<>();
		List<BaseLocalItem> backupedItems = new ArrayList<>();

		for (BaseLocalItem item : group.getItems()) {
			if (item instanceof LocalItem localItem && localItem.isBackuped(app)) {
				backupedItems.add(item);
			} else {
				activeItems.add(item);
			}
		}

		LocalItemType type = group.getType();
		if (type.isGroupingByCountrySupported()) {
			activeItems = groupItemsByCountry(type, activeItems, "activated");
			backupedItems = groupItemsByCountry(type, backupedItems, "deactivated");
		}

		sortItems(type, activeItems);
		sortItems(type, backupedItems);

		List<Object> result = new ArrayList<>(activeItems);
		if (!Algorithms.isEmpty(backupedItems)) {
			result.add(new HeaderGroup(app.getString(R.string.local_indexes_cat_backup), backupedItems));
			result.addAll(backupedItems);
		}
		return result;
	}

	@Nullable
	private MultipleLocalItem findFolderById(@NonNull List<Object> items, @NonNull String id) {
		for (Object item : items) {
			if (item instanceof MultipleLocalItem folder && Objects.equals(folder.getId(), id)) {
				return folder;
			}
		}
		return null;
	}

	@NonNull
	private List<BaseLocalItem> groupItemsByCountry(@NonNull LocalItemType type,
	                                                @NonNull List<BaseLocalItem> flatItems,
	                                                @NonNull String listKey) {
		Map<String, List<BaseLocalItem>> groups = new HashMap<>();
		List<BaseLocalItem> ungroupedItems = new ArrayList<>();
		OsmandRegions regions = app.getRegions();

		for (BaseLocalItem item : flatItems) {
			if (item instanceof LocalItem localItem) {
				String baseName = FileNameTranslationHelper.getBasename(app, localItem.getFileName());
				WorldRegion country = regions.getCountryRegionDataByDownloadName(baseName);
				if (country != null) {
					groups.computeIfAbsent(country.getLocaleName(), k -> new ArrayList<>()).add(localItem);
				} else {
					ungroupedItems.add(localItem);
				}
			} else {
				ungroupedItems.add(item);
			}
		}

		List<MultipleLocalItem> folders = new ArrayList<>();
		for (Map.Entry<String, List<BaseLocalItem>> entry : groups.entrySet()) {
			List<BaseLocalItem> folderItems = entry.getValue();
			if (folderItems.size() > 1) {
				String folderName = entry.getKey();
				String folderId = folderName.toLowerCase() + "_" + listKey;
				folders.add(new MultipleLocalItem(folderId, folderName, type, folderItems));
			} else if (!folderItems.isEmpty()) {
				ungroupedItems.add(folderItems.get(0));
			}
		}

		List<BaseLocalItem> result = new ArrayList<>(folders);
		result.addAll(ungroupedItems);
		return result;
	}

	private void sortItems(@NonNull LocalItemType type, @NonNull List<BaseLocalItem> items) {
		if (type.isSortingSupported()) {
			LocalSortMode sortMode = LocalItemUtils.getSortModePref(app, type).get();
			items.sort(new LocalItemsComparator(app, sortMode));
		} else {
			Collator collator = OsmAndCollator.primaryCollator();
			items.sort((o1, o2) -> collator.compare(o1.getName(app).toString(), o2.getName(app).toString()));
		}
	}
}