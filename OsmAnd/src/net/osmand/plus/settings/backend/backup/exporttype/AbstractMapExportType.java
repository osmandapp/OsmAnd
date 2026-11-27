package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMapExportType extends LocalResourcesExportType {

	public static final String OFFLINE_MAPS_EXPORT_TYPE_KEY = "OFFLINE_MAPS";

	@Override
	public boolean isMap() {
		return true;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		return collectSortedLocalResource(app);
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
		return Collections.singletonList(fileSettingsItem);
	}

	@NonNull
	private List<File> collectSortedLocalResource(@NonNull OsmandApplication app) {
		List<File> files = collectLocalResources(app);
		sortLocalFiles(app, files);
		return files;
	}

	private void sortLocalFiles(@NonNull OsmandApplication app, @NonNull List<File> files) {
		// The name of a .sqlitedb map source is read from its database: read each name once
		Map<File, String> names = new HashMap<>();
		for (File file : files) {
			names.put(file, FileNameTranslationHelper.getFileNameWithRegion(app, file.getName()));
		}
		Collator collator = OsmAndCollator.primaryCollator();
		files.sort((lhs, rhs) -> collator.compare(names.get(lhs), names.get(rhs)));
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.RESOURCES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.FILE;
	}
}
