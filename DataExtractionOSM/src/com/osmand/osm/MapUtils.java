package com.osmand.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.osmand.data.MapObject;

/**
 * This utility class includes : 
 * 1. distance algorithms
 * 2. finding center for array of nodes
 * 3. tile evaluation algorithms
 *   
 *
 */
public class MapUtils {
	public static double getDistance(Node e1, Node e2){
		return getDistance(e1.getLatitude(), e1.getLongitude(), e2.getLatitude(), e2.getLongitude());
	}
	
	public static double getDistance(LatLon l, double latitude, double longitude){
		return getDistance(l.getLatitude(), l.getLongitude(), latitude, longitude);
	}
	
	public static double getDistance(Node e1, double latitude, double longitude){
		return getDistance(e1.getLatitude(), e1.getLongitude(), latitude, longitude);
	}
	
	public static double getDistance(Node e1, LatLon point){
		return getDistance(e1.getLatitude(), e1.getLongitude(), point.getLatitude(), point.getLongitude());
	}
	
	
	/**
	 * Gets distance in meters
	 */
	public static double getDistance(double lat1, double lon1, double lat2, double lon2){
		double R = 6371; // km
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1); 
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * 
		        Math.sin(dLon/2) * Math.sin(dLon/2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return R * c * 1000;
	}
	
	
	/**
	 * Gets distance in meters
	 */
	public static double getDistance(LatLon l1, LatLon l2){
		return getDistance(l1.getLatitude(), l1.getLongitude(), l2.getLatitude(), l2.getLongitude());
	}

	public static LatLon getCenter(Entity e){
		if(e instanceof Node){
			return ((Node) e).getLatLon();
		} else if(e instanceof Way){
			return getWeightCenterForNodes(((Way) e).getNodes());
		} else if(e instanceof Relation){
			List<LatLon> list = new ArrayList<LatLon>();
			for(Entity fe : ((Relation) e).getMembers(null)){
				LatLon c = getCenter(fe);
				if(c != null){
					list.add(c);
				}
			}
			return getWeightCenter(list);
		}
		return null;
	}

	public static LatLon getWeightCenter(Collection<LatLon> nodes){
		if(nodes.isEmpty()){
			return null;
		}
		double longitude = 0;
		double latitude = 0;
		for(LatLon n : nodes){
			longitude += n.getLongitude();
			latitude += n.getLatitude();
		}
		return new LatLon(latitude/nodes.size(), longitude/nodes.size());
	}
	
	public static LatLon getWeightCenterForNodes(Collection<Node> nodes){
		if (nodes.isEmpty()) {
			return null;
		}
		double longitude = 0;
		double latitude = 0;
		int count = 0;
		for (Node n : nodes) {
			if (n != null) {
				count++;
				longitude += n.getLongitude();
				latitude += n.getLatitude();
			}
		}
		if (count == 0) {
			return null;
		}
		return new LatLon(latitude/count, longitude/count);
	}
	
	public static double checkLongitude(double longitude) {
		while (longitude < -180 || longitude > 180) {
			if (longitude < 0) {
				longitude += 360;
			} else {
				longitude -= 360;
			}
		}
		return longitude;
	}
	
	public static double checkLatitude(double latitude) {
		while (latitude < -90 || latitude > 90) {
			if (latitude < 0) {
				latitude += 180;
			} else {
				latitude -= 180;
			}
		}
		return latitude;
	}
	
	
	/**
	 * 
	 * Theses methods operate with degrees (evaluating tiles & vice versa) 
	 * degree longitude measurements (-180, 180) [27.56 Minsk]
	// degree latitude measurements (90, -90) [53.9]
	 */
	
	public static double getTileNumberX(int zoom, double longitude){
		longitude = checkLongitude(longitude);
		int n = 1 << zoom;
		return (longitude + 180d)/360d * n;
	}
	
	public static double getTileNumberY(int zoom,  double latitude){
		int n = 1 << zoom;
		latitude = checkLatitude(latitude);
		double eval = Math.log( Math.tan(Math.toRadians(latitude)) + 1/Math.cos(Math.toRadians(latitude)) );
		return  (1 - eval / Math.PI) / 2 * n;
	}
	
	public static double getLongitudeFromTile(int zoom, double x) {
		return x / (1 << zoom) * 360.0 - 180.0;
	}
	
	
	public static double getLatitudeFromTile(int zoom, double y){
		return Math.atan(Math.sinh(Math.PI * (1 - 2 * y / (1 << zoom)))) * 180d / Math.PI;
	}
	
	public static int getPixelShiftX(int zoom, double long1, double long2, int tileSize){
		return (int) ((getTileNumberX(zoom, long1) - getTileNumberX(zoom, long2)) * tileSize);
	}
	
	
	public static int getLengthXFromMeters(int zoom, double latitude, double longitude,  double meters, int tileSize, int widthOfDisplay) {
		double tileNumberX = MapUtils.getTileNumberX(zoom, longitude);
		double tileNumberLeft = tileNumberX - ((double) widthOfDisplay) / (2d * tileSize);
		double tileNumberRight = tileNumberX + ((double) widthOfDisplay) / (2d * tileSize);
		double dist = getDistance(latitude, getLongitudeFromTile(zoom, tileNumberLeft), latitude, getLongitudeFromTile(zoom,
				tileNumberRight));

		return (int) ((double) widthOfDisplay / dist * meters);
	}
	
	public static int getPixelShiftY(int zoom, double lat1, double lat2, int tileSize){
		return (int) ((getTileNumberY(zoom, lat1) - getTileNumberY(zoom, lat2)) * tileSize);
	}
	
	public static void addIdsToList(Collection<? extends Entity> source, List<Long> ids){
		for(Entity e : source){
			ids.add(e.getId());
		}
	}
	
	public static void sortListOfMapObject(List<? extends MapObject> list, final double lat, final double lon){
		Collections.sort(list, new Comparator<MapObject>() {
			@Override
			public int compare(MapObject o1, MapObject o2) {
				return Double.compare(MapUtils.getDistance(o1.getLocation(), lat, lon), MapUtils.getDistance(o2.getLocation(),
						lat, lon));
			}
		});
	}

}
