package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class HeaderViewHolder extends ViewHolder implements UnmovableItem {

	public final TextView title;

	public HeaderViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}
