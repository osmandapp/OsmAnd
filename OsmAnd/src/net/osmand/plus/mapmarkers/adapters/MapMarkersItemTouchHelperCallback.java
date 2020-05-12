package net.osmand.plus.mapmarkers.adapters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MapMarkersItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private final ItemTouchHelperAdapter adapter;
	private MapActivity mapActivity;
	private boolean swipeEnabled = true;

	private Paint backgroundPaint = new Paint();
	private Paint iconPaint = new Paint();
	private Paint textPaint = new Paint();

	private float marginSides;
	private Bitmap deleteBitmap;
	private Bitmap historyBitmap;
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
		deleteBitmap = AndroidUtils.bitmapFromDrawableRes(mapActivity, R.drawable.ic_action_delete_dark);
		historyBitmap = AndroidUtils.bitmapFromDrawableRes(mapActivity, R.drawable.ic_action_history);
		night = !mapActivity.getMyApplication().getSettings().isLightContent();

		backgroundPaint.setColor(ContextCompat.getColor(mapActivity, night ? R.color.divider_color_dark : R.color.divider_color_light));
		backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		backgroundPaint.setAntiAlias(true);
		iconPaint.setAntiAlias(true);
		iconPaint.setFilterBitmap(true);
		iconPaint.setDither(true);
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
				colorIcon = night ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
				colorText = night ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
			}
			if (colorIcon != 0) {
				iconPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(mapActivity, colorIcon), PorterDuff.Mode.SRC_IN));
			}
			textPaint.setColor(ContextCompat.getColor(mapActivity, colorText));
			float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
			c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
			float iconMarginTop = ((float) itemView.getHeight() - (float) historyBitmap.getHeight()) / 2;
			c.drawBitmap(historyBitmap, itemView.getLeft() + marginSides, itemView.getTop() + iconMarginTop, iconPaint);
			c.drawText(moveToHistoryStr, itemView.getLeft() + 2 * marginSides + historyBitmap.getWidth(),
					itemView.getTop() + textMarginTop + textHeight, textPaint);

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
