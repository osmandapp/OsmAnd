package net.osmand.search.example.core;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

public class SearchResult {
	// search phrase that makes search result valid 
	public SearchPhrase requiredSearchPhrase;
	
	public Object object;
	public ObjectType objectType;
	
	public double priority;
	public double priorityDistance;
	
	public SearchResult(SearchPhrase sp) {
		this.requiredSearchPhrase = sp;
	}
	public double getSearchDistance(LatLon location) {
		double distance = 0;
		if(location != null && this.location != null) {
			distance = MapUtils.getDistance(location, this.location);
		}
		return priority -  1 / ( 1 + priorityDistance * distance);
	}
	
	public LatLon location;
	public int preferredZoom = 15;
	public String mainName;

}
