package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.R;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.utils.AndroidUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public abstract class MultiColoringGeometryWayContext extends GeometryWayContext {

	private static final int SHADOW_COLOR = 0x80000000;

	private final Paint borderPaint;
	private final Paint circlePaint;

	private final Bitmap specialArrowBitmap;

	public MultiColoringGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
		borderPaint = createBorderPaint();
		circlePaint = createCirclePaint();
		specialArrowBitmap = AndroidUtils.drawableToBitmap(ContextCompat.getDrawable(ctx, R.drawable.mm_special_arrow_up));
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

	@NonNull
	public Paint getBorderPaint() {
		return borderPaint;
	}

	public float getBorderOutlineWidth() {
		return (hasMapRenderer() ? 1.5f : 2f) * getDensity();
	}

	public void updateBorderWidth(float routeLineWidth) {
		borderPaint.setStrokeWidth(routeLineWidth + getBorderOutlineWidth());
	}

	public Bitmap getSpecialArrowBitmap() {
		return specialArrowBitmap;
	}

	public Paint getCirclePaint() {
		return circlePaint;
	}
}