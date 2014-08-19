package net.osmand.plus.routing;

import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.Location;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.data.DataTileManager;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.R;
import net.osmand.plus.routing.AlarmInfo.AlarmInfoType;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.content.Context;

public class RouteCalculationResult {
	private static double distanceClosestToIntermediate = 400;
	// could not be null and immodifiable!
	private final List<Location> locations;
	private final List<RouteDirectionInfo> directions;
	private final List<RouteSegmentResult> segments;
	private final List<AlarmInfo> alarmInfo;
	private final String errorMessage;
	private final int[] listDistance;
	private final int[] intermediatePoints;
	private final float routingTime;
	
	protected int cacheCurrentTextDirectionInfo = -1;
	protected List<RouteDirectionInfo> cacheAgreggatedDirections;
	protected List<LocationPoint> locationPoints = new ArrayList<LocationPoint>();

	// Note always currentRoute > get(currentDirectionInfo).routeOffset, 
	//         but currentRoute <= get(currentDirectionInfo+1).routeOffset 
	protected int currentDirectionInfo = 0;
	protected int currentRoute = 0;
	protected int nextIntermediate = 0;
	protected int nextAlarmInfo = 0;
	protected int currentWaypointGPX = 0;
	protected int lastWaypointGPX = 0;

	public RouteCalculationResult(String errorMessage) {
		this.errorMessage = errorMessage;
		this.routingTime = 0;
		this.intermediatePoints = new int[0];
		this.locations = new ArrayList<Location>();
		this.segments = new ArrayList<RouteSegmentResult>();
		this.listDistance = new int[0];
		this.directions = new ArrayList<RouteDirectionInfo>();
		this.alarmInfo = new ArrayList<AlarmInfo>();
	}
	
	public RouteCalculationResult(List<Location> list, List<RouteDirectionInfo> directions, RouteCalculationParams params) {
		this.routingTime = 0;
		this.errorMessage = null;
		this.intermediatePoints = new int[params.intermediates == null ? 0 : params.intermediates.size()];
		List<Location> locations = list == null ? new ArrayList<Location>() : new ArrayList<Location>(list);
		List<RouteDirectionInfo> localDirections = directions == null? new ArrayList<RouteDirectionInfo>() : new ArrayList<RouteDirectionInfo>(directions);
		if (!locations.isEmpty()) {
			checkForDuplicatePoints(locations, localDirections);
		}
		boolean addMissingTurns = true;
		if(addMissingTurns) {
			removeUnnecessaryGoAhead(localDirections);
			addMissingTurnsToRoute(locations, localDirections, params.start,params.end, 
					params.mode, params.ctx, params.leftSide);
			// if there is no closest points to start - add it
			introduceFirstPointAndLastPoint(locations, localDirections, null, params.start, params.end);
		}
		
		this.locations = Collections.unmodifiableList(locations);
		this.segments = new ArrayList<RouteSegmentResult>();
		this.listDistance = new int[locations.size()];
		updateListDistanceTime(this.listDistance, this.locations);
		this.alarmInfo = new ArrayList<AlarmInfo>();
		calculateIntermediateIndexes(params.ctx, this.locations, params.intermediates, localDirections, this.intermediatePoints);
		this.directions = Collections.unmodifiableList(localDirections);
		updateDirectionsTime(this.directions, this.listDistance);
	}

	public RouteCalculationResult(List<RouteSegmentResult> list, Location start, LatLon end, List<LatLon> intermediates,  
			Context ctx, boolean leftSide, float routingTime, List<LocationPoint> waypoints) {
		this.routingTime = routingTime;
		if(waypoints != null) {
			this.locationPoints.addAll(waypoints);
		}
		List<RouteDirectionInfo> computeDirections = new ArrayList<RouteDirectionInfo>();
		this.errorMessage = null;
		this.intermediatePoints = new int[intermediates == null ? 0 : intermediates.size()];
		List<Location> locations = new ArrayList<Location>();
		ArrayList<AlarmInfo> alarms = new ArrayList<AlarmInfo>();
		List<RouteSegmentResult> segments = convertVectorResult(computeDirections, locations, list, alarms, ctx);
		introduceFirstPointAndLastPoint(locations, computeDirections, segments, start, end);
		
		this.locations = Collections.unmodifiableList(locations);
		this.segments = Collections.unmodifiableList(segments);
		this.listDistance = new int[locations.size()];
		calculateIntermediateIndexes(ctx, this.locations, intermediates, computeDirections, this.intermediatePoints);
		updateListDistanceTime(this.listDistance, this.locations);
		
		this.directions = Collections.unmodifiableList(computeDirections);
		updateDirectionsTime(this.directions, this.listDistance);
		this.alarmInfo = Collections.unmodifiableList(alarms);
	}

