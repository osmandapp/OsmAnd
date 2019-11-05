package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

public class AddMapPointParams implements Parcelable {
	private String layerId;
	private AMapPoint point;

	public AddMapPointParams(String layerId, AMapPoint point) {
		this.layerId = layerId;
		this.point = point;
	}

	public AddMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AddMapPointParams> CREATOR = new
			Creator<AddMapPointParams>() {
				public AddMapPointParams createFromParcel(Parcel in) {
					return new AddMapPointParams(in);
				}

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

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(layerId);
		out.writeParcelable(point, flags);
	}

	private void readFromParcel(Parcel in) {
		layerId = in.readString();
		point = in.readParcelable(AMapPoint.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
