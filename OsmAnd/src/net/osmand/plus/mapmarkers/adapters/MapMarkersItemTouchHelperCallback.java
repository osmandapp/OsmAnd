package net.osmand.plus.mapmarkers.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class MapMarkersItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private final ItemTouchHelperAdapter adapter;
	private MapActivity mapActivity;

	private Paint backgroundPaint = new Paint();
	private Paint iconPaint = new Paint();
	private Paint textPaint = new Paint();

	private float marginSides;
	private Bitmap deleteBitmap;
	private Bitmap resetBitmap;
	private boolean iconHidden;
	private boolean night;

	private String delStr;
	private String moveToHistoryStr;

	private int moveToHistoryStrWidth;
	private int textHeight;

	public MapMarkersItemTouchHelperCallback(MapActivity mapActivity, ItemTouchHelperAdapter adapter) {
		this.mapActivity = mapActivity;
		this.adapter = adapter;
		marginSides = mapActivity.getResources().getDimension(R.dimen.list_content_padding);
		deleteBitmap = BitmapFactory.decodeResource(mapActivity.getResources(), R.drawable.ic_action_delete_dark);
		resetBitmap = BitmapFactory.decodeResource(mapActivity.getResources(), R.drawable.ic_action_reset_to_default_dark);
		night = !mapActivity.getMyApplication().getSettings().isLightContent();

		backgroundPaint.setColor(ContextCompat.getColor(mapActivity, night ? R.color.dashboard_divider_dark : R.color.dashboard_divider_light));
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

		textPaint.getTextBounds(moveToHistoryStr, 0, moveToHistoryStr.length(), bounds);
		moveToHistoryStrWidth = bounds.width();
		textHeight = bounds.height();
	}

	@Override
	public boolean isLongPressDragEnabled() {
		return false;
	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		final int moveFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
		return makeMovementFlags(dragFlags, moveFlags);
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
		return adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
	}

	@Override
	public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
		if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder instanceof MapMarkerItemViewHolder) {
			if (!iconHidden && isCurrentlyActive) {
				((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.GONE);
				iconHidden = true;
			}
			View itemView = viewHolder.itemView;
			int colorIcon;
			int colorText;
			if (Math.abs(dX) > itemView.getWidth() / 2) {
				colorIcon = R.color.map_widget_blue;
				colorText = R.color.map_widget_blue;
			} else {
				colorIcon = night ? 0 : R.color.icon_color;
				colorText = R.color.dashboard_subheader_text_light;
			}
			if (colorIcon != 0) {
				iconPaint.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(mapActivity, colorIcon), PorterDuff.Mode.SRC_IN));
			}
			textPaint.setColor(ContextCompat.getColor(mapActivity, colorText));
			float textMarginTop = ((float) itemView.getHeight() - (float) textHeight) / 2;
			if (dX > 0) {
				c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), backgroundPaint);
				float iconMarginTop = ((float) itemView.getHeight() - (float) deleteBitmap.getHeight()) / 2;
				c.drawBitmap(deleteBitmap, itemView.getLeft() + marginSides, itemView.getTop() + iconMarginTop, iconPaint);
				c.drawText(delStr, itemView.getLeft() + 2 * marginSides + deleteBitmap.getWidth(),
						itemView.getTop() + textMarginTop + textHeight, textPaint);
			} else {
				c.drawRect(itemView.getRight() + dX, itemView.getTop(), itemView.getRight(), itemView.getBottom(), backgroundPaint);
				float iconMarginTop = ((float) itemView.getHeight() - (float) resetBitmap.getHeight()) / 2;
				c.drawBitmap(resetBitmap, itemView.getRight() - resetBitmap.getWidth() - marginSides, itemView.getTop() + iconMarginTop, iconPaint);
				c.drawText(moveToHistoryStr, itemView.getRight() - resetBitmap.getWidth() - 2 * marginSides - moveToHistoryStrWidth,
						itemView.getTop() + textMarginTop + textHeight, textPaint);
			}
		}
		super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
		adapter.onItemSwiped(viewHolder, i);
	}

	@Override
	public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);
		((MapMarkerItemViewHolder) viewHolder).optionsBtn.setVisibility(View.VISIBLE);
		iconHidden = false;
		adapter.onItemDismiss(viewHolder);
	}

	interface ItemTouchHelperAdapter {

		boolean onItemMove(int from, int to);

		void onItemSwiped(RecyclerView.ViewHolder holder, int direction);

		void onItemDismiss(RecyclerView.ViewHolder holder);
	}
}
