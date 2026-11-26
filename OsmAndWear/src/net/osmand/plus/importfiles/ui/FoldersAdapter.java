package net.osmand.plus.importfiles.ui;

import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.ColorUtilities.getActiveColorId;
import static net.osmand.plus.utils.ColorUtilities.getPrimaryTextColor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.measurementtool.adapter.FolderListAdapter;

class FoldersAdapter extends FolderListAdapter {

	public FoldersAdapter(@NonNull OsmandApplication app, @Nullable String selectedItem, boolean nightMode) {
		super(app, selectedItem, nightMode);
	}

	@Override
	protected void bindAddItem(@NonNull FolderViewHolder holder) {
		super.bindAddItem(holder);
		holder.groupName.setText(R.string.shared_string_add);
		holder.groupIcon.setImageDrawable(uiUtilities.getIcon(R.drawable.ic_action_folder_add, getActiveColorId(nightMode)));
	}

	@Override
	protected void bindFolderItem(@NonNull FolderViewHolder holder, @NonNull String item, boolean selected) {
		super.bindFolderItem(holder, item, selected);
		int color = selected ? getPrimaryTextColor(app, nightMode) : getActiveColor(app, nightMode);
		holder.groupName.setTextColor(color);
		holder.groupIcon.setImageDrawable(uiUtilities.getThemedIcon(R.drawable.ic_action_folder));
	}
}
