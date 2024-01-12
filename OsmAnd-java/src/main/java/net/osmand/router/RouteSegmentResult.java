package net.osmand.router;


import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataBundle;
import net.osmand.binary.RouteDataObject;
import net.osmand.binary.StringExternalizable;
import net.osmand.data.LatLon;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import gnu.trove.map.hash.TIntObjectHashMap;

import static net.osmand.gpx.GPXUtilities.RouteSegment.START_TRKPT_IDX_ATTR;


public class RouteSegmentResult implements StringExternalizable<RouteDataBundle> {
	// this should be bigger (50-80m) but tests need to be fixed first
	public static final float DIST_BEARING_DETECT = 15;
	
	public static final float DIST_BEARING_DETECT_UNMATCHED = 50;
	
	private RouteDataObject object;
	private int startPointIndex;
	private int endPointIndex;
	private List<RouteSegmentResult>[] attachedRoutes;
	private RouteSegmentResult[][] preAttachedRoutes;
	private float segmentTime;
	private float routingTime;
	private float speed;
	private float distance;
	private String[] description = null;
	// this make not possible to make turns in between segment result for now
	private TurnType turnType;
	private boolean leftside = false;

	// Evaluates street name that the route follows after turn within specified distance.
	// It is useful to find names for short segments on intersections
	private static final float DIST_TO_SEEK_STREET_NAME = 150;

	// Evaluates destination for exit from one road to another on the followed highway link within specified distance.
	// In most cases using on "cloverleaf" junctions
	private static final float DIST_TO_SEEK_DEST = 1000;

	public RouteSegmentResult(RouteDataObject object) {
		this.object = object;
	}

	public RouteSegmentResult(RouteDataObject object, boolean leftside) {
		this.object = object;
		this.leftside = leftside;
	}

	public RouteSegmentResult(RouteDataObject object, int startPointIndex, int endPointIndex) {
		this.object = object;
		this.startPointIndex = startPointIndex;
		this.endPointIndex = endPointIndex;
		updateCapacity();
	}

	public RouteSegmentResult(RouteDataObject object, int startPointIndex, int endPointIndex,
	                          RouteSegmentResult[][] preAttachedRoutes, float segmentTime,
	                          float routingTime, float speed, float distance, TurnType turnType) {
		this.object = object;
		this.startPointIndex = startPointIndex;
		this.endPointIndex = endPointIndex;
		this.preAttachedRoutes = preAttachedRoutes;
		this.segmentTime = segmentTime;
		this.routingTime = routingTime;
		this.speed = speed;
		this.distance = distance;
		this.turnType = turnType;
		updateCapacity();
	}

	public void collectTypes(RouteDataResources resources) {
		Map<RouteTypeRule, Integer> rules = resources.getRules();
		if (object.types != null) {
			collectRules(rules, object.types);
		}
		if (object.pointTypes != null) {
			int start = Math.min(startPointIndex, endPointIndex);
			int end = Math.max(startPointIndex, endPointIndex);
			for (int i = start; i <= end && i < object.pointTypes.length; i++) {
				int[] types = object.pointTypes[i];
				if (types != null) {
					collectRules(rules, types);
				}
			}
		}
	}

	public void collectNames(RouteDataResources resources) {
		Map<RouteTypeRule, Integer> rules = resources.getRules();
		RouteRegion region = object.region;
		if (region.getNameTypeRule() != -1) {
			RouteTypeRule r = region.quickGetEncodingRule(region.getNameTypeRule());
			if (!rules.containsKey(r)) {
				rules.put(r, rules.size());
			}
		}
		if (region.getRefTypeRule() != -1) {
			RouteTypeRule r = region.quickGetEncodingRule(region.getRefTypeRule());
			if (!rules.containsKey(r)) {
				rules.put(r, rules.size());
			}
		}
		if (object.nameIds != null) {
			for (int nameId : object.nameIds) {
				String name = object.names.get(nameId);
				String tag = region.quickGetEncodingRule(nameId).getTag();
				RouteTypeRule r = new RouteTypeRule(tag, name);
				if (!rules.containsKey(r)) {
					rules.put(r, rules.size());
				}
			}
		}
		if (object.pointNameTypes != null) {
			int start = Math.min(startPointIndex, endPointIndex);
			int end = Math.min(Math.max(startPointIndex, endPointIndex) + 1, object.pointNameTypes.length);
			for (int i = start; i < end; i++) {
				int[] types = object.pointNameTypes[i];
				if (types != null) {
					for (int type : types) {
						RouteTypeRule r = region.quickGetEncodingRule(type);
						if (!rules.containsKey(r)) {
							rules.put(r, rules.size());
						}
					}
				}
			}
		}
	}

