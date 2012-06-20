package net.osmand.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;


public class RouteSegmentResult {
	private final RouteDataObject object;
	private final int startPointIndex;
	private final int endPointIndex;
	private final List<RouteSegmentResult>[] attachedRoutes;
	private float segmentTime;
	private float speed;
	private float distance;
	private String description = "";
	
	@SuppressWarnings("unchecked")
	public RouteSegmentResult(RouteDataObject object, int startPointIndex, int endPointIndex) {
		this.object = object;
		this.startPointIndex = startPointIndex;
		this.endPointIndex = endPointIndex;
		int capacity = Math.abs(endPointIndex - startPointIndex) + 1;
		this.attachedRoutes = new List[capacity];
	}
	
	public void attachRoute(int roadIndex, RouteSegmentResult r){
		int st = startPointIndex < endPointIndex ? startPointIndex : endPointIndex;
		if(attachedRoutes[roadIndex - st] == null) {
			attachedRoutes[roadIndex - st] = new ArrayList<RouteSegmentResult>();
		}
		attachedRoutes[roadIndex - st].add(r);
	}
	
	public List<RouteSegmentResult> getAttachedRoutes(int routeInd) {
		int st = startPointIndex < endPointIndex ? startPointIndex : endPointIndex;
		List<RouteSegmentResult> list = attachedRoutes[routeInd - st];
		if(list == null) {
			return Collections.emptyList();
		}
		return list;
	}
	
	
	public RouteDataObject getObject() {
		return object;
	}
	
	public float getSegmentTime() {
		return segmentTime;
	}
	
	public float getBearingBegin() {
		return (float) (object.directionRoute(startPointIndex, startPointIndex < endPointIndex) / Math.PI * 180);
	}
	
	public float getBearingEnd() {
		return (float) (MapUtils.alignAngleDifference(object.directionRoute(endPointIndex, startPointIndex > endPointIndex) - Math.PI) / Math.PI * 180);
	}
	
	public void setSegmentTime(float segmentTime) {
		this.segmentTime = segmentTime;
	}
	
	public LatLon getStartPoint() {
		return convertPoint(object, startPointIndex);
	}
	
	public int getStartPointIndex() {
		return startPointIndex;
	}
	
	public int getEndPointIndex() {
		return endPointIndex;
	}
	
	public LatLon getPoint(int i) {
		return convertPoint(object, i);
	}
	
	public LatLon getEndPoint() {
		return convertPoint(object, endPointIndex);
	}
	
	private LatLon convertPoint(RouteDataObject o, int ind){
		return new LatLon(MapUtils.get31LatitudeY(o.getPoint31YTile(ind)), MapUtils.get31LongitudeX(o.getPoint31XTile(ind)));
	}

	public void setSegmentSpeed(float speed) {
		this.speed = speed;
	}
	
	public float getSegmentSpeed() {
		return speed;
	}
	
	public float getDistance() {
		return distance;
	}
	
	public void setDistance(float distance) {
		this.distance = distance;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
}