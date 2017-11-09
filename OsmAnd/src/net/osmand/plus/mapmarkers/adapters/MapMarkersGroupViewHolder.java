package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkersGroupViewHolder extends RecyclerView.ViewHolder {

	ImageView icon;
	TextView name;
	TextView numberCount;

	public MapMarkersGroupViewHolder(View itemView) {
		super(itemView);
		icon = itemView.findViewById(R.id.icon);
		name = itemView.findViewById(R.id.name_text);
		numberCount = itemView.findViewById(R.id.number_count_text);
	}
}
