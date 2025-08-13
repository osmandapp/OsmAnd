package net.osmand.plus.download.local;

import static net.osmand.plus.download.local.LocalItemType.LIVE_UPDATES;
import static net.osmand.plus.download.local.LocalItemType.OTHER;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OnResultCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.dialogs.LiveGroupItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class CollectLocalIndexesAlgorithm {

	private final OsmandApplication app;
	private final CollectLocalIndexesRules rules;
	private final OnResultCallback<LocalItem> onLocalItemAddedCallback;

	private CollectLocalIndexesAlgorithm(@NonNull CollectLocalIndexesRules rules,
	                                     @NonNull OnResultCallback<LocalItem> onLocalItemAddedCallback) {
		this.app = rules.getApp();
		this.rules = rules;
		this.onLocalItemAddedCallback = onLocalItemAddedCallback;
	}

	@NonNull
	private Map<CategoryType, LocalCategory> execute() {
		Map<CategoryType, LocalCategory> categories = new TreeMap<>();
		for (File directory : rules.getDirectories()) {
			collectFiles(categories, directory, rules.shouldAddUnknown(directory));
		}
		return categories;
	}

	private void collectFiles(@NonNull Map<CategoryType, LocalCategory> categories,
	                          @NonNull File directory, boolean addUnknown) {
		addUnknown = rules.shouldAddUnknown(directory, addUnknown);
		File[] listFiles = directory.listFiles();
		if (!Algorithms.isEmpty(listFiles)) {
			for (File file : listFiles) {
				LocalItemType type = addFile(categories, file, addUnknown);

				if (file.isDirectory() && type != TILES_DATA) {
					collectFiles(categories, file, addUnknown);
				}
			}
		}
	}

	@Nullable
	private LocalItemType addFile(@NonNull Map<CategoryType, LocalCategory> categories, @NonNull File file, boolean addUnknown) {
		LocalItemType itemType = LocalItemUtils.getItemType(app, file);
		if (itemType != null && (itemType != OTHER || addUnknown)) {
			CategoryType categoryType = itemType.getCategoryType();
			LocalCategory category = categories.get(categoryType);
			if (category == null) {
				category = new LocalCategory(categoryType);
				categories.put(categoryType, category);
			}
			addLocalItem(category, file, itemType);
		}
		return itemType;
	}

	private void addLocalItem(@NonNull LocalCategory category, @NonNull File file, @NonNull LocalItemType itemType) {
		if (itemType == LIVE_UPDATES) {
			addLiveItem(category, file, itemType);
		} else {
			LocalItem item = new LocalItem(file, itemType);
			LocalItemUtils.updateItem(app, item);
			category.addLocalItem(item);
			onLocalItemAddedCallback.onResult(item);
		}
	}

	private void addLiveItem(@NonNull LocalCategory category, @NonNull File file, @NonNull LocalItemType itemType) {
		String basename = FileNameTranslationHelper.getBasename(app, file.getName());
		String liveGroupName = getNameToDisplay(basename.replaceAll("(_\\d*)*$", ""), app);

		LocalGroup localGroup = category.getGroups().get(LIVE_UPDATES);
		if (localGroup == null) {
			localGroup = new LocalGroup(LIVE_UPDATES);
			category.getGroups().put(LIVE_UPDATES, localGroup);
		}
		LiveGroupItem liveGroup = (LiveGroupItem) localGroup.getItem(liveGroupName);
		if (liveGroup == null) {
			liveGroup = new LiveGroupItem(liveGroupName);
			localGroup.addItem(liveGroupName, liveGroup);
		}
		LocalItem item = new LocalItem(file, itemType);
		LocalItemUtils.updateItem(app, item);
		liveGroup.addLocalItem(item);
	}

	@NonNull
	public static Map<CategoryType, LocalCategory> execute(@NonNull CollectLocalIndexesRules rules,
	                                                       @NonNull OnResultCallback<LocalItem> onLocalItemAddedCallback) {
		return new CollectLocalIndexesAlgorithm(rules, onLocalItemAddedCallback).execute();
	}
}