package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationModeBean;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ProfileExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_profiles;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_manage_profiles;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<ApplicationModeBean> appModeBeans = new ArrayList<>();
		for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
			appModeBeans.add(appMode.toModeBean());
		}
		return appModeBeans;
	}

	@Override
	public boolean isAllowedInFreeVersion() {
		return true;
	}

	@NonNull
	@Override
	public ExportCategory relatedExportCategory() {
		return ExportCategory.SETTINGS;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.PROFILE;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return LocalItemType.PROFILES;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return null;
	}
}
