package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.PageItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetsAdapterListener;
import net.osmand.plus.widgets.TextViewEx;

public class PageViewHolder extends RecyclerView.ViewHolder {

	private final TextViewEx page;
	private final ImageButton deleteButton;
	private final AppCompatImageView moveButton;

	public PageViewHolder(@NonNull View itemView) {
		super(itemView);
		page = itemView.findViewById(R.id.page);
		deleteButton = itemView.findViewById(R.id.delete_page_button);
		moveButton = itemView.findViewById(R.id.move_button);
	}

	@SuppressLint("ClickableViewAccessibility")
	public void bind(@NonNull MapActivity mapActivity, @NonNull WidgetsAdapterListener listener,
	                 @NonNull ItemTouchHelper itemTouchHelper, @NonNull WidgetsListAdapter adapter,
	                 int position, @NonNull PageItem pageItem, boolean isVerticalPanel, boolean nightMode) {
		OsmandApplication app = mapActivity.getMyApplication();

		String text = app.getString(isVerticalPanel ? R.string.row_number : R.string.page_number, String.valueOf(pageItem.pageNumber));
		page.setText(text);

		OnClickListener deletePageListener;
		Drawable deleteIcon;

		int previousRowPosition = adapter.getPreviousRowPosition(position);
		boolean rowHasComplexWidget = adapter.rowHasComplexWidget(position, null);
		boolean previousRowHasComplexWidget = adapter.rowHasComplexWidget(previousRowPosition, null);
		boolean isRowEmpty = adapter.getRowWidgetIds(position, null).isEmpty();
		boolean isPreviousRowEmpty = adapter.getRowWidgetIds(previousRowPosition, null).isEmpty();

		boolean hideMoveIcon;
		if (adapter.isFirstPage(position)) {
			deletePageListener = null;
			deleteIcon = getDeleteIcon(app, nightMode, true);
			hideMoveIcon = true;
		} else if (isVerticalPanel) {
			if (rowHasComplexWidget && !isPreviousRowEmpty) {
				deletePageListener = v -> app.showToastMessage(app.getString(R.string.remove_widget_first));
				deleteIcon = getDeleteIcon(app, nightMode, true);
				hideMoveIcon = true;
			} else if (previousRowHasComplexWidget && !isRowEmpty) {
				deletePageListener = v -> app.showToastMessage(app.getString(R.string.previous_row_has_complex_widget));
				deleteIcon = getDeleteIcon(app, nightMode, true);
				hideMoveIcon = true;
			} else {
				deletePageListener = v -> listener.onPageDeleted(position, pageItem);
				deleteIcon = getDeleteIcon(app, nightMode, false);
				hideMoveIcon = false;
			}
		} else {
			deletePageListener = v -> listener.onPageDeleted(position, pageItem);
			deleteIcon = getDeleteIcon(app, nightMode, false);
			hideMoveIcon = false;
		}

		boolean editMode = listener.isEditMode();

		deleteButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
		deleteButton.setImageDrawable(deleteIcon);
		deleteButton.setOnClickListener(deletePageListener);

		moveButton.setVisibility(editMode && !hideMoveIcon ? View.VISIBLE : View.GONE);
		moveButton.setOnTouchListener((v, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onMoveStarted();
				itemTouchHelper.startDrag(this);
			}
			return false;
		});
	}

	@NonNull
	private Drawable getDeleteIcon(@NonNull OsmandApplication app, boolean nightMode, boolean disabled) {
		return disabled
				? app.getUIUtilities().getIcon(R.drawable.ic_action_remove, nightMode)
				: app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete);
	}

}