package net.osmand.plus.settings.backend.backup.exporttype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

import java.util.Collections;
import java.util.List;

class DepthDataExportType extends AbstractMapExportType {

	@Override
	public int getTitleId() {
		return R.string.nautical_maps;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_action_anchor;
	}

	@NonNull
	@Override
	public List<FileSubtype> getRelatedFileSubtypes() {
		return Collections.singletonList(FileSubtype.NAUTICAL_DEPTH);
	}

	@Nullable
	@Override
	public LocalItemType getRelatedLocalItemType() {
		return LocalItemType.DEPTH_DATA;
	}
}