	private void collectRules(Map<RouteTypeRule, Integer> rules, int[] types) {
		RouteRegion region = object.region;
		for (int type : types) {
			RouteTypeRule rule = region.quickGetEncodingRule(type);
			String tag = rule.getTag();
			if (tag.equals("osmand_ele_start") || tag.equals("osmand_ele_end")
					|| tag.equals("osmand_ele_asc") || tag.equals("osmand_ele_desc"))
			{
				continue;
			}
			if (!rules.containsKey(rule)) {
				rules.put(rule, rules.size());
			}
		}
	}

	private int[] convertTypes(int[] types, Map<RouteTypeRule, Integer> rules) {
		if (types == null || types.length == 0) {
			return null;
		}
		List<Integer> arr = new ArrayList<>();
		for (int i = 0; i < types.length; i++) {
			int type = types[i];
			RouteTypeRule rule = object.region.quickGetEncodingRule(type);
			Integer ruleId = rules.get(rule);
			if (ruleId != null) {
				arr.add(ruleId);
			}
		}
		int[] res = new int[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			res[i] = arr.get(i);
		}
		return res;
	}

	private int[][] convertTypes(int[][] types, Map<RouteTypeRule, Integer> rules) {
		if (types == null || types.length == 0) {
			return null;
		}
		int[][] res = new int[types.length][];
		for (int i = 0; i < types.length; i++) {
			int[] typesArr = types[i];
			if (typesArr != null) {
				res[i] = convertTypes(typesArr, rules);
			}
		}
		return res;
	}

	private int[] convertNameIds(int[] nameIds, Map<RouteTypeRule, Integer> rules) {
		if (nameIds == null || nameIds.length == 0) {
			return null;
		}
		int[] res = new int[nameIds.length];
		for (int i = 0; i < nameIds.length; i++) {
			int nameId = nameIds[i];
			String name = object.names.get(nameId);
			String tag = object.region.quickGetEncodingRule(nameId).getTag();
			RouteTypeRule rule = new RouteTypeRule(tag, name);
			Integer ruleId = rules.get(rule);
			if (ruleId == null) {
				throw new IllegalArgumentException("Cannot find collected rule: " + rule.toString());
			}
			res[i] = ruleId;
		}
		return res;
	}

	private int[][] convertPointNames(int[][] nameTypes, String[][] pointNames, Map<RouteTypeRule, Integer> rules) {
		if (nameTypes == null || nameTypes.length == 0) {
			return null;
		}
		int[][] res = new int[nameTypes.length][];
		for (int i = 0; i < nameTypes.length; i++) {
			int[] types = nameTypes[i];
			if (types != null) {
				int[] arr = new int[types.length];
				for (int k = 0; k < types.length; k++) {
					int type = types[k];
					String tag = object.region.quickGetEncodingRule(type).getTag();
					String name = pointNames[i][k];
					RouteTypeRule rule = new RouteTypeRule(tag, name);
					Integer ruleId = rules.get(rule);
					if (ruleId == null) {
						ruleId = rules.size();
						rules.put(rule, ruleId);
					}
					arr[k] = ruleId;
				}
				res[i] = arr;
			}
		}
		return res;
	}

