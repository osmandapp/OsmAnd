package net.osmand.aidlapi.maplayer.point;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class RemoveMapPointParams extends AidlParams {

	private String layerId;
	private String pointId;

	public RemoveMapPointParams(String layerId, String pointId) {
		this.layerId = layerId;
		this.pointId = pointId;
	}

	public RemoveMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveMapPointParams> CREATOR = new Creator<RemoveMapPointParams>() {
		@Override
		public RemoveMapPointParams createFromParcel(Parcel in) {
			return new RemoveMapPointParams(in);
		}

		@Override
		public RemoveMapPointParams[] newArray(int size) {
			return new RemoveMapPointParams[size];
		}
	};

	public String getLayerId() {
		return layerId;
	}

	public String getPointId() {
		return pointId;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("layerId", layerId);
		bundle.putString("pointId", pointId);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		super.readFromBundle(bundle);

		layerId = bundle.getString("layerId");
		pointId = bundle.getString("pointId");
	}
}