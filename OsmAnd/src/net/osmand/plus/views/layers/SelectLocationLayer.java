package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import net.osmand.data.BackgroundType;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.containers.ShiftedBitmap;
import net.osmand.plus.dialogs.SelectLocationController;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.plus.views.layers.base.OsmandMapLayer;

public class SelectLocationLayer extends OsmandMapLayer {

	private final Paint bitmapPaint;
	private Bitmap defaultIconDay;
	private Bitmap defaultIconNight;

	public SelectLocationLayer(@NonNull Context context) {
		super(context);
		bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		bitmapPaint.setFilterBitmap(true);
		bitmapPaint.setDither(true);
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
		SelectLocationController controller = SelectLocationController.getExistedInstance(app);

		if (controller != null) {
			Object iconObject = controller.getCenterPointIcon();
			if (iconObject instanceof PointImageDrawable icon) {
				float x = tileBox.getCenterPixelX();
				float y = tileBox.getCenterPixelY();
				BackgroundType backgroundType = icon.getBackgroundType();
				int offsetY = backgroundType.getOffsetY(view.getContext(), getTextScale());

				canvas.rotate(-tileBox.getRotate(), x, y);
				icon.drawPoint(canvas, x, y - offsetY, getTextScale(), false);
				canvas.rotate(tileBox.getRotate(), x, y);
			} else if (iconObject instanceof ShiftedBitmap icon) {
				drawCenterIcon(canvas, tileBox, icon.getBitmap(), icon.getMarginX(), icon.getMarginY());
			} else {
				Bitmap centerIcon = settings.isNightMode() ? defaultIconNight : defaultIconDay;
				drawCenterIcon(canvas, tileBox, centerIcon);
			}
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	private void drawCenterIcon(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @NonNull Bitmap icon) {
		drawCenterIcon(canvas, tileBox, icon, icon.getWidth() / 2f, icon.getHeight() / 2f);
	}

	private void drawCenterIcon(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox,
	                            @NonNull Bitmap bitmap, float marginX, float marginY) {
		float centerX = tileBox.getCenterPixelX();
		float centerY = tileBox.getCenterPixelY();

		canvas.save();
		canvas.rotate(-tileBox.getRotate(), centerX, centerY);
		canvas.drawBitmap(bitmap, centerX - marginX, centerY - marginY, bitmapPaint);
		canvas.restore();
	}
}
