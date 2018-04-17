package net.osmand.plus.mapmarkers.adapters;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkerWikivoyageArticleViewHolder extends RecyclerView.ViewHolder {

	final ImageView icon;
	final TextView title;
	final View divider;

	public MapMarkerWikivoyageArticleViewHolder(View itemView) {
		super(itemView);
		icon = (ImageView) itemView.findViewById(R.id.icon_right);
		title = (TextView) itemView.findViewById(R.id.title);
		divider = itemView.findViewById(R.id.divider);
	}
}
