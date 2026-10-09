package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.util.List;

interface IExportType {

	@StringRes
	int getTitleId();

	@DrawableRes
	int getIconId();

	boolean isMap();

	boolean isAvailableInFreeVersion();

	boolean isRelatedToCategory(@NonNull ExportCategory exportCategory);

	@NonNull
	List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup);

	@NonNull
	List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted);

	boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object);

	@NonNull
	ExportCategory getRelatedExportCategory();

	@NonNull
	SettingsItemType getRelatedSettingsItemType();

	@NonNull
	List<FileSubtype> getRelatedFileSubtypes();

	@Nullable
	LocalItemType getRelatedLocalItemType();

	@Nullable
	Class<? extends OsmandPlugin> getRelatedPluginClass();

}
