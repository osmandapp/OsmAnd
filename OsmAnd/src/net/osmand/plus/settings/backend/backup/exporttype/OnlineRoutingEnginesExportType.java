package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class OnlineRoutingEnginesExportType extends ExportType {

	public OnlineRoutingEnginesExportType() {
		super(R.string.online_routing_engines, R.drawable.ic_world_globe_dark, SettingsItemType.ONLINE_ROUTING_ENGINES);
	}

	@NonNull
	@Override
	public String getId() {
		return "ONLINE_ROUTING_ENGINES";
	}
}