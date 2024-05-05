package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class LocalResourcesExportType extends AbstractFileExportType {

	@NonNull
	protected List<File> collectLocalResources(@NonNull OsmandApplication app) {
		LocalItemType relatedLocalItemType = getRelatedLocalItemType();
		if (relatedLocalItemType != null) {
			List<LocalItem> localItems = collectLocalItems(app, relatedLocalItemType);
			return collectFilesFromLocalItems(localItems, relatedLocalItemType);
		}
		return Collections.emptyList();
	}

	@NonNull
	private List<LocalItem> collectLocalItems(@NonNull OsmandApplication app, @NonNull LocalItemType type) {
		boolean readFiles = true;
		boolean shouldUpdate = false;
		LocalIndexHelper indexHelper = new LocalIndexHelper(app);
		return indexHelper.getLocalIndexItems(readFiles, shouldUpdate, null, type);
	}

	@NonNull
	private List<File> collectFilesFromLocalItems(@NonNull List<LocalItem> items, @NonNull LocalItemType type) {
		List<File> result = new ArrayList<>();
		for (LocalItem item : items) {
			File file = new File(item.getPath());
			if (file.exists() && type == item.getType() && !shouldSkipLocalItem(item)) {
				result.add(file);
			}
		}
		return result;
	}

	protected boolean shouldSkipLocalItem(@NonNull LocalItem localItem) {
		return false;
	}
}
