package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoveMapPointParams implements Parcelable {
	private String layerId;
	private String pointId;

	public RemoveMapPointParams(String layerId, String pointId) {
		this.layerId = layerId;
		this.pointId = pointId;
	}

	public RemoveMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<RemoveMapPointParams> CREATOR = new
			Creator<RemoveMapPointParams>() {
				public RemoveMapPointParams createFromParcel(Parcel in) {
					return new RemoveMapPointParams(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(layerId);
		out.writeString(pointId);
	}

	private void readFromParcel(Parcel in) {
		layerId = in.readString();
		pointId = in.readString();
	}

	public int describeContents() {
		return 0;
	}
}
