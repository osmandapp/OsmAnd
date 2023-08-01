package net.osmand.aidlapi.navigation;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class NavigateGpxParams extends AidlParams {

	private Uri uri;
	private String data;
	private String fileName;
	private boolean force;
	private boolean needLocationPermission;
	private boolean passWholeRoute;
	private boolean snapToRoad;
	private String snapToRoadMode;
	private int snapToRoadThreshold;

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

	public Uri getUri() {
		return uri;
	}

	public String getData() {
		return data;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
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
		bundle.putString("fileName", fileName);
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
		fileName = bundle.getString("fileName");
	}
}