	public void fillNames(RouteDataResources resources) {
		if (object.nameIds != null && object.nameIds.length > 0) {
			RouteRegion region = object.region;
			int nameTypeRule = region.getNameTypeRule();
			int refTypeRule = region.getRefTypeRule();
			object.names = new TIntObjectHashMap<>();
			for (int nameId : object.nameIds) {
				RouteTypeRule rule = region.quickGetEncodingRule(nameId);
				if (rule != null) {
					if (nameTypeRule != -1 && "name".equals(rule.getTag())) {
						nameId = nameTypeRule;
					} else if (refTypeRule != -1 && "ref".equals(rule.getTag())) {
						nameId = refTypeRule;
					}
					object.names.put(nameId, rule.getValue());
				}
			}
		}
		String[][] pointNames = null;
		int[][] pointNameTypes = null;
		int[][] pointNamesArr = resources.getPointNamesMap().get(object);
		if (pointNamesArr != null) {
			pointNames = new String[pointNamesArr.length][];
			pointNameTypes = new int[pointNamesArr.length][];
			for (int i = 0; i < pointNamesArr.length; i++) {
				int[] namesIds = pointNamesArr[i];
				if (namesIds != null) {
					pointNames[i] = new String[namesIds.length];
					pointNameTypes[i] = new int[namesIds.length];
					for (int k = 0; k < namesIds.length; k++) {
						int id = namesIds[k];
						RouteTypeRule r = object.region.quickGetEncodingRule(id);
						if (r != null) {
							pointNames[i][k] = r.getValue();
							int nameType = object.region.searchRouteEncodingRule(r.getTag(), null);
							if (nameType != -1) {
								pointNameTypes[i][k] = nameType;
							}
						}
					}
				}
			}
		}
		object.pointNames = pointNames;
		object.pointNameTypes = pointNameTypes;
	}

	@Override
	public void writeToBundle(RouteDataBundle bundle) {
		RouteDataResources resources = bundle.getResources();
		Map<RouteTypeRule, Integer> rules = resources.getRules();

		boolean reversed = endPointIndex < startPointIndex;
		int length = Math.abs(endPointIndex - startPointIndex) + 1;

		bundle.putInt("length", length);
		bundle.putInt(START_TRKPT_IDX_ATTR, resources.getCurrentSegmentStartLocationIndex());
		bundle.putFloat("segmentTime", segmentTime, 2);
		bundle.putFloat("speed", speed, 2);
		if (turnType != null) {
			bundle.putString("turnType", turnType.toXmlString());
			if (turnType.isSkipToSpeak()) {
				bundle.putBoolean("skipTurn", turnType.isSkipToSpeak());
			}
			if (turnType.getTurnAngle() != 0) {
				bundle.putFloat("turnAngle", turnType.getTurnAngle(), 2);
			}
			int[] turnLanes = turnType.getLanes();
			if (turnLanes != null && turnLanes.length > 0) {
				bundle.putString("turnLanes", TurnType.lanesToString(turnLanes));
			}
		}
		bundle.putLong("id", object.id >> 6); // OsmAnd ID to OSM ID
		bundle.putArray("types", convertTypes(object.types, rules));

		int start = Math.min(startPointIndex, endPointIndex);
		int end = Math.max(startPointIndex, endPointIndex) + 1;
		if (object.pointTypes != null && start < object.pointTypes.length) {
			int[][] types = Arrays.copyOfRange(object.pointTypes, start, Math.min(end, object.pointTypes.length));
			if (reversed) {
				CollectionUtils.reverseArray(types);
			}
			bundle.putArray("pointTypes", convertTypes(types, rules));
		}
		if (object.nameIds != null) {
			bundle.putArray("names", convertNameIds(object.nameIds, rules));
		}
		if (object.pointNameTypes != null && start < object.pointNameTypes.length && object.pointNames != null) {
			int[][] types = Arrays.copyOfRange(object.pointNameTypes, start, Math.min(end, object.pointNameTypes.length));
			String[][] names = Arrays.copyOfRange(object.pointNames, start, Math.min(end, object.pointNames.length));
			if (reversed) {
				CollectionUtils.reverseArray(types);
				CollectionUtils.reverseArray(names);
			}
			bundle.putArray("pointNames", convertPointNames(types, names, rules));
		}

		resources.updateNextSegmentStartLocation(length);
	}

