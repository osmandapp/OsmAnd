package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkerDateViewHolder extends RecyclerView.ViewHolder {

	final TextView date;
	final ImageButton optionsBtn;

	public MapMarkerDateViewHolder(View itemView) {
		super(itemView);
		date = itemView.findViewById(R.id.date_title);
		optionsBtn = itemView.findViewById(R.id.date_options_button);
	}
}
