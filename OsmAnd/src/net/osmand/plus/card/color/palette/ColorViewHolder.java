package net.osmand.plus.card.color.palette;

import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

class ColorViewHolder extends RecyclerView.ViewHolder {

	public final ImageView outline;
	public final ImageView background;

	public ColorViewHolder(@NonNull View itemView) {
		super(itemView);
		outline = itemView.findViewById(R.id.outline);
		background = itemView.findViewById(R.id.background);
	}
}
