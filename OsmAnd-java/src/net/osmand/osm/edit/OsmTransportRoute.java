package net.osmand.osm.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.data.TransportRoute;

public class OsmTransportRoute extends TransportRoute {
	private List<Way> ways;
	
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
}