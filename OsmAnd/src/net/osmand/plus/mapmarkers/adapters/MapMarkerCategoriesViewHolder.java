package net.osmand.plus.mapmarkers.adapters;


import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkerCategoriesViewHolder extends RecyclerView.ViewHolder {

	final ImageView icon;
	final TextView title;

	public MapMarkerCategoriesViewHolder(View itemView) {
		super(itemView);
		icon = (ImageView) itemView.findViewById(R.id.icon_right);
		title = (TextView) itemView.findViewById(R.id.title);
	}
}