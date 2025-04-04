package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class DividerViewHolder extends RecyclerView.ViewHolder implements ReorderItemTouchHelperCallback.UnmovableItem {
	private final View topShadow;
	public DividerViewHolder(@NonNull View itemView) {
		super(itemView);
		topShadow = itemView.findViewById(R.id.bottomShadowView);
	}

	public void bind(){
		topShadow.setVisibility(View.GONE);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}