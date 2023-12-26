package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Collections;
import java.util.List;

class OsmEditsExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.osm_edits;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_openstreetmap_logo;
	}

	@NonNull
	@Override
	public ExportSettingsCategory relatedExportCategory() {
		return ExportSettingsCategory.MY_PLACES;
	}

	@NonNull
	@Override
	public SettingsItemType relatedSettingsItemType() {
		return SettingsItemType.OSM_EDITS;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return LocalItemType.OSM_EDITS;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> relatedPluginClass() {
		return OsmEditingPlugin.class;
	}

	@Override
	public boolean isAllowedInFreeVersion() {
		return true;
	}
}