package net.osmand.aidl.maplayer.point;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.map.ALatLon;

import java.util.ArrayList;
import java.util.List;

public class AMapPoint implements Parcelable {
	private String id;
	private String shortName;
	private String fullName;
	private String typeName;
	private int color;
	private ALatLon location;
	private List<String> details = new ArrayList<>();

	public AMapPoint(String id, String shortName, String fullName, String typeName, int color,
					 ALatLon location, List<String> details) {
		this.id = id;
		this.shortName = shortName;
		this.fullName = fullName;
		this.typeName = typeName;
		this.color = color;
		this.location = location;
		if (details != null) {
			this.details.addAll(details);
		}
	}

	public AMapPoint(Parcel in) {
		readFromParcel(in);
	}

	public static final Creator<AMapPoint> CREATOR = new
			Creator<AMapPoint>() {
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

	public String getTypeName() {
		return typeName;
	}

	public int getColor() {
		return color;
	}

	public ALatLon getLocation() {
		return location;
	}

	public List<String> getDetails() {
		return details;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(shortName);
		out.writeString(fullName);
		out.writeString(typeName);
		out.writeInt(color);
		out.writeParcelable(location, flags);
		out.writeStringList(details);
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		shortName = in.readString();
		fullName = in.readString();
		typeName = in.readString();
		color = in.readInt();
		location = in.readParcelable(ALatLon.class.getClassLoader());
		in.readStringList(details);
	}

	public int describeContents() {
		return 0;
	}
}
