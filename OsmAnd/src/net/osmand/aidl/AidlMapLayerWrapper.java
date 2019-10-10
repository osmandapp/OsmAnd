package net.osmand.aidl;

import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.point.AMapPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AidlMapLayerWrapper {

	private String id;
	private String name;
	private float zOrder;
	private Map<String, AidlMapPointWrapper> points = new ConcurrentHashMap<>();

	private boolean imagePoints;
	private int circlePointMinZoom;
	private int circlePointMaxZoom;
	private int smallPointMinZoom;
	private int smallPointMaxZoom;
	private int bigPointMinZoom;
	private int bigPointMaxZoom;

	public AidlMapLayerWrapper(AMapLayer aMapLayer) {
		id = aMapLayer.getId();
		name = aMapLayer.getName();
		zOrder = aMapLayer.getZOrder();
		imagePoints = aMapLayer.isImagePoints();
		circlePointMinZoom = aMapLayer.getCirclePointMinZoom();
		circlePointMaxZoom = aMapLayer.getCirclePointMaxZoom();
		smallPointMinZoom = aMapLayer.getSmallPointMinZoom();
		smallPointMaxZoom = aMapLayer.getSmallPointMaxZoom();
		bigPointMinZoom = aMapLayer.getBigPointMinZoom();
		bigPointMaxZoom = aMapLayer.getBigPointMaxZoom();

		List<AMapPoint> pointList = aMapLayer.getPoints();
		if (pointList != null) {
			for (AMapPoint p : pointList) {
				this.points.put(p.getId(), new AidlMapPointWrapper(p));
			}
		}
	}

	public AidlMapLayerWrapper(net.osmand.aidlapi.maplayer.AMapLayer aMapLayer) {
		id = aMapLayer.getId();
		name = aMapLayer.getName();
		zOrder = aMapLayer.getZOrder();
		imagePoints = aMapLayer.isImagePoints();
		circlePointMinZoom = aMapLayer.getCirclePointMinZoom();
		circlePointMaxZoom = aMapLayer.getCirclePointMaxZoom();
		smallPointMinZoom = aMapLayer.getSmallPointMinZoom();
		smallPointMaxZoom = aMapLayer.getSmallPointMaxZoom();
		bigPointMinZoom = aMapLayer.getBigPointMinZoom();
		bigPointMaxZoom = aMapLayer.getBigPointMaxZoom();

		List<net.osmand.aidlapi.maplayer.point.AMapPoint> pointList = aMapLayer.getPoints();
		if (pointList != null) {
			for (net.osmand.aidlapi.maplayer.point.AMapPoint p : pointList) {
				this.points.put(p.getId(), new AidlMapPointWrapper(p));
			}
		}
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public float getZOrder() {
		return zOrder;
	}

	public List<AidlMapPointWrapper> getPoints() {
		return new ArrayList<>(points.values());
	}

	public AidlMapPointWrapper getPoint(String pointId) {
		return points.get(pointId);
	}

	public boolean hasPoint(String pointId) {
		return points.containsKey(pointId);
	}

	public void putPoint(AidlMapPointWrapper point) {
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

	public void copyZoomBounds(AidlMapLayerWrapper layer) {
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
}