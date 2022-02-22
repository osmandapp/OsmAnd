package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class HeaderViewHolder extends RecyclerView.ViewHolder
		implements ReorderItemTouchHelperCallback.UnmovableItem {

	public final TextView tvTitle;

	public HeaderViewHolder(View itemView) {
		super(itemView);
		tvTitle = itemView.findViewById(R.id.title);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
