package net.osmand.plus.download.local;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum OperationType {

	DELETE_OPERATION(R.string.shared_string_delete, R.drawable.ic_action_delete_outlined),
	BACKUP_OPERATION(R.string.local_index_mi_backup, R.drawable.ic_type_archive),
	RESTORE_OPERATION(R.string.local_index_mi_restore, R.drawable.ic_type_archive),
	CLEAR_TILES_OPERATION(R.string.clear_tile_data, R.drawable.ic_action_remove_dark);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	OperationType(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}