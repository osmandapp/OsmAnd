package net.osmand.aidlapi.maplayer.point;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class AddMapPointParams extends AidlParams {

	private String layerId;
	private AMapPoint point;

	public AddMapPointParams(String layerId, AMapPoint point) {
		this.layerId = layerId;
		this.point = point;
	}

	public AddMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddMapPointParams> CREATOR = new Creator<AddMapPointParams>() {
		@Override
		public AddMapPointParams createFromParcel(Parcel in) {
			return new AddMapPointParams(in);
		}

		@Override
		public AddMapPointParams[] newArray(int size) {
			return new AddMapPointParams[size];
		}
	};

	public String getLayerId() {
		return layerId;
	}

	public AMapPoint getPoint() {
		return point;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("layerId", layerId);
		bundle.putParcelable("point", point);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapPoint.class.getClassLoader());

		layerId = bundle.getString("layerId");
		point = bundle.getParcelable("point");
	}
}