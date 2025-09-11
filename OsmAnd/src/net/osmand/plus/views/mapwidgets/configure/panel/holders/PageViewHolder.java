package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.PageItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetsAdapterListener;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

public class PageViewHolder extends RecyclerView.ViewHolder {

	private final TextViewEx page;
	private final ImageButton deleteButton;
	private final AppCompatImageView moveButton;

	public PageViewHolder(@NonNull OsmandApplication app, @NonNull ApplicationMode selectedAppMode, @NonNull View itemView) {
		super(itemView);
		page = itemView.findViewById(R.id.page);
		deleteButton = itemView.findViewById(R.id.delete_page_button);
		moveButton = itemView.findViewById(R.id.move_button);

		boolean disableAnimation = app.getSettings().DO_NOT_USE_ANIMATIONS.getModeValue(selectedAppMode);
		if (disableAnimation) {
			((ViewGroup) itemView).setLayoutTransition(null);
			((ViewGroup) itemView.findViewById(R.id.animated_layout)).setLayoutTransition(null);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	public void bind(@NonNull MapActivity mapActivity, @NonNull WidgetsAdapterListener listener,
	                 @NonNull ItemTouchHelper itemTouchHelper, @NonNull WidgetsListAdapter adapter,
	                 int position, @NonNull PageItem pageItem, boolean isVerticalPanel, boolean nightMode) {
		OsmandApplication app = mapActivity.getApp();

		updateTitle(app, pageItem, isVerticalPanel);
		updateButtons(app, listener, pageItem, position, nightMode);
		updateEditMode(listener, itemTouchHelper);
	}

	public void updateButtons(@NonNull OsmandApplication app, @NonNull WidgetsAdapterListener listener,
	                          @NonNull PageItem pageItem, int position, boolean nightMode) {
		OnClickListener deletePageListener = null;
		Drawable deleteIcon;

		boolean editMode = listener.isEditMode();
		boolean isMovable = pageItem.movable;

		if (isMovable) {
			deleteIcon = getDeleteIcon(app, nightMode, false);
			deletePageListener = v -> listener.onPageDeleted(position, pageItem);
		} else {
			deleteIcon = getDeleteIcon(app, nightMode, true);
			if (!Algorithms.isEmpty(pageItem.deleteMessage)) {
				deletePageListener = v -> app.showToastMessage(pageItem.deleteMessage);
			}
		}

		deleteButton.setImageDrawable(deleteIcon);
		deleteButton.setOnClickListener(deletePageListener);

		moveButton.setVisibility(editMode && isMovable ? View.VISIBLE : View.GONE);
	}

	@SuppressLint("ClickableViewAccessibility")
	public void updateEditMode(@NonNull WidgetsAdapterListener listener, @NonNull ItemTouchHelper itemTouchHelper) {
		boolean editMode = listener.isEditMode();

		deleteButton.setVisibility(editMode ? View.VISIBLE : View.GONE);

		if (editMode) {
			moveButton.setOnTouchListener((v, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					listener.onMoveStarted();
					itemTouchHelper.startDrag(this);
				}
				return false;
			});
		} else {
			moveButton.setVisibility(View.GONE);
			moveButton.setOnTouchListener(null);
		}
	}

	public void updateTitle(@NonNull OsmandApplication app, @NonNull PageItem pageItem, boolean isVerticalPanel) {
		String text = app.getString(isVerticalPanel ? R.string.row_number : R.string.page_number, String.valueOf(pageItem.pageNumber));
		page.setText(text);
	}

	@NonNull
	private Drawable getDeleteIcon(@NonNull OsmandApplication app, boolean nightMode, boolean disabled) {
		return disabled
				? app.getUIUtilities().getIcon(R.drawable.ic_action_remove, nightMode)
				: app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete);
	}

}