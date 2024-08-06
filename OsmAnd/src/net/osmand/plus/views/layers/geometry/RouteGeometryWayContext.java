package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.shared.routing.ColoringType;

public class RouteGeometryWayContext extends MultiColoringGeometryWayContext {

	public RouteGeometryWayContext(@NonNull Context ctx, float density) {
		super(ctx, density);
	}

	@NonNull
	@Override
	public Paint getCustomPaint() {
		return getAttrs().customColorPaint;
	}

	@NonNull
	@Override
	protected ColoringType getDefaultColoringType() {
		return ColoringType.DEFAULT;
	}

	@NonNull
	@Override
	protected String getDefaultGradientPalette() {
		return PaletteGradientColor.DEFAULT_NAME;
	}

	@Override
	protected int getArrowBitmapResId() {
		return R.drawable.map_route_direction_arrow;
	}
}