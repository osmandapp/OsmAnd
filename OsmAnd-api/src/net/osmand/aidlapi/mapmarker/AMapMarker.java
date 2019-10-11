package net.osmand.aidlapi.mapmarker;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.map.ALatLon;

public class AMapMarker extends AidlParams {

	private ALatLon latLon;
	private String name;

	public AMapMarker(ALatLon latLon, String name) {
		if (latLon == null) {
			throw new IllegalArgumentException("latLon cannot be null");
		}
		if (name == null) {
			name = "";
		}
		this.latLon = latLon;
		this.name = name;
	}

	public AMapMarker(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapMarker> CREATOR = new Creator<AMapMarker>() {
		@Override
		public AMapMarker createFromParcel(Parcel in) {
			return new AMapMarker(in);
		}

		@Override
		public AMapMarker[] newArray(int size) {
			return new AMapMarker[size];
		}
	};

	public ALatLon getLatLon() {
		return latLon;
	}

	public String getName() {
		return name;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("latLon", latLon);
		bundle.putString("name", name);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(ALatLon.class.getClassLoader());
		latLon = bundle.getParcelable("latLon");
		name = bundle.getString("name");
	}
}