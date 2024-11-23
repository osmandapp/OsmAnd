package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Arrays;
import java.util.List;

class TerrainDataExportType extends AbstractMapExportType {

	@Override
	public int getTitleId() {
		return R.string.topography_maps;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_terrain;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Arrays.asList(FileSubtype.SRTM_MAP, FileSubtype.TERRAIN_DATA);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.TERRAIN_DATA;
	}
}
