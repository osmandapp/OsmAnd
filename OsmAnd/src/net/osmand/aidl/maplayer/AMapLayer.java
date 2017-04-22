package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.maplayer.point.AMapPoint;

import java.util.HashMap;

public class AMapLayer implements Parcelable {
	private String id;
	private String name;
	private HashMap<String, AMapPoint> points = new HashMap<>();

	public AMapLayer(String id, String name, HashMap<String, AMapPoint> points) {
		this.id = id;
		this.name = name;
		this.points = points;
	}

	public AMapLayer(Parcel in) {
		readFromParcel(in);
	}

	public static final Parcelable.Creator<AMapLayer> CREATOR = new
			Parcelable.Creator<AMapLayer>() {
				public AMapLayer createFromParcel(Parcel in) {
					return new AMapLayer(in);
				}

				public AMapLayer[] newArray(int size) {
					return new AMapLayer[size];
				}
			};

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public HashMap<String, AMapPoint> getPoints() {
		return points;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(name);
		out.writeMap(points);
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		name = in.readString();
		in.readMap(points, HashMap.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}
}