	public List<LocationPoint> getLocationPoints() {
		return locationPoints;
	}

	private static void calculateIntermediateIndexes(Context ctx, List<Location> locations,
			List<LatLon> intermediates, List<RouteDirectionInfo> localDirections, int[] intermediatePoints) {
		if(intermediates != null && localDirections != null) {
			int[] interLocations = new int[intermediates.size()];
			int currentIntermediate = 0;
			int currentLocation = 0;
			double distanceThreshold = 25;
			double prevDistance = distanceThreshold * 4;
			while((currentIntermediate < intermediates.size() || prevDistance > distanceThreshold)
				&& currentLocation < locations.size()){
				if(currentIntermediate < intermediates.size() &&
						getDistanceToLocation(locations, intermediates.get(currentIntermediate), currentLocation) < distanceClosestToIntermediate) {
					prevDistance = getDistanceToLocation(locations, intermediates.get(currentIntermediate), currentLocation);
					interLocations[currentIntermediate] = currentLocation;
					currentIntermediate++;
				} else if(currentIntermediate > 0 && prevDistance > distanceThreshold && 
						getDistanceToLocation(locations, intermediates.get(currentIntermediate - 1), 
						currentLocation) < prevDistance) {
					prevDistance = getDistanceToLocation(locations, intermediates.get(currentIntermediate - 1), currentLocation);
					interLocations[currentIntermediate - 1] = currentLocation;
				}
				currentLocation ++;
			}
			int currentDirection = 0;
			currentIntermediate = 0;
			while(currentIntermediate < intermediates.size() && currentDirection < localDirections.size()){
				int locationIndex = localDirections.get(currentDirection).routePointOffset ;
				if (locationIndex >= interLocations[currentIntermediate]) {
					// split directions
					if (locationIndex > interLocations[currentIntermediate]
							&& getDistanceToLocation(locations, intermediates.get(currentIntermediate), locationIndex) > 50) {
						RouteDirectionInfo toSplit = localDirections.get(currentDirection);
						RouteDirectionInfo info = new RouteDirectionInfo(localDirections.get(currentDirection).getAverageSpeed(), TurnType.sraight());
						info.setRef(toSplit.getRef());
						info.setStreetName(toSplit.getStreetName());
						info.setDestinationName(toSplit.getDestinationName());
						info.routePointOffset = interLocations[currentIntermediate];
						info.setDescriptionRoute(ctx.getString(R.string.route_head));//; //$NON-NLS-1$
						localDirections.add(currentDirection, info);
					}
					intermediatePoints[currentIntermediate] = currentDirection;
					currentIntermediate++;
				}
				currentDirection ++;
			}
		}
	}

	private static double getDistanceToLocation(List<Location> locations, LatLon p, int currentLocation) {
		return MapUtils.getDistance(p, 
				locations.get(currentLocation).getLatitude(), locations.get(currentLocation).getLongitude());
	}

	private static void attachAlarmInfo(List<AlarmInfo> alarms, RouteSegmentResult res, int intId, int locInd) {
		int[] pointTypes = res.getObject().getPointTypes(intId);
		RouteRegion reg = res.getObject().region;
		if (pointTypes != null) {
			for (int r = 0; r < pointTypes.length; r++) {
				RouteTypeRule typeRule = reg.quickGetEncodingRule(pointTypes[r]);
				AlarmInfo info = AlarmInfo.createAlarmInfo(typeRule, locInd);
				if(info != null) {
					alarms.add(info);
				}
			}
		}
	}
	
	public List<RouteSegmentResult> getOriginalRoute() {
		if (segments.size() == 0) {
			return null;
		}
		List<RouteSegmentResult> list = new ArrayList<RouteSegmentResult>();
		list.add(segments.get(0));
		for (int i = 1; i < segments.size(); i++) {
			if (segments.get(i - 1) != segments.get(i)) {
				list.add(segments.get(i));
			}
		}
		return list;
	}

