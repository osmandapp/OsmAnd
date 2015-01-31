package net.osmand.osm;

public class PoiType {
	
	private String name;
	private String translationName;
	private MapPoiTypes poiTypes;
	public String getOsmTag() {
		return osmTag;
	}

	public void setOsmTag(String osmTag) {
		this.osmTag = osmTag;
	}

	public String getOsmTag2() {
		return osmTag2;
	}

	public void setOsmTag2(String osmTag2) {
		this.osmTag2 = osmTag2;
	}

	public String getOsmValue() {
		return osmValue;
	}

	public void setOsmValue(String osmValue) {
		this.osmValue = osmValue;
	}

	public String getOsmValue2() {
		return osmValue2;
	}

	public void setOsmValue2(String osmValue2) {
		this.osmValue2 = osmValue2;
	}

	private PoiCategory category;
	private String osmTag;
	private String osmTag2;
	private String osmValue;
	private String osmValue2;
	
	public PoiType(MapPoiTypes poiTypes, PoiCategory category, String name){
		this.poiTypes = poiTypes;
		this.category = category;
		this.name = name;
	}
	
	public PoiCategory getCategory() {
		return category;
	}
	
	public String getTranslationName() {
		return translationName;
	}
	
	public String getName() {
		return name;
	}

}
