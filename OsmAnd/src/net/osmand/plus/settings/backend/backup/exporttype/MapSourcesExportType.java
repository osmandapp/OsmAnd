package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.MapSourcesSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class MapSourcesExportType extends AbstractExportType {

	@Override
	public int getTitleId() {
		return R.string.quick_action_map_source_title;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_layers;
	}

	@NonNull
	@Override
	public List<?> fetchExportData(@NonNull OsmandApplication app, boolean offlineBackup) {
		List<ITileSource> iTileSources = new ArrayList<>();
		Set<String> tileSourceNames = app.getSettings().getTileSourceEntries(true).keySet();
		for (String name : tileSourceNames) {
			File file = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name);
			ITileSource template;
			if (file.getName().endsWith(SQLiteTileSource.EXT)) {
				template = new SQLiteTileSource(app, file, TileSourceManager.getKnownSourceTemplates());
			} else {
				template = TileSourceManager.createTileSourceTemplate(file);
			}
			if (template.getUrlTemplate() != null) {
				iTileSources.add(template);
			}
		}
		return iTileSources;
	}

	@NonNull
	@Override
	public List<?> fetchImportData(@NonNull SettingsItem settingsItem, boolean importCompleted) {
		if (settingsItem instanceof MapSourcesSettingsItem) {
			MapSourcesSettingsItem mapSourcesItem = (MapSourcesSettingsItem) settingsItem;
			if (importCompleted) {
				return mapSourcesItem.getAppliedItems();
			} else {
				return mapSourcesItem.getItems();
			}
		} else {
			return Collections.singletonList(settingsItem);
		}
	}

	@Override
	public boolean isRelatedObject(@NonNull OsmandApplication app, @NonNull Object object) {
		return object instanceof ITileSource;
	}

	@NonNull
	@Override
	public ExportCategory getRelatedExportCategory() {
		return ExportCategory.RESOURCES;
	}

	@NonNull
	@Override
	public SettingsItemType getRelatedSettingsItemType() {
		return SettingsItemType.MAP_SOURCES;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.TILES_MAP);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.TILES_DATA;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}