	/**
	 * PREPARATION 
	 */
	private static List<RouteSegmentResult> convertVectorResult(List<RouteDirectionInfo> directions, List<Location> locations, List<RouteSegmentResult> list,
			List<AlarmInfo> alarms, Context ctx) {
		float prevDirectionTime = 0;
		float prevDirectionDistance = 0;
		List<RouteSegmentResult> segmentsToPopulate = new ArrayList<RouteSegmentResult>();
		for (int routeInd = 0; routeInd < list.size(); routeInd++) {
			RouteSegmentResult s = list.get(routeInd);
			boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
			int i = s.getStartPointIndex();
			int prevLocationSize = locations.size();
			while (true) {
				Location n = new Location(""); //$NON-NLS-1$
				LatLon point = s.getPoint(i);
				n.setLatitude(point.getLatitude());
				n.setLongitude(point.getLongitude());
				if (i == s.getEndPointIndex() && routeInd != list.size() - 1) {
					break;
				}
				locations.add(n);
				attachAlarmInfo(alarms, s, i, locations.size());
				segmentsToPopulate.add(s);
				if (i == s.getEndPointIndex() ) {
					break;
				}
				if (plus) {
					i++;
				} else {
					i--;
				}
			}
			TurnType turn = s.getTurnType();

			if(turn != null) {
				RouteDirectionInfo info = new RouteDirectionInfo(s.getSegmentSpeed(), turn);
                if (routeInd  < list.size()) {
                    int lind = routeInd;
                    if(turn.isRoundAbout()) {
                    	// take next name for roundabout (not roundabout name)
                    	while(lind < list.size() -1 && list.get(lind).getObject().roundabout()) {
                    		lind++;
                    	}
                    }
                    RouteSegmentResult next = list.get(lind);
                    info.setRef(next.getObject().getRef());
                    info.setStreetName(next.getObject().getName());
                    info.setDestinationName(next.getObject().getDestinationName());
                }

                String description = toString(turn, ctx) + " " + RoutingHelper.formatStreetName(info.getStreetName(),
						info.getRef(), info.getDestinationName());
				info.setDescriptionRoute(description);
				info.routePointOffset = prevLocationSize;
				if(directions.size() > 0 && prevDirectionTime > 0 && prevDirectionDistance > 0) {
					RouteDirectionInfo prev = directions.get(directions.size() - 1);
					prev.setAverageSpeed(prevDirectionDistance / prevDirectionTime);
					prevDirectionDistance = 0;
					prevDirectionTime = 0;
				}
				directions.add(info);
			}
			prevDirectionDistance += s.getDistance();
			prevDirectionTime += s.getSegmentTime();
		}
		if(directions.size() > 0 && prevDirectionTime > 0 && prevDirectionDistance > 0) {
			RouteDirectionInfo prev = directions.get(directions.size() - 1);
			prev.setAverageSpeed(prevDirectionDistance / prevDirectionTime);
		}
		return segmentsToPopulate;
	}
	
