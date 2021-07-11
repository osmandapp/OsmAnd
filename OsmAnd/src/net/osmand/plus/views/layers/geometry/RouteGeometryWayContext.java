package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Paint;

import net.osmand.plus.R;

import androidx.annotation.NonNull;

public class RouteGeometryWayContext extends GeometryWayContext {

	private static final int SHADOW_COLOR = 0x80000000;

	private final Paint borderPaint;

	public RouteGeometryWayContext(Context ctx, float density) {
		super(ctx, density);
		borderPaint = createBorderPaint();
	}

	private Paint createBorderPaint() {
		Paint borderPaint = new Paint();
		borderPaint.setColor(SHADOW_COLOR);
		borderPaint.setAntiAlias(true);
		borderPaint.setStrokeCap(Paint.Cap.ROUND);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeJoin(Paint.Join.ROUND);
		borderPaint.setStrokeWidth(0);
		return borderPaint;
	}

	@NonNull
	public Paint getBorderPaint() {
		return borderPaint;
	}

	public void updateBorderWidth(float routeLineWidth) {
		borderPaint.setStrokeWidth(routeLineWidth + 2 * getDensity());
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.map_route_direction_arrow;
	}
}