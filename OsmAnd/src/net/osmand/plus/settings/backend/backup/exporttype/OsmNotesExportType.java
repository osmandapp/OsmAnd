package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class OsmNotesExportType extends ExportType {

	public OsmNotesExportType() {
		super(R.string.osm_notes, R.drawable.ic_action_openstreetmap_logo, SettingsItemType.OSM_NOTES);
	}

	@NonNull
	@Override
	public String getId() {
		return "OSM_NOTES";
	}
}
