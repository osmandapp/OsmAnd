package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackTabType;
import net.osmand.plus.track.BaseTracksTabsFragment;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class NoVisibleTracksViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final DialogButton button;

	public NoVisibleTracksViewHolder(@NonNull View view, @NonNull BaseTracksTabsFragment fragment) {
		super(view);
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();

		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
		icon = view.findViewById(R.id.icon);
		button = view.findViewById(R.id.action_button);
		button.setOnClickListener(v -> fragment.setSelectedTab(TrackTabType.ALL.name()));
	}

	public void bindView() {
		title.setText(R.string.no_visible_tracks);
		description.setText(R.string.no_visible_tracks_description);
		icon.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_track_disabled));
		button.setTitleId(R.string.show_all_tracks);
	}
}
