package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkersGroupHeaderViewHolder extends RecyclerView.ViewHolder {

	TextView title;
	TextView description;

	public MapMarkersGroupHeaderViewHolder(View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
	}
}
