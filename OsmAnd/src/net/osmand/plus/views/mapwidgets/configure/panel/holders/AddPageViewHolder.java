package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetsAdapterListener;

public class AddPageViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	private final TextView title;
	private final View selectableContainer;
	public AddPageViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.add_page);
		selectableContainer = itemView.findViewById(R.id.selectable_item);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public void bind(@NonNull OsmandApplication app, @NonNull WidgetsAdapterListener listener, boolean isVerticalPanel, @NonNull ApplicationMode selectedAppMode, boolean nightMode) {
		itemView.setOnClickListener(v -> {
			listener.onAddPageClicked();
		});
		title.setText(isVerticalPanel ? R.string.add_row : R.string.add_page);

		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(selectableContainer, drawable);
	}
}