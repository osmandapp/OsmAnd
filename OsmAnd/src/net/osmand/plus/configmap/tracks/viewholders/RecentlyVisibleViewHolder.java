package net.osmand.plus.configmap.tracks.viewholders;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.tracks.TrackTabsHelper;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.configmap.tracks.TracksFragment;
import net.osmand.plus.myplaces.tracks.ItemsSelectionHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.Set;

public class RecentlyVisibleViewHolder extends RecyclerView.ViewHolder {

	private final OsmandApplication app;
	private final TextView title;
	private final CheckBox checkbox;
	private final TracksFragment fragment;
	private final boolean nightMode;

	public RecentlyVisibleViewHolder(@NonNull View view, @NonNull TracksFragment fragment, boolean nightMode) {
		super(view);
		this.fragment = fragment;
		this.nightMode = nightMode;
		this.app = (OsmandApplication) view.getContext().getApplicationContext();

		title = view.findViewById(R.id.title);
		checkbox = view.findViewById(R.id.checkbox);
	}

	public void bindView() {
		TrackTabsHelper helper = fragment.getTrackTabsHelper();
		Set<TrackItem> recentlyVisibleTracks = helper.getRecentlyVisibleTracks();
		title.setText(app.getString(R.string.recently_visible, "(" + recentlyVisibleTracks.size() + ")"));

		ItemsSelectionHelper<TrackItem> selectionHelper =  helper.getItemsSelectionHelper();
		boolean selected = selectionHelper.isItemsSelected(recentlyVisibleTracks);
		checkbox.setChecked(selected);
		int color = ColorUtilities.getAppModeColor(app, nightMode);
		UiUtilities.setupCompoundButton(selected, color, checkbox);

		itemView.setOnClickListener(v -> fragment.onTrackItemsSelected(recentlyVisibleTracks, !selected));
	}
}
