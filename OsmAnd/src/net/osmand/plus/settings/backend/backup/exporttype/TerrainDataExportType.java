package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class TerrainDataExportType extends ExportType {

	public TerrainDataExportType() {
		super(R.string.topography_maps, R.drawable.ic_action_terrain, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "TERRAIN_DATA";
	}
}
