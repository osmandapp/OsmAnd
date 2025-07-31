package net.osmand.plus.mapmarkers.adapters;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class MapMarkersShowHideHistoryViewHolder extends RecyclerView.ViewHolder {

	final TextView title;
	final View bottomShadow;

	public MapMarkersShowHideHistoryViewHolder(View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.show_hide_history_title);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
	}
}
