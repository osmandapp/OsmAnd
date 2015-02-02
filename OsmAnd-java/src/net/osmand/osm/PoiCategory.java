package net.osmand.osm;

import java.util.ArrayList;
import java.util.List;

public class PoiCategory extends PoiFilter {
	
	private List<PoiFilter> poiFilters = new ArrayList<PoiFilter>();
	
	public PoiCategory(MapPoiTypes registry, String keyName){
		super(registry, null, keyName);
	}
	
	public void addPoiType(PoiFilter poi) {
		poiFilters.add(poi);
	}
	
	public List<PoiFilter> getPoiFilters() {
		return poiFilters;
	}
	
}
