package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.List;

interface IExportType {

	@StringRes
	int getTitleId();

	@DrawableRes
	int getIconId();

	boolean isMap();

	boolean isAllowedInFreeVersion();

	boolean isRelatedToCategory(@NonNull ExportSettingsCategory exportCategory);

	@NonNull
	ExportSettingsCategory relatedExportCategory();

	@NonNull
	SettingsItemType relatedSettingsItemType();

	@NonNull
	List<FileSubtype> relatedFileSubtypes();

	@Nullable
	LocalItemType relatedLocalItemType();

	@Nullable
	Class<? extends OsmandPlugin> relatedPluginClass();

}
