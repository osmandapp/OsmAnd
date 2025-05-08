package net.osmand.plus.mapmarkers.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class MapMarkersGroupViewHolder extends RecyclerView.ViewHolder {

	ImageView icon;
	TextView name;
	TextView numberCount;
	TextView description;

	public MapMarkersGroupViewHolder(View itemView) {
		super(itemView);
		icon = itemView.findViewById(R.id.icon);
		name = itemView.findViewById(R.id.name_text);
		numberCount = itemView.findViewById(R.id.number_count_text);
		description = itemView.findViewById(R.id.description_text);
	}
}
