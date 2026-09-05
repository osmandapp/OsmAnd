package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.plus.track.helpers.GpxUiHelper;

public class TrackFolderViewHolder extends TracksGroupViewHolder {

	public TrackFolderViewHolder(@NonNull View view, @Nullable TrackGroupsListener listener,
	                             boolean nightMode, boolean selectionMode) {
		super(view, listener, nightMode, selectionMode);
	}

	@Override
	public void bindView(@NonNull TracksGroup tracksGroup, boolean showDivider) {
		super.bindView(tracksGroup, showDivider);

		TrackFolder folder = (TrackFolder) tracksGroup;
		title.setText(folder.getName());
		description.setText(GpxUiHelper.getFolderDescription(app, folder));
		icon.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_folder, folder.getColor()));
	}
}
