package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

public class ShowMapPointParams implements Parcelable {

	private String layerId;
	private AMapPoint point;

	public ShowMapPointParams(String layerId, AMapPoint point) {
		this.layerId = layerId;
		this.point = point;
	}

	public ShowMapPointParams(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<ShowMapPointParams> CREATOR = new
			Creator<ShowMapPointParams>() {
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
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(layerId);
		out.writeParcelable(point, flags);
	}

	private void readFromParcel(Parcel in) {
		layerId = in.readString();
		point = in.readParcelable(AMapPoint.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
