package net.osmand.plus.download.local;

import static net.osmand.plus.download.local.LocalItemType.LIVE_UPDATES;
import static net.osmand.plus.download.local.LocalItemType.OTHER;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.dialogs.LiveGroupItem;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CollectLocalIndexesAlgorithm {

	private final OsmandApplication app;
	private final CollectLocalIndexesRules rules;

	private final Map<LocalItem, Long> separateSizeItemCalculations = new HashMap<>();
	private final Map<LocalItemType, Long> separateSizeTypeCalculations = new HashMap<>();
	private final Map<LocalItemType, Boolean> calculationLimitReached = new HashMap<>();

	private CollectLocalIndexesAlgorithm(@NonNull CollectLocalIndexesRules rules) {
		this.app = rules.getApp();
		this.rules = rules;
	}

	@NonNull
	private Map<CategoryType, LocalCategory> execute() {
		Map<CategoryType, LocalCategory> categories = new TreeMap<>();
		for (File directory : rules.getDirectories()) {
			collectFiles(categories, directory, rules.shouldAddUnknown(directory));
		}
		applySeparatelyCalculatedSize(categories.values());
		return categories;
	}

	private void collectFiles(@NonNull Map<CategoryType, LocalCategory> categories,
	                          @NonNull File directory, boolean addUnknown) {
		addUnknown = rules.shouldAddUnknown(directory, addUnknown);
		File[] listFiles = directory.listFiles();
		if (!Algorithms.isEmpty(listFiles) && !shouldSkipDirectory(directory)) {
			for (File file : listFiles) {
				addFile(categories, file, addUnknown);

				if (file.isDirectory()) {
					collectFiles(categories, file, addUnknown);
				} else {
					calculateSizeSeparatelyIfNeeded(file);
				}
			}
		}
	}

	private boolean shouldSkipDirectory(@NonNull File directory) {
		LocalItem localItem = getSeparatelyCalculationSizeItem(directory);
		return localItem != null && isCalculatedSizeLimitReached(localItem.getType());
	}

	private void addFile(@NonNull Map<CategoryType, LocalCategory> categories, @NonNull File file, boolean addUnknown) {
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
	}

	private void addLocalItem(@NonNull LocalCategory category, @NonNull File file, @NonNull LocalItemType itemType) {
		if (itemType == LIVE_UPDATES) {
			addLiveItem(category, file, itemType);
		} else {
			LocalItem item = new LocalItem(file, itemType);
			LocalItemUtils.updateItem(app, item);
			category.addLocalItem(item);
			addSeparatelyCalculationItemIfNeeded(item);
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

	private void addSeparatelyCalculationItemIfNeeded(@NonNull LocalItem item) {
		LocalItemType itemType = item.getType();
		if (rules.shouldCalculateSizeSeparately(itemType)) {
			separateSizeItemCalculations.put(item, 0L);
			if (!separateSizeTypeCalculations.containsKey(itemType)) {
				separateSizeTypeCalculations.put(itemType, 0L);
			}
		}
	}

	private void calculateSizeSeparatelyIfNeeded(@NonNull File file) {
		LocalItem localItem = getSeparatelyCalculationSizeItem(file);
		if (localItem == null) return;

		long fileSize = file.length();
		Long itemSize = separateSizeItemCalculations.get(localItem);
		itemSize = itemSize == null ? fileSize : itemSize + fileSize;
		separateSizeItemCalculations.put(localItem, itemSize);

		LocalItemType type = localItem.getType();
		Long typeSize = separateSizeTypeCalculations.get(type);
		typeSize = typeSize == null ? fileSize : typeSize + fileSize;
		separateSizeTypeCalculations.put(type, typeSize);

		boolean limitReached = rules.isSeparatelyCalculatedSizeLimitReached(type, typeSize);
		calculationLimitReached.put(type, limitReached);
	}

	@Nullable
	private LocalItem getSeparatelyCalculationSizeItem(@NonNull File file) {
		String filePath = file.getAbsolutePath();
		for (LocalItem localItem : separateSizeItemCalculations.keySet()) {
			String basePath = localItem.getPath();
			if (filePath.startsWith(basePath)) {
				return localItem;
			}
		}
		return null;
	}

	private void applySeparatelyCalculatedSize(@NonNull Collection<LocalCategory> categories) {
		for (LocalItemType type : separateSizeTypeCalculations.keySet()) {
			if (isCalculatedSizeLimitReached(type)) {
				Long limit = rules.getCalculationSizeLimit(type);
				LocalGroup group = getLocalGroupByType(categories, type);
				if (group != null && limit != null) {
					group.setSizeLimit(limit);
				}
			}
		}
		for (LocalItem localItem : separateSizeItemCalculations.keySet()) {
			Long size = separateSizeItemCalculations.get(localItem);
			if (size != null && !isCalculatedSizeLimitReached(localItem.getType())) {
				localItem.setSize(size);
			} else {
				localItem.setSize(-1);
			}
		}
	}

	private boolean isCalculatedSizeLimitReached(@NonNull LocalItemType type) {
		Boolean limitReached = calculationLimitReached.get(type);
		return limitReached != null && limitReached;
	}

	@Nullable
	private LocalGroup getLocalGroupByType(@NonNull Collection<LocalCategory> categories,
	                                       @NonNull LocalItemType type) {
		for (LocalCategory category : categories) {
			LocalGroup group = category.getGroups().get(type);
			if (group != null) {
				return group;
			}
		}
		return null;
	}

	@NonNull
	public static Map<CategoryType, LocalCategory> execute(@NonNull CollectLocalIndexesRules rules) {
		return new CollectLocalIndexesAlgorithm(rules).execute();
	}
}