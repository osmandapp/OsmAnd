package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class DepthDataExportType extends ExportType {

	public DepthDataExportType() {
		super(R.string.nautical_maps, R.drawable.ic_action_anchor, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "DEPTH_DATA";
	}
}