	protected static void addMissingTurnsToRoute(List<Location> locations, 
			List<RouteDirectionInfo> originalDirections, Location start, LatLon end, ApplicationMode mode, Context ctx,
			boolean leftSide){
		if(locations.isEmpty()){
			return;
		}
		// speed m/s
		float speed = mode.getDefaultSpeed(); 
		int minDistanceForTurn = mode.getMinDistanceForTurn();
		List<RouteDirectionInfo> computeDirections = new ArrayList<RouteDirectionInfo>();
		
		int[] listDistance = new int[locations.size()];
		listDistance[locations.size() - 1] = 0;
		for (int i = locations.size() - 1; i > 0; i--) {
			listDistance[i - 1] = (int) locations.get(i - 1).distanceTo(locations.get(i));
			listDistance[i - 1] += listDistance[i];
		}
		
		int previousLocation = 0;
		int prevBearingLocation = 0;
		RouteDirectionInfo previousInfo = new RouteDirectionInfo(speed, TurnType.sraight());
		previousInfo.routePointOffset = 0;
		previousInfo.setDescriptionRoute(ctx.getString( R.string.route_head));
		computeDirections.add(previousInfo);
		
		int distForTurn = 0;
		float previousBearing = 0;
		int startTurnPoint = 0;
		
		
		for (int i = 1; i < locations.size() - 1; i++) {
			
			Location next = locations.get(i + 1);
			Location current = locations.get(i);
			float bearing = current.bearingTo(next);
			// try to get close to current location if possible
			while(prevBearingLocation < i - 1){
				if(locations.get(prevBearingLocation + 1).distanceTo(current) > 70){
					prevBearingLocation ++;
				} else {
					break;
				}
			}
			
			if(distForTurn == 0){
				// measure only after turn
				previousBearing = locations.get(prevBearingLocation).bearingTo(current);
				startTurnPoint = i;
			}
			
			TurnType type = null;
			String description = null;
			float delta = previousBearing - bearing;
			while(delta < 0){
				delta += 360;
			}
			while(delta > 360){
				delta -= 360;
			}
			
			distForTurn += locations.get(i).distanceTo(locations.get(i + 1)); 
			if (i < locations.size() - 1 &&  distForTurn < minDistanceForTurn) {
				// For very smooth turn we try to accumulate whole distance
				// simply skip that turn needed for situation
				// 1) if you are going to have U-turn - not 2 left turns
				// 2) if there is a small gap between roads (turn right and after 4m next turn left) - so the direction head
				continue;
			}
			
			
			if(delta > 45 && delta < 315){
				
				if(delta < 60){
					type = TurnType.valueOf(TurnType.TSLL, leftSide);
					description = ctx.getString( R.string.route_tsll);
				} else if(delta < 120){
					type = TurnType.valueOf(TurnType.TL, leftSide);
					description = ctx.getString( R.string.route_tl);
				} else if(delta < 150){
					type = TurnType.valueOf(TurnType.TSHL, leftSide);
					description = ctx.getString( R.string.route_tshl);
				} else if(delta < 210){
					type = TurnType.valueOf(TurnType.TU, leftSide);
					description = ctx.getString( R.string.route_tu);
				} else if(delta < 240){
					description = ctx.getString( R.string.route_tshr);
					type = TurnType.valueOf(TurnType.TSHR, leftSide);
				} else if(delta < 300){
					description = ctx.getString( R.string.route_tr);
					type = TurnType.valueOf(TurnType.TR, leftSide);
				} else {
					description = ctx.getString( R.string.route_tslr);
					type = TurnType.valueOf(TurnType.TSLR, leftSide);
				}
				
				// calculate for previousRoute 
				previousInfo.distance = listDistance[previousLocation]- listDistance[i];
				type.setTurnAngle(360 - delta);
				previousInfo = new RouteDirectionInfo(speed, type);
				previousInfo.setDescriptionRoute(description);
				previousInfo.routePointOffset = startTurnPoint;
				computeDirections.add(previousInfo);
				previousLocation = startTurnPoint;
				prevBearingLocation = i; // for bearing using current location
			}
			// clear dist for turn
			distForTurn = 0;
		} 
			
		previousInfo.distance = listDistance[previousLocation];
		if (originalDirections.isEmpty()) {
			originalDirections.addAll(computeDirections);
		} else {
			int currentDirection = 0;
			// one more
			for (int i = 0; i <= originalDirections.size() && currentDirection < computeDirections.size(); i++) {
				while (currentDirection < computeDirections.size()) {
					int distanceAfter = 0;
					if (i < originalDirections.size()) {
						RouteDirectionInfo resInfo = originalDirections.get(i);
						int r1 = computeDirections.get(currentDirection).routePointOffset;
						int r2 = resInfo.routePointOffset;
						distanceAfter = listDistance[resInfo.routePointOffset];
						float dist = locations.get(r1).distanceTo(locations.get(r2));
						// take into account that move roundabout is special turn that could be very lengthy
						if (dist < 100) {
							// the same turn duplicate
							currentDirection++;
							continue; // while cycle
						} else if (computeDirections.get(currentDirection).routePointOffset > resInfo.routePointOffset) {
							// check it at the next point
							break;
						}
					}

					// add turn because it was missed
					RouteDirectionInfo toAdd = computeDirections.get(currentDirection);

					if (i > 0) {
						// update previous
						RouteDirectionInfo previous = originalDirections.get(i - 1);
						toAdd.setAverageSpeed(previous.getAverageSpeed());
					}
					toAdd.distance = listDistance[toAdd.routePointOffset] - distanceAfter;
					if (i < originalDirections.size()) {
						originalDirections.add(i, toAdd);
					} else {
						originalDirections.add(toAdd);
					}
					i++;
					currentDirection++;
				}
			}

		}
		
		int sum = 0;
		for (int i = originalDirections.size() - 1; i >= 0; i--) {
			originalDirections.get(i).afterLeftTime = sum;
			sum += originalDirections.get(i).getExpectedTime();
		}
	}
	
	
	public static String toString(TurnType type, Context ctx) {
		if(type.isRoundAbout()){
			return ctx.getString(R.string.route_roundabout, type.getExitOut());
		} else if(type.getValue().equals(TurnType.C)) {
			return ctx.getString(R.string.route_head);
		} else if(type.getValue().equals(TurnType.TSLL)) {
			return ctx.getString(R.string.route_tsll);
		} else if(type.getValue().equals(TurnType.TL)) {
			return ctx.getString(R.string.route_tl);
		} else if(type.getValue().equals(TurnType.TSHL)) {
			return ctx.getString(R.string.route_tshl);
		} else if(type.getValue().equals(TurnType.TSLR)) {
			return ctx.getString(R.string.route_tslr);
		} else if(type.getValue().equals(TurnType.TR)) {
			return ctx.getString(R.string.route_tr);
		} else if(type.getValue().equals(TurnType.TSHR)) {
			return ctx.getString(R.string.route_tshr);
		} else if(type.getValue().equals(TurnType.TU)) {
			return ctx.getString(R.string.route_tu);
		} else if(type.getValue().equals(TurnType.TRU)) {
			return ctx.getString(R.string.route_tu);
		} else if(type.getValue().equals(TurnType.KL)) {
			return ctx.getString(R.string.route_kl);
		} else if(type.getValue().equals(TurnType.KR)) {
			return ctx.getString(R.string.route_kr);
		}
		return "";
	}

