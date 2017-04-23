package net.osmand.aidl.maplayer;

import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.aidl.maplayer.point.AMapPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AMapLayer implements Parcelable {
	private String id;
	private String name;
	private float zOrder = 5.5f;
	private Map<String, AMapPoint> points = new ConcurrentHashMap<>();

	public AMapLayer(String id, String name, float zOrder, List<AMapPoint> pointList) {
		this.id = id;
		this.name = name;
		this.zOrder = zOrder;
		if (pointList != null) {
			for (AMapPoint p : pointList) {
				this.points.put(p.getId(), p);
			}
		}
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

	public float getZOrder() {
		return zOrder;
	}

	public List<AMapPoint> getPoints() {
		return new ArrayList<>(points.values());
	}

	public boolean hasPoint(String pointId) {
		return points.containsKey(pointId);
	}

	public void putPoint(AMapPoint point) {
		points.put(point.getId(), point);
	}

	public void removePoint(String pointId) {
		points.remove(pointId);
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(name);
		out.writeFloat(zOrder);
		out.writeTypedList(new ArrayList<>(points.values()));
	}

	private void readFromParcel(Parcel in) {
		id = in.readString();
		name = in.readString();
		zOrder = in.readFloat();
		List<AMapPoint> pointList = new ArrayList<>();
		in.readTypedList(pointList, AMapPoint.CREATOR);
		for (AMapPoint p : pointList) {
			this.points.put(p.getId(), p);
		}
	}

	public int describeContents() {
		return 0;
	}
}
