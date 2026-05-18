package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TracksGroup;

public class SmartFolderViewHolder extends TracksGroupViewHolder {

	public SmartFolderViewHolder(@NonNull View view, @Nullable TrackGroupsListener listener,
	                             boolean nightMode, boolean selectionMode) {
		super(view, listener, nightMode, selectionMode);
	}

	@Override
	public void bindView(@NonNull TracksGroup tracksGroup, boolean showDivider) {
		super.bindView(tracksGroup, showDivider);
		SmartFolder folder = (SmartFolder) tracksGroup;
		title.setText(folder.getName());
		description.setText(app.getString(R.string.number_of_tracks, String.valueOf(tracksGroup.getTrackItems().size())));
		icon.setImageDrawable(uiUtilities.getActiveIcon(R.drawable.ic_action_folder_smart, nightMode));
		AndroidUiHelper.updateVisibility(menuButton, !selectionMode);
		menuButton.setOnClickListener(v -> {
			if (listener != null) {
				listener.onTracksGroupOptionsSelected(v, tracksGroup);
			}
		});
	}
}
