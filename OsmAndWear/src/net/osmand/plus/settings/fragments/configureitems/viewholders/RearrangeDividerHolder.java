package net.osmand.plus.settings.fragments.configureitems.viewholders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class RearrangeDividerHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	public RearrangeDividerHolder(@NonNull View itemView) {
		super(itemView);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
