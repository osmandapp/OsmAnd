package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

public class QuickActionsExportType extends ExportType {

	public QuickActionsExportType() {
		super(R.string.configure_screen_quick_action, R.drawable.ic_quick_action, SettingsItemType.QUICK_ACTIONS);
	}

	@NonNull
	@Override
	public String getId() {
		return "QUICK_ACTIONS";
	}

}
