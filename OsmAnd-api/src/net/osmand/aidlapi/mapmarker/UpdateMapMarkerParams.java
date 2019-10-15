package net.osmand.aidlapi.mapmarker;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UpdateMapMarkerParams extends AidlParams {

	private AMapMarker markerPrev;
	private AMapMarker markerNew;
	private boolean ignoreCoordinates;

	public UpdateMapMarkerParams(AMapMarker markerPrev, AMapMarker markerNew) {
		this.markerPrev = markerPrev;
		this.markerNew = markerNew;
		this.ignoreCoordinates = false;
	}

	public UpdateMapMarkerParams(AMapMarker markerPrev, AMapMarker markerNew, boolean ignoreCoordinates) {
		this.markerPrev = markerPrev;
		this.markerNew = markerNew;
		this.ignoreCoordinates = ignoreCoordinates;
	}

	public UpdateMapMarkerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UpdateMapMarkerParams> CREATOR = new Creator<UpdateMapMarkerParams>() {
		@Override
		public UpdateMapMarkerParams createFromParcel(Parcel in) {
			return new UpdateMapMarkerParams(in);
		}

		@Override
		public UpdateMapMarkerParams[] newArray(int size) {
			return new UpdateMapMarkerParams[size];
		}
	};

	public AMapMarker getMarkerPrev() {
		return markerPrev;
	}

	public AMapMarker getMarkerNew() {
		return markerNew;
	}

	public boolean getIgnoreCoordinates() {
		return ignoreCoordinates;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("markerPrev", markerPrev);
		bundle.putParcelable("markerNew", markerNew);
		bundle.putBoolean("ignoreCoordinates", ignoreCoordinates);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapMarker.class.getClassLoader());
		markerPrev = bundle.getParcelable("markerPrev");
		markerNew = bundle.getParcelable("markerNew");
		ignoreCoordinates = bundle.getBoolean("ignoreCoordinates");
	}
}