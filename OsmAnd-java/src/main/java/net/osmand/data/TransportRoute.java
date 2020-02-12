package net.osmand.data;

import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransportRoute extends MapObject {
	private List<TransportStop> forwardStops = new ArrayList<TransportStop>();
	private String ref;
	private String operator;
	private String type;
	private Integer dist = null;
	private String color;
	private List<Way> forwardWays;
	private TransportSchedule schedule;
	public static final double SAME_STOP = 40;
	
	public TransportRoute() {
	}
	
	public TransportSchedule getSchedule() {
		return schedule;
	}
	
	public TransportSchedule getOrCreateSchedule() {
		if (schedule == null) {
			schedule = new TransportSchedule();
		}
		return schedule;
	}
	
	
	public List<TransportStop> getForwardStops() {
		return forwardStops;
	}
	
	public List<Way> getForwardWays() {
		if(forwardWays == null) {
			return Collections.emptyList();
		}
		return forwardWays;
	}
	
	
	public void mergeForwardWays() {
		boolean changed = true;
		// combine as many ways as possible
		while (changed && forwardWays != null) {
			changed = false;
			for(int k = 0; k < forwardWays.size(); ) {
				// scan to merge with the next segment
				Way first = forwardWays.get(k);
				double d = SAME_STOP;
				boolean reverseSecond = false;
				boolean reverseFirst = false;
				int secondInd = -1;
				for (int i = k + 1; i < forwardWays.size(); i++) {
					Way w = forwardWays.get(i);
					double distAttachAfter = MapUtils.getDistance(first.getLastNode().getLatLon(), w.getFirstNode().getLatLon());
					double distReverseAttach = MapUtils.getDistance(first.getLastNode().getLatLon(), w.getLastNode().getLatLon());
					double distAttachAfterReverse = MapUtils.getDistance(first.getFirstNode().getLatLon(), w.getFirstNode().getLatLon());
					double distReverseAttachReverse = MapUtils.getDistance(first.getFirstNode().getLatLon(), w.getLastNode().getLatLon());
					if (distAttachAfter < d) {
						reverseSecond = false;
						reverseFirst = false;
						d = distAttachAfter;
						secondInd = i; 
					}
					if (distReverseAttach < d) {
						reverseSecond = true;
						reverseFirst = false;
						d = distReverseAttach;
						secondInd = i;
					}
					if (distAttachAfterReverse < d) {
						reverseSecond = false;
						reverseFirst = true;
						d = distAttachAfterReverse;
						secondInd = i;
					}
					if (distReverseAttachReverse < d) {
						reverseSecond = true;
						reverseFirst = true;
						d = distReverseAttachReverse;
						secondInd = i;
					}
					if (d == 0) {
						break;
					}
				}
				if (secondInd != -1) {
					Way second = forwardWays.remove(secondInd);
					if(reverseFirst) {
						first.reverseNodes();
					}
					if(reverseSecond) {
						second.reverseNodes();
					}
					for (int i = 1; i < second.getNodes().size(); i++) {
						first.addNode(second.getNodes().get(i));
					}
					changed = true;
				} else {
					k++;
				}
			}
		}
		if (forwardStops.size() > 0) {
			// resort ways to stops order 
			final Map<Way, int[]> orderWays = new HashMap<Way, int[]>();
			for (Way w : getForwardWays()) {
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

	public String getAdjustedRouteRef(boolean small) {
		String adjustedRef = getRef();
		if (adjustedRef != null) {
			int charPos = adjustedRef.lastIndexOf(':');
			if (charPos != -1) {
				adjustedRef = adjustedRef.substring(0, charPos);
			}
			int maxRefLength = small ? 5 : 8;
			if (adjustedRef.length() > maxRefLength) {
				adjustedRef = adjustedRef.substring(0, maxRefLength - 1) + "…";
			}
		}
		return adjustedRef;
	}

	public boolean compareRoute(TransportRoute thatObj) {
		if (this.compareObject(thatObj) &&
				Algorithms.objectEquals(this.ref, thatObj.ref) &&
				Algorithms.objectEquals(this.operator, thatObj.operator) &&
				Algorithms.objectEquals(this.type, thatObj.type) &&
				Algorithms.objectEquals(this.color, thatObj.color) &&
				this.getDistance() == thatObj.getDistance() &&
				((this.schedule == null && thatObj.schedule == null) ||
						(this.schedule != null && thatObj.schedule != null && this.schedule.compareSchedule(thatObj.schedule))) &&
				this.forwardStops.size() == thatObj.forwardStops.size() &&
				((this.forwardWays == null && thatObj.forwardWays == null) ||
						(this.forwardWays != null && thatObj.forwardWays != null && this.forwardWays.size() == thatObj.forwardWays.size()))) {

			for (int i = 0; i < this.forwardStops.size(); i++) {
				if (!this.forwardStops.get(i).compareStop(thatObj.forwardStops.get(i))) {
					return false;
				}
			}
			if (this.forwardWays != null) {
				for (int i = 0; i < this.forwardWays.size(); i++) {
					if (!this.forwardWays.get(i).compareWay(thatObj.forwardWays.get(i))) {
						return false;
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}
}