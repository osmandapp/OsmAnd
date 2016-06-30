package net.osmand.search.example.core;

import net.osmand.data.LatLon;

public class SearchResult<T> {

	public T object;
	public ObjectType objectType;
	
	// calculated by formula priority - 1 / (1 + priorityDistance * distance)
	public double searchDistance;
	
	public LatLon location;
	public String mainName;
	// search phrase that makes search result valid 
	public SearchPhrase requiredSearchPhrase;
}