	public String getErrorMessage() {
		return errorMessage;
	}


	/**
	 * PREPARATION 
	 * Remove unnecessary go straight from CloudMade.
	 * Remove also last direction because it will be added after.
	 */
	private void removeUnnecessaryGoAhead(List<RouteDirectionInfo> directions) {
		if (directions != null && directions.size() > 1) {
			for (int i = 1; i < directions.size();) {
				RouteDirectionInfo r = directions.get(i);
				if (r.getTurnType().getValue().equals(TurnType.C)) {
					RouteDirectionInfo prev = directions.get(i - 1);
					prev.setAverageSpeed((prev.distance + r.distance)
							/ (prev.distance / prev.getAverageSpeed() + r.distance / r.getAverageSpeed()));
					directions.remove(i);
				} else {
					i++;
				}
			}
		}
	}
	
	/**
	 * PREPARATION
	 * 
	 */
	private int[] calculateWaypointIndexes(List<Location> list, DataTileManager<? extends LocationPoint> waypointsTm, List<LocationPoint> waypoints) {
		if(waypointsTm == null || waypointsTm.isEmpty() || list.size() == 0) {
			return new int[0];
		}
		TIntArrayList ls = new TIntArrayList();
		Location loc = list.get(0);
		Location ploc = list.get(0);
		Set<LocationPoint> added = new HashSet<LocationPoint>();
		int prev31x = MapUtils.get31TileNumberX(loc.getLatitude());
		int prev31y = MapUtils.get31TileNumberY(loc.getLongitude());
		for(int j = 1; j < list.size(); j++) {
			loc = list.get(j);
			int t31x = MapUtils.get31TileNumberX(loc.getLatitude());
			int t31y = MapUtils.get31TileNumberY(loc.getLongitude());
			List<? extends LocationPoint> ws = waypointsTm.getObjects(Math.min(prev31x, t31x) - Math.abs(t31x - prev31x) / 4,
					Math.min(prev31y, t31y) - Math.abs(t31y - prev31y) / 4,
					Math.max(prev31x, t31x) + Math.abs(t31x - prev31x) / 4,
					Math.max(prev31y, t31y) + Math.abs(t31y - prev31y) / 4);
			for(LocationPoint w : ws) {
				if (added.contains(w)) {
					double ds = MapUtils.getOrthogonalDistance(w.getLatitude(), w.getLongitude(), ploc.getLatitude(), ploc.getLongitude(), loc.getLatitude(),
							loc.getLongitude());
					if (ds < 160) {
						ls.add(j);
						waypoints.add(w);
						added.add(w);
					}
				}
			}
			
			prev31x = t31x;
			prev31y = t31y;
			ploc = loc;
		}
		return ls.toArray();
	}

	/**
	 * PREPARATION
	 * Check points for duplicates (it is very bad for routing) - cloudmade could return it
	 */
	private void checkForDuplicatePoints(List<Location> locations, List<RouteDirectionInfo> directions) {
		// 
		for (int i = 0; i < locations.size() - 1;) {
			if (locations.get(i).distanceTo(locations.get(i + 1)) == 0) {
				locations.remove(i);
				if (directions != null) {
					for (RouteDirectionInfo info : directions) {
						if (info.routePointOffset > i) {
							info.routePointOffset--;
						}
					}
				}
			} else {
				i++;
			}
		}
	}

