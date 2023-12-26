package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ExportSettingsCategory;

public abstract class AbstractExportType implements IExportType {

	public boolean isAllowedInFreeVersion() {
		return false;
	}

	public boolean isMap() {
		return false;
	}

	public boolean isRelatedToCategory(@NonNull ExportSettingsCategory category) {
		return relatedExportCategory() == category;
	}
}
