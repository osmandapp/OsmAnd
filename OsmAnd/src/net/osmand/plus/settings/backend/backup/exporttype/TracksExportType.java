package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class TracksExportType extends ExportType {

	public TracksExportType() {
		super(R.string.shared_string_tracks, R.drawable.ic_action_polygom_dark, SettingsItemType.GPX);
	}

	@NonNull
	@Override
	public String getId() {
		return "TRACKS";
	}
}