	@Override
	public void readFromBundle(RouteDataBundle bundle) {
		int length = bundle.getInt("length", 0);
		boolean plus = length >= 0;
		length = Math.abs(length);
		startPointIndex = plus ? 0 : length - 1;
		endPointIndex = plus ? length - 1 : 0;
		segmentTime = bundle.getFloat("segmentTime", segmentTime);
		speed = bundle.getFloat("speed", speed);
		String turnTypeStr = bundle.getString("turnType", null);
		if (!Algorithms.isEmpty(turnTypeStr)) {
			turnType = TurnType.fromString(turnTypeStr, leftside);
			turnType.setSkipToSpeak(bundle.getBoolean("skipTurn", turnType.isSkipToSpeak()));
			turnType.setTurnAngle(bundle.getFloat("turnAngle", turnType.getTurnAngle()));
			int[] turnLanes = TurnType.lanesFromString(bundle.getString("turnLanes", null));
			turnType.setLanes(turnLanes);
		}
		object.id = bundle.getLong("id", object.id) << 6;
		object.types = bundle.getIntArray("types", null);
		object.pointTypes = bundle.getIntIntArray("pointTypes", null);
		object.nameIds = bundle.getIntArray("names", null);
		int[][] pointNames = bundle.getIntIntArray("pointNames", null);
		if (pointNames != null) {
			bundle.getResources().getPointNamesMap().put(object, pointNames);
		}

		RouteDataResources resources = bundle.getResources();
		object.pointsX = new int[length];
		object.pointsY = new int[length];
		object.heightDistanceArray = new float[length * 2];
		int index = plus ? 0 : length - 1;
		float distance = 0;
		Location prevLocation = null;
		for (int i = 0; i < length; i++) {
			Location location = resources.getCurrentSegmentLocation(index);
			double dist = 0;
			if (prevLocation != null) {
				dist = MapUtils.getDistance(prevLocation.getLatitude(), prevLocation.getLongitude(), location.getLatitude(), location.getLongitude());
				distance += dist;
			}
			prevLocation = location;
			object.pointsX[i] = MapUtils.get31TileNumberX(location.getLongitude());
			object.pointsY[i] = MapUtils.get31TileNumberY(location.getLatitude());
			if (location.hasAltitude() && object.heightDistanceArray.length > 0) {
				object.heightDistanceArray[i * 2] = (float) dist;
				object.heightDistanceArray[i * 2 + 1] = (float) location.getAltitude();
			} else {
				object.heightDistanceArray = new float[0];
			}
			if (plus) {
				index++;
			} else {
				index--;
			}
		}
		this.distance = distance;

		resources.updateNextSegmentStartLocation(length);
	}

	public float[] getHeightValues() {
		float[] pf = object.calculateHeightArray();
		if(pf == null || pf.length == 0) {
			return new float[0];
		}
		boolean reverse = startPointIndex > endPointIndex;
		int st = Math.min(startPointIndex, endPointIndex);
		int end = Math.max(startPointIndex, endPointIndex);
		float[] res = new float[(end - st + 1) * 2];
		if (reverse) {
			for (int k = 1; k <= res.length / 2; k++) {
				int ind = (2 * (end--));
				if (ind < pf.length && k < res.length / 2) {
					res[2 * k] = pf[ind];
				}
				if (ind < pf.length) {
					res[2 * (k - 1) + 1] = pf[ind + 1];
				}
			}
		} else {
			for (int k = 0; k < res.length / 2; k++) {
				int ind = (2 * (st + k));
				if (k > 0 && ind < pf.length) {
					res[2 * k] = pf[ind];
				}
				if (ind < pf.length) {
					res[2 * k + 1] = pf[ind + 1];
				}
			}
		}

		return res;
	}
	

	@SuppressWarnings("unchecked")
	private void updateCapacity() {
		int capacity = Math.abs(endPointIndex - startPointIndex) + 1;
		List<RouteSegmentResult>[] old = this.attachedRoutes;
		this.attachedRoutes = new List[capacity];
		if (old != null) {
			System.arraycopy(old, 0, this.attachedRoutes, 0, Math.min(old.length, this.attachedRoutes.length));
		}
	}
	
