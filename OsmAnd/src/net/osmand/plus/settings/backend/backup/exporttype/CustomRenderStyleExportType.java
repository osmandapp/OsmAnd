package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class CustomRenderStyleExportType extends ExportType {

	public CustomRenderStyleExportType() {
		super(R.string.shared_string_rendering_style, R.drawable.ic_action_map_style, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "CUSTOM_RENDER_STYLE";
	}
}
