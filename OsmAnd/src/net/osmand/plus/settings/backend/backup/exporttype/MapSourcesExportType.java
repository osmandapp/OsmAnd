package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class MapSourcesExportType extends ExportType {

	public MapSourcesExportType() {
		super(R.string.quick_action_map_source_title, R.drawable.ic_action_layers, SettingsItemType.MAP_SOURCES);
	}

	@NonNull
	@Override
	public String getId() {
		return "MAP_SOURCES";
	}
}
