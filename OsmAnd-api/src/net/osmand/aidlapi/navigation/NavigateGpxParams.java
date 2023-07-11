package net.osmand.aidlapi.navigation;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class NavigateGpxParams extends AidlParams {

	private String data;
	private Uri uri;
	private boolean force;
	private boolean needLocationPermission;
	private boolean passWholeRoute;
	private boolean snapToRoad;
	private String snapToRoadMode;
	private int snapToRoadThreshold;
	private String customGpxFileName;

	public NavigateGpxParams(String data, boolean force, boolean needLocationPermission) {
		this.data = data;
		this.force = force;
		this.needLocationPermission = needLocationPermission;
	}

	public NavigateGpxParams(Uri uri, boolean force, boolean needLocationPermission) {
		this.uri = uri;
		this.force = force;
		this.needLocationPermission = needLocationPermission;
	}

	public NavigateGpxParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<NavigateGpxParams> CREATOR = new Creator<NavigateGpxParams>() {
		@Override
		public NavigateGpxParams createFromParcel(Parcel in) {
			return new NavigateGpxParams(in);
		}

		@Override
		public NavigateGpxParams[] newArray(int size) {
			return new NavigateGpxParams[size];
		}
	};

	public String getData() {
		return data;
	}

	public Uri getUri() {
		return uri;
	}

	public boolean isForce() {
		return force;
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

	public boolean isNeedLocationPermission() {
		return needLocationPermission;
	}

	public String getCustomGpxFileName() { return customGpxFileName; }

	public void setCustomGpxFileName(String customGpxFileName) { this.customGpxFileName = customGpxFileName; }


	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("data", data);
		bundle.putParcelable("uri", uri);
		bundle.putBoolean("force", force);
		bundle.putBoolean("needLocationPermission", needLocationPermission);
		bundle.putBoolean("passWholeRoute", passWholeRoute);
		bundle.putBoolean("snapToRoad", snapToRoad);
		bundle.putString("snapToRoadMode", snapToRoadMode);
		bundle.putInt("snapToRoadThreshold", snapToRoadThreshold);
		bundle.putString("customGpxFileName", customGpxFileName);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		data = bundle.getString("data");
		uri = bundle.getParcelable("uri");
		force = bundle.getBoolean("force");
		needLocationPermission = bundle.getBoolean("needLocationPermission");
		passWholeRoute = bundle.getBoolean("passWholeRoute");
		snapToRoad = bundle.getBoolean("snapToRoad");
		snapToRoadMode = bundle.getString("snapToRoadMode");
		snapToRoadThreshold = bundle.getInt("snapToRoadThreshold");
		customGpxFileName = bundle.getString("customGpxFileName");
	}
}