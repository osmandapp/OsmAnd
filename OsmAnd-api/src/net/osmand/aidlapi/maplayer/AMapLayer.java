package net.osmand.aidlapi.maplayer;

import android.os.Bundle;
import android.os.Parcel;

import net.osmand.aidlapi.AidlParams;
import net.osmand.aidlapi.maplayer.point.AMapPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AMapLayer extends AidlParams {

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

	public static final Creator<AMapLayer> CREATOR = new Creator<AMapLayer>() {
		@Override
		public AMapLayer createFromParcel(Parcel in) {
			return new AMapLayer(in);
		}

		@Override
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

	@Override
	public void writeToBundle(Bundle bundle) {
		bundle.putString("id", id);
		bundle.putString("name", name);
		bundle.putFloat("zOrder", zOrder);
		bundle.putParcelableArrayList("points", new ArrayList<>(points.values()));
		bundle.putBoolean("imagePoints", imagePoints);
		bundle.putInt("circlePointMinZoom", circlePointMinZoom);
		bundle.putInt("circlePointMaxZoom", circlePointMaxZoom);
		bundle.putInt("smallPointMinZoom", smallPointMinZoom);
		bundle.putInt("smallPointMaxZoom", smallPointMaxZoom);
		bundle.putInt("bigPointMinZoom", bigPointMinZoom);
		bundle.putInt("bigPointMaxZoom", bigPointMaxZoom);
	}

	@Override
	protected void readFromBundle(Bundle bundle) {
		id = bundle.getString("id");
		name = bundle.getString("name");
		zOrder = bundle.getFloat("zOrder");
		imagePoints = bundle.getBoolean("imagePoints");
		circlePointMinZoom = bundle.getInt("circlePointMinZoom");
		circlePointMaxZoom = bundle.getInt("circlePointMaxZoom");
		smallPointMinZoom = bundle.getInt("smallPointMinZoom");
		smallPointMaxZoom = bundle.getInt("smallPointMaxZoom");
		bigPointMinZoom = bundle.getInt("bigPointMinZoom");
		bigPointMaxZoom = bundle.getInt("bigPointMaxZoom");

		bundle.setClassLoader(AMapPoint.class.getClassLoader());
		List<AMapPoint> pointList = bundle.getParcelableArrayList("points");
		if (pointList != null) {
			for (AMapPoint p : pointList) {
				this.points.put(p.getId(), p);
			}
		}
	}
}