package net.osmand.aidlapi.mapmarker;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddMapMarkerParams extends AidlParams {

	private AMapMarker marker;

	public AddMapMarkerParams(AMapMarker marker) {
		this.marker = marker;
	}

	public AddMapMarkerParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddMapMarkerParams> CREATOR = new Creator<AddMapMarkerParams>() {
		@Override
		public AddMapMarkerParams createFromParcel(Parcel in) {
			return new AddMapMarkerParams(in);
		}

		@Override
		public AddMapMarkerParams[] newArray(int size) {
			return new AddMapMarkerParams[size];
		}
	};

	public AMapMarker getMarker() {
		return marker;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putParcelable("marker", marker);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapMarker.class.getClassLoader());
		marker = bundle.getParcelable("marker");
	}
}