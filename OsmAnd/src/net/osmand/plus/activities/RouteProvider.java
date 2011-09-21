package net.osmand.plus.activities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.GPXUtilities;
import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Route;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.RoutingHelper.RouteDirectionInfo;
import net.osmand.plus.activities.RoutingHelper.TurnType;
import net.osmand.router.BicycleRouter;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.CarRouter;
import net.osmand.router.PedestrianRouter;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingContext;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.Context;
import android.location.Location;

public class RouteProvider {
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RouteProvider.class);
	private static final String OSMAND_ROUTER = "OsmandRouter";
	
	public enum RouteService {
		CLOUDMADE("CloudMade"), YOURS("YOURS"), OSMAND("OsmAnd (offline)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		private final String name;
		private RouteService(String name){
			this.name = name;
		}
		public String getName() {
			return name;
		}
	}
	
	public RouteProvider(){
	}
	
	public static class GPXRouteParams {
		List<Location> points = new ArrayList<Location>();
		List<RouteDirectionInfo> directions;
	
		public GPXRouteParams(GPXFile file, boolean reverse){
			prepareEverything(file, reverse);
		}
		
		public void setStartPoint(Location startPoint) {
			points.add(0, startPoint);
		}
		
		
		public Location getStartPointForRoute(){
			if(!points.isEmpty()){
				return points.get(0);
			}
			return null;
		}

		public LatLon getLastPoint() {
			if(!points.isEmpty()){
				Location l = points.get(points.size() - 1);
				LatLon point = new LatLon(l.getLatitude(), l.getLongitude());
				return point;
			}
			return null;
		}
		
		private void prepareEverything(GPXFile file, boolean reverse){
			if(file.isCloudmadeRouteFile() || OSMAND_ROUTER.equals(file.author)){
				directions =  parseCloudmadeRoute(points, file, OSMAND_ROUTER.equals(file.author));
				if(reverse){
					// clear directions all turns should be recalculated
					directions = null;
					Collections.reverse(points);
				}
			} else {
				// first of all check tracks
				for (Track tr : file.tracks) {
					for (TrkSegment tkSeg : tr.segments) {
						for (WptPt pt : tkSeg.points) {
							points.add(createLocation(pt));
						}
					}
				}
				if (points.isEmpty()) {
					for (Route rte : file.routes) {
						for (WptPt pt : rte.points) {
							points.add(createLocation(pt));
						}
					}
				}
				if (reverse) {
					Collections.reverse(points);
				}
			}
		}		
	}
	
	private static Location createLocation(WptPt pt){
		Location loc = new Location("OsmandRouteProvider");
		loc.setLatitude(pt.lat);
		loc.setLongitude(pt.lon);
		loc.setSpeed((float) pt.speed);
		loc.setAltitude(pt.ele);
		loc.setAccuracy((float) pt.hdop);
		return loc;
	}
	
	
	public static class RouteCalculationResult {
		private final List<Location> locations;
		private List<RouteDirectionInfo> directions;
		private final String errorMessage;
		private int[] listDistance = null;
		
		public RouteCalculationResult(String errorMessage) {
			this(null, null, null, null, errorMessage);
		}
		public RouteCalculationResult(List<Location> list, List<RouteDirectionInfo> directions, Location start, LatLon end, String errorMessage) {
			this.directions = directions;
			this.errorMessage = errorMessage;
			this.locations = list;
			if (list != null) {
				prepareResult(start, end);
			}
		}
		
		public List<Location> getLocations() {
			return locations;
		}
		
		public List<RouteDirectionInfo> getDirections() {
			return directions;
		}
		
		public int[] getListDistance() {
			return listDistance;
		}
		
		private void prepareResult(Location start, LatLon end) {

			if (locations != null && !locations.isEmpty()) {
				// if there is no closest points to start - add it
				if (locations.get(0).distanceTo(start) > 200) {
					// add start point
					locations.add(0, start);
					if (directions != null) {
						for (RouteDirectionInfo i : directions) {
							i.routePointOffset++;
						}
						RouteDirectionInfo info = new RouteDirectionInfo();
						info.turnType = TurnType.valueOf(TurnType.C);
						info.routePointOffset = 0;
						info.descriptionRoute = "" ;//getString(ctx, R.string.route_head); //$NON-NLS-1$
						directions.add(0, info);
					}	
				}
				
				// check points for duplicates (it is very bad for routing) - cloudmade could return it 
				for (int i = locations.size() - 1; i >= 0; ) {
					if(locations.get(i).distanceTo(locations.get(i-1)) == 0){
						locations.remove(i);
						if (directions != null) {
							for (RouteDirectionInfo info : directions) {
								if(info.routePointOffset > i){
									info.routePointOffset--;
								}
							}
						}
					} else {
						i--;
					}
				}
				// Remove unnecessary go straight from CloudMade 
				// Remove also last direction because it will be added after
				if(directions != null && directions.size() > 1){
					for (int i = directions.size()-1; i > 0; ) {
						RouteDirectionInfo r = directions.get(i);
						if(r.turnType.getValue().equals(TurnType.C)){
							RouteDirectionInfo prev = directions.get(i-1);
							prev.expectedTime += r.expectedTime;
							directions.remove(i);
						} else {
							i--;
						}
					}
				}
			}
			
			listDistance = new int[locations.size()];
			if (!locations.isEmpty()) {
				listDistance[locations.size() - 1] = 0;
				for (int i = locations.size() - 1; i > 0; i--) {
					listDistance[i - 1] = (int) locations.get(i - 1).distanceTo(locations.get(i));
					listDistance[i - 1] += listDistance[i];
				}
			}
			if (directions != null) {
				int sum = 0;
				int dsize = directions.size(); 
				for (int i = dsize - 1; i >= 0; i--) {
					directions.get(i).afterLeftTime = sum;
					sum += directions.get(i).expectedTime;
					directions.get(i).distance = listDistance[directions.get(i).routePointOffset];
					if(i < dsize - 1){
						directions.get(i).distance -=listDistance[directions.get(i + 1).routePointOffset];
					}
				}
			}
		}
		public boolean isCalculated(){
			return locations != null && !locations.isEmpty();
		}
		
		public String getErrorMessage(){
			return errorMessage;
		}
		
	}

	public RouteCalculationResult calculateRouteImpl(Location start, LatLon end, ApplicationMode mode, RouteService type, Context ctx,
			GPXRouteParams gpxRoute, boolean fast){
		long time = System.currentTimeMillis();
		if (start != null && end != null) {
			if(log.isInfoEnabled()){
				log.info("Start finding route from " + start + " to " + end +" using " + type.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try {
				RouteCalculationResult res;
				if(gpxRoute != null && !gpxRoute.points.isEmpty()){
					res = calculateGpxRoute(start, end, gpxRoute);
					addMissingTurnsToRoute(res, start, end, mode, ctx);
				} else if (type == RouteService.YOURS) {
					res = findYOURSRoute(start, end, mode, fast);
					addMissingTurnsToRoute(res, start, end, mode, ctx);
				} else if (type == RouteService.OSMAND) {
					res = findVectorMapsRoute(start, end, mode, fast, (OsmandApplication)ctx.getApplicationContext());
					addMissingTurnsToRoute(res, start, end, mode, ctx);
				} else {
					res = findCloudMadeRoute(start, end, mode, ctx, fast);
					// for test purpose
					addMissingTurnsToRoute(res, start, end, mode, ctx);
				}
				if(log.isInfoEnabled() && res.locations != null){
					log.info("Finding route contained " + res.locations.size() + " points for " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return res; 
			} catch (IOException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (ParserConfigurationException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (SAXException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			}
		}
		return new RouteCalculationResult(null);
	}

	private RouteCalculationResult calculateGpxRoute(Location start, LatLon end, GPXRouteParams params) {
		RouteCalculationResult res;
		// get the closest point to start and to end
		float minDist = Integer.MAX_VALUE;
		int startI = 0;
		List<Location> gpxRoute = params.points;
		int endI = gpxRoute.size(); 
		if (start != null) {
			for (int i = 0; i < endI; i++) {
				float d = gpxRoute.get(i).distanceTo(start);
				if (d < minDist) {
					startI = i;
					minDist = d;
				}
			}
		} else {
			start = gpxRoute.get(0);
		}
		Location l = new Location("temp"); //$NON-NLS-1$
		l.setLatitude(end.getLatitude());
		l.setLongitude(end.getLongitude());
		minDist = Integer.MAX_VALUE;
		// get in reverse order taking into account ways with cycle
		for (int i = gpxRoute.size() - 1; i >= startI; i--) {
			float d = gpxRoute.get(i).distanceTo(l);
			if (d < minDist) {
				endI = i + 1;
				// slightly modify to allow last point to be added
				minDist = d - 40;
			}
		}
		ArrayList<Location> sublist = new ArrayList<Location>(gpxRoute.subList(startI, endI));
		if(params.directions == null){
			res = new RouteCalculationResult(sublist, params.directions, start, end, null);
		} else {
			List<RouteDirectionInfo> subdirections = new ArrayList<RouteDirectionInfo>();
			for (RouteDirectionInfo info : params.directions) {
				if(info.routePointOffset >= startI && info.routePointOffset < endI){
					RouteDirectionInfo ch = new RouteDirectionInfo();
					ch.routePointOffset = info.routePointOffset - startI;
					ch.descriptionRoute = info.descriptionRoute;
					ch.expectedTime = info.expectedTime;
					ch.turnType = info.turnType;
					
					// recalculate
					ch.distance = 0;
					ch.afterLeftTime = 0;
					subdirections.add(ch);
				}
			}
			res = new RouteCalculationResult(sublist, subdirections, start, end, null);
		}
		return res;
	}
	
	protected String getString(Context ctx, int resId){
		if(ctx == null){
			return ""; //$NON-NLS-1$
		}
		return ctx.getString(resId);
	}
	
	protected void addMissingTurnsToRoute(RouteCalculationResult res, Location start, LatLon end, ApplicationMode mode, Context ctx){
		if(!res.isCalculated()){
			return;
		}
		// speed m/s
		float speed = 1.5f;
		int minDistanceForTurn = 5;
		if(mode == ApplicationMode.CAR){
			speed = 15.3f;
			minDistanceForTurn = 35;
		} else if(mode == ApplicationMode.BICYCLE){
			speed = 5.5f;
			minDistanceForTurn = 12;
		}
		
		

		List<RouteDirectionInfo> directions = new ArrayList<RouteDirectionInfo>();
		int[] listDistance = res.getListDistance();
		List<Location> locations = res.getLocations();
		
		
		int previousLocation = 0;
		int prevBearingLocation = 0;
		RouteDirectionInfo previousInfo = new RouteDirectionInfo();
		previousInfo.turnType = TurnType.valueOf(TurnType.C);
		previousInfo.routePointOffset = 0;
		previousInfo.descriptionRoute = getString(ctx, R.string.route_head);
		directions.add(previousInfo);
		
		int distForTurn = 0;
		float previousBearing = 0;
		int startTurnPoint = 0;
		
		int lsize = locations.size(); 
		for (int i = 1; i < lsize - 1; i++) {
			
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
			if (i < lsize - 1 &&  distForTurn < minDistanceForTurn) {
				// For very smooth turn we try to accumulate whole distance
				// simply skip that turn needed for situation
				// 1) if you are going to have U-turn - not 2 left turns
				// 2) if there is a small gap between roads (turn right and after 4m next turn left) - so the direction head
				continue;
			}
			
			
			if(delta > 50 && delta < 310){
				
				if(delta < 70){
					type = TurnType.valueOf(TurnType.TSLL);
					description = getString(ctx, R.string.route_tsll);
				} else if(delta < 110){
					type = TurnType.valueOf(TurnType.TL);
					description = getString(ctx, R.string.route_tl);
				} else if(delta < 135){
					type = TurnType.valueOf(TurnType.TSHL);
					description = getString(ctx, R.string.route_tshl);
				} else if(delta < 225){
					type = TurnType.valueOf(TurnType.TU);
					description = getString(ctx, R.string.route_tu);
				} else if(delta < 250){
					description = getString(ctx, R.string.route_tshr);
					type = TurnType.valueOf(TurnType.TSHR);
				} else if(delta < 290){
					description = getString(ctx, R.string.route_tr);
					type = TurnType.valueOf(TurnType.TR);
				} else {
					description = getString(ctx, R.string.route_tslr);
					type = TurnType.valueOf(TurnType.TSLR);
				}
				
				// calculate for previousRoute 
				previousInfo.distance = listDistance[previousLocation]- listDistance[i];
				previousInfo.expectedTime = (int) (previousInfo.distance / speed);
				previousInfo.descriptionRoute += " " + OsmAndFormatter.getFormattedDistance(previousInfo.distance, ctx); //$NON-NLS-1$

				previousInfo = new RouteDirectionInfo();
				previousInfo.turnType = type;
				previousInfo.turnType.setTurnAngle(360 - delta);
				previousInfo.descriptionRoute = description;
				previousInfo.routePointOffset = startTurnPoint;
				directions.add(previousInfo);
				previousLocation = startTurnPoint;
				prevBearingLocation = i; // for bearing using current location
			}
			// clear dist for turn
			distForTurn = 0;
		} 
			
		previousInfo.distance = listDistance[previousLocation];
		previousInfo.expectedTime = (int) (previousInfo.distance / speed);
		previousInfo.descriptionRoute += " " + OsmAndFormatter.getFormattedDistance(previousInfo.distance, ctx); //$NON-NLS-1$
		
		// add last direction go straight (to show arrow in screen after all turns)
		if(previousInfo.distance > 80){
			RouteDirectionInfo info = new RouteDirectionInfo();
			info.expectedTime = 0;
			info.distance = 0;
			info.descriptionRoute = ""; //$NON-NLS-1$
			info.turnType = TurnType.valueOf(TurnType.C);
			info.routePointOffset = lsize - 1;
			directions.add(info);
		}
		
		if(res.directions == null || res.directions.isEmpty()){
			res.directions = new ArrayList<RouteDirectionInfo>(directions);
		} else {
			int currentDirection= 0;
			// one more
			int dirsize = directions.size();
			for (int i = 0; i <= res.directions.size() && currentDirection < dirsize; i++) {
				while(currentDirection < dirsize){
					int distanceAfter = 0;
					if (i < res.directions.size()) {
						RouteDirectionInfo resInfo = res.directions.get(i);
						int r1 = directions.get(currentDirection).routePointOffset;
						int r2 = resInfo.routePointOffset;
						distanceAfter = listDistance[resInfo.routePointOffset];
						float dist = locations.get(r1).distanceTo(locations.get(r2));
						// take into account that move roundabout is special turn that could be very lengthy
						if (dist < 100) {
							// the same turn duplicate
							currentDirection++;
							continue; // while cycle
						} else if (directions.get(currentDirection).routePointOffset > resInfo.routePointOffset) {
							// check it at the next point
							break;
						}
					}
					
					// add turn because it was missed
					RouteDirectionInfo toAdd = directions.get(currentDirection);
					float calcSpeed = toAdd.expectedTime == 0 ? speed :((float) toAdd.distance / toAdd.expectedTime);
					
					if(i > 0){
						// update previous
						RouteDirectionInfo previous = res.directions.get(i - 1);
						calcSpeed = previous.expectedTime == 0 ? calcSpeed :((float) previous.distance / previous.expectedTime);
						previous.distance = listDistance[previous.routePointOffset] - listDistance[toAdd.routePointOffset];
						previous.expectedTime = (int) ((float) previous.distance / calcSpeed); 
					}
					toAdd.distance = listDistance[toAdd.routePointOffset] - distanceAfter;
					toAdd.expectedTime = (int) ((float) toAdd.distance / calcSpeed);
					if(i < res.directions.size()){
						res.directions.add(i, toAdd);
					} else {
						res.directions.add(toAdd);
					}
					
					i++;
					currentDirection++;
				}
			}
		}
		
		int sum = 0;
		for (int i = res.directions.size() - 1; i >= 0; i--) {
			res.directions.get(i).afterLeftTime = sum;
			sum += res.directions.get(i).expectedTime;
		}
	}


	protected RouteCalculationResult findYOURSRoute(Location start, LatLon end, ApplicationMode mode, boolean fast) throws MalformedURLException, IOException,
			ParserConfigurationException, FactoryConfigurationError, SAXException {
		List<Location> res = new ArrayList<Location>();
		StringBuilder uri = new StringBuilder();
		uri.append("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml"); //$NON-NLS-1$
		uri.append("&flat=").append(start.getLatitude()); //$NON-NLS-1$
		uri.append("&flon=").append(start.getLongitude()); //$NON-NLS-1$
		uri.append("&tlat=").append(end.getLatitude()); //$NON-NLS-1$
		uri.append("&tlon=").append(end.getLongitude()); //$NON-NLS-1$
		if(ApplicationMode.PEDESTRIAN == mode){
			uri.append("&v=foot") ; //$NON-NLS-1$
		} else if(ApplicationMode.BICYCLE == mode){
			uri.append("&v=bicycle") ; //$NON-NLS-1$
		} else {
			uri.append("&v=motorcar"); //$NON-NLS-1$
		}
		uri.append("&fast=").append(fast ? "1" : "0").append("&layer=mapnik"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		URL url = new URL(uri.toString());
		URLConnection connection = url.openConnection();
		DocumentBuilder dom = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = dom.parse(new InputSource(new InputStreamReader(connection.getInputStream())));
		NodeList list = doc.getElementsByTagName("coordinates"); //$NON-NLS-1$
		for(int i=0; i<list.getLength(); i++){
			Node item = list.item(i);
			String str = item.getFirstChild().getNodeValue();
			if(str == null){
				continue;
			}
			int st = 0;
			int next = 0;
			while((next = str.indexOf('\n', st)) != -1){
				String coordinate = str.substring(st, next + 1);
				int s = coordinate.indexOf(',');
				if (s != -1) {
					try {
						double lon = Double.parseDouble(coordinate.substring(0, s));
						double lat = Double.parseDouble(coordinate.substring(s + 1));
						Location l = new Location("router"); //$NON-NLS-1$
						l.setLatitude(lat);
						l.setLongitude(lon);
						res.add(l);
					} catch (NumberFormatException e) {
					}
				}
				st = next + 1;
			}
		}
		if(list.getLength() == 0){
			if(doc.getChildNodes().getLength() == 1){
				Node item = doc.getChildNodes().item(0);
				return new RouteCalculationResult(item.getNodeValue());
				
			}
		}
		return new RouteCalculationResult(res, null, start, end, null);
	}
	
	protected RouteCalculationResult findVectorMapsRoute(Location start, LatLon end, ApplicationMode mode, boolean fast, OsmandApplication app) throws IOException {
		BinaryMapIndexReader[] files = app.getResourceManager().getRoutingMapFiles();
		BinaryRoutePlanner router = new BinaryRoutePlanner(files);
		RoutingContext ctx = new RoutingContext();
		ctx.setUsingShortestWay(!fast);
		if(mode == ApplicationMode.BICYCLE){
			ctx.setRouter(new BicycleRouter());
			ctx.setUseStrategyOfIncreasingRoadPriorities(false);
			ctx.setUseDynamicRoadPrioritising(true);
		} else if(mode == ApplicationMode.PEDESTRIAN){
			ctx.setRouter(new PedestrianRouter());
			ctx.setUseStrategyOfIncreasingRoadPriorities(false);
			ctx.setUseDynamicRoadPrioritising(false);
			ctx.setHeuristicCoefficient(2);
		} else {
			ctx.setRouter(new CarRouter());
			ctx.setUseStrategyOfIncreasingRoadPriorities(true);
			ctx.setUseDynamicRoadPrioritising(true);
		}
		RouteSegment st= router.findRouteSegment(start.getLatitude(), start.getLongitude(), ctx);
		if (st == null) {
			return new RouteCalculationResult("Start point is far from allowed road.");
		}
		RouteSegment en = router.findRouteSegment(end.getLatitude(), end.getLongitude(), ctx);
		if (en == null) {
			return new RouteCalculationResult("End point is far from allowed road.");
		}
		List<Location> res = new ArrayList<Location>();
		try {
			List<RouteSegmentResult> result = router.searchRoute(ctx, st, en);
			for (RouteSegmentResult s : result) {
				boolean plus = s.startPointIndex < s.endPointIndex;
				int i = s.startPointIndex;
				while (true) {
					Location n = new Location(""); //$NON-NLS-1$
					n.setLatitude(MapUtils.get31LatitudeY(s.object.getPoint31YTile(i)));
					n.setLongitude(MapUtils.get31LongitudeX(s.object.getPoint31XTile(i)));
					res.add(n);
					if (i == s.endPointIndex) {
						break;
					}
					if (plus) {
						i++;
					} else {
						i--;
					}
				}
			}
			return new RouteCalculationResult(res, null, start, end, null);
		} catch (OutOfMemoryError e) {
			return new RouteCalculationResult("Not enough memory");
		}
	}
	
	
	protected RouteCalculationResult findCloudMadeRoute(Location start, LatLon end, ApplicationMode mode, Context ctx, boolean fast)
			throws MalformedURLException, IOException, ParserConfigurationException, FactoryConfigurationError, SAXException {
		List<Location> res = new ArrayList<Location>();
		List<RouteDirectionInfo> directions = null;
		StringBuilder uri = new StringBuilder();
		// possibly hide that API key because it is privacy of osmand
		uri.append("http://routes.cloudmade.com/A6421860EBB04234AB5EF2D049F2CD8F/api/0.3/"); //$NON-NLS-1$
		uri.append(start.getLatitude() + "").append(","); //$NON-NLS-1$ //$NON-NLS-2$
		uri.append(start.getLongitude() + "").append(","); //$NON-NLS-1$ //$NON-NLS-2$
		uri.append(end.getLatitude() + "").append(","); //$NON-NLS-1$//$NON-NLS-2$
		uri.append(end.getLongitude() + "").append("/"); //$NON-NLS-1$ //$NON-NLS-2$

		if (ApplicationMode.PEDESTRIAN == mode) {
			uri.append("foot.gpx"); //$NON-NLS-1$
		} else if (ApplicationMode.BICYCLE == mode) {
			uri.append("bicycle.gpx"); //$NON-NLS-1$
		} else {
			if (fast) {
				uri.append("car.gpx"); //$NON-NLS-1$
			} else {
				uri.append("car/shortest.gpx"); //$NON-NLS-1$
			}
		}
		uri.append("?lang=").append(Locale.getDefault().getLanguage()); //$NON-NLS-1$
		URL url = new URL(uri.toString());
		URLConnection connection = url.openConnection();
		GPXFile gpxFile = GPXUtilities.loadGPXFile(ctx, connection.getInputStream(), false);
		directions = parseCloudmadeRoute(res, gpxFile, false);

		return new RouteCalculationResult(res, directions, start, end, null);
	}

	private static List<RouteDirectionInfo> parseCloudmadeRoute(List<Location> res, GPXFile gpxFile, boolean osmandRouter) {
		List<RouteDirectionInfo> directions = null;
		if (!osmandRouter) {
			for (WptPt pt : gpxFile.points) {
				res.add(createLocation(pt));
			}
		} else {
			for (Track tr : gpxFile.tracks) {
				for (TrkSegment ts : tr.segments) {
					for (WptPt p : ts.points) {
						res.add(createLocation(p));
					}
				}
			}
		}

		Route route = null;
		if (gpxFile.routes.size() > 0) {
			route = gpxFile.routes.get(0);
		}
		RouteDirectionInfo previous = null;
		if (route != null && route.points.size() > 0) {
			directions = new ArrayList<RouteDirectionInfo>();
			for (WptPt item :  route.points) {
				try {
					RouteDirectionInfo dirInfo = new RouteDirectionInfo();
					dirInfo.descriptionRoute = item.desc; //$NON-NLS-1$
					String stime = item.getExtensionsToRead().get("time");
					if (stime != null) {
						dirInfo.expectedTime = Integer.parseInt(stime);
					}
					String stype = item.getExtensionsToRead().get("turn"); //$NON-NLS-1$
					if (stype != null) {
						dirInfo.turnType = TurnType.valueOf(stype.toUpperCase());
					} else {
						dirInfo.turnType = TurnType.valueOf(TurnType.C);
					}
					String sturn = item.getExtensionsToRead().get("turn-angle"); //$NON-NLS-1$
					if (sturn != null) {
						dirInfo.turnType.setTurnAngle((float) Double.parseDouble(sturn));
					}

					int offset = Integer.parseInt(item.getExtensionsToRead().get("offset")); //$NON-NLS-1$
					dirInfo.routePointOffset = offset;

					if (previous != null && previous.turnType != null && !TurnType.C.equals(previous.turnType.getValue()) &&
							!osmandRouter) {
						// calculate angle
						if (previous.routePointOffset > 0) {
							float paz = res.get(previous.routePointOffset - 1).bearingTo(res.get(previous.routePointOffset));
							float caz;
							if (previous.turnType.isRoundAbout() && dirInfo.routePointOffset < res.size() - 1) {
								caz = res.get(dirInfo.routePointOffset).bearingTo(res.get(dirInfo.routePointOffset + 1));
							} else {
								caz = res.get(dirInfo.routePointOffset - 1).bearingTo(res.get(dirInfo.routePointOffset));
							}
							float angle = caz - paz;
							if (angle < 0) {
								angle += 360;
							} else if (angle > 360) {
								angle -= 360;
							}
							// that magic number helps to fix some errors for turn
							angle += 75;

							if (previous.turnType.getTurnAngle() < 0.5f) {
								previous.turnType.setTurnAngle(angle);
							}
						}
					}

					directions.add(dirInfo);

					previous = dirInfo;
				} catch (NumberFormatException e) {
					log.info("Exception", e); //$NON-NLS-1$
				} catch (IllegalArgumentException e) {
					log.info("Exception", e); //$NON-NLS-1$
				}
			}
		}
		if (previous != null && previous.turnType != null && !TurnType.C.equals(previous.turnType.getValue())) {
			// calculate angle
			if (previous.routePointOffset > 0 && previous.routePointOffset < res.size() - 1) {
				float paz = res.get(previous.routePointOffset - 1).bearingTo(res.get(previous.routePointOffset));
				float caz = res.get(previous.routePointOffset).bearingTo(res.get(res.size() - 1));
				float angle = caz - paz;
				if (angle < 0) {
					angle += 360;
				}
				if (previous.turnType.getTurnAngle() < 0.5f) {
					previous.turnType.setTurnAngle(angle);
				}
			}
		}
		return directions;
	}
	
	
	
	public GPXFile createOsmandRouterGPX(int currentRoute, List<Location> routeNodes, int currentDirectionInfo, List<RouteDirectionInfo> directionInfo){
		GPXFile gpx = new GPXFile();
		gpx.author = OSMAND_ROUTER;
		Track track = new Track();
		gpx.tracks.add(track);
		TrkSegment trkSegment = new TrkSegment();
		track.segments.add(trkSegment);
		int cRoute = currentRoute;
		int cDirInfo = currentDirectionInfo;
		
		int rNsize = routeNodes.size();
		for(int i = cRoute; i< rNsize; i++){
			Location loc = routeNodes.get(i);
			WptPt pt = new WptPt();
			pt.lat = loc.getLatitude();
			pt.lon = loc.getLongitude();
			if(loc.hasSpeed()){
				pt.speed = loc.getSpeed();
			}
			if(loc.hasAltitude()){
				pt.ele = loc.getAltitude();
			}
			if(loc.hasAccuracy()){
				pt.hdop = loc.getAccuracy();
			}
			trkSegment.points.add(pt);
		}
		Route route = new Route();
		gpx.routes.add(route);
		int dIsize = directionInfo.size();
		for (int i = cDirInfo; i < dIsize; i++) {
			RouteDirectionInfo dirInfo = directionInfo.get(i);
			if (dirInfo.routePointOffset >= cRoute) {
				Location loc = routeNodes.get(dirInfo.routePointOffset);
				WptPt pt = new WptPt();
				pt.lat = loc.getLatitude();
				pt.lon = loc.getLongitude();
				pt.desc = dirInfo.descriptionRoute;
				Map<String, String> extensions = pt.getExtensionsToWrite();
				extensions.put("time", dirInfo.expectedTime + "");
				String turnType = dirInfo.turnType.getValue();
				if (dirInfo.turnType.isRoundAbout()) {
					turnType += dirInfo.turnType.getExitOut();
				}
				if(!TurnType.C.equals(turnType)){
					extensions.put("turn", turnType);
					extensions.put("turn-angle", dirInfo.turnType.getTurnAngle() + "");
				}
				extensions.put("offset", (dirInfo.routePointOffset - cRoute) + "");
				route.points.add(pt);
			}
		}
		return gpx;
	}
	
}
