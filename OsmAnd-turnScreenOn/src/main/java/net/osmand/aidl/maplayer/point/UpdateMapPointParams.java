package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

public class UpdateMapPointParams implements Parcelable {
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

	public static final Creator<UpdateMapPointParams> CREATOR = new
			Creator<UpdateMapPointParams>() {
				public UpdateMapPointParams createFromParcel(Parcel in) {
					return new UpdateMapPointParams(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(layerId);
		out.writeParcelable(point, flags);
		out.writeInt(updateOpenedMenuAndMap ? 1 : 0);
	}

	private void readFromParcel(Parcel in) {
		layerId = in.readString();
		point = in.readParcelable(AMapPoint.class.getClassLoader());
		updateOpenedMenuAndMap = in.readInt() != 0;
	}

	public int describeContents() {
		return 0;
	}
}
