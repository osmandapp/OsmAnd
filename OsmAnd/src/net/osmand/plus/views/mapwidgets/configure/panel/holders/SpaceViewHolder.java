package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

public class SpaceViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	public SpaceViewHolder(@NonNull View itemView) {
		super(itemView);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public void bind(@NonNull MapActivity mapActivity) {
		int height = mapActivity.getResources().getDimensionPixelSize(R.dimen.bottom_space_height);
		itemView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
	}

}