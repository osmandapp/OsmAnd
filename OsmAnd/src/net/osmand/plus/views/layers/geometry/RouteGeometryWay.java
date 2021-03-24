package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Paint;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;

import java.util.Collections;
import java.util.List;

public class RouteGeometryWay extends GeometryWay<RouteGeometryWayContext, GeometryWayDrawer<RouteGeometryWayContext>> {

	private RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customColor;
	private Float customWidth;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new GeometryWayDrawer<>(context));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(@Nullable @ColorInt Integer color, @Nullable Float width) {
		this.customColor = color;
		this.customWidth = width;
	}

	@NonNull
	@Override
	public GeometryWayStyle<RouteGeometryWayContext> getDefaultWayStyle() {
		Paint paint = getContext().getAttrs().paint;
		int color = customColor != null ? customColor : paint.getColor();
		float width = customWidth != null ? customWidth : paint.getStrokeWidth();
		return new GeometrySolidWayStyle(getContext(), color, width);
	}

	public void updateRoute(RotatedTileBox tb, RouteCalculationResult route) {
		if (tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			List<Location> locations = route != null ? route.getImmutableAllLocations() : Collections.<Location>emptyList();
			updateWay(locations, tb);
		}
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	@Override
	public Location getNextVisiblePoint() {
		return helper.getRoute().getCurrentStraightAnglePoint();
	}

	private static class GeometrySolidWayStyle extends GeometryWayStyle<RouteGeometryWayContext> {

		GeometrySolidWayStyle(RouteGeometryWayContext context, Integer color, Float width) {
			super(context, color, width);
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometrySolidWayStyle;
		}
	}
}
