package net.osmand.osm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PoiFilter {
	
	private String keyName;
	private String translationName;
	private MapPoiTypes registry;
	private List<PoiType> poiTypes = new ArrayList<PoiType>();
	private Map<String, PoiType> map = new LinkedHashMap<String, PoiType>(); 

	public PoiFilter(MapPoiTypes registry, String keyName){
		this.registry = registry;
		this.keyName = keyName;
	}
	
	public String getTranslationName() {
		return translationName;
	}
	
	public void addPoiType(PoiType type) {
		if(!map.containsKey(type.getName())) {
			poiTypes.add(type);
			map.put(type.getName(), type);
		}
		
	}
	
	public List<PoiType> getPoiTypes() {
		return poiTypes;
	}
	
	public String getName() {
		return keyName;
	}


}
