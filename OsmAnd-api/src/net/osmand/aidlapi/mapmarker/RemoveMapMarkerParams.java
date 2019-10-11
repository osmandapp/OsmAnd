package net.osmand.aidlapi.mapmarker;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveMapMarkerParams extends AidlParams {

	private AMapMarker marker;
	private boolean ignoreCoordinates;

	public RemoveMapMarkerParams(AMapMarker marker) {
		this.marker = marker;
		this.ignoreCoordinates = false;
	}

	public RemoveMapMarkerParams(AMapMarker marker, boolean ignoreCoordinates) {
		this.marker = marker;
		this.ignoreCoordinates = ignoreCoordinates;
	}

	public RemoveMapMarkerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveMapMarkerParams> CREATOR = new Creator<RemoveMapMarkerParams>() {
		@Override
		public RemoveMapMarkerParams createFromParcel(Parcel in) {
			return new RemoveMapMarkerParams(in);
		}

		@Override
		public RemoveMapMarkerParams[] newArray(int size) {
			return new RemoveMapMarkerParams[size];
		}
	};

	public AMapMarker getMarker() {
		return marker;
	}

	public boolean getIgnoreCoordinates() {
		return ignoreCoordinates;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("marker", marker);
		bundle.putBoolean("ignoreCoordinates", ignoreCoordinates);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapMarker.class.getClassLoader());
		marker = bundle.getParcelable("marker");
		ignoreCoordinates = bundle.getBoolean("ignoreCoordinates");
	}
}