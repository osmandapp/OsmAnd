package net.osmand.plus.widgets.chips;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;

public class ChipViewHolder extends RecyclerView.ViewHolder {

	public final TextView title;
	public final ImageView image;
	public final ViewGroup button;
	public final LinearLayout container;
	public final View clickArea;

	ChipViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.chip_title);
		image = itemView.findViewById(R.id.chip_icon);
		button = itemView.findViewById(R.id.button);
		container = button.findViewById(R.id.button_container);
		clickArea = itemView;
	}

}
