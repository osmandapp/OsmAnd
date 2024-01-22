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
	private List<LocalItem> collectLocalItems(@NonNull OsmandApplication app,
	                                          @NonNull LocalItemType localItemType) {
		boolean readFiles = true;
		boolean shouldUpdate = false;
		LocalIndexHelper indexHelper = new LocalIndexHelper(app);
		return indexHelper.getLocalIndexItems(readFiles, shouldUpdate, null, localItemType);
	}

	@NonNull
	private List<File> collectFilesFromLocalItems(@NonNull List<LocalItem> localItems,
	                                              @NonNull LocalItemType localItemType) {
		List<File> result = new ArrayList<>();
		for (LocalItem localItem : localItems) {
			File file = new File(localItem.getPath());
			if (file.exists() && localItemType == localItem.getType() && !shouldSkipLocalItem(localItem)) {
				result.add(file);
			}
		}
		return result;
	}

	protected boolean shouldSkipLocalItem(@NonNull LocalItem localItem) {
		return false;
	}

}
