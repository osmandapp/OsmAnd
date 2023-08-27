package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

class RearrangeDividerHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	protected View divider;

	RearrangeDividerHolder(@NonNull View itemView) {
		super(itemView);
		divider = itemView.findViewById(R.id.divider);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
