package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

class RearrangeHeaderHolder extends ViewHolder implements UnmovableItem {

	protected final TextView title;
	protected final TextView description;
	protected final ImageView moveIcon;
	protected boolean movable = true;

	RearrangeHeaderHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.summary);
		moveIcon = itemView.findViewById(R.id.move_icon);
	}

	@Override
	public boolean isMovingDisabled() {
		return !movable;
	}
}
