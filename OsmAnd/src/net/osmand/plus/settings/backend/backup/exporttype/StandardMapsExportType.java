package net.osmand.plus.settings.backend.backup.exporttype;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.map.WorldRegion.WORLD_BASEMAP_MINI;
import static net.osmand.plus.download.local.LocalItemType.MAP_DATA;
import static net.osmand.plus.download.local.LocalItemType.TILES_DATA;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.OBF_MAP;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype.TILES_MAP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class StandardMapsExportType extends AbstractMapExportType {

	private static final String BASE_MINI = WORLD_BASEMAP_MINI + BINARY_MAP_INDEX_EXT;

	@Override
	public int getTitleId() {
		return R.string.standard_maps;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_map;
	}

	@Override
	protected boolean shouldSkipLocalItem(@NonNull OsmandApplication app, @NonNull LocalItem localItem) {
		return localItem.getType() == MAP_DATA && BASE_MINI.equalsIgnoreCase(localItem.getFileName());
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Arrays.asList(OBF_MAP, TILES_MAP);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return MAP_DATA;
	}

	@NonNull
	@Override
	protected List<File> collectLocalResources(@NonNull OsmandApplication app) {
		List<File> files = new ArrayList<>(super.collectLocalResources(app));

		List<LocalItem> items = collectLocalItems(app, TILES_DATA);
		files.addAll(collectFilesFromLocalItems(app, items, TILES_DATA));

		return files;
	}
}
