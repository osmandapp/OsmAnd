package net.osmand.osm;

import java.util.*;


public class PoiCategory extends PoiFilter {

	private List<PoiFilter> poiFilters = new ArrayList<PoiFilter>();
	private Set<PoiType> basemapPoi = null;
	private int regId;
	private String defaultTag;

	public PoiCategory(MapPoiTypes registry, String keyName, int regId) {
		super(registry, null, keyName, null);
		this.regId = regId;
	}

	public void addPoiType(PoiFilter poi) {
		poiFilters.add(poi);
	}

	public List<PoiFilter> getPoiFilters() {
		return poiFilters;
	}

	public PoiFilter getPoiFilterByName(String keyName) {
		for (PoiFilter f : poiFilters) {
			if (f.getKeyName().equals(keyName)) {
				return f;
			}
		}
		return null;
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
	
	public Map<PoiCategory, LinkedHashSet<String>> putTypes(
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
		acceptedTypes.put(this, null);
		addReferenceTypes(acceptedTypes);
		return acceptedTypes;
	}

	
	public boolean isWiki() {
		return keyName.equals(MapPoiTypes.OSM_WIKI_CATEGORY);
	}

	public boolean isRoutes() {
		return keyName.equals(MapPoiTypes.ROUTES);
	}

	public int ordinal() {
		return regId;
	}

	
	public void addBasemapPoi(PoiType pt) {
		if(basemapPoi == null) {
			basemapPoi = new HashSet<PoiType>();
		}
		basemapPoi.add(pt);
	}

	public boolean containsBasemapPoi(PoiType pt) {
		if(basemapPoi == null) {
			return false;
		}
		return basemapPoi.contains(pt);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		PoiCategory other = (PoiCategory) obj;
		return regId == other.regId &&
				(Objects.equals(keyName, other.keyName)) &&
				(Objects.equals(defaultTag, other.defaultTag));
	}
	
	@Override
	public int hashCode() {
		int result = 8;
		result = 88 * result + (keyName != null ? keyName.hashCode() : 0);
		result = 88 * result + regId;
		result = 88 * result + (defaultTag != null ? defaultTag.hashCode() : 0);
		return result;
	}
	
}
