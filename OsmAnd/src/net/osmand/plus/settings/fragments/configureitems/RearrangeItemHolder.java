package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

class RearrangeItemHolder extends ViewHolder implements UnmovableItem {

	protected final TextView title;
	protected final TextView description;
	protected final ImageView icon;
	protected final ImageView actionIcon;
	protected final ImageView moveIcon;
	protected final FrameLayout moveButton;
	protected final View divider;
	protected boolean movable = true;

	RearrangeItemHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		actionIcon = itemView.findViewById(R.id.action_icon);
		icon = itemView.findViewById(R.id.icon);
		moveIcon = itemView.findViewById(R.id.move_icon);
		moveButton = itemView.findViewById(R.id.move_button);
		divider = itemView.findViewById(R.id.divider);
	}

	@Override
	public boolean isMovingDisabled() {
		return !movable;
	}
}
