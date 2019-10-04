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

	private boolean imagePoints = false;
	private int circlePointMinZoom = 0;
	private int circlePointMaxZoom = 6;
	private int smallPointMinZoom = 7;
	private int smallPointMaxZoom = 13;
	private int bigPointMinZoom = 14;
	private int bigPointMaxZoom = 22;

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

	public AMapPoint getPoint(String pointId) {
		return points.get(pointId);
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

	public boolean isImagePoints() {
		return imagePoints;
	}

	public void setImagePoints(boolean imagePoints) {
		this.imagePoints = imagePoints;
	}

	public void copyZoomBounds(AMapLayer layer) {
		circlePointMinZoom = layer.circlePointMinZoom;
		circlePointMaxZoom = layer.circlePointMaxZoom;
		smallPointMinZoom = layer.smallPointMinZoom;
		smallPointMaxZoom = layer.smallPointMaxZoom;
		bigPointMinZoom = layer.bigPointMinZoom;
		bigPointMaxZoom = layer.bigPointMaxZoom;
	}

	public void setCirclePointZoomBounds(int min, int max) {
		circlePointMinZoom = min;
		circlePointMaxZoom = max;
	}

	public void setSmallPointZoomBounds(int min, int max) {
		smallPointMinZoom = min;
		smallPointMaxZoom = max;
	}

	public void setBigPointZoomBounds(int min, int max) {
		bigPointMinZoom = min;
		bigPointMaxZoom = max;
	}

	public int getCirclePointMinZoom() {
		return circlePointMinZoom;
	}

	public int getCirclePointMaxZoom() {
		return circlePointMaxZoom;
	}

	public int getSmallPointMinZoom() {
		return smallPointMinZoom;
	}

	public int getSmallPointMaxZoom() {
		return smallPointMaxZoom;
	}

	public int getBigPointMinZoom() {
		return bigPointMinZoom;
	}

	public int getBigPointMaxZoom() {
		return bigPointMaxZoom;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeString(id);
		out.writeString(name);
		out.writeFloat(zOrder);
		out.writeTypedList(new ArrayList<>(points.values()));
		out.writeByte((byte) (imagePoints ? 1 : 0));
		out.writeInt(circlePointMinZoom);
		out.writeInt(circlePointMaxZoom);
		out.writeInt(smallPointMinZoom);
		out.writeInt(smallPointMaxZoom);
		out.writeInt(bigPointMinZoom);
		out.writeInt(bigPointMaxZoom);
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
		imagePoints = in.readByte() == 1;
		circlePointMinZoom = in.readInt();
		circlePointMaxZoom = in.readInt();
		smallPointMinZoom = in.readInt();
		smallPointMaxZoom = in.readInt();
		bigPointMinZoom = in.readInt();
		bigPointMaxZoom = in.readInt();
	}

	public int describeContents() {
		return 0;
	}
}
