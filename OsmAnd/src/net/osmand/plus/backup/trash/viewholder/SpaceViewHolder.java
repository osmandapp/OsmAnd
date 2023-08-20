package net.osmand.plus.backup.trash.viewholder;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.View;
import android.widget.FrameLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpaceViewHolder extends RecyclerView.ViewHolder {

	public SpaceViewHolder(@NonNull View itemView, int hSpace) {
		super(itemView);
		itemView.setLayoutParams(new LayoutParams(MATCH_PARENT, hSpace));
	}

}
