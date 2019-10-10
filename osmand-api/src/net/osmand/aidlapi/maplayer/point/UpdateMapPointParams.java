package net.osmand.aidlapi.maplayer.point;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;

public class UpdateMapPointParams extends AidlParams {

	private String layerId;
	private AMapPoint point;
	private boolean updateOpenedMenuAndMap;

	public UpdateMapPointParams(String layerId, AMapPoint point, boolean updateOpenedMenuAndMap) {
		this.layerId = layerId;
		this.point = point;
		this.updateOpenedMenuAndMap = updateOpenedMenuAndMap;
	}

	public UpdateMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<UpdateMapPointParams> CREATOR = new Creator<UpdateMapPointParams>() {
		@Override
		public UpdateMapPointParams createFromParcel(Parcel in) {
			return new UpdateMapPointParams(in);
		}

		@Override
		public UpdateMapPointParams[] newArray(int size) {
			return new UpdateMapPointParams[size];
		}
	};

	public String getLayerId() {
		return layerId;
	}

	public AMapPoint getPoint() {
		return point;
	}

	public boolean isUpdateOpenedMenuAndMap() {
		return updateOpenedMenuAndMap;
	}

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("layerId", layerId);
		bundle.putParcelable("point", point);
		bundle.putBoolean("updateOpenedMenuAndMap", updateOpenedMenuAndMap);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		bundle.setClassLoader(AMapPoint.class.getClassLoader());
		layerId = bundle.getString("layerId");
		point = bundle.getParcelable("point");
		updateOpenedMenuAndMap = bundle.getBoolean("updateOpenedMenuAndMap");
	}
}