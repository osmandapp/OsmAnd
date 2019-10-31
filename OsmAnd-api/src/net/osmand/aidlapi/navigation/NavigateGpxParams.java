package net.osmand.aidlapi.navigation;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class NavigateGpxParams extends AidlParams {

	private String data;
	private Uri uri;
	private boolean force;

	public NavigateGpxParams(String data, boolean force) {
		this.data = data;
		this.force = force;
	}

	public NavigateGpxParams(Uri uri, boolean force) {
		this.uri = uri;
		this.force = force;
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

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("data", data);
		bundle.putParcelable("uri", uri);
		bundle.putBoolean("force", force);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		data = bundle.getString("data");
		uri = bundle.getParcelable("uri");
		force = bundle.getBoolean("force");
	}
}