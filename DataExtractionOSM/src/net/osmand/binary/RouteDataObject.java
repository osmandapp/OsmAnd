package net.osmand.binary;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;

public class RouteDataObject {
	/*private */static final int RESTRICTION_SHIFT = 3;
	/*private */static final int RESTRICTION_MASK = 7;
	
	public final RouteRegion region;
	// all these arrays supposed to be immutable!
	// These feilds accessible from C++
	public int[] types;
	public int[] pointsX;
	public int[] pointsY;
	public long[] restrictions;
	public int[][] pointTypes;
	public long id;
	public TIntObjectHashMap<String> names;

	public RouteDataObject(RouteRegion region) {
		this.region = region;
	}
	
	public RouteDataObject(RouteRegion region, int[] nameIds, String[] nameValues) {
		this.region = region;
		if (nameIds.length > 0) {
			names = new TIntObjectHashMap<String>();
		}
		for (int i = 0; i < nameIds.length; i++) {
			names.put(nameIds[i], nameValues[i]);
		}
	}

	public RouteDataObject(RouteDataObject copy) {
		this.region = copy.region;
		this.pointsX = copy.pointsX;
		this.pointsY = copy.pointsY;
		this.types = copy.types;
		this.restrictions = copy.restrictions;
		this.pointTypes = copy.pointTypes;
		this.id = copy.id;
	}

	public long getId() {
		return id;
	}
	
	public String getName(){
		if(names != null ) {
			return names.get(region.nameTypeRule);
		}
		return null;
	}
	
	public String getRef(){
		if(names != null ) {
			return names.get(region.refTypeRule);
		}
		return null;
	}

	public int getPoint31XTile(int i) {
		return pointsX[i];
	}

	public int getPoint31YTile(int i) {
		return pointsY[i];
	}

	public int getPointsLength() {
		return pointsX.length;
	}

	public int getRestrictionLength() {
		return restrictions == null ? 0 : restrictions.length;
	}

	public int getRestrictionType(int i) {
		return (int) (restrictions[i] & RESTRICTION_MASK);
	}

	public long getRestrictionId(int i) {
		return restrictions[i] >> RESTRICTION_SHIFT;
	}

	public void insert(int pos, int x31, int y31) {
		int[] opointsX = pointsX;
		int[] opointsY = pointsY;
		int[][] opointTypes = pointTypes;
		pointsX = new int[pointsX.length + 1];
		pointsY = new int[pointsY.length + 1];
		boolean insTypes = this.pointTypes != null && this.pointTypes.length > pos;
		if (insTypes) {
			pointTypes = new int[opointTypes.length + 1][];
		}
		int i = 0;
		for (; i < pos; i++) {
			pointsX[i] = opointsX[i];
			pointsY[i] = opointsY[i];
			if (insTypes) {
				pointTypes[i] = opointTypes[i];
			}
		}
		pointsX[i] = x31;
		pointsY[i] = y31;
		if (insTypes) {
			pointTypes[i] = null;
		}
		for (i = i + 1; i < pointsX.length; i++) {
			pointsX[i] = opointsX[i - 1];
			pointsY[i] = opointsY[i - 1];
			if (insTypes && i < pointTypes.length) {
				pointTypes[i] = opointTypes[i - 1];
			}
		}

	}

	public int[] getPointTypes(int ind) {
		if (pointTypes == null || ind >= pointTypes.length) {
			return null;
		}
		return pointTypes[ind];
	}
	
	public int[] getTypes() {
		return types;
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
	
	public boolean loop(){
		return pointsX[0] == pointsX[pointsX.length - 1] && pointsY[0] == pointsY[pointsY.length - 1] ; 
	}
	
	public boolean roundabout(){
		int sz = types.length;
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if(r.roundabout()) {
				return true;
			} else if(r.onewayDirection() != 0 && loop()) {
				return true;
			}
		}
		return false;
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
	
	public int getLanes() {
		int sz = types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			int ln = r.lanes();
			if (ln > 0) {
				return ln;
			}
		}
		return -1;
	}
	
	public double directionRoute(int startPoint, boolean plus) {
		// Victor : the problem to put more than 5 meters that BinaryRoutePlanner will treat
		// 2 consequent Turn Right as UT and here 2 points will have same turn angle
		// So it should be fix in both places
		return directionRoute(startPoint, plus, 5);
	}

	// Gives route direction of EAST degrees from NORTH ]-PI, PI]
	public double directionRoute(int startPoint, boolean plus, float dist) {
		int x = getPoint31XTile(startPoint);
		int y = getPoint31YTile(startPoint);
		int nx = startPoint;
		int px = x;
		int py = y;
		double total = 0;
		do {
			if (plus) {
				nx++;
				if (nx >= getPointsLength()) {
					break;
				}
			} else {
				nx--;
				if (nx < 0) {
					break;
				}
			}
			px = getPoint31XTile(nx);
			py = getPoint31YTile(nx);
			// translate into meters
			total += Math.abs(px - x) * 0.011d + Math.abs(py - y) * 0.01863d;
		} while (total < dist);
		return -Math.atan2( x - px, y - py );
	}
	
	@Override
	public String toString() {
		String name = getName();
		String rf = getRef();
		return String.format("Road id %s name %s ref %s", getId()+"", name == null ? "" : name, rf == null ? "" : rf);
	}
}