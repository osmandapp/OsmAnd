package net.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class MapAlgorithms {
	
	public static void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, List<Node> result){
		if(zoom > 31){
			zoom = 31;
		}
		ArrayList<Integer> l = new ArrayList<Integer>();
		int first = 0;
		int nsize = n.size(); 
		while(first < nsize){
			if(n.get(first) != null){
				break;
			}
			first++;
		}
		int last = nsize - 1;
		while (last >= 0) {
			if (n.get(last) != null) {
				break;
			}
			last--;
		}
		if (last - first < 1) {
			return;
		}
		// check for possible cycle
		boolean checkCycle = true;
		boolean cycle = false;
		while (checkCycle && last > first) {
			checkCycle = false;

			double x1 = MapUtils.getTileNumberX(zoom, n.get(first).getLongitude());
			double y1 = MapUtils.getTileNumberY(zoom, n.get(first).getLatitude());
			double x2 = MapUtils.getTileNumberX(zoom, n.get(last).getLongitude());
			double y2 = MapUtils.getTileNumberY(zoom, n.get(last).getLatitude());
			if (Math.abs(x1 - x2) + Math.abs(y1 - y2) < 0.001) {
				last--;
				cycle = true;
				checkCycle = true;
			}
		}
		if(last - first < 1){
			return;
		}
		simplifyDouglasPeucker(n, zoom, epsilon, l, first, last);
		result.add(n.get(first));
		int lsize = l.size();
		for (int i = 0; i < lsize; i++) {
			result.add(n.get(l.get(i)));
		}
		if (cycle) {
			result.add(n.get(first));
		}
	}

	private static void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, List<Integer> ints, int start, int end){
		double dmax = -1;
		int index = -1;
		for (int i = start + 1; i <= end - 1; i++) {
			if(n.get(i) == null){
				continue;
			}
			double d = orthogonalDistance(zoom, n.get(start), n.get(end), n.get(i));// calculate distance from line
			if (d > dmax) {
				dmax = d;
				index = i;
			}
		}
		if(dmax >= epsilon){
			simplifyDouglasPeucker(n, zoom, epsilon, ints, start, index);
			simplifyDouglasPeucker(n, zoom, epsilon, ints, index, end);
		} else {
			ints.add(end);
		}
	}
	
	private static double orthogonalDistance(int zoom, Node nodeLineStart, Node nodeLineEnd, Node node) {
		double x1 = MapUtils.getTileNumberX(zoom, nodeLineStart.getLongitude());
		double y1 = MapUtils.getTileNumberY(zoom, nodeLineStart.getLatitude());
		double x2 = MapUtils.getTileNumberX(zoom, nodeLineEnd.getLongitude());
		double y2 = MapUtils.getTileNumberY(zoom, nodeLineEnd.getLatitude());
		double x = MapUtils.getTileNumberX(zoom, node.getLongitude());
		double y = MapUtils.getTileNumberY(zoom, node.getLatitude());
		double A = x - x1;
		double B = y - y1;
		double C = x2 - x1;
		double D = y2 - y1;
		return Math.abs(A * D - C * B) / Math.sqrt(C * C + D * D);
	}
	
	public static boolean isClockwiseWay(Way w){
		return isClockwiseWay(Collections.singletonList(w));
	}
	
	public static boolean isClockwiseWay(List<Way> ways){
		if(ways.isEmpty()){
			return true;
		}
		LatLon latLon = ways.get(0).getLatLon();
		double lat = latLon.getLatitude();
		double lon = 180;
		double firstLon = -360;
		boolean firstDirectionUp = false;
		double previousLon = -360;
		
		double clockwiseSum = 0;
		
		Node prev = null;
		boolean firstWay = true;
		for(Way w : ways){
			List<Node> ns = w.getNodes();
			int startInd = 0;
			int nssize = ns.size();
			if(firstWay && nssize > 0){
				prev = ns.get(0);
				startInd = 1;
				firstWay = false;
			}
			for(int i = startInd; i < nssize;i++) {
				Node next = ns.get(i);
				double rlon = ray_intersect_lon(prev, next, lat, lon);
				if(rlon != - 360d){
					boolean skipSameSide = (prev.getLatitude() <= lat) == (next.getLatitude() <= lat);
					if(skipSameSide){
						continue;
					}
					boolean directionUp = prev.getLatitude() <= lat;
					if(firstLon == - 360){
						firstDirectionUp = directionUp;
						firstLon = rlon;
					} else {
						boolean clockwise = (!directionUp) == (previousLon < rlon);
						if(clockwise){
							clockwiseSum += Math.abs(previousLon - rlon);
						} else {
							clockwiseSum -= Math.abs(previousLon - rlon);
						}
					}
					previousLon = rlon;
				}
				prev = next;
			}
		}
		
		if(firstLon != -360){
			boolean clockwise = (!firstDirectionUp) == (previousLon < firstLon);
			if(clockwise){
				clockwiseSum += Math.abs(previousLon - firstLon);
			} else {
				clockwiseSum -= Math.abs(previousLon - firstLon);
			}
		}
		
		return clockwiseSum >= 0;
	}
	
	// try to intersect from left to right
	public static double ray_intersect_lon(Node node, Node node2, double latitude, double longitude) {
		// a node below
		Node a = node.getLatitude() < node2.getLatitude() ? node : node2;
		// b node above
		Node b = a == node2 ? node : node2;
		if (latitude == a.getLatitude() || latitude == b.getLatitude()) {
			latitude += 0.00000001d;
		}
		if (latitude < a.getLatitude() || latitude > b.getLatitude()) {
			return -360d;
		} else {
			if (longitude < Math.min(a.getLongitude(), b.getLongitude())) {
				return -360d;
			} else {
				if (a.getLongitude() == b.getLongitude() && longitude == a.getLongitude()) {
					// the node on the boundary !!!
					return longitude;
				}
				// that tested on all cases (left/right)
				double lon = b.getLongitude() -
					(b.getLatitude() - latitude) * (b.getLongitude() - a.getLongitude()) / (b.getLatitude() - a.getLatitude());
				if (lon <= longitude) {
					return lon;
				} else {
					return -360d;
				}
			}
		}
	}
}