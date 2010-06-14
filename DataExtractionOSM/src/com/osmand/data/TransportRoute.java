package com.osmand.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.osmand.osm.Relation;
import com.osmand.osm.Way;

public class TransportRoute extends MapObject {
	private List<Way> ways;
	private List<TransportStop> forwardStops = new ArrayList<TransportStop>();
	private List<TransportStop> backwardStops = new ArrayList<TransportStop>();
	private String ref;
	
	public TransportRoute(Relation r, String ref){
		super(r);
		this.ref = ref;
	}
	
	public List<TransportStop> getForwardStops() {
		return forwardStops;
	}
	
	public List<TransportStop> getBackwardStops() {
		return backwardStops;
	}
	
	public List<Way> getWays() {
		if(ways == null){
			return Collections.emptyList();
		}
		return ways;
	}
	
	public void addWay(Way w){
		if(ways == null){
			ways = new ArrayList<Way>();
		}
		ways.add(w);
	}
	
	public String getRef() {
		return ref;
	}
	
	public void setRef(String ref) {
		this.ref = ref;
	}
	
	
}
