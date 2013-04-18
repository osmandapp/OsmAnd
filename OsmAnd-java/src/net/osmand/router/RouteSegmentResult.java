package net.osmand.router;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;


public class RouteSegmentResult {
	private final RouteDataObject object;
	private int startPointIndex;
	private int endPointIndex;
	private List<RouteSegmentResult>[] attachedRoutes;
	private RouteSegmentResult[][] preAttachedRoutes;
	private float segmentTime;
	private float speed;
	private float distance;
	private String description = "";
	// this make not possible to make turns in between segment result for now
	private TurnType turnType;
	
	
	public RouteSegmentResult(RouteDataObject object, int startPointIndex, int endPointIndex) {
		this.object = object;
		this.startPointIndex = startPointIndex;
		this.endPointIndex = endPointIndex;
		updateCapacity();
	}
	

	@SuppressWarnings("unchecked")
	private void updateCapacity() {
		int capacity = Math.abs(endPointIndex - startPointIndex) + 1;
		List<RouteSegmentResult>[] old = this.attachedRoutes ;
		this.attachedRoutes = new List[capacity];
		if(old != null){
			System.arraycopy(old, 0, this.attachedRoutes, 0, Math.min(old.length, this.attachedRoutes.length));
		}
	}
	
	public void attachRoute(int roadIndex, RouteSegmentResult r){
		int st = Math.abs(roadIndex - startPointIndex);
		if(attachedRoutes[st] == null) {
			attachedRoutes[st] = new ArrayList<RouteSegmentResult>();
		}
		attachedRoutes[st].add(r);
	}
	
	public void copyPreattachedRoutes(RouteSegmentResult toCopy, int shift) {
		if(toCopy.preAttachedRoutes != null) {
			int l = toCopy.preAttachedRoutes.length - shift;
			preAttachedRoutes = new RouteSegmentResult[l][];
			System.arraycopy(toCopy.preAttachedRoutes, shift, preAttachedRoutes, 0, l);
		}
	}
	
	public RouteSegmentResult[] getPreAttachedRoutes(int routeInd) {
		int st = Math.abs(routeInd - startPointIndex);
		if(preAttachedRoutes != null && st < preAttachedRoutes.length) {
			return preAttachedRoutes[st];
		}
		return null;
	}
	
	public List<RouteSegmentResult> getAttachedRoutes(int routeInd) {
		int st = Math.abs(routeInd - startPointIndex);
		List<RouteSegmentResult> list = attachedRoutes[st];
		if(list == null) {
			return Collections.emptyList();
		}
		return list;
	}
	
	public TurnType getTurnType() {
		return turnType;
	}
	
	public void setTurnType(TurnType turnType) {
		this.turnType = turnType;
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
	
	public float getBearing(int point, boolean plus) {
		return (float) (object.directionRoute(point, plus) / Math.PI * 180);
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
	
	public void setEndPointIndex(int endPointIndex) {
		this.endPointIndex = endPointIndex;
		updateCapacity();
	}
	
	public void setStartPointIndex(int startPointIndex) {
		this.startPointIndex = startPointIndex;
		updateCapacity();
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