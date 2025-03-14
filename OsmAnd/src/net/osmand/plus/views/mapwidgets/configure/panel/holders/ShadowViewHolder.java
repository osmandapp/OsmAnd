package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class ShadowViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	public ShadowViewHolder(@NonNull View itemView) {
		super(itemView);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

}