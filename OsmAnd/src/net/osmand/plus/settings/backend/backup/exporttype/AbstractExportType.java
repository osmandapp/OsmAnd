package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.ExportCategory;

abstract class AbstractExportType implements IExportType {

	public boolean isAvailableInFreeVersion() {
		return false;
	}

	public boolean isMap() {
		return false;
	}

	public boolean isRelatedToCategory(@NonNull ExportCategory category) {
		return getRelatedExportCategory() == category;
	}
}
