package net.osmand.data;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.MapUtils;

public class TransportRoute extends MapObject {
	private List<TransportStop> forwardStops = new ArrayList<TransportStop>();
	private String ref;
	private String operator;
	private String type;
	private Integer dist = null;
	private String color;
	private List<Way> forwardWays;
	public static final double SAME_STOP = 25;
	
	public TransportRoute(){
	}
	
	public List<TransportStop> getForwardStops() {
		return forwardStops;
	}
	
	public List<Way> getForwardWays() {
		return forwardWays;
	}
	
	
	public void mergeForwardWays() {
		boolean changed = true;
		// combine as many ways as possible
		while (changed) {
			changed = false;
			Iterator<Way> it = forwardWays.iterator();
			while (it.hasNext() && !changed) {
				// scan to merge with the next segment
				double d = SAME_STOP;
				Way w = it.next();
				Way toCombine = null;
				boolean reverseOriginal = false;
				boolean reverseCombine = false;
				for (int i = 0; i < forwardWays.size(); i++) {
					Way combine = forwardWays.get(i);
					if (combine == w) {
						continue;
					}
					double distAttachAfter = MapUtils.getDistance(w.getFirstNode().getLatLon(), combine.getLastNode().getLatLon());
					double distReverseAttachAfter = MapUtils.getDistance(w.getLastNode().getLatLon(), combine.getLastNode()
							.getLatLon());
					double distAttachAfterReverse = MapUtils.getDistance(w.getFirstNode().getLatLon(), combine.getFirstNode().getLatLon());
					if (distAttachAfter < d) {
						toCombine = combine;
						reverseOriginal = false;
						reverseCombine = false;
						d = distAttachAfter;
					} else if (distReverseAttachAfter < d) {
						toCombine = combine;
						reverseOriginal = true;
						reverseCombine = false;
						d = distReverseAttachAfter;
					} else if (distAttachAfterReverse < d) {
						toCombine = combine;
						reverseOriginal = false;
						reverseCombine = true;
						d = distAttachAfterReverse;
					}
				}
				if (toCombine != null) {
					if(reverseCombine) {
						toCombine.reverseNodes();
					}
					if(reverseOriginal) {
						w.reverseNodes();
					}
					for (int i = 1; i < w.getNodes().size(); i++) {
						toCombine.addNode(w.getNodes().get(i));
					}
					it.remove();
					changed = true;
				}
			}
		}
		if (forwardStops.size() > 0) {
			// resort ways to stops order 
			final Map<Way, int[]> orderWays = new HashMap<Way, int[]>();
			for (Way w : forwardWays) {
				int[] pair = new int[] { 0, 0 };
				Node firstNode = w.getFirstNode();
				TransportStop st = forwardStops.get(0);
				double firstDistance = MapUtils.getDistance(st.getLocation(), firstNode.getLatitude(),
						firstNode.getLongitude());
				Node lastNode = w.getLastNode();
				double lastDistance = MapUtils.getDistance(st.getLocation(), lastNode.getLatitude(),
						lastNode.getLongitude());
				for (int i = 1; i < forwardStops.size(); i++) {
					st = forwardStops.get(i);
					double firstd = MapUtils.getDistance(st.getLocation(), firstNode.getLatitude(),
							firstNode.getLongitude());
					double lastd = MapUtils.getDistance(st.getLocation(), lastNode.getLatitude(),
							lastNode.getLongitude());
					if (firstd < firstDistance) {
						pair[0] = i;
						firstDistance = firstd;
					}
					if (lastd < lastDistance) {
						pair[1] = i;
						lastDistance = lastd;
					}
				}
				orderWays.put(w, pair);
				if(pair[0] > pair[1]) {
					w.reverseNodes();
				}
			}
			if(orderWays.size() > 1) {
				Collections.sort(forwardWays, new Comparator<Way>() {
					@Override
					public int compare(Way o1, Way o2) {
						int[] is1 = orderWays.get(o1);
						int[] is2 = orderWays.get(o2);
						int i1 = is1 != null ? Math.min(is1[0], is1[1]) : 0;
						int i2 = is2 != null ? Math.min(is2[0], is2[1]) : 0;
						return Integer.compare(i1, i2);
					}
				});
			}
			
		}
	}
	
	
	public String getColor() {
		return color;
	}
	
	public void addWay(Way w) {
		if (forwardWays == null) {
			forwardWays = new ArrayList<>();
		}
		forwardWays.add(w);
	}
	
	public String getRef() {
		return ref;
	}
	
	public void setRef(String ref) {
		this.ref = ref;
	}
	
	public String getOperator() {
		return operator;
	}
	
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public int getDistance(){
		if(dist == null){
			dist = getAvgBothDistance();
		}
		return dist;
	}
	
	public void setDistance(Integer dist) {
		this.dist = dist;
	}
	
	public void setColor(String color) {
		this.color = color;
	}
	
	public int getAvgBothDistance() {
		int d = 0;
		int fSsize = forwardStops.size();
		for (int i = 1; i < fSsize; i++) {
			d += MapUtils.getDistance(forwardStops.get(i - 1).getLocation(), forwardStops.get(i).getLocation());
		}
		return d;
	}
}