	public void attachRoute(int roadIndex, RouteSegmentResult r){
		if(r.getObject().isRoadDeleted()) {
			return;
		}
		int st = Math.abs(roadIndex - startPointIndex);
		if(attachedRoutes[st] == null) {
			attachedRoutes[st] = new ArrayList<RouteSegmentResult>();
		}
		attachedRoutes[st].add(r);
	}

	public void copyPreattachedRoutes(RouteSegmentResult toCopy, int shift) {
		if (toCopy.preAttachedRoutes != null) {
			int l = toCopy.preAttachedRoutes.length - shift;
			preAttachedRoutes = new RouteSegmentResult[l][];
			System.arraycopy(toCopy.preAttachedRoutes, shift, preAttachedRoutes, 0, l);
		}
	}

	public void clearAttachedRoutes() {
		attachedRoutes = null;
	}

	public void clearPreattachedRoutes() {
		preAttachedRoutes = null;
	}

	public RouteSegmentResult[] getPreAttachedRoutes(int routeInd) {
		int st = Math.abs(routeInd - startPointIndex);
		if (preAttachedRoutes != null && st < preAttachedRoutes.length) {
			return preAttachedRoutes[st];
		}
		return null;
	}

	public List<RouteSegmentResult> getAttachedRoutes(int routeInd) {
		int st = Math.abs(routeInd - startPointIndex);
		List<RouteSegmentResult> list = attachedRoutes[st];
		if (list == null) {
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
		return getBearingBegin(startPointIndex, distance > 0 && distance < DIST_BEARING_DETECT ? distance : DIST_BEARING_DETECT);
	}
	
	public float getBearingBegin(int point, float dist) {
		return getBearing(point, true, dist);
	}
	
	public float getBearingEnd() {
		return getBearingEnd(endPointIndex, distance > 0 && distance < DIST_BEARING_DETECT ? distance : DIST_BEARING_DETECT);
	}
	
	public float getBearingEnd(int point, float dist) {
		return getBearing(point, false, dist);
	}
	
	public float getBearing(int point, boolean begin, float dist) {
		if (begin) {
			return (float) (object.directionRoute(point, startPointIndex < endPointIndex, dist) / Math.PI * 180);
		} else {
			double dr = object.directionRoute(point, startPointIndex > endPointIndex, dist);
			return (float) (MapUtils.alignAngleDifference(dr - Math.PI) / Math.PI * 180);
		}
	}
	
	public float getDistance(int point, boolean plus) {
		return (float) (plus ? object.distance(point, endPointIndex) : object.distance(startPointIndex, point));
	}
	
	public void setSegmentTime(float segmentTime) {
		this.segmentTime = segmentTime;
	}
	
	public void setRoutingTime(float routingTime) {
		this.routingTime = routingTime;
	}
	
	public float getRoutingTime() {
		return routingTime;
	}
	
	public LatLon getStartPoint() {
		return convertPoint(object, startPointIndex);
	}
	
	public int getStartPointIndex() {
		return startPointIndex;
	}
	
	public int getStartPointX() {
		return object.getPoint31XTile(startPointIndex);
	}
	
	public int getStartPointY() {
		return object.getPoint31YTile(startPointIndex);
	}

	public int getEndPointIndex() {
		return endPointIndex;
	}
	
	public int getEndPointX() {
		return object.getPoint31XTile(endPointIndex);
	}
	
	public int getEndPointY() {
		return object.getPoint31YTile(endPointIndex);
	}

	public LatLon getPoint(int i) {
		return convertPoint(object, i);
	}

	public LatLon getEndPoint() {
		return convertPoint(object, endPointIndex);
	}

	public boolean continuesBeyondRouteSegment(RouteSegmentResult segment) {
		boolean commonX = object.pointsX[startPointIndex] == segment.object.pointsX[segment.endPointIndex];
		boolean commonY = object.pointsY[startPointIndex] == segment.object.pointsY[segment.endPointIndex];
		return commonX && commonY;
	}

	public boolean isForwardDirection() {
		return endPointIndex - startPointIndex > 0;
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
	
	public String getDescription(boolean full) {
		if(description == null || description.length == 0) {
			return "";
		}
		if(full && description.length > 1) {
			return description[1];
		}
		return description[0];
	}
	
	public void setDescription(String shortD, String full) {
		this.description = new String[] {shortD, full};
	}
	
	public void clearDescription() {
		this.description = null;
	}
	
	public void setObject(RouteDataObject r) {
		this.object = r;
	}

	@Override
	public String toString() {
		return object.toString() + ": " + startPointIndex + "-" + endPointIndex;
	}

	public String getDestinationName(String lang, boolean transliterate, List<RouteSegmentResult> list, int routeInd) {
		String dnRef = getObject().getDestinationRef(lang, transliterate, isForwardDirection());
		String destinationName = getObject().getDestinationName(lang, transliterate, isForwardDirection());
		if (Algorithms.isEmpty(destinationName)) {
			// try to get destination name from following segments
			float distanceFromTurn = getDistance();
			for (int n = routeInd + 1; n + 1 < list.size(); n++) {
				RouteSegmentResult s1 = list.get(n);
				String s1DnRef = s1.getObject().getDestinationRef(lang,	transliterate, isForwardDirection());
				boolean dnRefIsEqual = !Algorithms.isEmpty(s1DnRef) && !Algorithms.isEmpty(dnRef) && s1DnRef.equals(dnRef);
				boolean isMotorwayLink = "motorway_link".equals(s1.getObject().getHighway());
				if (distanceFromTurn < DIST_TO_SEEK_DEST && (isMotorwayLink || dnRefIsEqual)
						&& Algorithms.isEmpty(destinationName)) {
					destinationName = s1.getObject().getDestinationName(lang, transliterate, s1.isForwardDirection());
				}
				distanceFromTurn += s1.getDistance();
				if (distanceFromTurn > DIST_TO_SEEK_DEST || !Algorithms.isEmpty(destinationName)) {
					break;
				}
			}
		}
		if (!Algorithms.isEmpty(dnRef) && !Algorithms.isEmpty(destinationName)) {
			destinationName = dnRef + ", " + destinationName;
		} else if (!Algorithms.isEmpty(dnRef) && Algorithms.isEmpty(destinationName)) {
			destinationName = dnRef;
		}
		return destinationName;
	}

	public String getStreetName(String lang, boolean transliterate, List<RouteSegmentResult> list, int routeInd) {
		String streetName = getObject().getName(lang, transliterate);
		if (Algorithms.isEmpty(streetName)) {
			// try to get street name from following segments
			float distanceFromTurn = getDistance();
			boolean hasNewTurn = false;
			for (int n = routeInd + 1; n + 1 < list.size(); n++) {
				RouteSegmentResult s1 = list.get(n);
				if (s1.getTurnType() != null) {
					hasNewTurn = true;
				}
				if (!hasNewTurn && distanceFromTurn < DIST_TO_SEEK_STREET_NAME
						&& Algorithms.isEmpty(streetName)) {
					streetName = s1.getObject().getName(lang, transliterate);
				}
				distanceFromTurn += s1.getDistance();
				if (distanceFromTurn > DIST_TO_SEEK_STREET_NAME || !Algorithms.isEmpty(streetName)) {
					break;
				}
			}
		}
		return streetName;
	}

	public String getRef(String lang, boolean transliterate) {
		return getObject().getRef(lang, transliterate, isForwardDirection());
	}

	public RouteDataObject getObjectWithShield(List<RouteSegmentResult> list, int routeInd) {
		RouteDataObject rdo = null;
		boolean isNextShieldFound = getObject().hasNameTagStartsWith("road_ref");
		for (int ind = routeInd; ind < list.size() && !isNextShieldFound; ind++) {
			if (list.get(ind).getTurnType() != null) {
				isNextShieldFound = true;
			} else {
				RouteDataObject obj = list.get(ind).getObject();
				if (obj.hasNameTagStartsWith("road_ref")) {
					rdo = obj;
					isNextShieldFound = true;
				}
			}
		}
		return rdo;
	}
}