package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PageViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	public final View topDivider;
	public final ImageButton actionButton;
	public final TextView pageText;

	public PageViewHolder(@NonNull View itemView) {
		super(itemView);
		topDivider = itemView.findViewById(R.id.top_divider);
		actionButton = itemView.findViewById(R.id.action_button);
		pageText = itemView.findViewById(R.id.page);
	}

	@Override
	public boolean isMovingDisabled() {
		return false;
	}
}