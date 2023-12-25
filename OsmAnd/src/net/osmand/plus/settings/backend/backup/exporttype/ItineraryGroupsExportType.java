package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class ItineraryGroupsExportType extends ExportType {

	public ItineraryGroupsExportType() {
		super(R.string.shared_string_itinerary, R.drawable.ic_action_flag, SettingsItemType.ITINERARY_GROUPS);
	}

	@NonNull
	@Override
	public String getId() {
		return "ITINERARY_GROUPS";
	}
}
