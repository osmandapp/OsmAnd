package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class SpaceViewHolder extends ViewHolder implements UnmovableItem {

	public SpaceViewHolder(@NonNull View itemView) {
		super(itemView);
	}

	public void setHeight(int height) {
		itemView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
