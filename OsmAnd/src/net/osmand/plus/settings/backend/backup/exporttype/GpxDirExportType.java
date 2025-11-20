package net.osmand.plus.settings.backend.backup.exporttype;

import static net.osmand.shared.gpx.GpxParameter.APPEARANCE_LAST_MODIFIED_TIME;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GpxDirSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.shared.gpx.GpxDirItem;
import net.osmand.shared.io.KFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GpxDirExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.track_folder_appearance;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_folder_tracks;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<GpxDirItem> items = new ArrayList<>();
		for (GpxDirItem item : app.getGpxDbHelper().getDirItems()) {
			KFile file = item.getFile();
			Long time = item.getParameter(APPEARANCE_LAST_MODIFIED_TIME);
			if (file.exists() && time != null && time > 0) {
				items.add(item);
			}
		}
		return items;
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		return Collections.singletonList(((GpxDirSettingsItem) settingsItem).getDirItem());
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		return object instanceof GpxDirItem;
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.GPX_DIR;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return null;
	}
}