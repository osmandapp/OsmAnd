package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.io.File;

abstract class AbstractFileExportType extends AbstractExportType {

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		if (object instanceof File) {
			File file = (File) object;
			FileSubtype fileSubtype = FileSubtype.getSubtypeByPath(app, file.getPath());
			return getRelatedFileSubtypes().contains(fileSubtype);
		}
		return false;
	}
}
