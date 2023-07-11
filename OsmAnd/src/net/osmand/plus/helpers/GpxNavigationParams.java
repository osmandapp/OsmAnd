package net.osmand.plus.helpers;

public class GpxNavigationParams {

	private boolean force;
	private boolean checkLocationPermission;
	private boolean passWholeRoute;
	private boolean snapToRoad;
	private String snapToRoadMode;
	private int snapToRoadThreshold;
	private String customGpxFileName;

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isCheckLocationPermission() {
		return checkLocationPermission;
	}

	public void setCheckLocationPermission(boolean checkLocationPermission) {
		this.checkLocationPermission = checkLocationPermission;
	}

	public boolean isPassWholeRoute() {
		return passWholeRoute;
	}

	public void setPassWholeRoute(boolean passWholeRoute) {
		this.passWholeRoute = passWholeRoute;
	}

	public boolean isSnapToRoad() {
		return snapToRoad;
	}

	public void setSnapToRoad(boolean snapToRoad) {
		this.snapToRoad = snapToRoad;
	}

	public String getSnapToRoadMode() {
		return snapToRoadMode;
	}

	public void setSnapToRoadMode(String snapToRoadMode) {
		this.snapToRoadMode = snapToRoadMode;
	}

	public int getSnapToRoadThreshold() {
		return snapToRoadThreshold;
	}

	public void setSnapToRoadThreshold(int snapToRoadThreshold) {
		this.snapToRoadThreshold = snapToRoadThreshold;
	}
	public String getCustomGpxFileName() { return customGpxFileName; }

	public void setCustomGpxFileName(String customGpxFileName) { this.customGpxFileName = customGpxFileName; }
}
