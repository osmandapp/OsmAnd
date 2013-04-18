package net.osmand.binary;


import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

public class RouteDataBorderLinePoint extends RouteDataObject {
	// all these arrays supposed to be immutable!
	// These feilds accessible from C++
	public int x;
	public int y;
	public boolean direction;
	
	// used in context calculation
	public float distanceToStartPoint;
	public float distanceToEndPoint;

	public RouteDataBorderLinePoint(RouteRegion region) {
		super(region);
	}
	
	public float getMaximumSpeed(){
		int sz = types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			float maxSpeed = r.maxSpeed();
			if (maxSpeed > 0) {
				return maxSpeed;
			}
		}
		return 0;
	}
	
	public int getOneway() {
		int sz = types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if (r.onewayDirection() != 0) {
				return r.onewayDirection();
			} else if (r.roundabout()) {
				return 1;
			}
		}
		return 0;
	}
	
	public String getRoute() {
		int sz = types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if ("route".equals(r.getTag())) {
				return r.getValue();
			}
		}
		return null;
	}

	public String getHighway() {
		String highway = null;
		int sz = types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			highway = r.highwayRoad();
			if (highway != null) {
				break;
			}
		}
		return highway;
	}
	
	@Override
	public String toString() {
		return "Border line " + id;
	}
}