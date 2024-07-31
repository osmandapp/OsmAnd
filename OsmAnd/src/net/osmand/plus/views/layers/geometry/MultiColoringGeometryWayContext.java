package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.routing.ColoringType;

public abstract class MultiColoringGeometryWayContext extends GeometryWayContext {

	private static final int SHADOW_COLOR = 0x80000000;

	private final Paint borderPaint;
	private final Paint circlePaint;

	private final Bitmap specialArrowBitmap;

	public MultiColoringGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
		borderPaint = createBorderPaint();
		circlePaint = createCirclePaint();
		float scale = getApp().getOsmandMap().getCarDensityScaleCoef();
		Bitmap specialArrowBitmap = AndroidUtils.drawableToBitmap(ContextCompat.getDrawable(ctx, R.drawable.mm_special_arrow_up));
		if (specialArrowBitmap != null && scale != 1f && scale > 0) {
			specialArrowBitmap = AndroidUtils.scaleBitmap(specialArrowBitmap,
					(int) (specialArrowBitmap.getWidth() * scale), (int) (specialArrowBitmap.getHeight() * scale), false);
		}
		this.specialArrowBitmap = specialArrowBitmap;
	}

	@NonNull
	private Paint createBorderPaint() {
		Paint borderPaint = new Paint();
		borderPaint.setColor(SHADOW_COLOR);
		borderPaint.setAntiAlias(true);
		borderPaint.setStrokeCap(Paint.Cap.ROUND);
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeJoin(Paint.Join.ROUND);
		return borderPaint;
	}

	@NonNull
	private Paint createCirclePaint() {
		Paint circlePaint = new Paint();
		circlePaint.setDither(true);
		circlePaint.setAntiAlias(true);
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(0x33000000);
		return circlePaint;
	}

	@NonNull
	public abstract Paint getCustomPaint();

	@NonNull
	protected abstract ColoringType getDefaultColoringType();
	protected abstract String getDefaultGradientPalette();

	@NonNull
	public Paint getBorderPaint() {
		return borderPaint;
	}

	public void updateBorderWidth(float routeLineWidth) {
		float enlargement = getDensity() * 2f; // One dp on each side of line
		borderPaint.setStrokeWidth(routeLineWidth + enlargement);
	}

	public Bitmap getSpecialArrowBitmap() {
		return specialArrowBitmap;
	}

	public Paint getCirclePaint() {
		return circlePaint;
	}
}