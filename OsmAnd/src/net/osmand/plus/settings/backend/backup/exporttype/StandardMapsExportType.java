package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.map.WorldRegion;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Collections;
import java.util.List;

class StandardMapsExportType extends AbstractMapExportType {

	private static final String BASE_MINI =
			WorldRegion.WORLD_BASEMAP_MINI + IndexConstants.BINARY_MAP_INDEX_EXT;

	@Override
	public int getTitleId() {
		return R.string.standard_maps;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_map;
	}

	@Override
	protected boolean shouldSkipLocalItem(@NonNull LocalItem localItem) {
		return localItem.getType() == LocalItemType.MAP_DATA
				&& BASE_MINI.equalsIgnoreCase(localItem.getFileName());
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.OBF_MAP);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.MAP_DATA;
	}
}
