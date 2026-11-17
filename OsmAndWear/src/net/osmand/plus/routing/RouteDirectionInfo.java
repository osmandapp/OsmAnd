package net.osmand.plus.routing;

import androidx.annotation.Nullable;

import net.osmand.binary.RouteDataObject;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.router.ExitInfo;
import net.osmand.router.TurnType;

public class RouteDirectionInfo {
	// location when you should action (turn or go ahead)
	public int routePointOffset;
	// location where direction end. useful for roundabouts.
	public int routeEndPointOffset;
	// Type of action to take
	private final TurnType turnType;
	// Description of the turn and route after
	private String descriptionRoute = ""; //$NON-NLS-1$
	// Speed after the action till next turn
	private float averageSpeed;

	private String ref;

	private String streetName;

	private String destinationName;

	private RouteDataObject routeDataObject;

	@Nullable
	private ExitInfo exitInfo;

	public String getDestinationName() {
		return destinationName;
	}

	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	// Constructor to verify average speed always > 0
	public RouteDirectionInfo(float averageSpeed, TurnType turnType) {
		this.averageSpeed = averageSpeed == 0 ? 1 : averageSpeed;
		this.turnType = turnType;
	}

	public RouteDataObject getRouteDataObject() {
		return routeDataObject;
	}

	public void setRouteDataObject(RouteDataObject routeDataObject) {
		this.routeDataObject = routeDataObject;
	}

	public String getDescriptionRoute(OsmandApplication ctx) {
		if (!descriptionRoute.endsWith(OsmAndFormatter.getFormattedDistance(distance, ctx))) {
			descriptionRoute += " " + OsmAndFormatter.getFormattedDistance(distance, ctx);
		}
		return descriptionRoute.trim();
	}

	public String getDescriptionRoute(OsmandApplication ctx, int collectedDistance) {
		if (!descriptionRoute.endsWith(OsmAndFormatter.getFormattedDistance(collectedDistance, ctx))) {
			descriptionRoute += " " + OsmAndFormatter.getFormattedDistance(collectedDistance, ctx);
		}
		return descriptionRoute.trim();
	}

	public String getDescriptionRoutePart() {
		return descriptionRoute;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getStreetName() {
		return streetName;
	}

	public void setStreetName(String streetName) {
		this.streetName = streetName;
	}

	public void setDescriptionRoute(String descriptionRoute) {
		this.descriptionRoute = descriptionRoute;
	}

	public float getAverageSpeed() {
		return averageSpeed;
	}

	public void setAverageSpeed(float averageSpeed) {
		this.averageSpeed = averageSpeed == 0 ? 1 : averageSpeed;
	}

	// expected time after route point
	public int getExpectedTime() {
		return Math.round(distance / averageSpeed);
	}


	public TurnType getTurnType() {
		return turnType;
	}


	// calculated vars
	// after action (excluding expectedTime)
	public int afterLeftTime;
	// distance after action (for i.e. after turn to next turn)
	public int distance;

	public int getDistance() {
		return distance;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	@Nullable
	public ExitInfo getExitInfo() {
		return exitInfo;
	}

	public void setExitInfo(@Nullable ExitInfo exitInfo) {
		this.exitInfo = exitInfo;
	}
}