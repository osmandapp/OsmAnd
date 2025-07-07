package net.osmand.plus.views.layers;

import static net.osmand.util.MapUtils.HIGH_LATLON_PRECISION;

import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.shared.routing.ColoringType;
import net.osmand.util.MapUtils;

class RouteRenderState {

	private Location lastProjection = null;
	private int startLocationIndex = -1;
	private int publicTransportRoute = -1;
	private ColoringType coloringType = ColoringType.DEFAULT;
	private int routeColor = -1;
	private float routeWidth = -1f;
	private int currentRoute = -1;
	private int zoom = -1;
	private boolean shouldShowTurnArrows;
	private boolean shouldShowDirectionArrows;

	boolean shouldRebuildRoute;
	boolean shouldRebuildTransportRoute;
	boolean shouldUpdateRoute;
	boolean shouldUpdateActionPoints;

	public void updateRouteState(@Nullable Location lastProjection, int startLocationIndex,
			ColoringType coloringType, int routeColor, float routeWidth,
			int currentRoute, int zoom,
			boolean shouldShowTurnArrows, boolean shouldShowDirectionArrows) {
		this.shouldRebuildRoute = this.coloringType != coloringType
				|| this.routeColor != routeColor
				|| this.routeWidth != routeWidth
				|| this.shouldShowDirectionArrows != shouldShowDirectionArrows;

		this.shouldUpdateRoute = (!MapUtils.areLatLonEqual(this.lastProjection, lastProjection, HIGH_LATLON_PRECISION)
				|| this.startLocationIndex != startLocationIndex)
				&& this.coloringType == coloringType
				&& this.routeColor == routeColor
				&& this.routeWidth == routeWidth
				&& this.shouldShowDirectionArrows == shouldShowDirectionArrows;

		this.shouldUpdateActionPoints = this.shouldRebuildRoute
				|| this.startLocationIndex != startLocationIndex
				|| this.shouldShowTurnArrows != shouldShowTurnArrows
				|| this.currentRoute != currentRoute
				|| this.zoom != zoom;

		this.lastProjection = lastProjection;
		this.startLocationIndex = startLocationIndex;
		this.coloringType = coloringType;
		this.routeColor = routeColor;
		this.routeWidth = routeWidth;
		this.currentRoute = currentRoute;
		this.zoom = zoom;
		this.shouldShowTurnArrows = shouldShowTurnArrows;
		this.shouldShowDirectionArrows = shouldShowDirectionArrows;
	}

	public void updateTransportRouteState(int publicTransportRoute) {
		this.shouldRebuildTransportRoute = this.publicTransportRoute != publicTransportRoute;
		this.publicTransportRoute = publicTransportRoute;
	}
}
