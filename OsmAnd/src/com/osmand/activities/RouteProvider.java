package com.osmand.activities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.location.Location;

import com.osmand.LogUtil;
import com.osmand.OsmandSettings.ApplicationMode;
import com.osmand.activities.RoutingHelper.RouteDirectionInfo;
import com.osmand.activities.RoutingHelper.TurnType;
import com.osmand.osm.LatLon;

public class RouteProvider {
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(RouteProvider.class);
	
	public enum RouteService {
		CLOUDMADE("CloudMade"), YOURS("YOURS"); //$NON-NLS-1$ //$NON-NLS-2$
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
	
	
	public static class RouteCalculationResult {
		private final List<Location> locations;
		private final List<RouteDirectionInfo> directions;
		private final String errorMessage;
		private int[] listDistance = null;
		
		public RouteCalculationResult(String errorMessage) {
			this(null, null, errorMessage);
		}
		public RouteCalculationResult(List<Location> list, List<RouteDirectionInfo> directions, String errorMessage) {
			this.directions = directions;
			this.errorMessage = errorMessage;
			this.locations = list;
			if (list != null) {
				prepareResult();
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
		
		private void prepareResult() {
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
				for (int i = directions.size() - 1; i >= 0; i--) {
					directions.get(i).afterLeftTime = sum;
					sum += directions.get(i).expectedTime;
					directions.get(i).distance = listDistance[directions.get(i).routePointOffset];
					if(i < directions.size() - 1){
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

	public RouteCalculationResult calculateRouteImpl(Location start, LatLon end, ApplicationMode mode, RouteService type){
		long time = System.currentTimeMillis();
		if (start != null && end != null) {
			if(log.isInfoEnabled()){
				log.info("Start finding route from " + start + " to " + end +" using " + type.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try {
				RouteCalculationResult res;
				if (type == RouteService.YOURS) {
					res = findYOURSRoute(start, end, mode);
				} else {
					res = findCloudMadeRoute(start, end, mode);
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


	protected RouteCalculationResult findYOURSRoute(Location start, LatLon end, ApplicationMode mode) throws MalformedURLException, IOException,
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
				return new RouteCalculationResult(item.getNodeValue());
				
			}
		}
		return new RouteCalculationResult(res, null, null);
	}
	
	
	protected RouteCalculationResult findCloudMadeRoute(Location start, LatLon end, ApplicationMode mode) throws MalformedURLException, IOException,
	ParserConfigurationException, FactoryConfigurationError, SAXException {
		List<Location> res = new ArrayList<Location>();
		List<RouteDirectionInfo> directions = null;
		StringBuilder uri = new StringBuilder();
		// possibly hide that API key because it is privacy of osmand
		uri.append("http://routes.cloudmade.com/A6421860EBB04234AB5EF2D049F2CD8F/api/0.3/"); //$NON-NLS-1$
		uri.append(start.getLatitude()+"").append(","); //$NON-NLS-1$ //$NON-NLS-2$
		uri.append(start.getLongitude()+"").append(","); //$NON-NLS-1$ //$NON-NLS-2$
		uri.append(end.getLatitude()+"").append(",");  //$NON-NLS-1$//$NON-NLS-2$
		uri.append(end.getLongitude()+"").append("/"); //$NON-NLS-1$ //$NON-NLS-2$

		if (ApplicationMode.PEDESTRIAN == mode) {
			uri.append("foot.gpx"); //$NON-NLS-1$
		} else if (ApplicationMode.BICYCLE == mode) {
			uri.append("bicycle.gpx"); //$NON-NLS-1$
		} else {
			uri.append("car.gpx"); //$NON-NLS-1$
		}
		uri.append("?lang=").append(Locale.getDefault().getLanguage()); //$NON-NLS-1$

		URL url = new URL(uri.toString());
		URLConnection connection = url.openConnection();
		DocumentBuilder dom = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = dom.parse(new InputSource(new InputStreamReader(connection.getInputStream())));
		// TODO how to find that error occurred ? API gpx doesn't say nothing
		NodeList list = doc.getElementsByTagName("wpt"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			Element item = (Element) list.item(i);
			try {
				Location l = new Location("router"); //$NON-NLS-1$
				l.setLatitude(Double.parseDouble(item.getAttribute("lat"))); //$NON-NLS-1$
				l.setLongitude(Double.parseDouble(item.getAttribute("lon"))); //$NON-NLS-1$
				res.add(l);
			} catch (NumberFormatException e) {
			}
		}


		
		list = doc.getElementsByTagName("rtept"); //$NON-NLS-1$
		if(list.getLength() > 0){
			directions = new ArrayList<RouteDirectionInfo>();
		}
		RouteDirectionInfo previous = null;
		for (int i = 0; i < list.getLength(); i++) {
			Element item = (Element) list.item(i);
			try {
				RouteDirectionInfo dirInfo = new RouteDirectionInfo();
				dirInfo.descriptionRoute = getContentFromNode(item, "desc"); //$NON-NLS-1$
				String stime = getContentFromNode(item, "time"); //$NON-NLS-1$
				if(stime != null){
					dirInfo.expectedTime = Integer.parseInt(stime);
				}
				String sturn = getContentFromNode(item, "turn-angle"); //$NON-NLS-1$
				if(sturn != null){
					dirInfo.turnAngle = (float) Double.parseDouble(sturn);
				}
				String stype = getContentFromNode(item, "turn"); //$NON-NLS-1$
				if(stype != null){
					dirInfo.turnType = TurnType.valueOf(stype.toUpperCase());
				} else {
					dirInfo.turnType = TurnType.C;
				}
				int offset = Integer.parseInt(getContentFromNode(item, "offset")); //$NON-NLS-1$
				dirInfo.routePointOffset = offset;
				
				if(previous != null && previous.turnType != TurnType.C && previous.turnType != null){
					// calculate angle
					if(previous.routePointOffset > 0){
						float paz = res.get(previous.routePointOffset - 1).bearingTo(res.get(previous.routePointOffset));
						float caz;
						if(previous.turnType.isExit() && dirInfo.routePointOffset < res.size() - 1){
							caz = res.get(dirInfo.routePointOffset).bearingTo(res.get(dirInfo.routePointOffset + 1));
						} else {
							caz = res.get(dirInfo.routePointOffset - 1).bearingTo(res.get(dirInfo.routePointOffset));
						}
						float angle = caz  - paz;
						if(angle < 0){
							angle += 360;
						}
						if(previous.turnAngle == 0f){
							previous.turnAngle = angle;
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
		if(previous != null && previous.turnType != TurnType.C && previous.turnType != null){
			// calculate angle
			if(previous.routePointOffset > 0 && previous.routePointOffset < res.size() - 1){
				float paz = res.get(previous.routePointOffset - 1).bearingTo(res.get(previous.routePointOffset));
				float caz = res.get(previous.routePointOffset).bearingTo(res.get(res.size() -1));
				float angle = caz  - paz;
				if(angle < 0){
					angle += 360;
				}
				if(previous.turnAngle == 0f){
					previous.turnAngle = angle;
				}
			}
		}
		
		
		
		return new RouteCalculationResult(res, directions, null);
	}
	
	private String getContentFromNode(Element item, String tagName){
		NodeList list = item.getElementsByTagName(tagName);
		if(list.getLength() > 0){
			return list.item(0).getFirstChild().getNodeValue();
		}
		return null;
	}
	
}
