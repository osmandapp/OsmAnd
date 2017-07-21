package net.osmand.plus.routing;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.Version;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.router.PrecalculatedRouteDirection;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingContext;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import net.osmand.util.GeoPolylineParserUtil;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import btools.routingapp.IBRouterService;


public class RouteProvider {
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(RouteProvider.class);
	private static final String OSMAND_ROUTER = "OsmAndRouter";

	public enum RouteService {
			OSMAND("OsmAnd (offline)"), YOURS("YOURS"), 
//			ORS("OpenRouteService"), // disable ors due to no public rest service (testing2015 doesn't seem stable) 
			OSRM("OSRM (only car)"),
			BROUTER("BRouter (offline)"), STRAIGHT("Straight line");

		private final String name;

		private RouteService(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public boolean isOnline() {
			return this != OSMAND && this != BROUTER;
		}

		boolean isAvailable(OsmandApplication ctx) {
			if (this == BROUTER) {
				return ctx.getBRouterService() != null;
			}
			return true;
		}

		public static RouteService[] getAvailableRouters(OsmandApplication ctx) {
			List<RouteService> list = new ArrayList<RouteProvider.RouteService>();
			for(RouteService r : values()) {
				if (r.isAvailable(ctx)) {
					list.add(r);
				}
			}
			return list.toArray(new RouteService[list.size()]);
		}
	}

	public RouteProvider() {
	}

	public static class GPXRouteParamsBuilder {
		boolean calculateOsmAndRoute = false;
		// parameters
		private final GPXFile file;
		private boolean reverse;
		private boolean leftSide;
		private boolean passWholeRoute;
		private boolean calculateOsmAndRouteParts;
		private boolean useIntermediatePointsRTE;

		public GPXRouteParamsBuilder(GPXFile file, OsmandSettings settings) {
			leftSide = settings.DRIVING_REGION.get().leftHandDriving;
			this.file = file;
		}

		public boolean isReverse() {
			return reverse;
		}

		public boolean isCalculateOsmAndRouteParts() {
			return calculateOsmAndRouteParts;
		}

		public void setCalculateOsmAndRouteParts(boolean calculateOsmAndRouteParts) {
			this.calculateOsmAndRouteParts = calculateOsmAndRouteParts;
		}

		public void setUseIntermediatePointsRTE(boolean useIntermediatePointsRTE) {
			this.useIntermediatePointsRTE = useIntermediatePointsRTE;
		}

		public boolean isUseIntermediatePointsRTE() {
			return useIntermediatePointsRTE;
		}

		public boolean isCalculateOsmAndRoute() {
			return calculateOsmAndRoute;
		}

		public void setCalculateOsmAndRoute(boolean calculateOsmAndRoute) {
			this.calculateOsmAndRoute = calculateOsmAndRoute;
		}

		public void setPassWholeRoute(boolean passWholeRoute) {
			this.passWholeRoute = passWholeRoute;
		}

		public boolean isPassWholeRoute() {
			return passWholeRoute;
		}

		public GPXRouteParams build(Location start, OsmandSettings settings) {
			GPXRouteParams res = new GPXRouteParams();
			res.prepareGPXFile(this);
//			if (passWholeRoute && start != null) {
//				res.points.add(0, start);
//			}
			return res;
		}

		public void setReverse(boolean reverse) {
			this.reverse = reverse;
		}

		public GPXFile getFile() {
			return file;
		}

		public List<Location> getPoints() {
			GPXRouteParams copy = new GPXRouteParams();
			copy.prepareGPXFile(this);
			return copy.getPoints();
		}
	}

	public static class GPXRouteParams {
		List<Location> points = new ArrayList<Location>();
		List<RouteDirectionInfo> directions;
		boolean calculateOsmAndRoute;
		boolean passWholeRoute;
		boolean calculateOsmAndRouteParts;
		boolean useIntermediatePointsRTE;
		private List<LocationPoint> wpt;

		boolean addMissingTurns = true;

		public List<Location> getPoints() {
			return points;
		}

		public Location getStartPointForRoute(){
			if(!points.isEmpty()){
				return points.get(0);
			}
			return null;
		}

