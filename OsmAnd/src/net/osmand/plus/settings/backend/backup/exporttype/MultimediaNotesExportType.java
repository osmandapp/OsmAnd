package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

class MultimediaNotesExportType extends ExportType {

	public MultimediaNotesExportType() {
		super(R.string.notes, R.drawable.ic_grouped_by_type, SettingsItemType.FILE);
	}

	@NonNull
	@Override
	public String getId() {
		return "MULTIMEDIA_NOTES";
	}
}