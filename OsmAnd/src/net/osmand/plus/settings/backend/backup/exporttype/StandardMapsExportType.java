package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class StandardMapsExportType extends ExportType {

	public StandardMapsExportType() {
		super(R.string.standard_maps, R.drawable.ic_map, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "STANDARD_MAPS";
	}
}
