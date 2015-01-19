package net.osmand.osm;

public class PoiType {
	
	private String name;
	private String translationName;
	private MapPoiTypes poiTypes;
	private PoiCategory category;
	
	public PoiType(MapPoiTypes poiTypes, PoiCategory category){
		this.poiTypes = poiTypes;
		this.category = category;
	}
	
	public String getTranslationName() {
		return translationName;
	}
	
	public String getName() {
		return name;
	}

}
