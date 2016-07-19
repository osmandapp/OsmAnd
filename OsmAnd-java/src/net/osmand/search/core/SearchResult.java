package net.osmand.search.core;

import java.util.Collection;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

public class SearchResult {
	// search phrase that makes search result valid 
	public SearchPhrase requiredSearchPhrase;
	
	public Object object;
	public ObjectType objectType;
	public BinaryMapIndexReader file;
	
	public double priority;
	public double priorityDistance;
	
	public SearchResult(SearchPhrase sp) {
		this.requiredSearchPhrase = sp;
	}

	public double getSearchDistance(LatLon location) {
		double distance = 0;
		if (location != null && this.location != null) {
			distance = MapUtils.getDistance(location, this.location);
		}
		return priority - 1 / (1 + priorityDistance * distance);
	}
	
	public LatLon location;
	public int preferredZoom = 15;
	public String localeName;
	
	public Collection<String> otherNames;
	
	public String localeRelatedObjectName;
	public Object relatedObject;
	public double distRelatedObjectName;
	
	public int wordsSpan = 1;


	
	

}