	/**
	 * PREPARATION
	 * If beginning is too far from start point, then introduce GO Ahead
	 * @param end 
	 */
	private static void introduceFirstPointAndLastPoint(List<Location> locations, List<RouteDirectionInfo> directions, List<RouteSegmentResult> segs, Location start, 
			LatLon end) {
		if (!locations.isEmpty() && locations.get(0).distanceTo(start) > 50) {
			// add start point
			locations.add(0, start);
			if(segs != null) {
				segs.add(0, segs.get(0));
			}
			if (directions != null && !directions.isEmpty()) {
				for (RouteDirectionInfo i : directions) {
					i.routePointOffset++;
				}
				RouteDirectionInfo info = new RouteDirectionInfo(directions.get(0).getAverageSpeed(),
						TurnType.sraight());
				info.routePointOffset = 0;
				// info.setDescriptionRoute(ctx.getString( R.string.route_head));//; //$NON-NLS-1$
				directions.add(0, info);
			}
		}
		RouteDirectionInfo lastDirInf = directions.size() > 0 ? directions.get(directions.size() - 1) : null;
		if((lastDirInf == null || lastDirInf.routePointOffset < locations.size() - 1) && locations.size() - 1 > 0) {
			String type = TurnType.C;
			Location prevLast = locations.get(locations.size() - 2);
			float lastBearing = prevLast.bearingTo(locations.get(locations.size() - 1));
			float[] compute = new float[2];
			Location.distanceBetween(prevLast.getLatitude(), prevLast.getLongitude(), 
					end.getLatitude(), end.getLongitude(), compute);
			float bearingToEnd = compute[1];
			double diff = MapUtils.degreesDiff(lastBearing, bearingToEnd);
			if(Math.abs(diff) > 10) {
				type = diff > 0 ? TurnType.KL : TurnType.KR; 
			}
			RouteDirectionInfo info = new RouteDirectionInfo(1, TurnType.valueOf(type, false));
			info.distance = 0;
			info.afterLeftTime = 0;
			info.routePointOffset = locations.size() - 1;			
			directions.add(info);
		}
	}

	/**
	 * PREPARATION
	 * At the end always update listDistance local vars and time
	 */
	private static void updateListDistanceTime(int[] listDistance, List<Location> locations) {
		if (listDistance.length > 0) {
			listDistance[locations.size() - 1] = 0;
			for (int i = locations.size() - 1; i > 0; i--) {
				listDistance[i - 1] = (int) locations.get(i - 1).distanceTo(locations.get(i));
				listDistance[i - 1] += listDistance[i];
			}
		}
	}
	
	/**
	 * PREPARATION
	 * At the end always update listDistance local vars and time
	 */
	private static void updateDirectionsTime(List<RouteDirectionInfo> directions, int[] listDistance) {
		int sum = 0;
		for (int i = directions.size() - 1; i >= 0; i--) {
			directions.get(i).afterLeftTime = sum;
			directions.get(i).distance = listDistance[directions.get(i).routePointOffset];
			if (i < directions.size() - 1) {
				directions.get(i).distance -= listDistance[directions.get(i + 1).routePointOffset];
			}
			sum += directions.get(i).getExpectedTime();
		}
	}
	
	//////////////////// MUST BE ALL SYNCHRONIZED ??? //////////////////////
	
	public List<Location> getImmutableAllLocations() {
		return locations;
	}
	
	public List<RouteDirectionInfo> getImmutableAllDirections() {
		return directions;
	}
	
	
	public List<Location> getRouteLocations() {
		if(currentRoute < locations.size()) {
			return locations.subList(currentRoute, locations.size());
		}
		return Collections.emptyList();
	}
	
	public RouteSegmentResult getCurrentSegmentResult() {
		int cs = currentRoute > 0 ? currentRoute - 1 : 0;
		if(cs < segments.size()) {
			return segments.get(cs);
		}
		return null;
	}
	
	public List<RouteSegmentResult> getUpcomingTunnel(float distToStart) {
		int cs = currentRoute > 0 ? currentRoute - 1 : 0;
		if(cs < segments.size()) {
			RouteSegmentResult prev = null;
			boolean tunnel = false;
			while(cs < segments.size() && distToStart > 0) {
				RouteSegmentResult segment = segments.get(cs);
				if(segment != prev ) {
					if(segment.getObject().tunnel()){
						tunnel = true; 
						break;
					} else {
						distToStart -= segment.getDistance();
						prev = segment;
					}
				}
				cs++;
			}
			if(tunnel) {
				List<RouteSegmentResult> list = new ArrayList<RouteSegmentResult>();
				while(cs < segments.size()) {
					RouteSegmentResult segment = segments.get(cs);
					if(segment != prev ) {
						if(segment.getObject().tunnel()) {
							list.add(segment);
						} else {
							break;
						}
						prev = segment;
					}
					cs++;
				}
				return list;
			}
		}
		
		return null;
	}
	
	public float getCurrentMaxSpeed() {
		RouteSegmentResult res = getCurrentSegmentResult();
		if(res != null) {
			return res.getObject().getMaximumSpeed();
		}
		return 0;
	}
	
