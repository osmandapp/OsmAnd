package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class OsmEditsExportType extends ExportType {

	public OsmEditsExportType() {
		super(R.string.osm_edits, R.drawable.ic_action_openstreetmap_logo, SettingsItemType.OSM_EDITS);
	}

	@NonNull
	@Override
	public String getId() {
		return "OSM_EDITS";
	}
}