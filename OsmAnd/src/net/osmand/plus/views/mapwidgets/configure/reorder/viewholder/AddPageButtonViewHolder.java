package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.graphics.drawable.Drawable;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AddPageButtonViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	public final View buttonContainer;

	public AddPageButtonViewHolder(@NonNull View itemView, @ColorInt int profileColor) {
		super(itemView);
		buttonContainer = itemView.findViewById(R.id.button_container);
		Drawable background = UiUtilities.getColoredSelectableDrawable(itemView.getContext(), profileColor, 0.3f);
		buttonContainer.setBackground(background);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}
}