package net.osmand.plus.mapmarkers.adapters;


import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class MapMarkerCategoriesViewHolder extends RecyclerView.ViewHolder {

	final TextView title;
	final TextView button;
	final View divider;

	public MapMarkerCategoriesViewHolder(View itemView) {
		super(itemView);
		title = (TextView) itemView.findViewById(R.id.title);
		button = (TextView) itemView.findViewById(R.id.categories_button);
		divider = itemView.findViewById(R.id.divider);
	}
}