package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.resources.AssetsCollection;
import net.osmand.plus.resources.ResourceManager;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class LocalResourcesExportType extends AbstractFileExportType {

	private static final Log LOG = PlatformUtil.getLog(LocalResourcesExportType.class);

	@NonNull
	protected List<File> collectLocalResources(@NonNull OsmandApplication app) {
		LocalItemType relatedLocalItemType = getRelatedLocalItemType();
		if (relatedLocalItemType != null) {
			List<LocalItem> localItems = collectLocalItems(app, relatedLocalItemType);
			return collectFilesFromLocalItems(app, localItems, relatedLocalItemType);
		}
		return Collections.emptyList();
	}

	@NonNull
	protected List<LocalItem> collectLocalItems(@NonNull OsmandApplication app,	@NonNull LocalItemType type) {
		boolean readFiles = true;
		boolean shouldUpdate = false;
		LocalIndexHelper indexHelper = new LocalIndexHelper(app);
		return indexHelper.getLocalIndexItems(readFiles, shouldUpdate, null, type);
	}

	@NonNull
	protected List<File> collectFilesFromLocalItems(@NonNull OsmandApplication app,
	                                                @NonNull List<LocalItem> items,
	                                                @NonNull LocalItemType type) {
		List<File> result = new ArrayList<>();
		for (LocalItem item : items) {
			File file = new File(item.getPath());
			if (file.exists() && type == item.getType() && !shouldSkipLocalItem(app, item)) {
				result.add(file);
			}
		}
		return result;
	}

	protected boolean shouldSkipLocalItem(@NonNull OsmandApplication app, @NonNull LocalItem localItem) {
		if (!localItem.getType().isDerivedFromAssets()) {
			return false;
		}
		ResourceManager resourceManager = app.getResourceManager();
		try {
			// Skip files derived from assets that don't contain a version or haven't been modified.
			File file = localItem.getFile();
			AssetsCollection assets = resourceManager.getAssets();
			if (assets.isFileDerivedFromAssets(file)) {
				long lastModified = file.lastModified();
				Long version = assets.getVersionTime(file);
				return version == null || version == lastModified;
			}
		} catch (IOException e) {
			LOG.error(e);
		}
		return false;
	}
}
