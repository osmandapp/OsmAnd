package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetsAdapterListener;
import net.osmand.plus.widgets.TextViewEx;

public class WidgetViewHolder extends RecyclerView.ViewHolder {
	private final TextViewEx title;
	private final ImageView icon;
	private final ImageButton deleteButton;
	private final AppCompatImageView moveIcon;
	private final View bottomDivider;
	private final View selectableBackground;
	private final View bottomShadow;

	public WidgetViewHolder(@NonNull OsmandApplication app, @NonNull ApplicationMode selectedAppMode, @NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		deleteButton = itemView.findViewById(R.id.delete_widget_button);
		moveIcon = itemView.findViewById(R.id.move_icon);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);
		selectableBackground = itemView.findViewById(R.id.selectable_widget_background);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);

		boolean disableAnimation = app.getSettings().DO_NOT_USE_ANIMATIONS.getModeValue(selectedAppMode);
		if (disableAnimation) {
			((ViewGroup) itemView).setLayoutTransition(null);
			((ViewGroup) itemView.findViewById(R.id.animated_layout)).setLayoutTransition(null);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	public void bind(@NonNull MapActivity mapActivity, @NonNull ApplicationMode selectedAppMode, @NonNull WidgetsAdapterListener listener,
	                 @NonNull ItemTouchHelper itemTouchHelper, @NonNull WidgetItem widgetItem,
	                 @NonNull WidgetIconsHelper iconsHelper, int position, boolean nightMode) {
		OsmandApplication app = mapActivity.getApp();

		title.setText(widgetItem.mapWidgetInfo.getTitle(app));

		iconsHelper.updateWidgetIcon(icon, widgetItem.mapWidgetInfo);
		moveIcon.setVisibility(View.VISIBLE);

		deleteButton.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete));
		updatePosition(listener, position, widgetItem);
		updateDivider(widgetItem);
		updateEditMode(app, listener, itemTouchHelper, selectedAppMode, widgetItem, nightMode);
	}

	public void updatePosition(@NonNull WidgetsAdapterListener listener, int position, @NonNull WidgetItem widgetItem) {
		deleteButton.setOnClickListener(v -> {
			listener.onWidgetDeleted(position, widgetItem.mapWidgetInfo);
		});
	}

	public void updateDivider(@NonNull WidgetItem widgetItem) {
		bottomDivider.setVisibility(widgetItem.showBottomDivider ? View.VISIBLE : View.INVISIBLE);
		bottomShadow.setVisibility(widgetItem.showBottomShadow ? View.VISIBLE : View.GONE);
	}

	@SuppressLint("ClickableViewAccessibility")
	public void updateEditMode(@NonNull OsmandApplication app, @NonNull WidgetsAdapterListener listener,
	                           @NonNull ItemTouchHelper itemTouchHelper, @NonNull ApplicationMode selectedAppMode,
	                           @NonNull WidgetItem widgetItem, boolean nightMode) {
		boolean editMode = listener.isEditMode();

		deleteButton.setVisibility(editMode ? View.VISIBLE : View.GONE);

		if (editMode) {

			moveIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_item_move, ColorUtilities.getDefaultIconColor(app, nightMode)));
			moveIcon.setOnTouchListener((v, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					listener.onMoveStarted();
					itemTouchHelper.startDrag(this);
				}
				return false;
			});
			itemView.setOnClickListener(null);
		} else {

			moveIcon.setOnTouchListener(null);
			int iconId = nightMode ? R.drawable.ic_action_info_dark : R.drawable.ic_action_info;
			moveIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconId, ColorUtilities.getDefaultIconColor(app, nightMode)));
			itemView.setOnClickListener(view -> {
				listener.onWidgetClick(widgetItem.mapWidgetInfo);
			});
		}
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable drawable = editMode ? null : UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(selectableBackground, drawable);
	}
}