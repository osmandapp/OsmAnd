package net.osmand.plus.mapmarkers.adapters;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class MapMarkerHeaderViewHolder extends RecyclerView.ViewHolder {

	final ImageView icon;
	final View iconSpace;
	final TextView title;
	final TextView content;
	final TextView clearButton;
	final TextView button;
	final SwitchCompat disableGroupSwitch;
	final View bottomShadow;
	final View articleDescription;

	public MapMarkerHeaderViewHolder(View itemView) {
		super(itemView);
		icon = itemView.findViewById(R.id.icon);
		iconSpace = itemView.findViewById(R.id.icon_space);
		title = itemView.findViewById(R.id.title);
		disableGroupSwitch = itemView.findViewById(R.id.disable_group_switch);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
		content = itemView.findViewById(R.id.content);
		clearButton = itemView.findViewById(R.id.clear_button);
		button = itemView.findViewById(R.id.text_button);
		articleDescription = itemView.findViewById(R.id.article_description);
	}
}
