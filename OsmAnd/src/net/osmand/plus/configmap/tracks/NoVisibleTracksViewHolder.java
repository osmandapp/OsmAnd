package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.utils.UiUtilities.DialogButtonType.SECONDARY;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TracksAdapter.TracksVisibilityListener;
import net.osmand.plus.utils.UiUtilities;

public class NoVisibleTracksViewHolder extends RecyclerView.ViewHolder {

	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final View button;

	public NoVisibleTracksViewHolder(@NonNull View view, @NonNull TracksVisibilityListener listener) {
		super(view);
		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		button = view.findViewById(R.id.action_button);
		button.setOnClickListener(v -> listener.showAllTracks());
	}

	public void bindView(@NonNull OsmandApplication app, boolean nightMode) {
		title.setText(R.string.no_visible_tracks);
		description.setText(R.string.no_visible_tracks_description);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_track_disabled));
		UiUtilities.setupDialogButton(nightMode, button, SECONDARY, R.string.show_all_tracks);
	}
}
