package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class CustomRenderStyleExportType extends AbstractFileExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_rendering_style;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_map_style;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		Map<String, File> externalRenderers = app.getRendererRegistry().getExternalRenderers();
		return new ArrayList<>(externalRenderers.values());
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
		return Collections.singletonList(fileSettingsItem.getFile());
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

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.RENDERING_STYLE);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.RENDERING_STYLES;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}
