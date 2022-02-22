package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class SpaceViewHolder extends RecyclerView.ViewHolder
		implements ReorderItemTouchHelperCallback.UnmovableItem {

	View space;

	public SpaceViewHolder(View itemView) {
		super(itemView);
		space = itemView;
	}

	public void setSpace(int hSpace) {
		ViewGroup.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hSpace);
		space.setLayoutParams(lp);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
