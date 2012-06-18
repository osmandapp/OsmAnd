package net.osmand.plus.routing;

public class RouteDirectionInfo {
	public String descriptionRoute = ""; //$NON-NLS-1$
	private float averageSpeed;

	// Constructor to verify average speed always > 0
	public RouteDirectionInfo(float averageSpeed) {
		this.averageSpeed = averageSpeed == 0 ? 1 : averageSpeed;

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

	// FIXME How it can be null? (fix by constructor and revert MapInfoLayer)
	public TurnType turnType;
	// location when you should action (turn or go ahead)
	public int routePointOffset;

	// TODO add from parser
	public String ref;
	public String streetName;
	// speed limit in m/s (should be array of speed limits?)
	public float speedLimit;

	// calculated vars
	// after action (excluding expectedTime)
	public int afterLeftTime;
	// distance after action (for i.e. after turn to next turn)
	public int distance;
}