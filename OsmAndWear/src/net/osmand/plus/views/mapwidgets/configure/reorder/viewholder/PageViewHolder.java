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
	public final ImageButton deletePageButton;
	public final TextView pageText;
	public final View moveIcon;

	public PageViewHolder(@NonNull View itemView) {
		super(itemView);
		topDivider = itemView.findViewById(R.id.top_divider);
		deletePageButton = itemView.findViewById(R.id.delete_page_button);
		pageText = itemView.findViewById(R.id.page);
		moveIcon = itemView.findViewById(R.id.move_button);
	}

	@Override
	public boolean isMovingDisabled() {
		return false;
	}

	// This class is needed as int wrapper, because pages reordering with plain int is buggy
	public static class PageUiInfo {

		public int index;

		public PageUiInfo(int index) {
			this.index = index;
		}

		public void onPreviousPageDeleted() {
			index--;
		}

		public void onPreviousPageRestored() {
			index++;
		}
	}
}