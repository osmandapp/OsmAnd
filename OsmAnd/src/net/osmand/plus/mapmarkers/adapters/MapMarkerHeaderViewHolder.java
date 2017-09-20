package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkerHeaderViewHolder extends RecyclerView.ViewHolder {

	final View topShadowDivider;
	final View topDivider;
	final ImageView icon;
	final View iconSpace;
	final TextView title;
	final ImageButton optionsBtn;

	public MapMarkerHeaderViewHolder(View itemView) {
		super(itemView);
		topShadowDivider = itemView.findViewById(R.id.top_shadow_divider);
		topDivider = itemView.findViewById(R.id.top_divider);
		icon = (ImageView) itemView.findViewById(R.id.icon);
		iconSpace = itemView.findViewById(R.id.icon_space);
		title = (TextView) itemView.findViewById(R.id.title);
		optionsBtn = (ImageButton) itemView.findViewById(R.id.date_options_button);
	}
}
