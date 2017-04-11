package net.osmand.binary;


import gnu.trove.map.hash.TIntObjectHashMap;

import java.text.MessageFormat;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.Location;
import net.sf.junidecode.Junidecode;


public class RouteDataObject {
	/*private */static final int RESTRICTION_SHIFT = 3;
	/*private */static final int RESTRICTION_MASK = 7;
	public static int HEIGHT_UNDEFINED = -80000;
	
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
	// mixed array [0, height, cumulative_distance height, cumulative_distance, height, ...] - length is length(points)*2
	public float[] heightDistanceArray = null;
	
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
	
	public float[] calculateHeightArray() {
		if(heightDistanceArray != null) {
			return heightDistanceArray;
		}
		int startHeight = Algorithms.parseIntSilently(getValue("osmand_ele_start"), HEIGHT_UNDEFINED);
		int endHeight = Algorithms.parseIntSilently(getValue("osmand_ele_end"), startHeight);
		if(startHeight == HEIGHT_UNDEFINED) {
			heightDistanceArray = new float[0];
			return heightDistanceArray;
		}
		
		heightDistanceArray = new float[2*getPointsLength()]; 
		double plon = 0;
		double plat = 0;
		float prevHeight = startHeight;
		for(int k = 0; k < getPointsLength(); k++) {
			double lon = MapUtils.get31LongitudeX(getPoint31XTile(k));
			double lat = MapUtils.get31LatitudeY(getPoint31YTile(k));
			if(k > 0) {
				double dd = MapUtils.getDistance(plat, plon, lat, lon);
				float height = HEIGHT_UNDEFINED;
				if(k == getPointsLength() - 1) {
					height = endHeight;
				} else {
					int[] tps = getPointTypes(k);
					if (tps != null) {
						for (int id : tps) {
							RouteTypeRule rt = region.quickGetEncodingRule(id);
							if (rt.getTag().equals("osmand_ele_asc")) {
								height = (prevHeight + Float.parseFloat(rt.getValue()));
								break;
							} else if (rt.getTag().equals("osmand_ele_desc")) {
								height = (prevHeight - Float.parseFloat(rt.getValue()));
								break;
							}
						}
					}
				}
				heightDistanceArray[2*k] = (float) dd;
				heightDistanceArray[2*k+1] = height;
				if(height != HEIGHT_UNDEFINED) {
					// interpolate undefined
					double totalDistance = dd;
					int startUndefined = k;
					while(startUndefined - 1 >= 0 && heightDistanceArray[2*(startUndefined - 1)+1] == HEIGHT_UNDEFINED) {
						startUndefined --;
						totalDistance += heightDistanceArray[2*(startUndefined)];
					}
					if(totalDistance > 0) {
						double angle = (height - prevHeight) / totalDistance;
						for(int j = startUndefined; j < k; j++) {
							heightDistanceArray[2*j+1] = (float) ((heightDistanceArray[2*j] * angle) + heightDistanceArray[2*j-1]);
						}
					}
					prevHeight = height;
				}
				
			} else {
				heightDistanceArray[0] = 0;
				heightDistanceArray[1] = startHeight;
			}
			plat = lat;
			plon = lon;
		}
		return heightDistanceArray;
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
		return getName(lang, false);
	}
	
