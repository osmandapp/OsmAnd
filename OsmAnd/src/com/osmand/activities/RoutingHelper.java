package com.osmand.activities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.location.Location;
import android.util.FloatMath;
import android.widget.Toast;

import com.osmand.LogUtil;
import com.osmand.OsmandSettings.ApplicationMode;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;

public class RoutingHelper {
	
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RoutingHelper.class);

	private final MapActivity activity;
	
	private List<Location> routeNodes = new ArrayList<Location>();
	private int[] listDistance = null;
	private int currentRoute = 0;
	
	
	private LatLon finalLocation;
	private Location lastFixedLocation;
	private Thread currentRunningJob;
	private long lastTimeEvaluatedRoute = 0;
	private int evalWaitInterval = 3000;

	private ApplicationMode mode;
	
	
	// TEST CODE
//	private static List<Location> testRoute = new ArrayList<Location>();
//	private static void addTestLocation(double lat, double lon){
//		Location l = new Location("p");
//		l.setLatitude(lat);
//		l.setLongitude(lon);
//		testRoute.add(l);
//	}
//	static {
//		addTestLocation(53.83330108605482, 27.590844306640605);
//		addTestLocation(53.83319978227928, 27.580802116088847);
//		addTestLocation(53.83319978227928, 27.57196155517576);
//		addTestLocation(53.833149130299624, 27.563893470458964);
//		addTestLocation(53.8330984782587, 27.55651203124998);
//		addTestLocation(53.83340238958535, 27.551448020629863);
//		addTestLocation(53.834263459701916, 27.543208274536113);
//		addTestLocation(53.835428408740796, 27.53325191467283);
//		addTestLocation(53.83598554682643, 27.531964454345683);
//		addTestLocation(53.83654267750025, 27.532221946411113);
//		addTestLocation(53.836441381565386, 27.533166083984355);
//		addTestLocation(53.8341621582541, 27.53325191467283);
//		addTestLocation(53.83016055507444, 27.533595237426738);
//		addTestLocation(53.82696912916281, 27.533938560180644);
//		addTestLocation(53.8255506396008, 27.53376689880369);
//		addTestLocation(53.82119354992355, 27.531706962280253);
//		addTestLocation(53.817646747077355, 27.53050533264158);
//		addTestLocation(53.81156581517512, 27.529389533691386);
//		addTestLocation(53.81404896894648, 27.5300761791992);
//		addTestLocation(53.80994408432531, 27.529217872314433);
//		addTestLocation(53.8062949605079, 27.529217872314433);
//		addTestLocation(53.80203724791403, 27.529132041625957);
//		addTestLocation(53.794838702875424, 27.52878871887205);
//		addTestLocation(53.79052912343052, 27.52758708923338);
//		addTestLocation(53.77922071028288, 27.524497184448222);
//		addTestLocation(53.76714827905046, 27.520978126220683);
//		addTestLocation(53.756138036056214, 27.515914115600566);
//		addTestLocation(53.75121547304873, 27.51385417907713);
//		addTestLocation(53.74745972778275, 27.513425025634746);
//		addTestLocation(53.74284071585075, 27.51402584045408);
//		addTestLocation(53.738373496098994, 27.514884147338847);
//		addTestLocation(53.735175081196715, 27.516257438354472);
//		addTestLocation(53.711966594109235, 27.519604835205058);
//		addTestLocation(53.70769930787764, 27.520463142089824);
//		addTestLocation(53.69992563864831, 27.52381053894041);
//		addTestLocation(53.69728328030487, 27.521235618286113);
//	}
	// END TEST CODE
	
	
	public RoutingHelper(MapActivity activity){
		this.activity = activity;
		
	}
	
	public void setFinalLocation(LatLon finalLocation){
		this.finalLocation = finalLocation;
	}
	
	public void setAppMode(ApplicationMode mode){
		this.mode = mode;
	}
	
	public ApplicationMode getAppMode() {
		return mode;
	}
	
	public LatLon getFinalLocation() {
		return finalLocation;
	}
	
	
	public boolean isRouterEnabled(){
		return finalLocation != null && lastFixedLocation != null;
	}
	
	
	public boolean finishAtLocation(Location currentLocation) {
		Location lastPoint = routeNodes.get(routeNodes.size() - 1);
		if(currentRoute > routeNodes.size() - 3 && currentLocation.distanceTo(lastPoint) < 60){
			if(lastFixedLocation != null && lastFixedLocation.distanceTo(lastPoint) < 60){
				// TODO mark as finished 
				showMessage("You arrived at destination point");
				currentRoute = routeNodes.size() - 1;
			}
			lastFixedLocation = currentLocation;
			// ??? even if you go away from place you can't navigate once time
//			finalLocation = null
			return true;
		}
		return false;
	}
	
	public Location getLastFixedLocation() {
		return lastFixedLocation;
	}
	
	
	public void setCurrentLocation(Location currentLocation) {
		if(finalLocation == null || currentLocation == null){
			return;
		}
		boolean calculateRoute  = false;
		synchronized (this) {
			if(routeNodes.isEmpty() || routeNodes.size() <= currentRoute){
				calculateRoute = true;
			} else {
				// 1. try to mark passed route (move forward)
				float dist = currentLocation.distanceTo(routeNodes.get(currentRoute));
				while(currentRoute + 1 < routeNodes.size()){
					float newDist = currentLocation.distanceTo(routeNodes.get(currentRoute + 1));
					if (newDist < dist) {
						// that node already passed
						currentRoute++;
						dist = newDist;
					} else {
						break;
					}
				}
				// 2. check if destination found
				if(finishAtLocation(currentLocation)){
					return;
				}
				
				// 3. check if closest location already passed
				if(currentRoute + 1 < routeNodes.size()){
					float bearing = routeNodes.get(currentRoute).bearingTo(routeNodes.get(currentRoute + 1));
					float bearingMovement = currentLocation.bearingTo(routeNodes.get(currentRoute));
					if(Math.abs(bearing - bearingMovement) > 130 && Math.abs(bearing - bearingMovement) < 230){
						currentRoute++;
					}
				}
				// 4. evaluate distance to the route and reevaluate if needed
				if(currentRoute > 0){
					float bearing = routeNodes.get(currentRoute - 1).bearingTo(routeNodes.get(currentRoute));
					float bearingMovement = currentLocation.bearingTo(routeNodes.get(currentRoute));
					float d = Math.abs(currentLocation.distanceTo(routeNodes.get(currentRoute)) * FloatMath.sin((bearingMovement - bearing)*3.14f/180f));
					if(d > 50) {
						log.info("Recalculate route, because correlation  : " + d); //$NON-NLS-1$
						calculateRoute = true;
					}
				} 
				
				// 5. also check bearing by summing distance
				if(!calculateRoute){
					float d = currentLocation.distanceTo(routeNodes.get(currentRoute));
					if (d > 80) {
						if (currentRoute > 0) {
							// possibly that case is not needed (often it is covered by 4.)
							float f1 = currentLocation.distanceTo(routeNodes.get(currentRoute - 1)) + d;
							float c = routeNodes.get(currentRoute - 1).distanceTo(routeNodes.get(currentRoute));
							if (c * 2 < d + f1) {
								log.info("Recalculate route, because too far from points : " + d + " " + f1 + " >> " + c); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								calculateRoute = true;
							}
						} else {
							// that case is needed
							log.info("Recalculate route, because too far from start : " + d); //$NON-NLS-1$
							calculateRoute = true;
						}
					}
				}
				
				// 5. Also bearing could be checked (is it same direction)
//				float bearing;
//				if(currentLocation.hasBearing()){
//					bearing = currentLocation.getBearing();
//				} else if(lastFixedLocation != null){
//					bearing = lastFixedLocation.bearingTo(currentLocation);
//				}
//				bearingRoute = currentLocation.bearingTo(routeNodes.get(currentRoute));
//				if (Math.abs(bearing - bearingRoute) > 60f && 360 - Math.abs(bearing - bearingRoute) > 60f) {
//				      something wrong however it could be starting movement
//				}
			}
		}

		lastFixedLocation = currentLocation;
		if(calculateRoute){
			calculateRoute(lastFixedLocation, finalLocation);
		}
	}
	
	private void setNewRoute(List<Location> locations){
		routeNodes = locations;
		listDistance = new int[locations.size()];
		if (!locations.isEmpty()) {
			listDistance[locations.size() - 1] = 0;
			for (int i = locations.size() - 1; i > 0; i--) {
				listDistance[i - 1] = (int) locations.get(i - 1).distanceTo(locations.get(i));
				listDistance[i - 1] += listDistance[i];
			}
		}
		currentRoute = 0;
	}
	
	public synchronized int getDistance(double lat, double lon){
		if(listDistance != null && currentRoute < listDistance.length){
			int dist = listDistance[currentRoute];
			Location l = routeNodes.get(currentRoute);
			dist += MapUtils.getDistance(lat, lon, l.getLatitude(), l.getLongitude());
			return dist;
		}
		return 0;
	}
	
	public void calculateRoute(final Location start, final LatLon end){
		if(currentRunningJob == null){
			// do not evaluate very often
			if (System.currentTimeMillis() - lastTimeEvaluatedRoute > evalWaitInterval) {
				synchronized (this) {
					currentRunningJob = new Thread(new Runnable() {
						@Override
						public void run() {
							RouteCalculationResult res = calculateRouteImpl(start, end);
							synchronized (RoutingHelper.this) {
								if (res.isCalculated()) {
									setNewRoute(res.list);
									// reset error wait interval
									evalWaitInterval = 3000;
								} else {
									evalWaitInterval = evalWaitInterval * 3 / 2;
									if(evalWaitInterval > 120000){
										evalWaitInterval  = 120000;
									}
								}
								currentRunningJob = null;
							}
							if(res.isCalculated()){
								showMessage("New route is calculated distance : " + MapUtils.getFormattedDistance(sumDistance(res.list)));
								// be aware that is non ui thread
								activity.getMapView().refreshMap();
							} else {
								if(res.errorMessage != null){
									showMessage("Error calculating route : " + res.errorMessage);
								} else if(res.list == null){
									showMessage("Error occurred while calculating route");
								} else {
									showMessage("Empty route is calculated");
								}
							}
							lastTimeEvaluatedRoute = System.currentTimeMillis();
						}
					}, "Calculating route"); //$NON-NLS-1$
					currentRunningJob.start();
				}
			}
		}
	}
	
	private int sumDistance(List<Location> locations) {
		int d = 0;
		if (locations.size() > 1) {
			for (int i = 1; i < locations.size(); i++) {
				d += locations.get(i - 1).distanceTo(locations.get(i));
			}
		}
		return d;
	}
	
	private void showMessage(final String msg){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	public boolean hasPointsToShow(){
		return finalLocation != null && !routeNodes.isEmpty();
	}
	
	
	public synchronized void fillLocationsToShow(double topLatitude, double leftLongitude, double bottomLatitude,double rightLongitude, List<Location> l){
		l.clear();
		boolean previousVisible = false;
		if(lastFixedLocation != null){
			if(leftLongitude <= lastFixedLocation.getLongitude() && lastFixedLocation.getLongitude() <= rightLongitude &&
					bottomLatitude <= lastFixedLocation.getLatitude() && lastFixedLocation.getLatitude() <= topLatitude){
				l.add(lastFixedLocation);
				previousVisible = true;
			}
		}
		
		for (int i = currentRoute; i < routeNodes.size(); i++) {
			Location ls = routeNodes.get(i);
			if(leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude &&
					bottomLatitude <= ls.getLatitude() && ls.getLatitude() <= topLatitude){
				l.add(ls);
				if (!previousVisible) {
					if (i > currentRoute) {
						l.add(0, routeNodes.get(i - 1));
					} else if (lastFixedLocation != null) {
						l.add(0, lastFixedLocation);
					}
				}
				previousVisible = true;
			} else if(previousVisible){
				l.add(ls);
				previousVisible = false;
				// do not continue make method more efficient (because it calls in UI thread)
				// this break also has logical sense !
				break;
			}
		}
	}
	
	
	private static class RouteCalculationResult {
		public List<Location> list;
		public String errorMessage;
		public RouteCalculationResult( List<Location> list, String errorMessage) {
			this.errorMessage = errorMessage;
			this.list = list;
		}
		public boolean isCalculated(){
			return list != null && !list.isEmpty();
		}
		
	}
	
	private RouteCalculationResult calculateRouteImpl(Location start, LatLon end){
		long time = System.currentTimeMillis();
		if (start != null && end != null) {
			List<Location> res = new ArrayList<Location>();
			if(log.isInfoEnabled()){
				log.info("Start finding route from " + start + " to " + end); //$NON-NLS-1$ //$NON-NLS-2$
			}
			try {
				StringBuilder uri = new StringBuilder();
				uri.append("http://www.yournavigation.org/api/1.0/gosmore.php?format=kml"); //$NON-NLS-1$
				uri.append("&flat=").append(start.getLatitude()); //$NON-NLS-1$
				uri.append("&flon=").append(start.getLongitude()); //$NON-NLS-1$
				uri.append("&tlat=").append(end.getLatitude()); //$NON-NLS-1$
				uri.append("&tlon=").append(end.getLongitude()); //$NON-NLS-1$
				if(ApplicationMode.PEDESTRIAN== mode){
					uri.append("&v=foot") ; //$NON-NLS-1$
				} else if(ApplicationMode.BICYCLE == mode){
					uri.append("&v=bicycle") ; //$NON-NLS-1$
				} else {
					uri.append("&v=motorcar"); //$NON-NLS-1$
				}
				uri.append("&fast=1").append("&layer=mapnik"); //$NON-NLS-1$ //$NON-NLS-2$

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
						return new RouteCalculationResult(null, item.getNodeValue());
						
					}
				}
				if(log.isInfoEnabled()){
					log.info("Finding route contained " + res.size() + " points for " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

				return new RouteCalculationResult(res, null);
			} catch (IOException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (ParserConfigurationException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			} catch (SAXException e) {
				log.error("Failed to find route ", e); //$NON-NLS-1$
			}
		}
		return new RouteCalculationResult(null, null);
	}
	
	

}
