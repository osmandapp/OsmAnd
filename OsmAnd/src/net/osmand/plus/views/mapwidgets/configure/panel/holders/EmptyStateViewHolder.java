package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetsAdapterListener;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

public class EmptyStateViewHolder extends RecyclerView.ViewHolder implements UnmovableItem {

	private final DialogButton addButton;
	private final AppCompatImageView icon;

	public EmptyStateViewHolder(@NonNull View itemView) {
		super(itemView);
		addButton = itemView.findViewById(R.id.add_button);
		icon = itemView.findViewById(R.id.no_widgets_image);
	}

	@Override
	public boolean isMovingDisabled() {
		return true;
	}

	public void bind(@NonNull OsmandApplication app, @NonNull WidgetsAdapterListener listener, boolean nightMode) {
		icon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_screen_side_left, ColorUtilities.getDefaultIconColor(app, nightMode)));
		addButton.setOnClickListener(view -> listener.addNewWidget());
	}
}