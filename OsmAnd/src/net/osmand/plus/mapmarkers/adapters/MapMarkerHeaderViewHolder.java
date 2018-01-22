package net.osmand.plus.mapmarkers.adapters;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;

public class MapMarkerHeaderViewHolder extends RecyclerView.ViewHolder {

	final ImageView icon;
	final View iconSpace;
	final TextView title;
	final SwitchCompat disableGroupSwitch;
	final View bottomShadow;

	public MapMarkerHeaderViewHolder(View itemView) {
		super(itemView);
		icon = (ImageView) itemView.findViewById(R.id.icon);
		iconSpace = itemView.findViewById(R.id.icon_space);
		title = (TextView) itemView.findViewById(R.id.title);
		disableGroupSwitch = (SwitchCompat) itemView.findViewById(R.id.disable_group_switch);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
	}
}