		public Location getEndPointForRoute(){
			if(!points.isEmpty()){
				return points.get(points.size());
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

		public GPXRouteParams prepareGPXFile(GPXRouteParamsBuilder builder) {
			GPXFile file = builder.file;
			boolean reverse = builder.reverse; 
			passWholeRoute = builder.passWholeRoute;
			calculateOsmAndRouteParts = builder.calculateOsmAndRouteParts;
			useIntermediatePointsRTE = builder.useIntermediatePointsRTE;
			builder.calculateOsmAndRoute = false; // Disabled temporary builder.calculateOsmAndRoute;
			if (!file.points.isEmpty()) {
				wpt = new ArrayList<LocationPoint>(file.points);
			}
			if (file.isCloudmadeRouteFile() || OSMAND_ROUTER.equals(file.author)) {
				directions =  parseOsmAndGPXRoute(points, file, OSMAND_ROUTER.equals(file.author), builder.leftSide, 10);
				if (OSMAND_ROUTER.equals(file.author)) {
					// For files generated by OSMAND_ROUTER use directions contained unaltered
					addMissingTurns = false;
				}
				if (reverse) {
					// clear directions all turns should be recalculated
					directions = null;
					Collections.reverse(points);
					addMissingTurns = true;
				}
			} else {
				// first of all check tracks
				if (!useIntermediatePointsRTE) {
					for (Track tr : file.tracks) {
						for (TrkSegment tkSeg : tr.segments) {
							for (WptPt pt : tkSeg.points) {
								points.add(createLocation(pt));
							}
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
			return this;
		}
	}

	private static Location createLocation(WptPt pt){
		Location loc = new Location("OsmandRouteProvider");
		loc.setLatitude(pt.lat);
		loc.setLongitude(pt.lon);
		loc.setSpeed((float) pt.speed);
		if(!Double.isNaN(pt.ele)) {
			loc.setAltitude(pt.ele);
		}
		loc.setTime(pt.time);
		if(!Double.isNaN(pt.hdop)) {
			loc.setAccuracy((float) pt.hdop);
		}
		return loc;
	}

	public RouteCalculationResult calculateRouteImpl(RouteCalculationParams params){
		long time = System.currentTimeMillis();
		if (params.start != null && params.end != null) {
			if(log.isInfoEnabled()){
				log.info("Start finding route from " + params.start + " to " + params.end +" using " + 
						params.type.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try {
				RouteCalculationResult res;
				boolean calcGPXRoute = params.gpxRoute != null && !params.gpxRoute.points.isEmpty();
				if(calcGPXRoute && !params.gpxRoute.calculateOsmAndRoute){
					res = calculateGpxRoute(params);
				} else if (params.type == RouteService.OSMAND) {
					res = findVectorMapsRoute(params, calcGPXRoute);
				} else if (params.type == RouteService.BROUTER) {
					res = findBROUTERRoute(params);
				} else if (params.type == RouteService.YOURS) {
					res = findYOURSRoute(params);
//				} else if (params.type == RouteService.ORS) {
//					res = findORSRoute(params);
				} else if (params.type == RouteService.OSRM) {
					res = findOSRMRoute(params);
				} else if (params.type == RouteService.STRAIGHT){
					res = findStraightRoute(params);
				}
				else {
					res = new RouteCalculationResult("Selected route service is not available");
				}
				if(log.isInfoEnabled() ){
					log.info("Finding route contained " + res.getImmutableAllLocations().size() + " points for " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return res; 
			} catch (IOException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (ParserConfigurationException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (SAXException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (JSONException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			}
		}
		return new RouteCalculationResult(null);
	}

	public RouteCalculationResult recalculatePartOfflineRoute(RouteCalculationResult res, RouteCalculationParams params) {
		RouteCalculationResult rcr = params.previousToRecalculate;
		List<Location> locs = new ArrayList<Location>(rcr.getRouteLocations());
		try {
			int[] startI = new int[]{0};
			int[] endI = new int[]{locs.size()}; 
			locs = findStartAndEndLocationsFromRoute(locs, params.start, params.end, startI, endI);
			List<RouteDirectionInfo> directions = calcDirections(startI, endI, rcr.getRouteDirections());
			insertInitialSegment(params, locs, directions, true);
			res = new RouteCalculationResult(locs, directions, params, null, true);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		return res;
	}

	private RouteCalculationResult calculateGpxRoute(RouteCalculationParams routeParams) throws IOException {
		// get the closest point to start and to end
		GPXRouteParams gpxParams = routeParams.gpxRoute;
		if(routeParams.gpxRoute.useIntermediatePointsRTE){
			return calculateOsmAndRouteWithIntermediatePoints(routeParams, gpxParams.points);
		}
		List<Location> gpxRoute ;
		int[] startI = new int[]{0};
		int[] endI = new int[]{gpxParams.points.size()}; 
		if(routeParams.gpxRoute.passWholeRoute) {
			gpxRoute = gpxParams.points;
		} else {
			gpxRoute = findStartAndEndLocationsFromRoute(gpxParams.points,
					routeParams.start, routeParams.end, startI, endI);
		}
		final List<RouteDirectionInfo> inputDirections = gpxParams.directions;
		List<RouteDirectionInfo> gpxDirections = calcDirections(startI, endI, inputDirections);
		boolean calculateOsmAndRouteParts = gpxParams.calculateOsmAndRouteParts;
		insertInitialSegment(routeParams, gpxRoute, gpxDirections, calculateOsmAndRouteParts);
		insertFinalSegment(routeParams, gpxRoute, gpxDirections, calculateOsmAndRouteParts);

		for (RouteDirectionInfo info : gpxDirections) {
			// recalculate
			info.distance = 0;
			info.afterLeftTime = 0;			
		}
		RouteCalculationResult res = new RouteCalculationResult(gpxRoute, gpxDirections, routeParams, 
				gpxParams  == null? null: gpxParams.wpt, routeParams.gpxRoute.addMissingTurns);
		return res;
	}

	private RouteCalculationResult calculateOsmAndRouteWithIntermediatePoints(RouteCalculationParams routeParams,
			final List<Location> intermediates) throws IOException {
		RouteCalculationParams rp = new RouteCalculationParams();
		rp.calculationProgress = routeParams.calculationProgress;
		rp.ctx = routeParams.ctx;
		rp.mode = routeParams.mode;
		rp.start = routeParams.start;
		rp.end = routeParams.end;
		rp.leftSide = routeParams.leftSide;
		rp.type = routeParams.type;
		rp.fast = routeParams.fast;
		rp.onlyStartPointChanged = routeParams.onlyStartPointChanged;
		rp.previousToRecalculate =  routeParams.previousToRecalculate;
		rp.intermediates = new ArrayList<LatLon>();
		int closest = 0;
		double maxDist = Double.POSITIVE_INFINITY;
		for (int i = 0; i < intermediates.size(); i++) {
			Location loc = intermediates.get(i);
			double dist = MapUtils.getDistance(loc.getLatitude(), loc.getLongitude(), rp.start.getLatitude(),
					rp.start.getLongitude());
			if (dist <= maxDist) {
				closest = i;
				maxDist = dist;
			}
		}
		for(int i = closest; i< intermediates.size() ; i++ ){
			Location w = intermediates.get(i);
			rp.intermediates.add(new LatLon(w.getLatitude(), w.getLongitude()));
		}
		return findVectorMapsRoute(rp, false);
	}

	private List<RouteDirectionInfo> calcDirections(int[] startI, int[] endI,
			final List<RouteDirectionInfo> inputDirections) {
		List<RouteDirectionInfo> directions = new ArrayList<RouteDirectionInfo>();
		if (inputDirections != null) {
			for (RouteDirectionInfo info : inputDirections) {
				if (info.routePointOffset >= startI[0] && info.routePointOffset < endI[0]) {
					RouteDirectionInfo ch = new RouteDirectionInfo(info.getAverageSpeed(), info.getTurnType());
					ch.routePointOffset = info.routePointOffset - startI[0];
					if(info.routeEndPointOffset != 0) {
						ch.routeEndPointOffset = info.routeEndPointOffset - startI[0];
					}
					ch.setDescriptionRoute(info.getDescriptionRoutePart());

					// Issue #2894
					if (info.getRef() != null && !"null".equals(info.getRef())) {
						ch.setRef(info.getRef());
					}
					if (info.getStreetName() != null && !"null".equals(info.getStreetName())) {
						ch.setStreetName(info.getStreetName());
					}
					if (info.getDestinationName() != null && !"null".equals(info.getDestinationName())) {
						ch.setDestinationName(info.getDestinationName());
					}

					directions.add(ch);
				}
			}
		}
		return directions;
	}

	private void insertFinalSegment(RouteCalculationParams routeParams, List<Location> points,
			List<RouteDirectionInfo> directions, boolean calculateOsmAndRouteParts) {
		if(points.size() > 0) {
			Location routeEnd = points.get(points.size() - 1);
			LatLon e = routeEnd == null ? null : new LatLon(routeEnd.getLatitude(), routeEnd.getLongitude());
			LatLon finalEnd = routeParams.end;
			if (finalEnd != null && MapUtils.getDistance(finalEnd, e) > 60) {
				RouteCalculationResult newRes = null;
				if (calculateOsmAndRouteParts) {
					newRes = findOfflineRouteSegment(routeParams, routeEnd, finalEnd);
				}
				List<Location> loct;
				List<RouteDirectionInfo> dt;
				if (newRes != null && newRes.isCalculated()) {
					loct = newRes.getImmutableAllLocations();
					dt = newRes.getImmutableAllDirections();
				} else {
					loct = new ArrayList<Location>();
					Location l = new Location("");
					l.setLatitude(finalEnd.getLatitude());
					l.setLongitude(finalEnd.getLongitude());
					loct.add(l);
					dt = new ArrayList<RouteDirectionInfo>();
				}
				for (RouteDirectionInfo i : dt) {
					i.routePointOffset += points.size();
				}
				points.addAll(loct);
				directions.addAll(dt);
			}
		}
	}

	public void insertInitialSegment(RouteCalculationParams routeParams, List<Location> points,
			List<RouteDirectionInfo> directions, boolean calculateOsmAndRouteParts) {
		Location realStart = routeParams.start;
		if (realStart != null && points.size() > 0 && realStart.distanceTo(points.get(0)) > 60) {
			Location trackStart = points.get(0);
			RouteCalculationResult newRes = null;
			if (calculateOsmAndRouteParts) {
				LatLon end = new LatLon(trackStart.getLatitude(), trackStart.getLongitude());
				newRes = findOfflineRouteSegment(routeParams, realStart, end);
			}
			List<Location> loct;
			List<RouteDirectionInfo> dt;
			if (newRes != null && newRes.isCalculated()) {
				loct = newRes.getImmutableAllLocations();
				dt = newRes.getImmutableAllDirections();
			} else {
				loct = new ArrayList<Location>();
				loct.add(realStart);
				dt = new ArrayList<RouteDirectionInfo>();
			}
			points.addAll(0, loct);
			directions.addAll(0, dt);
			for (int i = dt.size(); i < directions.size(); i++) {
				directions.get(i).routePointOffset += loct.size();
			}
		}
	}

	private RouteCalculationResult findOfflineRouteSegment(RouteCalculationParams rParams, Location start, 
			LatLon end) {
		RouteCalculationParams newParams = new RouteCalculationParams();
		newParams.start = start;
		newParams.end = end;
		newParams.ctx = rParams.ctx;
		newParams.calculationProgress = rParams.calculationProgress;
		newParams.mode = rParams.mode;
		newParams.type = RouteService.OSMAND;
		newParams.leftSide = rParams.leftSide;
		RouteCalculationResult newRes = null;
		try {
			newRes = findVectorMapsRoute(newParams, false);
		} catch (IOException e) {
		}
		return newRes;
	}

	private ArrayList<Location> findStartAndEndLocationsFromRoute(List<Location> route, Location startLoc, LatLon endLoc, int[] startI, int[] endI) {
		float minDist = Integer.MAX_VALUE;
		int start = 0;
		int end = route.size();
		if (startLoc != null) {
			for (int i = 0; i < route.size(); i++) {
				float d = route.get(i).distanceTo(startLoc);
				if (d < minDist) {
					start = i;
					minDist = d;
				}
			}
		} else {
			startLoc = route.get(0);
		}
		Location l = new Location("temp"); //$NON-NLS-1$
		l.setLatitude(endLoc.getLatitude());
		l.setLongitude(endLoc.getLongitude());
		minDist = Integer.MAX_VALUE;
		// get in reverse order taking into account ways with cycle
		for (int i = route.size() - 1; i >= start; i--) {
			float d = route.get(i).distanceTo(l);
			if (d < minDist) {
				end = i + 1;
				// slightly modify to allow last point to be added
				minDist = d - 40;
			}
		}
		ArrayList<Location> sublist = new ArrayList<Location>(route.subList(start, end));
		if(startI != null) {
			startI[0] = start;
		}
		if(endI != null) {
			endI[0] = end;
		}
		return sublist;
	}

	protected String getString(Context ctx, int resId){
		if(ctx == null){
			return ""; //$NON-NLS-1$
		}
		return ctx.getString(resId);
	}

	protected RouteCalculationResult findYOURSRoute(RouteCalculationParams params) throws MalformedURLException, IOException,
			ParserConfigurationException, FactoryConfigurationError, SAXException {
		List<Location> res = new ArrayList<Location>();
		StringBuilder uri = new StringBuilder();
		uri.append("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml"); //$NON-NLS-1$
		uri.append("&flat=").append(params.start.getLatitude()); //$NON-NLS-1$
		uri.append("&flon=").append(params.start.getLongitude()); //$NON-NLS-1$
		uri.append("&tlat=").append(params.end.getLatitude()); //$NON-NLS-1$
		uri.append("&tlon=").append(params.end.getLongitude()); //$NON-NLS-1$
		if (params.mode.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			uri.append("&v=bicycle") ; //$NON-NLS-1$
		} else if (params.mode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			uri.append("&v=foot") ; //$NON-NLS-1$
		} else if(params.mode.isDerivedRoutingFrom(ApplicationMode.CAR)){
			uri.append("&v=motorcar"); //$NON-NLS-1$
		} else {
			return applicationModeNotSupported(params);
		}
		uri.append("&fast=").append(params.fast ? "1" : "0").append("&layer=mapnik"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		log.info("URL route " + uri);
		URLConnection connection = NetworkUtils.getHttpURLConnection(uri.toString());
		connection.setRequestProperty("User-Agent", Version.getFullVersion(params.ctx));
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
		params.intermediates = null;
		return new RouteCalculationResult(res, null, params, null, true);
	}

	protected RouteCalculationResult findVectorMapsRoute(final RouteCalculationParams params, boolean calcGPXRoute) throws IOException {
		BinaryMapIndexReader[] files = params.ctx.getResourceManager().getRoutingMapFiles();
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd(false);
		OsmandSettings settings = params.ctx.getSettings();
		router.setUseFastRecalculation(settings.USE_FAST_RECALCULATION.get());
		
		RoutingConfiguration.Builder config = params.ctx.getDefaultRoutingConfig();
		GeneralRouter generalRouter = SettingsNavigationActivity.getRouter(config, params.mode);
		if(generalRouter == null) {
			return applicationModeNotSupported(params);
		}
		RoutingConfiguration cf = initOsmAndRoutingConfig(config, params, settings, generalRouter);
		if(cf == null){
			return applicationModeNotSupported(params);
		}
		PrecalculatedRouteDirection precalculated = null;
		if(calcGPXRoute) {
			ArrayList<Location> sublist = findStartAndEndLocationsFromRoute(params.gpxRoute.points,
					params.start, params.end, null, null);
			LatLon[] latLon = new LatLon[sublist.size()];
			for(int k = 0; k < latLon.length; k ++) {
				latLon[k] = new LatLon(sublist.get(k).getLatitude(), sublist.get(k).getLongitude());
			}
			precalculated = PrecalculatedRouteDirection.build(latLon, generalRouter.getMaxDefaultSpeed());
			precalculated.setFollowNext(true);
			//cf.planRoadDirection = 1;
		}
		// BUILD context
		NativeOsmandLibrary lib = settings.SAFE_MODE.get() ? null : NativeOsmandLibrary.getLoadedLibrary();
		// check loaded files
		int leftX = MapUtils.get31TileNumberX(params.start.getLongitude());
		int rightX = leftX;
		int bottomY = MapUtils.get31TileNumberY(params.start.getLatitude());
		int topY = bottomY;
		if (params.intermediates != null) {
			for (LatLon l : params.intermediates) {
				leftX = Math.min(MapUtils.get31TileNumberX(l.getLongitude()), leftX);
				rightX = Math.max(MapUtils.get31TileNumberX(l.getLongitude()), rightX);
				bottomY = Math.max(MapUtils.get31TileNumberY(l.getLatitude()), bottomY);
				topY = Math.min(MapUtils.get31TileNumberY(l.getLatitude()), topY);
			}
		}
		LatLon l = params.end;
		leftX = Math.min(MapUtils.get31TileNumberX(l.getLongitude()), leftX);
		rightX = Math.max(MapUtils.get31TileNumberX(l.getLongitude()), rightX);
		bottomY = Math.max(MapUtils.get31TileNumberY(l.getLatitude()), bottomY);
		topY = Math.min(MapUtils.get31TileNumberY(l.getLatitude()), topY);
		
		params.ctx.getResourceManager().getRenderer().checkInitialized(15, lib, leftX, rightX, bottomY, topY);
		
		RoutingContext ctx = router.buildRoutingContext(cf,
				lib, files, 
				RouteCalculationMode.NORMAL);
		
		RoutingContext complexCtx = null;
		boolean complex = params.mode.isDerivedRoutingFrom(ApplicationMode.CAR) && !settings.DISABLE_COMPLEX_ROUTING.get()
				&& precalculated == null;
		ctx.leftSideNavigation = params.leftSide;
		ctx.calculationProgress = params.calculationProgress;
		if(params.previousToRecalculate != null && params.onlyStartPointChanged) {
			int currentRoute = params.previousToRecalculate.getCurrentRoute();
			List<RouteSegmentResult> originalRoute = params.previousToRecalculate.getOriginalRoute();
			if(originalRoute != null && currentRoute < originalRoute.size()) {
				ctx.previouslyCalculatedRoute = originalRoute.subList(currentRoute, originalRoute.size());
			}
		}
		if(complex && router.getRecalculationEnd(ctx) != null) {
			complex = false;
		}
		if(complex) {
			complexCtx = router.buildRoutingContext(cf, lib,files,
				RouteCalculationMode.COMPLEX);
			complexCtx.calculationProgress = params.calculationProgress;
			complexCtx.leftSideNavigation = params.leftSide;
			complexCtx.previouslyCalculatedRoute = ctx.previouslyCalculatedRoute;
		}

		LatLon st = new LatLon(params.start.getLatitude(), params.start.getLongitude());
		LatLon en = new LatLon(params.end.getLatitude(), params.end.getLongitude());
		List<LatLon> inters  = new ArrayList<LatLon>();
		if (params.intermediates != null) {
			inters  = new ArrayList<LatLon>(params.intermediates);
		}
		return calcOfflineRouteImpl(params, router, ctx, complexCtx, st, en, inters, precalculated);
	}

	private RoutingConfiguration initOsmAndRoutingConfig(Builder config, final RouteCalculationParams params, OsmandSettings settings,
			GeneralRouter generalRouter) throws IOException, FileNotFoundException {
		GeneralRouterProfile p ;
		if (params.mode.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			p = GeneralRouterProfile.BICYCLE;
		} else if (params.mode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			p = GeneralRouterProfile.PEDESTRIAN;
		} else if(params.mode.isDerivedRoutingFrom(ApplicationMode.CAR)){
			p = GeneralRouterProfile.CAR;
		} else {
			return null;
		}
		
		Map<String, String> paramsR = new LinkedHashMap<String, String>();
		for(Map.Entry<String, RoutingParameter> e : generalRouter.getParameters().entrySet()){
			String key = e.getKey();
			RoutingParameter pr = e.getValue();
			String vl;
			if(key.equals(GeneralRouter.USE_SHORTEST_WAY)) {
				Boolean bool = !settings.FAST_ROUTE_MODE.getModeValue(params.mode);
				vl = bool ? "true" : null;
			} else if(pr.getType() == RoutingParameterType.BOOLEAN) {
				CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(key, pr.getDefaultBoolean());
				Boolean bool = pref.getModeValue(params.mode);
				vl = bool ? "true" : null;
			} else {
				vl = settings.getCustomRoutingProperty(key, "").getModeValue(params.mode);
			}
			if(vl != null && vl.length() > 0) {
				paramsR.put(key, vl);
			}
		}
		float mb = (1 << 20);
		Runtime rt = Runtime.getRuntime();
		// make visible
		int memoryLimit = (int) (0.95 * ((rt.maxMemory() - rt.totalMemory()) + rt.freeMemory()) / mb);
		log.warn("Use " + memoryLimit +  " MB Free " + rt.freeMemory() / mb + " of " + rt.totalMemory() / mb + " max " + rt.maxMemory() / mb);
		
		RoutingConfiguration cf = config.build(p.name().toLowerCase(), params.start.hasBearing() ? 
				params.start.getBearing() / 180d * Math.PI : null, 
				memoryLimit, paramsR);
		return cf;
	}

	private RouteCalculationResult calcOfflineRouteImpl(final RouteCalculationParams params,
			RoutePlannerFrontEnd router, RoutingContext ctx, RoutingContext complexCtx, LatLon st, LatLon en,
			List<LatLon> inters, PrecalculatedRouteDirection precalculated) throws IOException {
		try {
			List<RouteSegmentResult> result ;
			if(complexCtx != null) {
				try {
					result = router.searchRoute(complexCtx, st, en, inters, precalculated);
					// discard ctx and replace with calculated
					ctx = complexCtx;
				} catch(final RuntimeException e) {
					params.ctx.runInUIThread(new Runnable() {
						@Override
						public void run() {
							params.ctx.showToastMessage(R.string.complex_route_calculation_failed, e.getMessage());							
						}
					});
					result = router.searchRoute(ctx, st, en, inters);
				}
			} else {
				result = router.searchRoute(ctx, st, en, inters);
			}
			
			if(result == null || result.isEmpty()) {
				if(ctx.calculationProgress.segmentNotFound == 0) {
					return new RouteCalculationResult(params.ctx.getString(R.string.starting_point_too_far));
				} else if(ctx.calculationProgress.segmentNotFound == inters.size() + 1) {
					return new RouteCalculationResult(params.ctx.getString(R.string.ending_point_too_far));
				} else if(ctx.calculationProgress.segmentNotFound > 0) {
					return new RouteCalculationResult(params.ctx.getString(R.string.intermediate_point_too_far, "'" + ctx.calculationProgress.segmentNotFound + "'"));
				}
				if(ctx.calculationProgress.directSegmentQueueSize == 0) {
					return new RouteCalculationResult("Route can not be found from start point (" +ctx.calculationProgress.distanceFromBegin/1000f+" km)");
				} else if(ctx.calculationProgress.reverseSegmentQueueSize == 0) {
					return new RouteCalculationResult("Route can not be found from end point (" +ctx.calculationProgress.distanceFromEnd/1000f+" km)");
				}
				if(ctx.calculationProgress.isCancelled) {
					return interrupted();
				}
				// something really strange better to see that message on the scren
				return emptyResult();
			} else {
				RouteCalculationResult res = new RouteCalculationResult(result, params.start, params.end,
						params.intermediates, params.ctx, params.leftSide, ctx.routingTime, params.gpxRoute  == null? null: params.gpxRoute.wpt,
								params.mode);
				return res;
			}
		} catch (RuntimeException e) {
			return new RouteCalculationResult(e.getMessage() );
		} catch (InterruptedException e) {
			return interrupted();
		} catch (OutOfMemoryError e) {
//			ActivityManager activityManager = (ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
//			ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//			activityManager.getMemoryInfo(memoryInfo);
//			int avl = (int) (memoryInfo.availMem / (1 << 20));
			int max = (int) (Runtime.getRuntime().maxMemory() / (1 << 20)); 
			int avl = (int) (Runtime.getRuntime().freeMemory() / (1 << 20));
			String s = " (" + avl + " MB available of " + max  + ") ";
			return new RouteCalculationResult("Not enough process memory "+ s);
		}
	}

	private RouteCalculationResult applicationModeNotSupported(RouteCalculationParams params) {
		return new RouteCalculationResult("Application mode '"+ params.mode.toHumanStringCtx(params.ctx)+ "'is not supported.");
	}

	private RouteCalculationResult interrupted() {
		return new RouteCalculationResult("Route calculation was interrupted");
	}

	private RouteCalculationResult emptyResult() {
		return new RouteCalculationResult("Empty result");
	}

	private static List<RouteDirectionInfo> parseOsmAndGPXRoute(List<Location> res, GPXFile gpxFile, boolean osmandRouter,
			boolean leftSide, float defSpeed) {
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
		float[] distanceToEnd  = new float[res.size()];
		for (int i = res.size() - 2; i >= 0; i--) {
			distanceToEnd[i] = distanceToEnd[i + 1] + res.get(i).distanceTo(res.get(i + 1));
		}

		Route route = null;
		if (gpxFile.routes.size() > 0) {
			route = gpxFile.routes.get(0);
		}
		RouteDirectionInfo previous = null;
		if (route != null && route.points.size() > 0) {
			directions = new ArrayList<RouteDirectionInfo>();
			Iterator<WptPt> iterator = route.points.iterator();
			while(iterator.hasNext()){
				WptPt item = iterator.next();
				try {
					String stime = item.getExtensionsToRead().get("time");
					int time  = 0;
					if (stime != null) {
						time = Integer.parseInt(stime);
					}
					int offset = Integer.parseInt(item.getExtensionsToRead().get("offset")); //$NON-NLS-1$
					if(directions.size() > 0) {
						RouteDirectionInfo last = directions.get(directions.size() - 1);
						// update speed using time and idstance
						last.setAverageSpeed((distanceToEnd[last.routePointOffset] - distanceToEnd[offset])/last.getAverageSpeed());
						last.distance = (int) Math.round(distanceToEnd[last.routePointOffset] - distanceToEnd[offset]);
					} 
					// save time as a speed because we don't know distance of the route segment
					float avgSpeed = time;
					if(!iterator.hasNext() && time > 0) {
						avgSpeed = distanceToEnd[offset] / time;
					}
					String stype = item.getExtensionsToRead().get("turn"); //$NON-NLS-1$
					TurnType turnType;
					if (stype != null) {
						turnType = TurnType.fromString(stype.toUpperCase(), leftSide);
					} else {
						turnType = TurnType.straight();
					}
					String sturn = item.getExtensionsToRead().get("turn-angle"); //$NON-NLS-1$
					if (sturn != null) {
						turnType.setTurnAngle((float) Double.parseDouble(sturn));
					}
					RouteDirectionInfo dirInfo = new RouteDirectionInfo(avgSpeed, turnType);
					dirInfo.setDescriptionRoute(item.desc); //$NON-NLS-1$
					dirInfo.routePointOffset = offset;

					// Issue #2894
					String sref = item.getExtensionsToRead().get("ref"); //$NON-NLS-1$
					if (sref != null && !"null".equals(sref)) {
						dirInfo.setRef(sref); //$NON-NLS-1$
					}
					String sstreetname = item.getExtensionsToRead().get("street-name"); //$NON-NLS-1$
					if (sstreetname != null && !"null".equals(sstreetname)) {
						dirInfo.setStreetName(sstreetname); //$NON-NLS-1$
					}
					String sdest = item.getExtensionsToRead().get("dest"); //$NON-NLS-1$
					if (sdest != null && !"null".equals(sdest)) {
						dirInfo.setDestinationName(sdest); //$NON-NLS-1$
					}

					if (previous != null && TurnType.C != previous.getTurnType().getValue() &&
							!osmandRouter) {
						// calculate angle
						if (previous.routePointOffset > 0) {
							float paz = res.get(previous.routePointOffset - 1).bearingTo(res.get(previous.routePointOffset));
							float caz;
							if (previous.getTurnType().isRoundAbout() && dirInfo.routePointOffset < res.size() - 1) {
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

							if (previous.getTurnType().getTurnAngle() < 0.5f) {
								previous.getTurnType().setTurnAngle(angle);
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
		if (previous != null && TurnType.C != previous.getTurnType().getValue()) {
			// calculate angle
			if (previous.routePointOffset > 0 && previous.routePointOffset < res.size() - 1) {
				float paz = res.get(previous.routePointOffset - 1).bearingTo(res.get(previous.routePointOffset));
				float caz = res.get(previous.routePointOffset).bearingTo(res.get(res.size() - 1));
				float angle = caz - paz;
				if (angle < 0) {
					angle += 360;
				}
				if (previous.getTurnType().getTurnAngle() < 0.5f) {
					previous.getTurnType().setTurnAngle(angle);
				}
			}
		}
		return directions;
	}

	public GPXFile createOsmandRouterGPX(RouteCalculationResult srcRoute, OsmandApplication ctx) {	
		TargetPointsHelper helper = ctx.getTargetPointsHelper();
		int currentRoute = srcRoute.currentRoute;
		List<Location> routeNodes = srcRoute.getImmutableAllLocations();
		List<RouteDirectionInfo> directionInfo = srcRoute.getImmutableAllDirections();
		int currentDirectionInfo = srcRoute.currentDirectionInfo;

		GPXFile gpx = new GPXFile();
		gpx.author = OSMAND_ROUTER;
		Track track = new Track();
		gpx.tracks.add(track);
		TrkSegment trkSegment = new TrkSegment();
		track.segments.add(trkSegment);
		int cRoute = currentRoute;
		int cDirInfo = currentDirectionInfo;

		// Save the start point to gpx file's trkpt section unless already contained
		WptPt startpoint = new WptPt();
		TargetPoint sc = helper.getPointToStart();
		int routePointOffsetAdjusted = 0;
		if (sc != null && ((float) sc.getLatitude() != (float) routeNodes.get(cRoute).getLatitude() || (float) sc.getLongitude() != (float) routeNodes.get(cRoute).getLongitude())){
			startpoint.lat = sc.getLatitude();
			startpoint.lon = sc.getLongitude();
			trkSegment.points.add(startpoint);
			if (directionInfo != null && !directionInfo.isEmpty()) {
				for (RouteDirectionInfo i : directionInfo) {
					i.routePointOffset++;
				}
			}
			routePointOffsetAdjusted = 1;
		}

		for (int i = cRoute; i< routeNodes.size(); i++) {
			Location loc = routeNodes.get(i);
			WptPt pt = new WptPt();
			pt.lat = loc.getLatitude();
			pt.lon = loc.getLongitude();
			if (loc.hasSpeed()) {
				pt.speed = loc.getSpeed();
			}
			if (loc.hasAltitude()) {
				pt.ele = loc.getAltitude();
			}
			if (loc.hasAccuracy()) {
				pt.hdop = loc.getAccuracy();
			}
			trkSegment.points.add(pt);
		}

		Route route = new Route();
		gpx.routes.add(route);
		for (int i = cDirInfo; i < directionInfo.size(); i++) {
			RouteDirectionInfo dirInfo = directionInfo.get(i);
			if (dirInfo.routePointOffset - routePointOffsetAdjusted >= cRoute) {
				if (dirInfo.getTurnType() != null && !dirInfo.getTurnType().isSkipToSpeak() &&
						dirInfo.routePointOffset - routePointOffsetAdjusted < routeNodes.size()) {
					Location loc = routeNodes.get(dirInfo.routePointOffset - routePointOffsetAdjusted);
					WptPt pt = new WptPt();
					pt.lat = loc.getLatitude();
					pt.lon = loc.getLongitude();

					// Collect distances and times for subsequent suppressed turns
					int collectedDistance = 0;
					int collectedTime = 0;
					for (int j = i + 1; j < directionInfo.size(); j++) {
						if (directionInfo.get(j).getTurnType() != null && directionInfo.get(j).getTurnType().isSkipToSpeak()) {
							collectedDistance += directionInfo.get(j).getDistance();
							collectedTime += directionInfo.get(j).getExpectedTime();
						} else {
							break;
						}
					}
					pt.desc = dirInfo.getDescriptionRoute(ctx, collectedDistance + dirInfo.getDistance());
					Map<String, String> extensions = pt.getExtensionsToWrite();
					extensions.put("time", (collectedTime + dirInfo.getExpectedTime()) + "");
					int turnType = dirInfo.getTurnType().getValue();
					if (TurnType.C != turnType) {
						extensions.put("turn", dirInfo.getTurnType().toXmlString());
						extensions.put("turn-angle", dirInfo.getTurnType().getTurnAngle() + "");
					}
					extensions.put("offset", (dirInfo.routePointOffset - cRoute) + "");

					// Issue #2894
					if (dirInfo.getRef() != null && !"null".equals(dirInfo.getRef())) {
						extensions.put("ref", dirInfo.getRef() + "");
					}
					if (dirInfo.getStreetName() != null && !"null".equals(dirInfo.getStreetName())) {
						extensions.put("street-name", dirInfo.getStreetName() + "");
					}
					if (dirInfo.getDestinationName() != null && !"null".equals(dirInfo.getDestinationName())) {
						extensions.put("dest", dirInfo.getDestinationName() + "");
					}

					route.points.add(pt);
				}
			}
		}
		List<TargetPoint> ps = helper.getIntermediatePointsWithTarget();
		for (int k = 0; k < ps.size(); k++) {
			WptPt pt = new WptPt();
			pt.lat = ps.get(k).getLatitude();
			pt.lon = ps.get(k).getLongitude();
			if (k < ps.size()) {
				pt.name = ps.get(k).getOnlyName() +"";
				if (k == ps.size() - 1) {
					String target = ctx.getString(R.string.destination_point, "");
					if (pt.name.startsWith(target)) {
						pt.name = ctx.getString(R.string.destination_point, pt.name);
					}
				} else {
					String prefix = (k + 1) +". ";
					if(Algorithms.isEmpty(pt.name)) {
						pt.name = ctx.getString(R.string.target_point, pt.name);
					}
					if (pt.name.startsWith(prefix)) {
						pt.name = prefix + pt.name;
					}
				}
				pt.desc = pt.name;
			}
			gpx.points.add(pt);
		}
	return gpx;
	}

	private void appendOSRMLoc(StringBuilder uri, LatLon il) {
		uri.append(";").append(String.valueOf(il.getLongitude()));
		uri.append(",").append(String.valueOf(il.getLatitude()));
	}

	protected RouteCalculationResult findOSRMRoute(RouteCalculationParams params)
			throws MalformedURLException, IOException, JSONException {
		// http://router.project-osrm.org/route/v1/driving/4.83,52.28;4.95,52.28
		List<Location> res = new ArrayList<Location>();
		StringBuilder uri = new StringBuilder();
		// possibly hide that API key because it is privacy of osmand
		// A6421860EBB04234AB5EF2D049F2CD8F key is compromised
		String scheme = "";
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			scheme = "https";
		} else {
			scheme = "http";
		}
		uri.append(scheme + "://router.project-osrm.org/route/v1/driving/"); //$NON-NLS-1$
		uri.append(String.valueOf(params.start.getLongitude()));
		uri.append(",").append(String.valueOf(params.start.getLatitude()));
		if(params.intermediates != null && params.intermediates.size() > 0) {
			for(LatLon il : params.intermediates) {
				appendOSRMLoc(uri, il);
			}
		}
		appendOSRMLoc(uri, params.end);
// to get more waypoints, add overview=full option
//		uri.append("?overview=full")

		log.info("URL route " + uri);

		URLConnection connection = NetworkUtils.getHttpURLConnection(uri.toString());
		connection.setRequestProperty("User-Agent", Version.getFullVersion(params.ctx));
		StringBuilder content = new StringBuilder();
		BufferedReader rs = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String s;
		while((s = rs.readLine()) != null) {
			content.append(s);
		}
		JSONObject obj = new JSONObject(content.toString());
		try {
			rs.close();
		} catch(IOException e){
		}
		List<LatLon> route = GeoPolylineParserUtil.parse(obj.getJSONArray("routes").getJSONObject(0).getString("geometry"),
				GeoPolylineParserUtil.PRECISION_5);
		if (route.isEmpty()) {
			return new RouteCalculationResult("Route is empty");
		}
		for (LatLon pt : route) {
			WptPt wpt = new WptPt();
			wpt.lat = pt.getLatitude();
			wpt.lon = pt.getLongitude();
			res.add(createLocation(wpt));
		}
		params.intermediates = null;
		return new RouteCalculationResult(res, null, params, null, true);
	}

	protected RouteCalculationResult findBROUTERRoute(RouteCalculationParams params) throws MalformedURLException,
			IOException, ParserConfigurationException, FactoryConfigurationError, SAXException {
		int numpoints = 2 + (params.intermediates != null ? params.intermediates.size() : 0);
		double[] lats = new double[numpoints];
		double[] lons = new double[numpoints];
		int index = 0;
		String mode;
		lats[index] = params.start.getLatitude();
		lons[index] = params.start.getLongitude();
		index++;
		if(params.intermediates != null && params.intermediates.size() > 0) {
			for(LatLon il : params.intermediates) {
				lats[index] = il.getLatitude();
				lons[index] = il.getLongitude();
				index++;
			}
		}
		lats[index] = params.end.getLatitude();
		lons[index] = params.end.getLongitude();
		if (ApplicationMode.PEDESTRIAN == params.mode) {
			mode = "foot"; //$NON-NLS-1$
		} else if (ApplicationMode.BICYCLE == params.mode) {
			mode = "bicycle"; //$NON-NLS-1$
		} else {
			mode = "motorcar"; //$NON-NLS-1$
		}
		Bundle bpars = new Bundle();
		bpars.putDoubleArray("lats", lats);
		bpars.putDoubleArray("lons", lons);
		bpars.putString("fast", params.fast ? "1" : "0");
		bpars.putString("v", mode);
		bpars.putString("trackFormat", "gpx");

		OsmandApplication ctx = (OsmandApplication) params.ctx;
		List<Location> res = new ArrayList<Location>();

		IBRouterService brouterService = ctx.getBRouterService();
		if (brouterService == null) {
			return new RouteCalculationResult("BRouter service is not available");
		}
		try {
			String gpxMessage = brouterService.getTrackFromParams(bpars);
			if (gpxMessage == null)
				gpxMessage = "no result from brouter";
			if (!gpxMessage.startsWith("<")) {
				return new RouteCalculationResult(gpxMessage);
			}

			GPXFile gpxFile = GPXUtilities.loadGPXFile(
					ctx, new ByteArrayInputStream(gpxMessage.getBytes("UTF-8")));

			for (Track track : gpxFile.tracks) {
				for (TrkSegment ts : track.segments) {
					for (WptPt p : ts.points) {
						Location l = new Location("router"); //$NON-NLS-1$
						l.setLatitude(p.lat);
						l.setLongitude(p.lon);
						if (p.ele != Double.NaN) {
							l.setAltitude(p.ele);
						}
						res.add(l);
					}
				}
			}
		} catch (Exception e) {
			return new RouteCalculationResult("Exception calling BRouter: " + e); //$NON-NLS-1$
		}
		return new RouteCalculationResult(res, null, params, null, true);
	}

	private RouteCalculationResult findStraightRoute(RouteCalculationParams params) {
		double[] lats = new double[] { params.start.getLatitude(), params.end.getLatitude() };
		double[] lons = new double[] { params.start.getLongitude(), params.end.getLongitude() };
		List<LatLon> intermediates = params.intermediates;
		List<Location> dots = new ArrayList<Location>();
		//writing start location
		Location location = new Location(String.valueOf("start"));
		location.setLatitude(lats[0]);
		location.setLongitude(lons[0]);
		//adding intermediate dots if they exists
		if (intermediates != null){
			for(int i =0; i<intermediates.size();i++){
				location = new Location(String.valueOf(i));
				location.setLatitude(intermediates.get(i).getLatitude());
				location.setLongitude(intermediates.get(i).getLongitude());
				dots.add(location);
			}
		}
		//writing end location
		location = new Location(String.valueOf("end"));
		location.setLatitude(lats[1]);
		location.setLongitude(lons[1]);
		dots.add(location);
		return new RouteCalculationResult(dots, null, params, null, true);
	}
}
