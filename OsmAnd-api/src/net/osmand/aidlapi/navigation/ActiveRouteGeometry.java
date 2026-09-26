package net.osmand.aidlapi.navigation;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

import java.util.ArrayList;
import java.util.List;

public class ActiveRouteGeometry extends AidlParams {

	public static final int ROUTE_SOURCE_OSMAND = 0;
	public static final int ROUTE_SOURCE_BROUTER = 1;
	public static final int ROUTE_SOURCE_STRAIGHT = 2;
	public static final int ROUTE_SOURCE_DIRECT_TO = 3;
	public static final int ROUTE_SOURCE_ONLINE = 4;
	public static final int ROUTE_SOURCE_UNKNOWN = 5;

	private ArrayList<ALatLon> points = new ArrayList<>();
	private int totalDistanceM;
	private int remainingDistanceM;
	private String profileKey;
	private int routeSource = ROUTE_SOURCE_UNKNOWN;
	private int routeVersion;
	private long calculatedAt;
	private boolean planningMode;

	public ActiveRouteGeometry() {
	}

	protected ActiveRouteGeometry(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ActiveRouteGeometry> CREATOR = new Creator<ActiveRouteGeometry>() {
		@Override
		public ActiveRouteGeometry createFromParcel(Parcel in) {
			return new ActiveRouteGeometry(in);
		}

		@Override
		public ActiveRouteGeometry[] newArray(int size) {
			return new ActiveRouteGeometry[size];
		}
	};

	public List<ALatLon> getPoints() {
		return points;
	}

	public void setPoints(List<ALatLon> points) {
		this.points = new ArrayList<>();
		if (points != null) {
			this.points.addAll(points);
		}
	}

	public int getTotalDistanceM() {
		return totalDistanceM;
	}

	public void setTotalDistanceM(int totalDistanceM) {
		this.totalDistanceM = totalDistanceM;
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

	public int getRouteVersion() {
		return routeVersion;
	}

	public void setRouteVersion(int routeVersion) {
		this.routeVersion = routeVersion;
	}

	public long getCalculatedAt() {
		return calculatedAt;
	}

	public void setCalculatedAt(long calculatedAt) {
		this.calculatedAt = calculatedAt;
	}

	public boolean isPlanningMode() {
		return planningMode;
	}

	public void setPlanningMode(boolean planningMode) {
		this.planningMode = planningMode;
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(ALatLon.class.getClassLoader());
		ArrayList<ALatLon> readPoints = bundle.getParcelableArrayList("points");
		points = readPoints != null ? readPoints : new ArrayList<>();
		totalDistanceM = bundle.getInt("totalDistanceM");
		remainingDistanceM = bundle.getInt("remainingDistanceM");
		profileKey = bundle.getString("profileKey");
		routeSource = bundle.getInt("routeSource", ROUTE_SOURCE_UNKNOWN);
		routeVersion = bundle.getInt("routeVersion");
		calculatedAt = bundle.getLong("calculatedAt");
		planningMode = bundle.getBoolean("planningMode");
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelableArrayList("points", points);
		bundle.putInt("totalDistanceM", totalDistanceM);
		bundle.putInt("remainingDistanceM", remainingDistanceM);
		bundle.putString("profileKey", profileKey);
		bundle.putInt("routeSource", routeSource);
		bundle.putInt("routeVersion", routeVersion);
		bundle.putLong("calculatedAt", calculatedAt);
		bundle.putBoolean("planningMode", planningMode);
	}
}
