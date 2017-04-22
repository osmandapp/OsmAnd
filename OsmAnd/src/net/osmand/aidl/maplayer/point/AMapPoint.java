package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.ALatLon;

public class AMapPoint implements Parcelable {
	private String id;
	private String shortName;
	private String fullName;
	private int color;
	private ALatLon location;

	public AMapPoint(String id, String shortName, String fullName, int color, ALatLon location) {
		this.id = id;
		this.shortName = shortName;
		this.fullName = fullName;
		this.color = color;
		this.location = location;
	}

	public AMapPoint(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<AMapPoint> CREATOR = new
			Parcelable.Creator<AMapPoint>() {
				public AMapPoint createFromParcel(Parcel in) {
					return new AMapPoint(in);
				}

				public AMapPoint[] newArray(int size) {
					return new AMapPoint[size];
				}
			};

	public String getId() {
		return id;
	}

	public String getShortName() {
		return shortName;
	}

	public String getFullName() {
		return fullName;
	}

	public int getColor() {
		return color;
	}

	public ALatLon getLocation() {
		return location;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(shortName);
		out.writeString(fullName);
		out.writeInt(color);
		out.writeParcelable(location, flags);
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		shortName = in.readString();
		shortName = in.readString();
		color = in.readInt();
		location = in.readParcelable(ALatLon.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
