package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class DividerViewHolder extends RecyclerView.ViewHolder
		implements ReorderItemTouchHelperCallback.UnmovableItem {

	public DividerViewHolder(View itemView) {
		super(itemView);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
