package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

class RearrangeButtonHolder extends ViewHolder implements UnmovableItem {

	protected final ImageView icon;
	protected final TextView title;

	RearrangeButtonHolder(@NonNull View itemView) {
		super(itemView);
		icon = itemView.findViewById(android.R.id.icon);
		title = itemView.findViewById(android.R.id.title);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
