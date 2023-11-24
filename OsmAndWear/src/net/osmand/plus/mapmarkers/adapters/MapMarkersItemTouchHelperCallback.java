package net.osmand.plus.mapmarkers.adapters;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MapMarkersItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private final ItemTouchHelperAdapter adapter;
	private MapActivity mapActivity;
	private boolean swipeEnabled = true;

	private final Paint backgroundPaint = new Paint();
	private final Paint textPaint = new Paint();

	private float marginSides;
	private boolean iconHidden;
	private boolean night;

	private String delStr;
	private String moveToHistoryStr;

	private int delStrWidth;
	private int textHeight;

	public MapMarkersItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
		this.adapter = adapter;
		swipeEnabled = false;
	}

	public MapMarkersItemTouchHelperCallback(MapActivity mapActivity, ItemTouchHelperAdapter adapter) {
		this.mapActivity = mapActivity;
		this.adapter = adapter;
		marginSides = mapActivity.getResources().getDimension(R.dimen.list_content_padding);
		night = !mapActivity.getMyApplication().getSettings().isLightContent();

		backgroundPaint.setColor(ColorUtilities.getDividerColor(mapActivity, night));
		backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		backgroundPaint.setAntiAlias(true);
		textPaint.setTextSize(mapActivity.getResources().getDimension(R.dimen.default_desc_text_size));
		textPaint.setFakeBoldText(true);
		textPaint.setAntiAlias(true);

		delStr = mapActivity.getString(R.string.shared_string_delete).toUpperCase();
		moveToHistoryStr = mapActivity.getString(R.string.move_to_history).toUpperCase();
		Rect bounds = new Rect();

		textPaint.getTextBounds(delStr, 0, delStr.length(), bounds);
		delStrWidth = bounds.width();
		textHeight = bounds.height();
	}

	@Override
	public boolean isLongPressDragEnabled() {
		return false;
	}

	@Override
	public boolean isItemViewSwipeEnabled() {
		return swipeEnabled;
	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		final int moveFlags = ItemTouchHelper.RIGHT;
		return makeMovementFlags(dragFlags, swipeEnabled ? moveFlags : 0);
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
		int from = source.getAdapterPosition();
		int to = target.getAdapterPosition();
		if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
			return false;
		}
		return adapter.onItemMove(from, to);
	}

	@Override
	public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
		if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder instanceof MapMarkerItemViewHolder) {
			OsmandApplication app = mapActivity.getMyApplication();
			if (!iconHidden && isCurrentlyActive) {
				((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.GONE);
				iconHidden = true;
				adapter.onSwipeStarted();
			}
			View itemView = viewHolder.itemView;
			int colorIcon;
			int colorText;
			if (Math.abs(dX) > itemView.getWidth() / 2) {
				colorIcon = R.color.map_widget_blue;
				colorText = R.color.map_widget_blue;
			} else {
				colorIcon = ColorUtilities.getDefaultIconColorId(night);
				colorText = ColorUtilities.getSecondaryTextColorId(night);
			}
			textPaint.setColor(ContextCompat.getColor(mapActivity, colorText));
			Drawable icon = app.getUIUtilities().getIcon(R.drawable.ic_action_history, colorIcon);
			int iconWidth = icon.getIntrinsicWidth();
			int iconHeight = icon.getIntrinsicHeight();
			float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
			float iconMarginTop = ((float) itemView.getHeight() - (float) iconHeight) / 2;
			int iconTopY = itemView.getTop() + (int) iconMarginTop;
			int iconLeftX = itemView.getLeft() + (int) marginSides;
			c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
			c.drawText(moveToHistoryStr, itemView.getLeft() + 2 * marginSides + iconWidth,
					itemView.getTop() + textMarginTop + textHeight, textPaint);
			icon.setBounds(iconLeftX, iconTopY, iconLeftX + iconWidth, iconTopY + iconHeight);
			icon.draw(c);
		}
		super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
		adapter.onItemSwiped(viewHolder);
	}

	@Override
	public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);
		if (iconHidden) {
			((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.VISIBLE);
			iconHidden = false;
		}
		adapter.onItemDismiss(viewHolder);
	}

	interface ItemTouchHelperAdapter {

		void onSwipeStarted();

		boolean onItemMove(int from, int to);

		void onItemSwiped(RecyclerView.ViewHolder holder);

		void onItemDismiss(RecyclerView.ViewHolder holder);
	}
}
