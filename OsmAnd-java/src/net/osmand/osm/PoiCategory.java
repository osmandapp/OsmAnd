package net.osmand.osm;

public class PoiCategory {
	
	private String name;
	private String translationName;
	private MapPoiTypes poiTypes;
	
	public PoiCategory(MapPoiTypes poiTypes){
		this.poiTypes = poiTypes;
	}
	
	public String getTranslationName() {
		return translationName;
	}
	
	public String getName() {
		return name;
	}

}
