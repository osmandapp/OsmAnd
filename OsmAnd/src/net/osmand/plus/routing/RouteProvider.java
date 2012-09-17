package net.osmand.plus.routing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Route;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.LogUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.Interruptable;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingContext;
import net.osmand.router.TurnType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;

public class RouteProvider {
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RouteProvider.class);
	private static final String OSMAND_ROUTER = "OsmAndRouter";
	
	public enum RouteService {
		OSMAND("OsmAnd (offline)"), CLOUDMADE("CloudMade"), YOURS("YOURS"), ORS("OpenRouteService"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
	
		public GPXRouteParams(GPXFile file, boolean reverse, OsmandSettings settings){
			prepareEverything(file, reverse, settings.LEFT_SIDE_NAVIGATION.get());
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
		
		private void prepareEverything(GPXFile file, boolean reverse, boolean leftSide){
			if(file.isCloudmadeRouteFile() || OSMAND_ROUTER.equals(file.author)){
				directions =  parseCloudmadeRoute(points, file, OSMAND_ROUTER.equals(file.author), leftSide, 10);
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
		loc.setTime(pt.time);
		loc.setAccuracy((float) pt.hdop);
		return loc;
	}
	
	
	

	public RouteCalculationResult calculateRouteImpl(Location start, LatLon end, List<LatLon> intermediates, ApplicationMode mode, RouteService type, Context ctx,
			GPXRouteParams gpxRoute, RouteCalculationResult previousToRecalculate, boolean fast, boolean leftSide, Interruptable interruptable){
		long time = System.currentTimeMillis();
		if (start != null && end != null) {
			if(log.isInfoEnabled()){
				log.info("Start finding route from " + start + " to " + end +" using " + type.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try {
				RouteCalculationResult res;
				if(gpxRoute != null && !gpxRoute.points.isEmpty()){
					res = calculateGpxRoute(start, end, gpxRoute, ctx, leftSide);
				} else if (type == RouteService.YOURS) {
					res = findYOURSRoute(start, end, mode, fast, ctx, leftSide);
				} else if (type == RouteService.ORS) {
					res = findORSRoute(start, end, mode, fast, ctx, leftSide);
				} else if (type == RouteService.OSMAND) {
					List<RouteSegmentResult> originalRoute = null;
					if(previousToRecalculate != null) {
						originalRoute = previousToRecalculate.getOriginalRoute();
					}
					res = findVectorMapsRoute(start, end, intermediates, mode, (OsmandApplication)ctx.getApplicationContext(), originalRoute, leftSide, interruptable);
				} else {
					res = findCloudMadeRoute(start, end, intermediates, mode, ctx, fast, leftSide);
				}
				if(log.isInfoEnabled() ){
					log.info("Finding route contained " + res.getImmutableLocations().size() + " points for " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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

	private RouteCalculationResult calculateGpxRoute(Location start, LatLon end, GPXRouteParams params, Context ctx, boolean leftSide) {
		RouteCalculationResult res;
		// get the closest point to start and to end
		float minDist = Integer.MAX_VALUE;
		int startI = 0;
		List<Location> gpxRoute = params.points;
		int endI = gpxRoute.size(); 
		if (start != null) {
			for (int i = 0; i < gpxRoute.size(); i++) {
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
			res = new RouteCalculationResult(sublist, params.directions, start, end, null, null, ctx, leftSide, true);
		} else {
			List<RouteDirectionInfo> subdirections = new ArrayList<RouteDirectionInfo>();
			for (RouteDirectionInfo info : params.directions) {
				if(info.routePointOffset >= startI && info.routePointOffset < endI){
					RouteDirectionInfo ch = new RouteDirectionInfo(info.getAverageSpeed(), info.getTurnType());
					ch.routePointOffset = info.routePointOffset - startI;
					ch.setDescriptionRoute(info.getDescriptionRoute());
					
					// recalculate
					ch.distance = 0;
					ch.afterLeftTime = 0;
					subdirections.add(ch);
				}
			}
			res = new RouteCalculationResult(sublist, subdirections, start, end, null, null, ctx, leftSide, true);
		}
		return res;
	}
	
	protected String getString(Context ctx, int resId){
		if(ctx == null){
			return ""; //$NON-NLS-1$
		}
		return ctx.getString(resId);
	}
	
	


	protected RouteCalculationResult findYOURSRoute(Location start, LatLon end, ApplicationMode mode, boolean fast, Context ctx, boolean leftSide) throws MalformedURLException, IOException,
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
		log.info("URL route " + uri);
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
		return new RouteCalculationResult(res, null, start, end, null, null, ctx, leftSide, true);
	}
	
	protected RouteCalculationResult findVectorMapsRoute(Location start, LatLon end, List<LatLon> intermediates, ApplicationMode mode, OsmandApplication app,
			List<RouteSegmentResult> previousRoute,
			boolean leftSide, Interruptable interruptable) throws IOException {
		BinaryMapIndexReader[] files = app.getResourceManager().getRoutingMapFiles();
		BinaryRoutePlanner router = new BinaryRoutePlanner(NativeOsmandLibrary.getLoadedLibrary(), files);
		File routingXml = app.getSettings().extendOsmandPath(ResourceManager.ROUTING_XML);
		RoutingConfiguration.Builder config ;
		if (routingXml.exists() && routingXml.canRead()) {
			try {
				config = RoutingConfiguration.parseFromInputStream(new FileInputStream(routingXml));
			} catch (SAXException e) {
				throw new IllegalStateException(e);
			}
		} else {
			config = RoutingConfiguration.getDefault();
		}
		
		GeneralRouterProfile p ;
		if (mode == ApplicationMode.BICYCLE) {
			p = GeneralRouterProfile.BICYCLE;
		} else if (mode == ApplicationMode.PEDESTRIAN) {
			p = GeneralRouterProfile.PEDESTRIAN;
		} else {
			p = GeneralRouterProfile.CAR;
		}
		List<String> specs = new ArrayList<String>();
		if (!app.getSettings().FAST_ROUTE_MODE.get()) {
			specs.add(GeneralRouter.USE_SHORTEST_WAY);
		}
		if(app.getSettings().AVOID_FERRIES.get()){
			specs.add(GeneralRouter.AVOID_FERRIES);
		}
		if(app.getSettings().AVOID_TOLL_ROADS.get()){
			specs.add(GeneralRouter.AVOID_TOLL);
		}
		if(app.getSettings().AVOID_MOTORWAY.get()){
			specs.add(GeneralRouter.AVOID_MOTORWAY);
		}
		if(app.getSettings().AVOID_UNPAVED_ROADS.get()){
			specs.add(GeneralRouter.AVOID_UNPAVED);
		}
		String[] specialization = specs.toArray(new String[specs.size()]);
		RoutingContext ctx = new RoutingContext(config.build(p.name().toLowerCase(), start.hasBearing() ?  start.getBearing() / 180d * Math.PI : null, specialization));
		ctx.interruptable = interruptable;
		ctx.previouslyCalculatedRoute = previousRoute;
		RouteSegment st= router.findRouteSegment(start.getLatitude(), start.getLongitude(), ctx);
		if (st == null) {
			return new RouteCalculationResult(app.getString(R.string.starting_point_too_far));
		}
		RouteSegment en = router.findRouteSegment(end.getLatitude(), end.getLongitude(), ctx);
		if (en == null) {
			return new RouteCalculationResult(app.getString(R.string.ending_point_too_far));
		}
		List<RouteSegment> inters  = new ArrayList<BinaryRoutePlanner.RouteSegment>();
		if (intermediates != null) {
			int ind = 1;
			for (LatLon il : intermediates) {
				RouteSegment is = router.findRouteSegment(il.getLatitude(), il.getLongitude(), ctx);
				if (is == null) {
					return new RouteCalculationResult(app.getString(R.string.intermediate_point_too_far, "'" + ind + "'"));
				}
				inters.add(is);
				ind++;
			}
		}
		try {
			List<RouteSegmentResult> result; 
			if(inters.size() > 0){
				result = router.searchRoute(ctx, st, en, inters, leftSide);
			} else {
				result = router.searchRoute(ctx, st, en, leftSide);
			}
			return new RouteCalculationResult(result, start, end, intermediates, app, leftSide);
		} catch (OutOfMemoryError e) {
			ActivityManager activityManager = (ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
			ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
			activityManager.getMemoryInfo(memoryInfo);
			return new RouteCalculationResult("Not enough process memory "+ "(" + memoryInfo.availMem / 1048576L + " MB available) ");
		}
	}
	
	
	protected RouteCalculationResult findCloudMadeRoute(Location start, LatLon end, List<LatLon> intermediates, ApplicationMode mode, Context ctx, boolean fast, boolean leftSide)
			throws MalformedURLException, IOException, ParserConfigurationException, FactoryConfigurationError, SAXException {
		List<Location> res = new ArrayList<Location>();
		List<RouteDirectionInfo> directions = null;
		StringBuilder uri = new StringBuilder();
		// possibly hide that API key because it is privacy of osmand
		uri.append("http://routes.cloudmade.com/A6421860EBB04234AB5EF2D049F2CD8F/api/0.3/"); //$NON-NLS-1$
		uri.append(start.getLatitude() + "").append(","); //$NON-NLS-1$ //$NON-NLS-2$
		uri.append(start.getLongitude() + "").append(","); //$NON-NLS-1$ //$NON-NLS-2$
		if(intermediates != null && intermediates.size() > 0) {
			uri.append("[");
			boolean first = true;
			for(LatLon il : intermediates) {
				if(!first){
					uri.append(",");
				} else {
					first = false;
				}
				uri.append(il.getLatitude() + "").append(","); //$NON-NLS-1$ //$NON-NLS-2$
				uri.append(il.getLongitude() + ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			uri.append("],");
		}
		uri.append(end.getLatitude() + "").append(","); //$NON-NLS-1$//$NON-NLS-2$
		uri.append(end.getLongitude() + "").append("/"); //$NON-NLS-1$ //$NON-NLS-2$

		float speed = 1.5f;
		if (ApplicationMode.PEDESTRIAN == mode) {
			uri.append("foot.gpx"); //$NON-NLS-1$
		} else if (ApplicationMode.BICYCLE == mode) {
			speed = 5.5f;
			uri.append("bicycle.gpx"); //$NON-NLS-1$
		} else {
			speed = 15.3f;
			if (fast) {
				uri.append("car.gpx"); //$NON-NLS-1$
			} else {
				uri.append("car/shortest.gpx"); //$NON-NLS-1$
			}
		}
		uri.append("?lang=").append(Locale.getDefault().getLanguage()); //$NON-NLS-1$
		log.info("URL route " + uri);
		URL url = new URL(uri.toString());
		URLConnection connection = url.openConnection();
		GPXFile gpxFile = GPXUtilities.loadGPXFile(ctx, connection.getInputStream(), false);
		directions = parseCloudmadeRoute(res, gpxFile, false, leftSide, speed);

		return new RouteCalculationResult(res, directions, start, end, intermediates, null, ctx, leftSide, true);
	}

	private static List<RouteDirectionInfo> parseCloudmadeRoute(List<Location> res, GPXFile gpxFile, boolean osmandRouter,
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
						last.distance = (int) (distanceToEnd[last.routePointOffset] - distanceToEnd[offset]);
					} 
					// save time as a speed because we don't know distance of the route segment
					float avgSpeed = time;
					if(!iterator.hasNext() && time > 0) {
						avgSpeed = distanceToEnd[offset] / time;
					}
					String stype = item.getExtensionsToRead().get("turn"); //$NON-NLS-1$
					TurnType turnType;
					if (stype != null) {
						turnType = TurnType.valueOf(stype.toUpperCase(), leftSide);
					} else {
						turnType = TurnType.valueOf(TurnType.C, leftSide);
					}
					String sturn = item.getExtensionsToRead().get("turn-angle"); //$NON-NLS-1$
					if (sturn != null) {
						turnType.setTurnAngle((float) Double.parseDouble(sturn));
					}
					RouteDirectionInfo dirInfo = new RouteDirectionInfo(avgSpeed, turnType);
					dirInfo.setDescriptionRoute(item.desc); //$NON-NLS-1$
					dirInfo.routePointOffset = offset;
					if (previous != null && !TurnType.C.equals(previous.getTurnType().getValue()) &&
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
		if (previous != null && !TurnType.C.equals(previous.getTurnType().getValue())) {
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
	
	protected RouteCalculationResult findORSRoute(Location start, LatLon end, ApplicationMode mode, boolean fast, Context ctx,
			boolean leftSide) throws MalformedURLException, IOException, ParserConfigurationException, FactoryConfigurationError,
			SAXException {
		List<Location> res = new ArrayList<Location>();

		String rpref = "Fastest";
		if (ApplicationMode.PEDESTRIAN == mode) {
			rpref = "Pedestrian";
		} else if (ApplicationMode.BICYCLE == mode) {
			rpref = "Bicycle";
			// } else if (ApplicationMode.LOWTRAFFIC == mode) {
			// rpref = "BicycleSafety";
			// } else if (ApplicationMode.RACEBIKE == mode) {
			// rpref = "BicycleRacer";
			// } else if (ApplicationMode.TOURBIKE == mode) {
			// rpref = "BicycleRoute";
			// } else if (ApplicationMode.MTBIKE == mode) {
			// rpref = "BicycleMTB";
		} else if (!fast) {
			rpref = "Shortest";
		}

		StringBuilder request = new StringBuilder();
		request.append("http://openls.geog.uni-heidelberg.de/osm/eu/routing?").append("start=").append(start.getLongitude()).append(',')
				.append(start.getLatitude()).append("&end=").append(end.getLongitude()).append(',').append(end.getLatitude())
				.append("&preference=").append(rpref);
		// TODO if we would get instructions from the service, we could use this language setting
		// .append("&language=").append(Locale.getDefault().getLanguage());

		log.info("URL route " + request.toString());
		URI uri = URI.create(request.toString());
		URL url = uri.toURL();
		URLConnection connection = url.openConnection();

		DocumentBuilder dom = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = dom.parse(new InputSource(new InputStreamReader(connection.getInputStream())));
		NodeList list = doc.getElementsByTagName("xls:RouteGeometry"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			NodeList poslist = ((Element) list.item(i)).getElementsByTagName("gml:pos"); //$NON-NLS-1$
			for (int j = 0; j < poslist.getLength(); j++) {
				String text = poslist.item(j).getFirstChild().getNodeValue();
				int s = text.indexOf(' ');
				try {
					double lon = Double.parseDouble(text.substring(0, s));
					double lat = Double.parseDouble(text.substring(s + 1));
					Location l = new Location("router"); //$NON-NLS-1$
					l.setLatitude(lat);
					l.setLongitude(lon);
					res.add(l);
				} catch (NumberFormatException nfe) {
				}
			}
		}
		if (list.getLength() == 0) {
			if (doc.getChildNodes().getLength() == 1) {
				Node item = doc.getChildNodes().item(0);
				return new RouteCalculationResult(item.getNodeValue());

			}
		}
		return new RouteCalculationResult(res, null, start, end, null, null, ctx, leftSide, true);
	}
	
	public GPXFile createOsmandRouterGPX(RouteCalculationResult srcRoute){
		int currentRoute = srcRoute.currentRoute;
		List<Location> routeNodes = srcRoute.getImmutableLocations();
		List<RouteDirectionInfo> directionInfo = srcRoute.getDirections();
		int currentDirectionInfo = srcRoute.currentDirectionInfo;
		
		GPXFile gpx = new GPXFile();
		gpx.author = OSMAND_ROUTER;
		Track track = new Track();
		gpx.tracks.add(track);
		TrkSegment trkSegment = new TrkSegment();
		track.segments.add(trkSegment);
		int cRoute = currentRoute;
		int cDirInfo = currentDirectionInfo;
		
		for(int i = cRoute; i< routeNodes.size(); i++){
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
		for (int i = cDirInfo; i < directionInfo.size(); i++) {
			RouteDirectionInfo dirInfo = directionInfo.get(i);
			if (dirInfo.routePointOffset >= cRoute) {
				Location loc = routeNodes.get(dirInfo.routePointOffset);
				WptPt pt = new WptPt();
				pt.lat = loc.getLatitude();
				pt.lon = loc.getLongitude();
				pt.desc = dirInfo.getDescriptionRoute();
				Map<String, String> extensions = pt.getExtensionsToWrite();
				extensions.put("time", dirInfo.getExpectedTime() + "");
				String turnType = dirInfo.getTurnType().getValue();
				if (dirInfo.getTurnType().isRoundAbout()) {
					turnType += dirInfo.getTurnType().getExitOut();
				}
				if(!TurnType.C.equals(turnType)){
					extensions.put("turn", turnType);
					extensions.put("turn-angle", dirInfo.getTurnType().getTurnAngle() + "");
				}
				extensions.put("offset", (dirInfo.routePointOffset - cRoute) + "");
				route.points.add(pt);
			}
		}
		return gpx;
	}
	
}
