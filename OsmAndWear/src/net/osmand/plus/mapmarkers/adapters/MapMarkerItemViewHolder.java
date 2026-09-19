package net.osmand.plus.mapmarkers.adapters;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class MapMarkerItemViewHolder extends RecyclerView.ViewHolder {

	final View mainLayout;
	final View topDivider;
	final ImageView iconDirection;
	final TextView numberText;
	final ImageView iconReorder;
	final ImageView icon;
	final TextView title;
	final TextView firstDescription;
	final TextView distance;
	final View flagIconLeftSpace;
	final View leftPointSpace;
	final TextView point;
	final View rightPointSpace;
	final TextView description;
	public final ImageButton optionsBtn;
	final View checkBoxContainer;
	final CheckBox checkBox;
	final View divider;
	final View bottomShadow;

	public MapMarkerItemViewHolder(View view) {
		super(view);
		mainLayout = view.findViewById(R.id.main_layout);
		topDivider = view.findViewById(R.id.top_divider);
		iconDirection = view.findViewById(R.id.map_marker_direction_icon);
		numberText = view.findViewById(R.id.map_marker_number_text_view);
		iconReorder = view.findViewById(R.id.map_marker_reorder_icon);
		icon = view.findViewById(R.id.map_marker_icon);
		title = view.findViewById(R.id.map_marker_title);
		firstDescription = view.findViewById(R.id.map_marker_first_descr);
		distance = view.findViewById(R.id.map_marker_distance);
		flagIconLeftSpace = view.findViewById(R.id.flag_icon_left_space);
		leftPointSpace = view.findViewById(R.id.map_marker_left_point_space);
		point = view.findViewById(R.id.map_marker_point_text_view);
		rightPointSpace = view.findViewById(R.id.map_marker_right_point_space);
		description = view.findViewById(R.id.map_marker_description);
		optionsBtn = view.findViewById(R.id.map_marker_options_button);
		checkBoxContainer = view.findViewById(R.id.check_box_container);
		checkBox = view.findViewById(R.id.map_marker_check_box);
		divider = view.findViewById(R.id.divider);
		bottomShadow = view.findViewById(R.id.bottom_shadow);
	}
}
