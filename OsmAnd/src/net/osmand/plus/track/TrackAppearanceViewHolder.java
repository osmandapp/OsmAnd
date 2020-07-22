package net.osmand.plus.track;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class TrackAppearanceViewHolder extends RecyclerView.ViewHolder {

	final TextView title;
	final ImageView icon;
	final ImageView button;

	TrackAppearanceViewHolder(View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.groupName);
		icon = itemView.findViewById(R.id.groupIcon);
		button = itemView.findViewById(R.id.outlineRect);
	}
}