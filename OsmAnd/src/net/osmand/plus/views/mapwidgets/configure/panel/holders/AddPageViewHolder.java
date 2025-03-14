package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter;

public class AddPageViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	public AddPageViewHolder(@NonNull View itemView) {
		super(itemView);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public void bind(@NonNull WidgetsListAdapter.WidgetsAdapterListener listener) {
		itemView.setOnClickListener(v -> {
			listener.onAddPageClicked();
		});
	}
}