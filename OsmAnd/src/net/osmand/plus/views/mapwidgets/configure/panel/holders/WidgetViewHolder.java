package net.osmand.plus.views.mapwidgets.configure.panel.holders;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

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
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetsAdapterListener;
import net.osmand.plus.widgets.TextViewEx;

public class WidgetViewHolder extends RecyclerView.ViewHolder {
	private final TextViewEx title;
	private final ImageView icon;
	private final ImageButton deleteButton;
	private final AppCompatImageView moveIcon;
	private final View bottomDivider;
	private final View selectableBackground;

	public WidgetViewHolder(@NonNull View itemView) {
		super(itemView);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		deleteButton = itemView.findViewById(R.id.delete_widget_button);
		moveIcon = itemView.findViewById(R.id.move_icon);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);
		selectableBackground = itemView.findViewById(R.id.selectable_widget_background);
	}

	@SuppressLint("ClickableViewAccessibility")
	public void bind(@NonNull MapActivity mapActivity, @NonNull ApplicationMode selectedAppMode, @NonNull WidgetsAdapterListener listener,
	                 @NonNull ItemTouchHelper itemTouchHelper, @NonNull MapWidgetInfo widgetInfo,
	                 @NonNull WidgetIconsHelper iconsHelper, int position, boolean nightMode, boolean showDivider) {
		OsmandApplication app = mapActivity.getMyApplication();
		boolean editMode = listener.isEditMode();
		int color = selectedAppMode.getProfileColor(nightMode);
		Drawable drawable = editMode ? null : UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(selectableBackground, drawable);

		title.setText(widgetInfo.getTitle(app));

		int startMargin;
		if (editMode) {
			startMargin = app.getResources().getDimensionPixelSize(R.dimen.fab_margin_bottom_big);
		} else {
			startMargin = app.getResources().getDimensionPixelSize(R.dimen.list_content_padding);
		}
		LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) title.getLayoutParams();
		titleParams.setMargins(startMargin, titleParams.topMargin, titleParams.rightMargin, titleParams.bottomMargin);
		title.setLayoutParams(titleParams);

		iconsHelper.updateWidgetIcon(icon, widgetInfo);
		moveIcon.setVisibility(View.VISIBLE);

		deleteButton.setVisibility(editMode ? View.VISIBLE : View.GONE);
		deleteButton.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete));
		deleteButton.setOnClickListener(v -> {
			listener.onWidgetDeleted(position, widgetInfo);
		});

		itemView.setOnClickListener(view -> {
			if (!editMode) {
				listener.onWidgetClick(widgetInfo);
			}
		});

		if (editMode) {
			moveIcon.setImageDrawable(app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_item_move, ColorUtilities.getDefaultIconColor(app, nightMode)));
		} else {
			int iconId = nightMode ? R.drawable.ic_action_info_dark : R.drawable.ic_action_info;
			moveIcon.setImageDrawable(app.getUIUtilities().getIcon(iconId));
		}
		moveIcon.setOnTouchListener((v, event) -> {
			if (editMode && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onMoveStarted();
				itemTouchHelper.startDrag(this);
			}
			return false;
		});

		bottomDivider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
	}
}