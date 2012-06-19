package net.osmand.router;

import net.osmand.binary.RouteDataObject;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;


public class RouteSegmentResult {
	private RouteDataObject object;
	private int startPointIndex;
	private int endPointIndex;
	private float segmentTime;
	private float speed;
	private float distance;
	
	public RouteSegmentResult(RouteDataObject object, int startPointIndex, int endPointIndex) {
		this.object = object;
		this.startPointIndex = startPointIndex;
		this.endPointIndex = endPointIndex;
		
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
	
	
	
}