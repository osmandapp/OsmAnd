package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.data.OrganizedTracks;
import net.osmand.shared.gpx.data.TracksGroup;

public class OrganizedTracksViewHolder extends TracksGroupViewHolder {

	public OrganizedTracksViewHolder(@NonNull View view, @Nullable TrackGroupsListener listener,
	                                 boolean nightMode, boolean selectionMode) {
		super(view, listener, nightMode, selectionMode);
	}

	@Override
	public void bindView(@NonNull TracksGroup tracksGroup, boolean showDivider) {
		super.bindView(tracksGroup, showDivider);

		OrganizedTracks organizedTracks = (OrganizedTracks) tracksGroup;
		title.setText(organizedTracks.getName());
		int itemsCount = tracksGroup.getTrackItems().size();
		description.setText(app.getString(R.string.number_of_tracks, String.valueOf(itemsCount)));

		String iconName = organizedTracks.getIconName();
		int iconId = AndroidUtils.getDrawableId(app, iconName, R.drawable.ic_action_folder);
		icon.setImageDrawable(uiUtilities.getActiveIcon(iconId, nightMode));
	}

	@Override
	protected boolean hasOptionsMenu(@NonNull TracksGroup tracksGroup) {
		return true;
	}
}
