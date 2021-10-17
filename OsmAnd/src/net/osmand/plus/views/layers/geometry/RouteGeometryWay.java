package net.osmand.plus.views.layers.geometry;

import android.graphics.Paint;

import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.util.Algorithms;

import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RouteGeometryWay extends
		MultiColoringGeometryWay<RouteGeometryWayContext, MultiColoringGeometryWayDrawer<RouteGeometryWayContext>> {

	private final RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customDirectionArrowColor;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new MultiColoringGeometryWayDrawer<>(context));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(int pathColor,
	                                float pathWidth,
	                                @Nullable @ColorInt Integer directionArrowColor,
	                                @NonNull ColoringType routeColoringType,
	                                @Nullable String routeInfoAttribute) {
		this.coloringChanged = this.coloringType != routeColoringType
				|| routeColoringType == ColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		boolean widthChanged = !Algorithms.objectEquals(customWidth, pathWidth);
		if (widthChanged) {
			updateStylesWidth(pathWidth);
		}
		updatePaints(pathWidth, routeColoringType);
		getDrawer().setColoringType(routeColoringType);

		this.customColor = pathColor;
		this.customWidth = pathWidth;
		this.customDirectionArrowColor = directionArrowColor;
		this.coloringType = routeColoringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	@Override
	protected void updatePaints(@Nullable Float width, @NonNull ColoringType routeColoringType) {
		super.updatePaints(width, routeColoringType);
		Paint.Cap cap = routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute() ?
				Paint.Cap.ROUND : getContext().getAttrs().paint.getStrokeCap();
		getContext().getAttrs().customColorPaint.setStrokeCap(cap);
	}

	public void updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			coloringChanged = false;
			List<Location> locations = route.getImmutableAllLocations();

			if (coloringType.isGradient()) {
				updateGradientRoute(tb, locations);
			} else if (coloringType.isRouteInfoAttribute()) {
				updateSolidMultiColorRoute(tb, locations, route.getOriginalRoute());
			} else {
				updateWay(locations, tb);
			}
		}
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		if (coloringType.isGradient()) {
			return new GeometryGradientWayStyle(getContext(), customColor, customWidth);
		}
		return new GeometrySolidWayStyle<>(getContext(), customColor, customWidth, customDirectionArrowColor);
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<RouteGeometryWayContext> getSolidWayStyle(int lineColor) {
		return new GeometrySolidWayStyle<>(getContext(), lineColor, customWidth, customDirectionArrowColor);
	}

	@Override
	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb) {
		if (coloringType.isGradient()) {
			int zoom = tb.getZoom();
			PathGeometryZoom zm = zooms.get(zoom);
			if (zm == null) {
				zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true);
				zooms.put(zoom, zm);
			}
			return zm;
		}
		return super.getGeometryZoom(tb);
	}

	@Override
	public Location getNextVisiblePoint() {
		return helper.getRoute().getCurrentStraightAnglePoint();
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}
}