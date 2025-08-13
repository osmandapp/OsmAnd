package net.osmand.plus.views.layers;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.BackgroundType;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ShiftedBitmap;
import net.osmand.plus.dialogs.selectlocation.SelectLocationController;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class SelectLocationLayer extends OsmandMapLayer {

	private final Paint bitmapPaint;
	private final Paint mTextPaint;
	private Bitmap defaultIconDay;
	private Bitmap defaultIconNight;
	private float textScale = 1f;
	private SelectLocationController<?> selectLocationController;

	public SelectLocationLayer(@NonNull Context context) {
		super(context);
		bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bitmapPaint.setFilterBitmap(true);
		bitmapPaint.setDither(true);

		mTextPaint = new Paint();
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setAntiAlias(true);
		updateTextSize();
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		defaultIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		defaultIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		OsmandApplication app = getApplication();
		selectLocationController = SelectLocationController.getExistedInstance(app);

		if (selectLocationController != null) {
			float textScale = getTextScale();
			if (this.textScale != textScale) {
				this.textScale = textScale;
				updateTextSize();
			}
			float marginX, marginY = 0, iconWidth;
			Object iconObject = selectLocationController.getCenterPointIcon();
			if (iconObject instanceof PointImageDrawable drawable) {
				drawTargetDrawable(canvas, tileBox, drawable);
				iconWidth = drawable.getIntrinsicWidth();
			} else if (iconObject instanceof ShiftedBitmap icon) {
				Bitmap bitmap = icon.getBitmap();
				marginX = icon.getMarginX();
				marginY = icon.getMarginY();
				iconWidth = bitmap.getWidth();
				drawTargetBitmap(canvas, tileBox, bitmap, marginX, marginY, icon.getScale());
			} else {
				Bitmap centerIcon = settings.isNightMode() ? defaultIconNight : defaultIconDay;
				drawTargetBitmap(canvas, tileBox, centerIcon);
				iconWidth = centerIcon.getWidth();
			}
			String label = selectLocationController.getCenterPointLabel();
			if (label != null) {
				marginX = iconWidth / 3f;
				drawText(canvas, tileBox, label, marginX, marginY);
			}
		}
	}

	private void updateTextSize() {
		mTextPaint.setTextSize(18f * Resources.getSystem().getDisplayMetrics().scaledDensity
				* getApplication().getOsmandMap().getCarDensityScaleCoef());
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	private void drawTargetDrawable(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                                @NonNull PointImageDrawable drawable) {
		float targetX = tileBox.getCenterPixelX();
		float targetY = tileBox.getCenterPixelY();
		BackgroundType backgroundType = drawable.getBackgroundType();
		int offsetY = backgroundType.getOffsetY(view.getContext(), getTextScale());
		drawable.drawPoint(canvas, targetX, targetY - offsetY, getTextScale(), false);
	}

	private void drawTargetBitmap(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @NonNull Bitmap icon) {
		drawTargetBitmap(canvas, tileBox, icon, icon.getWidth() / 2f, icon.getHeight() / 2f, 1.0f);
	}

	private void drawTargetBitmap(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                              @NonNull Bitmap bitmap, float marginX, float marginY, @Nullable Float scale) {
		float x = tileBox.getCenterPixelX();
		float y = tileBox.getCenterPixelY();
		if (scale == null) {
			canvas.drawBitmap(bitmap, x - marginX, y - marginY, bitmapPaint);
			return;
		}
		Rect rect = getIconDestinationRect(x - marginX, y - marginY, bitmap.getWidth(), bitmap.getHeight(), scale);
		canvas.drawBitmap(bitmap, null, rect, bitmapPaint);
	}

	private void drawText(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                      @NonNull String text, float marginX, float marginY) {
		float x = tileBox.getCenterPixelX();
		float y = tileBox.getCenterPixelY();
		canvas.drawText(text, x + marginX, y - 3 * marginY / 5f, mTextPaint);
	}

	@Override
	public boolean onSingleTap(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		// Handle single tap if we're in Location Selection mode
		// to prevent other layers from processing it
		return selectLocationController != null;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		// Handle long press f we're in Location Selection mode
		// to prevent other layers from processing it
		return selectLocationController != null;
	}
}
