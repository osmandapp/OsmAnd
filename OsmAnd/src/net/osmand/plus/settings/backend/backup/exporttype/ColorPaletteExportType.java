package net.osmand.plus.settings.backend.backup.exporttype;

import static net.osmand.plus.helpers.ColorsPaletteUtils.getPaletteName;
import static net.osmand.plus.helpers.ColorsPaletteUtils.getPaletteTypeName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
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
import java.util.Collections;
import java.util.List;

public class ColorPaletteExportType extends LocalResourcesExportType {

	@Override
	public int getTitleId() {
		return R.string.shared_string_colors;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_file_color_palette;
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
		Collator collator = OsmAndCollator.primaryCollator();
		files.sort((lhs, rhs) -> {
			int r = collator.compare(getPaletteTypeName(app, lhs), getPaletteTypeName(app, rhs));
			return r == 0 ? collator.compare(getPaletteName(lhs), getPaletteName(rhs)) : r;
		});
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
		return Collections.singletonList(FileSubtype.COLOR_PALETTE);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.COLOR_DATA;
	}

	@Nullable
	@Override
	public Class<? extends OsmandPlugin> getRelatedPluginClass() {
		return null;
	}
}
