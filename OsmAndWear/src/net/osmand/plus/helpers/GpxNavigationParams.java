package net.osmand.plus.helpers;

import static net.osmand.plus.routing.GpxApproximator.DEFAULT_POINT_APPROXIMATION;

public class GpxNavigationParams {

	private boolean force;
	private boolean checkLocationPermission;
	private boolean passWholeRoute;
	private boolean snapToRoad;
	private String snapToRoadMode;
	private int snapToRoadThreshold = DEFAULT_POINT_APPROXIMATION;
	private boolean importedByApi;

	public boolean isForce() {
		return force;
	}

	public GpxNavigationParams setForce(boolean force) {
		this.force = force;
		return this;
	}

	public boolean isCheckLocationPermission() {
		return checkLocationPermission;
	}

	public GpxNavigationParams setCheckLocationPermission(boolean checkLocationPermission) {
		this.checkLocationPermission = checkLocationPermission;
		return this;
	}

	public boolean isPassWholeRoute() {
		return passWholeRoute;
	}

	public GpxNavigationParams setPassWholeRoute(boolean passWholeRoute) {
		this.passWholeRoute = passWholeRoute;
		return this;
	}

	public boolean isSnapToRoad() {
		return snapToRoad;
	}

	public GpxNavigationParams setSnapToRoad(boolean snapToRoad) {
		this.snapToRoad = snapToRoad;
		return this;
	}

	public String getSnapToRoadMode() {
		return snapToRoadMode;
	}

	public GpxNavigationParams setSnapToRoadMode(String snapToRoadMode) {
		this.snapToRoadMode = snapToRoadMode;
		return this;
	}

	public int getSnapToRoadThreshold() {
		return snapToRoadThreshold;
	}

	public GpxNavigationParams setSnapToRoadThreshold(int snapToRoadThreshold) {
		this.snapToRoadThreshold = snapToRoadThreshold;
		return this;
	}

	public boolean isImportedByApi() {
		return importedByApi;
	}

	public GpxNavigationParams setImportedByApi(boolean importedByApi) {
		this.importedByApi = importedByApi;
		return this;
	}
}
