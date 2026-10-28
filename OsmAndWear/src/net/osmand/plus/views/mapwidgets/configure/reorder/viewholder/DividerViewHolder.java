package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class DividerViewHolder extends ViewHolder implements UnmovableItem {

	public DividerViewHolder(@NonNull View itemView) {
		super(itemView);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