	public float getRoutingTime() {
		return routingTime;
	}
	
	
	public int getWholeDistance() {
		if(listDistance.length > 0) {
			return listDistance[0];
		}
		return 0;
	}
	
	public boolean isCalculated() {
		return !locations.isEmpty();
	}
	
	public boolean isEmpty() {
		return locations.isEmpty() || currentRoute >= locations.size();
	}
	
	
	public void updateCurrentRoute(int currentRoute) {
		this.currentRoute = currentRoute;
		while (currentDirectionInfo < directions.size() - 1 && directions.get(currentDirectionInfo + 1).routePointOffset < currentRoute) {
			currentDirectionInfo++;
		}
		while (nextAlarmInfo < alarmInfo.size() && alarmInfo.get(nextAlarmInfo).locationIndex < currentRoute) {
			nextAlarmInfo++;
		}
		while(nextIntermediate < intermediatePoints.length) {
			RouteDirectionInfo dir = directions.get(intermediatePoints[nextIntermediate]);
			if(dir.routePointOffset < currentRoute) {
				nextIntermediate ++;
			} else {
				break;
			}
		}
	}
	
	public void passIntermediatePoint(){
		nextIntermediate ++ ;
	}
	
	public Location getLocationFromRouteDirection(RouteDirectionInfo i){
		if(i.routePointOffset < locations.size()){
			return locations.get(i.routePointOffset);
		}
		return null;
	}
	
	
	/*public */NextDirectionInfo getNextRouteDirectionInfo(NextDirectionInfo info, Location fromLoc, boolean toSpeak) {
		int dirInfo = currentDirectionInfo;
		if (dirInfo < directions.size()) {
			int dist = listDistance[currentRoute];
			int nextInd = dirInfo + 1;
			if (toSpeak) {
				while (nextInd < directions.size()) {
					RouteDirectionInfo i = directions.get(nextInd);
					if (i.getTurnType() != null && !i.getTurnType().isSkipToSpeak()) {
						break;
					}
					nextInd++;
				}
			}
			if (fromLoc != null) {
				dist += fromLoc.distanceTo(locations.get(currentRoute));
			}
			if (nextInd < directions.size()) {
				info.directionInfo = directions.get(nextInd);
				dist -= listDistance[directions.get(nextInd).routePointOffset];
			}
			if(intermediatePoints != null && nextIntermediate < intermediatePoints.length) {
				info.intermediatePoint = intermediatePoints[nextIntermediate] == nextInd;
			}
			info.directionInfoInd = nextInd;
			info.distanceTo = dist;
			return info;
		}
		info.directionInfoInd = -1;
		info.distanceTo = -1;
		info.directionInfo = null;
		return null;
	}
	
	/*public */NextDirectionInfo getNextRouteDirectionInfoAfter(NextDirectionInfo prev, NextDirectionInfo next, boolean toSpeak) {
		int dirInfo = prev.directionInfoInd;
		if (dirInfo < directions.size() && prev.directionInfo != null) {
			int dist = listDistance[prev.directionInfo.routePointOffset];
			int nextInd = dirInfo + 1;
			if (toSpeak) {
				while (nextInd < directions.size()) {
					RouteDirectionInfo i = directions.get(nextInd);
					if (i.getTurnType() != null && !i.getTurnType().isSkipToSpeak()) {
						break;
					}
					nextInd++;
				}
			}
			if (nextInd < directions.size()) {
				next.directionInfo = directions.get(nextInd);
				dist -= listDistance[directions.get(nextInd).routePointOffset];
			}
			if(intermediatePoints != null && nextIntermediate < intermediatePoints.length) {
				next.intermediatePoint = intermediatePoints[nextIntermediate] == nextInd;
			}
			next.distanceTo = dist;
			next.directionInfoInd = nextInd;
			return next;
		}
		next.directionInfoInd = -1;
		next.distanceTo = -1;
		next.directionInfo = null;
		return null;
	}
	
	
	
