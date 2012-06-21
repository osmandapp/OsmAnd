package net.osmand.plus.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.OsmAndFormatter;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.TurnType;
import android.content.Context;
import android.location.Location;

public class RouteCalculationResult {
	protected final List<Location> locations;
	protected List<RouteDirectionInfo> directions;
	private final String errorMessage;
	private int[] listDistance = new int[0];
	
	protected GPXRouteParams currentGPXRoute = null;

	// Note always currentRoute > get(currentDirectionInfo).routeOffset, 
	//         but currentRoute <= get(currentDirectionInfo+1).routeOffset 
	protected int currentDirectionInfo = 0;
	protected int currentRoute = 0;

	public RouteCalculationResult(String errorMessage) {
		this(null, null, null, null, errorMessage);
	}

	public RouteCalculationResult(List<Location> list, List<RouteDirectionInfo> directions, Location start, LatLon end, String errorMessage) {
		this.directions = directions;
		this.errorMessage = errorMessage;
		this.locations = list == null? new ArrayList<Location>():list;
		if (!locations.isEmpty()) {
			// if there is no closest points to start - add it
			introduceFirstPoint(start);
			checkForDuplicatePoints();
			removeUnnecessaryGoAhead();
			updateListDistanceTime();
		}
		
	}
	
	public String toString(TurnType type, Context ctx) {
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

	public RouteCalculationResult(List<RouteSegmentResult> list, Location start, LatLon end, 
			Context ctx, boolean leftSide) {
		this.directions = new ArrayList<RouteDirectionInfo>();
		this.errorMessage = null;
		this.locations = new ArrayList<Location>();

		float prevDirectionTime = 0;
		float prevDirectionDistance = 0;
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
				String description = toString(turn, ctx);
				info.setDescriptionRoute(description);
				info.routePointOffset = prevLocationSize;
				if(directions.size() > 0 && prevDirectionTime > 0 && prevDirectionDistance > 0) {
					RouteDirectionInfo prev = directions.get(directions.size() - 1);
					prev.setAverageSpeed(prevDirectionDistance / prevDirectionTime);
					prev.setDescriptionRoute(prev.getDescriptionRoute() + " " + OsmAndFormatter.getFormattedDistance(prevDirectionDistance, ctx));
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
			prev.setDescriptionRoute(prev.getDescriptionRoute() + " " + OsmAndFormatter.getFormattedDistance(prevDirectionDistance, ctx));
		}
		introduceFirstPoint(start);
		updateListDistanceTime();
	}

	
	public String getErrorMessage() {
		return errorMessage;
	}


	/**
	 * PREPARATION 
	 * Remove unnecessary go straight from CloudMade.
	 * Remove also last direction because it will be added after.
	 */
	private void removeUnnecessaryGoAhead() {
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
	 * Check points for duplicates (it is very bad for routing) - cloudmade could return it
	 */
	private void checkForDuplicatePoints() {
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
	 */
	private void introduceFirstPoint(Location start) {
		if (!locations.isEmpty() && locations.get(0).distanceTo(start) > 200) {
			// add start point
			locations.add(0, start);
			if (directions != null && !directions.isEmpty()) {
				for (RouteDirectionInfo i : directions) {
					i.routePointOffset++;
				}
				RouteDirectionInfo info = new RouteDirectionInfo(directions.get(0).getAverageSpeed(),
						TurnType.valueOf(TurnType.C, false));
				info.routePointOffset = 0;
				// info.setDescriptionRoute(getString(ctx, R.string.route_head));//; //$NON-NLS-1$
				directions.add(0, info);
			}
		}
	}

	/**
	 * PREPARATION
	 * At the end always update listDistance local vars and time
	 */
	private void updateListDistanceTime() {
		listDistance = new int[locations == null ? 0 : locations.size()];
		if (listDistance.length > 0) {
			listDistance[locations.size() - 1] = 0;
			for (int i = locations.size() - 1; i > 0; i--) {
				listDistance[i - 1] = (int) locations.get(i - 1).distanceTo(locations.get(i));
				listDistance[i - 1] += listDistance[i];
			}
		}
		if (directions != null) {
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
	}
	
	//////////////////// MUST BE ALL SYNCHRONIZED ??? //////////////////////
	
	public List<Location> getLocations() {
		return locations;
	}
	
	public List<Location> getNextLocations() {
		if(locations != null && currentRoute < locations.size()) {
			return locations.subList(currentRoute, locations.size());
		}
		return Collections.emptyList();
	}

	public List<RouteDirectionInfo> getDirections() {
		return directions;
	}

	public int[] getListDistance() {
		return listDistance;
	}

	public void clear() {
		this.locations.clear();
		listDistance = null;
		if(directions != null) {
			directions.clear();
		}
	}
	
	public boolean isCalculated() {
		return locations != null && !locations.isEmpty();
	}
	
	public boolean isEmpty() {
		return locations == null || locations.isEmpty() || currentRoute >= locations.size();
	}
	
	public void setCurrentGPXRoute(GPXRouteParams currentGPXRoute) {
		this.currentGPXRoute = currentGPXRoute;
	}
	public GPXRouteParams getCurrentGPXRoute() {
		return currentGPXRoute;
	}
	
	public void updateCurrentRoute(int currentRoute){
		this.currentRoute = currentRoute;
		if(directions != null){
			while(currentDirectionInfo < directions.size() - 1 && 
					directions.get(currentDirectionInfo + 1).routePointOffset < currentRoute){
				currentDirectionInfo ++;
			}
		}
	}
	
	public Location getLocationFromRouteDirection(RouteDirectionInfo i){
		if(i.routePointOffset < locations.size()){
			return locations.get(i.routePointOffset);
		}
		return null;
	}
	
	public RouteDirectionInfo getNextRouteDirectionInfo(){
		if(directions != null && currentDirectionInfo < directions.size() - 1){
			return directions.get(currentDirectionInfo + 1);
		}
		return null;
	}
	
	public RouteDirectionInfo getCurrentRouteDirectionInfo(){
		if(directions != null && currentDirectionInfo < directions.size()){
			return directions.get(currentDirectionInfo);
		}
		return null;
	}
	public RouteDirectionInfo getNextNextRouteDirectionInfo(){
		if(directions != null && currentDirectionInfo < directions.size() - 2){
			return directions.get(currentDirectionInfo + 2);
		}
		return null;
	}

	public List<RouteDirectionInfo> getRouteDirections() {
		if(directions != null && currentDirectionInfo < directions.size()){
			if(currentDirectionInfo == 0){
				return directions;
			}
			if(currentDirectionInfo < directions.size() - 1){
				return directions.subList(currentDirectionInfo + 1, directions.size());
			}
		}
		return Collections.emptyList();
	}
	
	public Location getNextRouteLocation() {
		if(currentRoute < locations.size()) {
			return locations.get(currentRoute);
		}
		return null;
	}
	
	public Location getNextRouteLocation(int after) {
		if(currentRoute + after < locations.size()) {
			return locations.get(currentRoute + after);
		}
		return null;
	}
	
	public boolean directionsAvailable(){
		return directions != null && currentDirectionInfo < directions.size();
	}
	
	public int getDistanceToNextTurn(Location fromLoc) {
		if (directions != null && currentDirectionInfo < directions.size()) {
			int dist = listDistance[currentRoute];
			if (currentDirectionInfo < directions.size() - 1) {
				dist -= listDistance[directions.get(currentDirectionInfo + 1).routePointOffset];
			}
			if (fromLoc != null) {
				dist += fromLoc.distanceTo(locations.get(currentRoute));
			}
			return dist;
		}
		return -1;
	}
	
	public int getDistanceFromNextToNextNextTurn() {
		if (directions != null && currentDirectionInfo < directions.size() - 1) {
			int dist = listDistance[directions.get(currentDirectionInfo + 1).routePointOffset];
			if (currentDirectionInfo < directions.size() - 2) {
				dist -= listDistance[directions.get(currentDirectionInfo + 2).routePointOffset];
			}
			return dist;
		}
		return -1;
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

	public void setDirections(ArrayList<RouteDirectionInfo> directions) {
		this.directions = directions;
	}
}