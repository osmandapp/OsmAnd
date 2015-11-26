package net.osmand.binary;


import gnu.trove.map.hash.TIntObjectHashMap;

import java.text.MessageFormat;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.util.Algorithms;

public class RouteDataObject {
	/*private */static final int RESTRICTION_SHIFT = 3;
	/*private */static final int RESTRICTION_MASK = 7;
	
	public final RouteRegion region;
	// all these arrays supposed to be immutable!
	// These fields accessible from C++
	public int[] types;
	public int[] pointsX;
	public int[] pointsY;
	public long[] restrictions;
	public int[][] pointTypes;
	public String[][] pointNames;
	public int[][] pointNameTypes;
	public long id;
	public TIntObjectHashMap<String> names;
	public final static float NONE_MAX_SPEED = 40f;
	public int[] nameIds;
	
	public RouteDataObject(RouteRegion region) {
		this.region = region;
	}
	
	public RouteDataObject(RouteRegion region, int[] nameIds, String[] nameValues) {
		this.region = region;
		this.nameIds = nameIds;
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
		this.names = copy.names;
		this.restrictions = copy.restrictions;
		this.pointTypes = copy.pointTypes;
		this.pointNames = copy.pointNames;
		this.pointNameTypes = copy.pointNameTypes;
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
	
	
	public String getName(String lang){
		if(names != null ) {
			if(Algorithms.isEmpty(lang)) {
				return names.get(region.nameTypeRule);
			}
			int[] kt = names.keys();
			for(int i = 0 ; i < kt.length; i++) {
				int k = kt[i];
				if(region.routeEncodingRules.size() > k) {
					if(("name:"+lang).equals(region.routeEncodingRules.get(k).getTag())) {
						return names.get(k);
					}
				}
			}
			return names.get(region.nameTypeRule);
		}
		return null;
	}
	
	public int[] getNameIds() {
		return nameIds;
	}
	
	public TIntObjectHashMap<String> getNames() {
		return names;
	}
	
	public String getRef(){
		if (names != null) {
			String ref = names.get(region.destinationRefTypeRule);
			if (ref != null) {
				return ref;
			}
			return names.get(region.refTypeRule);
		}
		return null;
	}

	public String getDestinationName(String lang){
		if(names != null) {
			if(Algorithms.isEmpty(lang)) {
				return names.get(region.destinationTypeRule);
			}
			int[] kt = names.keys();
			for(int i = 0 ; i < kt.length; i++) {
				int k = kt[i];
				if(region.routeEncodingRules.size() > k) {
					if(("destination:"+lang).equals(region.routeEncodingRules.get(k).getTag())) {
						return names.get(k);
					}
				}
 				
			}
			return names.get(region.destinationTypeRule);
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
	
	public String[] getPointNames(int ind) {
		if (pointNames == null || ind >= pointNames.length) {
			return null;
		}
		return pointNames[ind];
	}
	
	public int[] getPointNameTypes(int ind) {
		if (pointNameTypes == null || ind >= pointNameTypes.length) {
			return null;
		}
		return pointNameTypes[ind];
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
	
	public float getMaximumSpeed(boolean direction){
		int sz = types.length;
        float maxSpeed = 0;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if(r.isForward() != 0) {
				if((r.isForward() > 1) != direction) {
					continue;
				}
			}
            float mx = r.maxSpeed();
            if (mx > 0) {
                maxSpeed = mx;
                // conditional has priority
                if(r.conditional()) {
                    break;
                }
            }
		}
		return maxSpeed ;
	}
	
	
	public static float parseSpeed(String v, float def) {
		if(v.equals("none")) {
			return RouteDataObject.NONE_MAX_SPEED;
		} else {
			int i = Algorithms.findFirstNumberEndIndex(v);
			if (i > 0) {
				float f = Float.parseFloat(v.substring(0, i));
				f /= 3.6; // km/h -> m/s
				if (v.contains("mph")) {
					f *= 1.6;
				}
				return f;
			}
		}
		return def;
	}
	
	public static float parseLength(String v, float def) {
		float f = 0;
		// 14'10" 14 - inches, 10 feet
		int i = Algorithms.findFirstNumberEndIndex(v);
		if (i > 0) {
			f += Float.parseFloat(v.substring(0, i));
			String pref = v.substring(i, v.length()).trim();
			float add = 0;
			for(int ik = 0; ik < pref.length(); ik++) {
				if(Algorithms.isDigit(pref.charAt(ik)) || pref.charAt(ik) == '.' || pref.charAt(ik) == '-') {
					int first = Algorithms.findFirstNumberEndIndex(pref.substring(ik));
					if(first != -1) {
						add = parseLength(pref.substring(ik), 0);
						pref = pref.substring(0, ik);
					}
					break;
				}
			}
			if(pref.contains("km")) {
				f *= 1000;  
			}
			if(pref.contains("\"")) {
				f *= 0.0254; 
			} else if (pref.contains("\'") || pref.contains("ft")) {
				// foot to meters
				f *= 0.3048;
			}
			return f + add;
		}
		return def;
	}
	
	public static float parseWeightInTon(String v, float def) {
		int i = Algorithms.findFirstNumberEndIndex(v);
		if (i > 0) {
			float f = Float.parseFloat(v.substring(0, i));
			if (v.contains("\"") || v.contains("lbs")) {
				// lbs -> kg -> ton
				f = (f * 0.4535f) / 1000f;
			}
			return f;
		}
		return def;
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
	
	public boolean tunnel(){
		int sz = types.length;
		for(int i=0; i<sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if(r.getTag().equals("tunnel") && r.getValue().equals("yes")) {
				return true;
			}
			if(r.getTag().equals("layer") && r.getValue().equals("-1")) {
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
		return getHighway(types, region);
	}

	public String getValue(String tag) {
		for (int i = 0; i < types.length; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if (r.getTag().equals(tag)) {
				return r.getValue();
			}
		}
		return null;
	}
	
	public static String getHighway(int[] types, RouteRegion region) {
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
		// same goes to C++
		// Victor : the problem to put more than 5 meters that BinaryRoutePlanner will treat
		// 2 consequent Turn Right as UT and here 2 points will have same turn angle
		// So it should be fix in both places
		return directionRoute(startPoint, plus, 5);
	}
	
	public double distance(int startPoint, int endPoint) {
		if(startPoint > endPoint) {
			int k = endPoint;
			endPoint = startPoint;
			startPoint = k;
		}
		double d = 0;
		for(int k = startPoint; k < endPoint && k < getPointsLength() -1; k++) {
			int x = getPoint31XTile(k);
			int y = getPoint31YTile(k);
			int kx = getPoint31XTile(k + 1);
			int ky = getPoint31YTile(k + 1);
			d += simplifyDistance(kx, ky, x, y);
			
		}
		return d;
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
			total += simplifyDistance(x, y, px, py);
		} while (total < dist);
		return -Math.atan2( x - px, y - py );
	}

	private double simplifyDistance(int x, int y, int px, int py) {
		return Math.abs(px - x) * 0.011d + Math.abs(py - y) * 0.01863d;
	}
	
	private static void assertTrueLength(String vl, float exp){
		float dest = parseLength(vl, 0);
		if(exp != dest) {
			System.err.println("FAIL " + vl + " " + dest);
		} else {
			System.out.println("OK " + vl);
		}
	}
	
	public static void main(String[] args) {
		assertTrueLength("10 km", 10000);
		assertTrueLength("0.01 km", 10);
		assertTrueLength("0.01 km 10 m", 20);
		assertTrueLength("10 m", 10);
		assertTrueLength("10m", 10);
		assertTrueLength("3.4 m", 3.4f);
		assertTrueLength("14'10\"", 4.5212f);
		assertTrueLength("14.5'", 4.4196f);
		assertTrueLength("15ft", 4.572f);
	}
	
	
	@Override
	public String toString() {
		String name = getName();
		String rf = getRef();
		return MessageFormat.format("Road id {0} name {1} ref {2}", getId()+"", name == null ? "" : name, rf == null ? "" : rf);
	}
}
