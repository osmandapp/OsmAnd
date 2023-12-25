package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class RoadMapsExportType extends ExportType {

	public RoadMapsExportType() {
		super(R.string.shared_string_road_maps, R.drawable.ic_map, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "ROAD_MAPS";
	}
}
