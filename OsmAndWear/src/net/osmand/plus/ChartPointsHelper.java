package net.osmand.plus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.LayerDrawable;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.utils.AndroidUtils;

import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

public class ChartPointsHelper {

	private final LayerDrawable highlightedPointIcon;

	private final Paint innerCirclePaint;
	private final Paint outerCirclePaint;

	public ChartPointsHelper(@NonNull Context context) {
		this.highlightedPointIcon = (LayerDrawable) AppCompatResources.getDrawable(context,
				R.drawable.map_location_default);
		this.innerCirclePaint = createPaint(0);
		this.outerCirclePaint = createPaint(Color.WHITE);
		outerCirclePaint.setAlpha(204);
	}

	private Paint createPaint(@ColorInt int color) {
		Paint paint = new Paint();
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setAntiAlias(true);
		paint.setColor(color);
		return paint;
	}

	public void drawHighlightedPoint(@NonNull LatLon highlightedPoint,
	                                 @NonNull Canvas canvas,
	                                 @NonNull RotatedTileBox tileBox) {
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		if (highlightedPoint.getLongitude() >= latLonBounds.left
				&& highlightedPoint.getLatitude() <= latLonBounds.top
				&& highlightedPoint.getLongitude() <= latLonBounds.right
				&& highlightedPoint.getLatitude() >= latLonBounds.bottom) {
			int x = (int) tileBox.getPixXFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
			int intrinsicW2 = highlightedPointIcon.getIntrinsicWidth() / 2;
			int intrinsicH2 = highlightedPointIcon.getIntrinsicHeight() / 2;
			highlightedPointIcon.setBounds(x - intrinsicW2, y - intrinsicH2, x + intrinsicW2,
					y + intrinsicH2);
			highlightedPointIcon.draw(canvas);
		}
	}

	public void drawXAxisPoints(@NonNull List<LatLon> xAxisPoints, @ColorInt int xAxisPointColor,
	                            @NonNull Canvas canvas, @NonNull RotatedTileBox tileBox) {
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		float r = 3 * tileBox.getDensity();
		float density = (float) Math.ceil(tileBox.getDensity());
		float outerRadius = r + 2 * density;
		float innerRadius = r + density;
		QuadRect prevPointRect = null;

		innerCirclePaint.setColor(xAxisPointColor);
		innerCirclePaint.setAlpha(255);

		for (LatLon axisPoint : xAxisPoints) {
			if (axisPoint != null
					&& axisPoint.getLatitude() >= latLonBounds.bottom
					&& axisPoint.getLatitude() <= latLonBounds.top
					&& axisPoint.getLongitude() >= latLonBounds.left
					&& axisPoint.getLongitude() <= latLonBounds.right) {
				float x = tileBox.getPixXFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				float y = tileBox.getPixYFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				QuadRect pointRect = new QuadRect(x - outerRadius, y - outerRadius,
						x + outerRadius, y + outerRadius);
				if (prevPointRect == null || !QuadRect.intersects(prevPointRect, pointRect)) {
					canvas.drawCircle(x, y, outerRadius, outerCirclePaint);
					canvas.drawCircle(x, y, innerRadius, innerCirclePaint);
					prevPointRect = pointRect;
				}
			}
		}
	}

	public Bitmap createHighlightedPointBitmap() {
		return AndroidUtils.drawableToBitmap(highlightedPointIcon);
	}

	public Bitmap createXAxisPointBitmap(@ColorInt int pointColor, float density) {
		float r = 3 * density;
		density = (float) Math.ceil(density);
		float outerRadius = r + 2 * density;
		float innerRadius = r + density;
		int margin = 2;
		innerCirclePaint.setColor(pointColor);
		innerCirclePaint.setAlpha(255);
		Bitmap bitmap = Bitmap.createBitmap((int) (outerRadius * 2 + margin * 2), (int) (outerRadius * 2 + margin * 2), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		float x = outerRadius + margin;
		float y = outerRadius + margin;
		canvas.drawCircle(x, y, outerRadius, outerCirclePaint);
		canvas.drawCircle(x, y, innerRadius, innerCirclePaint);
		return bitmap;
	}
}