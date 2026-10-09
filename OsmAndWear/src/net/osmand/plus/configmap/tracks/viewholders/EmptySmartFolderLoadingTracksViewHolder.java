package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class EmptySmartFolderLoadingTracksViewHolder extends RecyclerView.ViewHolder {

	protected final OsmandApplication app;
	protected final TextView title;
	protected final TextView description;

	public EmptySmartFolderLoadingTracksViewHolder(@NonNull View view) {
		super(view);
		app = (OsmandApplication) itemView.getContext().getApplicationContext().getApplicationContext();
		title = view.findViewById(R.id.title);
		description = view.findViewById(R.id.description);
	}

	public void bindView() {
		title.setText(R.string.shared_string_loading);
		description.setText(R.string.searching_matching_tracks);

	}
}