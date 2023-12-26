package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Collections;
import java.util.List;

class TracksExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_tracks;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_polygom_dark;
	}

	@NonNull
	@Override
	public ExportSettingsCategory relatedExportCategory() {
		return ExportSettingsCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.GPX;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.GPX);
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return LocalItemType.TRACKS;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return null;
	}
}
