package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class CustomRoutingExportType extends ExportType {

	public CustomRoutingExportType() {
		super(R.string.shared_string_routing, R.drawable.ic_action_route_distance, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "CUSTOM_ROUTING";
	}
}
