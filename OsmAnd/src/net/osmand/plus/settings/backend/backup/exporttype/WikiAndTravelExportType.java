package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class WikiAndTravelExportType extends ExportType {

	public WikiAndTravelExportType() {
		super(R.string.wikipedia_and_travel_maps, R.drawable.ic_action_wikipedia, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "WIKI_AND_TRAVEL";
	}
}