	public List<RouteDirectionInfo> getRouteDirections() {
		if(currentDirectionInfo < directions.size() - 1){
			if(cacheCurrentTextDirectionInfo != currentDirectionInfo) {
				cacheCurrentTextDirectionInfo = currentDirectionInfo;
				List<RouteDirectionInfo> list = currentDirectionInfo == 0 ? directions : 
					directions.subList(currentDirectionInfo + 1, directions.size());
				cacheAgreggatedDirections = new ArrayList<RouteDirectionInfo>();
				RouteDirectionInfo p = null;
				for(RouteDirectionInfo i : list) {
					if(p == null || !i.getTurnType().isSkipToSpeak() || 
							(!Algorithms.objectEquals(p.getRef(), i.getRef()) && 
									!Algorithms.objectEquals(p.getStreetName(), i.getStreetName()))) {
						p = new RouteDirectionInfo(i.getAverageSpeed(), i.getTurnType());
						p.routePointOffset = i.routePointOffset;
						p.setDestinationName(i.getDestinationName());
						p.setRef(i.getRef());
						p.setStreetName(i.getStreetName());
						p.setDescriptionRoute(i.getDescriptionRoutePart());
						cacheAgreggatedDirections.add(p);
					}
					float time = i.getExpectedTime() + p.getExpectedTime();
					p.distance += i.distance;
					p.setAverageSpeed(p.distance / time);
					p.afterLeftTime = i.afterLeftTime;
				}
			}
			return cacheAgreggatedDirections;
		}
		return Collections.emptyList();
	}
	
	public Location getNextRouteLocation() {
		if(currentRoute < locations.size()) {
			return locations.get(currentRoute);
		}
		return null;
	}
	
	public AlarmInfo getMostImportantAlarm(Location fromLoc, AlarmInfo speedAlarm, boolean showCameras) {
		int cRoute = currentRoute;
		AlarmInfo mostImportant = speedAlarm;
		int value = speedAlarm != null? speedAlarm.updateDistanceAndGetPriority(0, 0) : Integer.MAX_VALUE;
		int aInfo = nextAlarmInfo;
		if (aInfo < alarmInfo.size()) {
			int dist = 0;
			float speed = 0;
			if (fromLoc != null && fromLoc.hasSpeed()) {
				speed = fromLoc.getSpeed();
			}
			if (fromLoc != null) {
				dist += fromLoc.distanceTo(locations.get(cRoute));
			}
			dist += listDistance[cRoute];
			while(aInfo < alarmInfo.size()) {
				AlarmInfo inf = alarmInfo.get(aInfo);
				int d = dist - listDistance[inf.locationIndex];
				if(d > 250){
					break;
				}
				float time = speed > 0 ? d / speed : Integer.MAX_VALUE;
				int vl = inf.updateDistanceAndGetPriority(time, d);
				if(vl < value && (showCameras || inf.getType() != AlarmInfoType.SPEED_CAMERA)){
					mostImportant = inf;
					value = vl;
				}
				aInfo++;
			}
		}
		return mostImportant;
	}
	
	public Location getNextRouteLocation(int after) {
		if(currentRoute + after < locations.size()) {
			return locations.get(currentRoute + after);
		}
		return null;
	}
	
	public boolean directionsAvailable(){
		return currentDirectionInfo < directions.size();
	}
	
	public int getDistanceToFinish(Location fromLoc) {
		if(listDistance != null && currentRoute < listDistance.length){
			int dist = listDistance[currentRoute];
			Location l = locations.get(currentRoute);
			if(fromLoc != null){
				dist += fromLoc.distanceTo(l);
			}
			return dist;
		}
		return 0;
	}
	
	public int getDistanceToNextIntermediate(Location fromLoc) {
		if(listDistance != null && currentRoute < listDistance.length){
			int dist = listDistance[currentRoute];
			Location l = locations.get(currentRoute);
			if(fromLoc != null){
				dist += fromLoc.distanceTo(l);
			}
			if(nextIntermediate >= intermediatePoints.length ){
				return 0;
			} else {
				int directionInd = intermediatePoints[nextIntermediate];
				return dist - listDistance[directions.get(directionInd).routePointOffset];	
			}
		}
		return 0;
	}
	
	public int getIntermediatePointsToPass(){
		if(nextIntermediate >= intermediatePoints.length) {
			return 0;
		}
		return intermediatePoints.length - nextIntermediate;
	}
	
	public int getLeftTime(Location fromLoc){
		int time = 0;
		if(currentDirectionInfo < directions.size()) {
			RouteDirectionInfo current = directions.get(currentDirectionInfo);
			time = current.afterLeftTime;
			
			int distanceToNextTurn = listDistance[currentRoute];
			if(currentDirectionInfo + 1 < directions.size()) {
				distanceToNextTurn -= listDistance[directions.get(currentDirectionInfo + 1).routePointOffset];
			}
			Location l = locations.get(currentRoute);
			if(fromLoc != null){
				distanceToNextTurn += fromLoc.distanceTo(l);
			}
			time += distanceToNextTurn / current.getAverageSpeed();
		}
		return time;
	}

	
	public static class NextDirectionInfo {
		public RouteDirectionInfo directionInfo;
		public int distanceTo;
		public boolean intermediatePoint;
		public String pointName;
		public int imminent;
		private int directionInfoInd;
	}
	
}
