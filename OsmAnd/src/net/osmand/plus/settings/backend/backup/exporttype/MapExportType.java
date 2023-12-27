package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class MapExportType extends LocalResourcesExportType {

	public static final String OLD_OFFLINE_MAPS_EXPORT_TYPE_KEY = "OFFLINE_MAPS";

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
	private List<File> collectSortedLocalResource(@NonNull OsmandApplication app) {
		List<File> files = collectLocalResources(app);
		sortLocalFiles(app, files);
		return files;
	}

	private void sortLocalFiles(@NonNull OsmandApplication app, @NonNull List<File> files) {
		Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(files, new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				return collator.compare(getNameToDisplay(lhs), getNameToDisplay(rhs));
			}

			private String getNameToDisplay(File item) {
				return FileNameTranslationHelper.getFileNameWithRegion(app, item.getName());
			}
		});
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.RESOURCES;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.FILE;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return null;
	}
}
