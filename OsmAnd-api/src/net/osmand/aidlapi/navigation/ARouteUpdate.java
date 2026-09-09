package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ARouteUpdate extends AidlParams {

	public static final int ROUTE_EVENT_RECALCULATED = 0;
	public static final int ROUTE_EVENT_CANCELLED = 1;
	public static final int ROUTE_EVENT_FINISHED = 2;

	private int eventType;
	private boolean newRoute;
	private int routeVersion;
	private int remainingDistanceM;
	private String profileKey;
	private int routeSource = ActiveRouteGeometry.ROUTE_SOURCE_UNKNOWN;
	private boolean planningMode;

	public ARouteUpdate() {
	}

	public ARouteUpdate(int eventType, boolean newRoute, int routeVersion, int remainingDistanceM,
	                    String profileKey, int routeSource, boolean planningMode) {
		this.eventType = eventType;
		this.newRoute = newRoute;
		this.routeVersion = routeVersion;
		this.remainingDistanceM = remainingDistanceM;
		this.profileKey = profileKey;
		this.routeSource = routeSource;
		this.planningMode = planningMode;
	}

	protected ARouteUpdate(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ARouteUpdate> CREATOR = new Creator<ARouteUpdate>() {
		@Override
		public ARouteUpdate createFromParcel(Parcel in) {
			return new ARouteUpdate(in);
		}

		@Override
		public ARouteUpdate[] newArray(int size) {
			return new ARouteUpdate[size];
		}
	};

	public int getEventType() {
		return eventType;
	}

	public void setEventType(int eventType) {
		this.eventType = eventType;
	}

	public boolean isNewRoute() {
		return newRoute;
	}

	public void setNewRoute(boolean newRoute) {
		this.newRoute = newRoute;
	}

	public int getRouteVersion() {
		return routeVersion;
	}

	public void setRouteVersion(int routeVersion) {
		this.routeVersion = routeVersion;
	}

	public int getRemainingDistanceM() {
		return remainingDistanceM;
	}

	public void setRemainingDistanceM(int remainingDistanceM) {
		this.remainingDistanceM = remainingDistanceM;
	}

	public String getProfileKey() {
		return profileKey;
	}

	public void setProfileKey(String profileKey) {
		this.profileKey = profileKey;
	}

	public int getRouteSource() {
		return routeSource;
	}

	public void setRouteSource(int routeSource) {
		this.routeSource = routeSource;
	}

	public boolean isPlanningMode() {
		return planningMode;
	}

	public void setPlanningMode(boolean planningMode) {
		this.planningMode = planningMode;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		eventType = bundle.getInt("eventType");
		newRoute = bundle.getBoolean("newRoute");
		routeVersion = bundle.getInt("routeVersion");
		remainingDistanceM = bundle.getInt("remainingDistanceM");
		profileKey = bundle.getString("profileKey");
		routeSource = bundle.getInt("routeSource", ActiveRouteGeometry.ROUTE_SOURCE_UNKNOWN);
		planningMode = bundle.getBoolean("planningMode");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putInt("eventType", eventType);
		bundle.putBoolean("newRoute", newRoute);
		bundle.putInt("routeVersion", routeVersion);
		bundle.putInt("remainingDistanceM", remainingDistanceM);
		bundle.putString("profileKey", profileKey);
		bundle.putInt("routeSource", routeSource);
		bundle.putBoolean("planningMode", planningMode);
	}
}