	public String getName(String lang, boolean transliterate){
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
			String nmDef = names.get(region.nameTypeRule);
			if(transliterate && nmDef != null && nmDef.length() > 0) {
				return Junidecode.unidecode(nmDef);
			}
			return nmDef;
		}
		return null;
	}
	
	public int[] getNameIds() {
		return nameIds;
	}
	
	public TIntObjectHashMap<String> getNames() {
		return names;
	}

	public String getRef(String lang, boolean transliterate, boolean direction) {
		//if (getDestinationRef(direction) != null) {
		//	return getDestinationRef(direction);
		//}
		if (names != null) {
			if(Algorithms.isEmpty(lang)) {
				return names.get(region.refTypeRule);
			}
			int[] kt = names.keys();
			for(int i = 0 ; i < kt.length; i++) {
				int k = kt[i];
				if(region.routeEncodingRules.size() > k) {
					if(("ref:"+lang).equals(region.routeEncodingRules.get(k).getTag())) {
						return names.get(k);
					}
				}
			}
			String refDefault = names.get(region.refTypeRule);
			if(transliterate && refDefault != null && refDefault.length() > 0) {
				return Junidecode.unidecode(refDefault);
			}
			return refDefault;
		}
		return null;
	}

	public String getDestinationRef(boolean direction) {
		if (names != null) {
			int[] kt = names.keys();
			String refTag = (direction == true) ? "destination:ref:forward" : "destination:ref:backward";
			String refTagDefault = "destination:ref";
			String refDefault = null;

			for(int i = 0 ; i < kt.length; i++) {
				int k = kt[i];
				if(region.routeEncodingRules.size() > k) {
					if(refTag.equals(region.routeEncodingRules.get(k).getTag())) {
						return names.get(k);
					}
					if(refTagDefault.equals(region.routeEncodingRules.get(k).getTag())) {
						refDefault = names.get(k);
					}
				}
			}
			if (refDefault != null) {
				return refDefault;
			}
			//return names.get(region.refTypeRule);
		}
		return null;
	}

	public String getDestinationName(String lang, boolean transliterate, boolean direction){
		//Issue #3289: Treat destination:ref like a destination, not like a ref
		String destRef = ((getDestinationRef(direction) == null) || getDestinationRef(direction).equals(getRef(lang, transliterate, direction))) ? "" : getDestinationRef(direction);
		String destRef1 = ("".equals(destRef)) ? "" : destRef + ", ";

		if(names != null) {
			int[] kt = names.keys();

			// Issue #3181: Parse destination keys in this order:
			//              destination:lang:XX:forward/backward
			//              destination:forward/backward
			//              destination:lang:XX
			//              destination

			String destinationTagLangFB = "destination:lang:XX";
			if(!Algorithms.isEmpty(lang)) {
				destinationTagLangFB = (direction == true) ? "destination:lang:" + lang + ":forward" : "destination:lang:" + lang + ":backward";
			}
			String destinationTagFB = (direction == true) ? "destination:forward" : "destination:backward";
			String destinationTagLang = "destination:lang:XX";
			if(!Algorithms.isEmpty(lang)) {
				destinationTagLang = "destination:lang:" + lang;
			}
			String destinationTagDefault = "destination";
			String destinationDefault = null;

			for(int i = 0 ; i < kt.length; i++) {
				int k = kt[i];
				if(region.routeEncodingRules.size() > k) {
					if(!Algorithms.isEmpty(lang) && destinationTagLangFB.equals(region.routeEncodingRules.get(k).getTag())) {
						return destRef1 + ((transliterate) ? Junidecode.unidecode(names.get(k)) : names.get(k));
					}
					if(destinationTagFB.equals(region.routeEncodingRules.get(k).getTag())) {
						return destRef1 + ((transliterate) ? Junidecode.unidecode(names.get(k)) : names.get(k));
					}
					if(!Algorithms.isEmpty(lang) && destinationTagLang.equals(region.routeEncodingRules.get(k).getTag())) {
						return destRef1 + ((transliterate) ? Junidecode.unidecode(names.get(k)) : names.get(k));
					}
					if(destinationTagDefault.equals(region.routeEncodingRules.get(k).getTag())) {
						destinationDefault = names.get(k);
					}
				}
			}
			if(destinationDefault != null) {
				return destRef1 + ((transliterate) ? Junidecode.unidecode(destinationDefault) : destinationDefault);
			}
		}
		return "".equals(destRef) ? null : destRef;
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
	
	public boolean hasPointTypes() {
		return pointTypes != null;
	}
	
	public boolean hasPointNames() {
		return pointNames != null;
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
				if((r.isForward() == 1) != direction) {
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
			if(pref.contains("\"") ||  pref.contains("in")) {
				f *= 0.0254; 
			} else if (pref.contains("\'") || pref.contains("ft") || pref.contains("feet")) {
				// foot to meters
				f *= 0.3048;
			} else if (pref.contains("cm")) {
				f *= 0.01;
			} else if (pref.contains("mile")) {
				f *= 1609.34f;
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

	public boolean hasPrivateAccess() {
		int sz = types.length;
		for (int i = 0; i < sz; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if ("motorcar".equals(r.getTag())
					|| "motor_vehicle".equals(r.getTag())
					|| "vehicle".equals(r.getTag())
					|| "access".equals(r.getTag())) {
				if (r.getValue().equals("private")) {
					return true;
				}
			}
		}
		return false;
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

	public boolean bearingVsRouteDirection(Location loc) {
		boolean direction = true;
		if(loc != null && loc.hasBearing()) {
			double diff = MapUtils.alignAngleDifference(directionRoute(0, true) - loc.getBearing() / 180f * Math.PI);
			direction = Math.abs(diff) < Math.PI / 2f;
		}
		return direction;
	}

	public boolean isStopDirectionOpposite(boolean direction) {
		for(int i=0; i<types.length; i++) {
			RouteTypeRule r = region.quickGetEncodingRule(types[i]);
			if (r.getTag().equals("highway") && r.getValue().equals("stop")) {
				for (int j=0; j<types.length; j++) {
					if (direction = true && r.getTag().equals("direction") && r.getValue().equals("backward")) {
						return true;
					}
					if (direction = false && r.getTag().equals("direction") && r.getValue().equals("forward")) {
						return true;
					}
				}
			}
		}
		return false;
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
 		assertTrueLength("3.40 m", 3.4f);
 		assertTrueLength("10 m 10m", 20);
  		assertTrueLength("14'10\"", 4.5212f);
  		assertTrueLength("14.5'", 4.4196f);
 		assertTrueLength("14.5 ft", 4.4196f);
 		assertTrueLength("14'0\"", 4.2672f);
  		assertTrueLength("15ft", 4.572f);
 		assertTrueLength("15 ft 1 in", 4.5974f);
 		assertTrueLength("4.1 metres", 4.1f);
 		assertTrueLength("14'0''", 4.2672f);
 		assertTrueLength("14 feet", 4.2672f);
 		assertTrueLength("14 mile", 22530.76f);
 		assertTrueLength("14 cm", 0.14f);
 		
// 		float badValue = -1;
// 		assertTrueLength("none", badValue);
// 		assertTrueLength("m 4.1", badValue);
// 		assertTrueLength("1F4 m", badValue);
	}

	public String coordinates() {
		StringBuilder b = new StringBuilder();
		b.append(" lat/lon : ");
		for (int i = 0; i < getPointsLength(); i++) {
			float x = (float) MapUtils.get31LongitudeX(getPoint31XTile(i));
			float y = (float) MapUtils.get31LatitudeY(getPoint31YTile(i));
			b.append(y).append(" / ").append(x).append(" , ");
		}
		return b.toString();
	}

	@Override
	public String toString() {
		String name = getName();
		String rf = getRef("", false, true);
		return MessageFormat.format("Road id {0} name {1} ref {2}", (getId() / 64) + "", name == null ? "" : name,
				rf == null ? "" : rf);
	}
}
