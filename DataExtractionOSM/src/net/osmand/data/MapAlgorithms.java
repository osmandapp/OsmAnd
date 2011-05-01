package net.osmand.data;

import java.util.ArrayList;
import java.util.List;

import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.Way;

public class MapAlgorithms {
	
	public static void simplifyDouglasPeucker(List<Node> n, int zoom, int epsilon, Way w){
		ArrayList<Integer> l = new ArrayList<Integer>();
		int first = 0;
		while(first < n.size()){
			if(n.get(first) != null){
				break;
			}
			first++;
		}
		int last = n.size() - 1;
		while (last >= 0) {
			if (n.get(last) != null) {
				break;
			}
			last--;
		}
		if(last - first < 1){
			return;
		}
		boolean cycle = n.get(first).getId() == n.get(last).getId();
		simplifyDouglasPeucker(n, zoom, epsilon, l, first, cycle ? last - 1: last);
		w.addNode(n.get(first));
		for (int i = 0; i < l.size(); i++) {
			w.addNode(n.get(l.get(i)));
		}
		if (cycle) {
			w.addNode(n.get(first));
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
}
