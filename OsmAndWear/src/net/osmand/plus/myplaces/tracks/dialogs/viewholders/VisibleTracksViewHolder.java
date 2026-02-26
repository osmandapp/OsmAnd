package net.osmand.plus.myplaces.tracks.dialogs.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.shared.gpx.data.TracksGroup;

public class VisibleTracksViewHolder extends TracksGroupViewHolder {

	public VisibleTracksViewHolder(@NonNull View view, @Nullable TrackGroupsListener listener,
	                               boolean nightMode, boolean selectionMode) {
		super(view, listener, nightMode, selectionMode);
	}

	@Override
	public void bindView(@NonNull TracksGroup tracksGroup, boolean showDivider) {
		super.bindView(tracksGroup, showDivider);

		title.setText(tracksGroup.getName());
		icon.setImageDrawable(uiUtilities.getActiveIcon(R.drawable.ic_show_on_map, nightMode));

		int count = tracksGroup.getTrackItems().size();
		String tracks = app.getString(R.string.shared_string_gpx_tracks);
		description.setText(app.getString(R.string.ltr_or_rtl_combine_via_space, tracks, String.valueOf(count)));
	}
}
