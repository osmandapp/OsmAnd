package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

public class PoiTypesExportType extends ExportType {

	public PoiTypesExportType() {
		super(R.string.poi_dialog_poi_type, R.drawable.ic_action_info_dark, SettingsItemType.POI_UI_FILTERS);
	}

	@NonNull
	@Override
	public String getId() {
		return "POI_TYPES";
	}
}
