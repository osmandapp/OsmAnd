package net.osmand.aidlapi.maplayer.point;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class ShowMapPointParams extends AidlParams {

	private String layerId;
	private AMapPoint point;

	public ShowMapPointParams(String layerId, AMapPoint point) {
		this.layerId = layerId;
		this.point = point;
	}

	public ShowMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ShowMapPointParams> CREATOR = new Creator<ShowMapPointParams>() {
		@Override
		public ShowMapPointParams createFromParcel(Parcel in) {
			return new ShowMapPointParams(in);
		}

		@Override
		public ShowMapPointParams[] newArray(int size) {
			return new ShowMapPointParams[size];
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