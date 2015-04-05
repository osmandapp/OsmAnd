package net.osmand.osm;

import java.util.ArrayList;
import java.util.List;


public class PoiCategory extends PoiFilter {

	private List<PoiFilter> poiFilters = new ArrayList<PoiFilter>();
	private int regId;
	private String defaultTag;

	public PoiCategory(MapPoiTypes registry, String keyName, int regId) {
		super(registry, null, keyName);
		this.regId = regId;
	}

	public void addPoiType(PoiFilter poi) {
		poiFilters.add(poi);
	}

	public List<PoiFilter> getPoiFilters() {
		return poiFilters;
	}
	
	public String getDefaultTag() {
		if(defaultTag == null) {
			return keyName;
		}
		return defaultTag;
	}
	
	public void setDefaultTag(String defaultTag) {
		this.defaultTag = defaultTag;
	}

	
	public boolean isWiki() {
		return keyName.equals(MapPoiTypes.OSM_WIKI_CATEGORY);
	}

	public String getKey() {
		return keyName;
	}

	public int ordinal() {
		return regId;
	}

	

}
