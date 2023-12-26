package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Arrays;
import java.util.List;

class WikiAndTravelExportType extends MapExportType {

	@Override
	public int getTitleId() {
		return R.string.wikipedia_and_travel_maps;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_wikipedia;
	}

	@NonNull
	@Override
	public List<FileSubtype> relatedFileSubtypes() {
		return Arrays.asList(FileSubtype.WIKI_MAP, FileSubtype.TRAVEL);
	}

	@Nullable
	@Override
	public LocalItemType relatedLocalItemType() {
		return LocalItemType.WIKI_AND_TRAVEL_MAPS;
	}
}