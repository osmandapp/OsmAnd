package net.osmand.plus.routing;

import net.osmand.router.TurnType;

public class RouteDirectionInfo {
	// location when you should action (turn or go ahead)
	public int routePointOffset;
	// Type of action to take
	private TurnType turnType;
	// Description of the turn and route after
	private String descriptionRoute = ""; //$NON-NLS-1$
	// Speed after the action till next turn
	private float averageSpeed;

	// Constructor to verify average speed always > 0
	public RouteDirectionInfo(float averageSpeed, TurnType turnType) {
		this.averageSpeed = averageSpeed == 0 ? 1 : averageSpeed;
		this.turnType = turnType;
	}
	
	public String getDescriptionRoute() {
		return descriptionRoute;
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
		return (int) (distance / averageSpeed);
	}

	
	public TurnType getTurnType() {
		return turnType;
	}


	// TODO add from parser
//	public String ref;
//	public String streetName;
//	// speed limit in m/s (should be array of speed limits?)
//	public float speedLimit;

	// calculated vars
	// after action (excluding expectedTime)
	public int afterLeftTime;
	// distance after action (for i.e. after turn to next turn)
	public int distance;
}