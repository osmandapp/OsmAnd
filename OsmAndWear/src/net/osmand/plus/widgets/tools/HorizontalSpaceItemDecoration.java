package net.osmand.plus.widgets.tools;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HorizontalSpaceItemDecoration extends RecyclerView.ItemDecoration {

	private final int horizontalSpaceDp;

	public HorizontalSpaceItemDecoration(int horizontalSpaceDp) {
		this.horizontalSpaceDp = horizontalSpaceDp;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
	                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		// Apply horizontal spacing to all items except the first one
		if (parent.getChildAdapterPosition(view) != 0) {
			outRect.left = horizontalSpaceDp;
		}
	}
}
