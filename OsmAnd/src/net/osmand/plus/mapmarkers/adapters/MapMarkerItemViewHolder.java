package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkerItemViewHolder extends RecyclerView.ViewHolder {

	final ImageView iconDirection;
	final ImageView iconReorder;
	final ImageView icon;
	final TextView title;
	final TextView distance;
	final TextView point;
	final TextView description;
	final ImageButton optionsBtn;

	public MapMarkerItemViewHolder(View view) {
		super(view);
		iconDirection = (ImageView) view.findViewById(R.id.map_marker_direction_icon);
		iconReorder = (ImageView) view.findViewById(R.id.map_marker_reorder_icon);
		icon = (ImageView) view.findViewById(R.id.map_marker_icon);
		title = (TextView) view.findViewById(R.id.map_marker_title);
		distance = (TextView) view.findViewById(R.id.map_marker_distance);
		point = (TextView) view.findViewById(R.id.map_marker_point_text_view);
		description = (TextView) view.findViewById(R.id.map_marker_description);
		optionsBtn = (ImageButton) view.findViewById(R.id.map_marker_options_button);
	}
}
