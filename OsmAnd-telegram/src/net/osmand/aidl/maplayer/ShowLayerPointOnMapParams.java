package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class ShowLayerPointOnMapParams implements Parcelable {

	private String layerId;
	private String pointId;

	public ShowLayerPointOnMapParams(String layerId, String pointId) {
		this.layerId = layerId;
		this.pointId = pointId;
	}

	public ShowLayerPointOnMapParams(Parcel in) {
		layerId = in.readString();
		pointId = in.readString();
	}

	public String getLayerId() {
		return layerId;
	}

	public String getPointId() {
		return pointId;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(layerId);
		dest.writeString(pointId);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public static final Creator<ShowLayerPointOnMapParams> CREATOR = new Creator<ShowLayerPointOnMapParams>() {
		@Override
		public ShowLayerPointOnMapParams createFromParcel(Parcel in) {
			return new ShowLayerPointOnMapParams(in);
		}

		@Override
		public ShowLayerPointOnMapParams[] newArray(int size) {
			return new ShowLayerPointOnMapParams[size];
		}
	};
}
