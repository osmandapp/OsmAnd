package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.shared.routing.ColoringType;

public class GpxGeometryWayContext extends MultiColoringGeometryWayContext {

	private final Paint strokePaint;

	public GpxGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
		Paint paint = getPaintIcon();
		paint.setStrokeCap(Paint.Cap.ROUND);
		strokePaint = createStrokePaint();
	}

	private Paint createStrokePaint() {
		Paint strokePaint = new Paint();
		strokePaint.setDither(true);
		strokePaint.setAntiAlias(true);
		strokePaint.setStyle(Style.STROKE);
		strokePaint.setStrokeCap(Cap.ROUND);
		strokePaint.setStrokeJoin(Join.ROUND);
		return strokePaint;
	}

	@NonNull
	@Override
	public Paint getCustomPaint() {
		return strokePaint;
	}

	@NonNull
	@Override
	protected ColoringType getDefaultColoringType() {
		return ColoringType.TRACK_SOLID;
	}

	@NonNull
	@Override
	protected String getDefaultGradientPalette() {
		return PaletteGradientColor.DEFAULT_NAME;
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.ic_action_direction_arrow;
	}
}