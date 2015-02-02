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
	private PoiCategory pc; 

	public PoiFilter(MapPoiTypes registry, PoiCategory pc, String keyName){
		this.registry = registry;
		this.pc = pc;
		this.keyName = keyName;
	}
	
	public PoiCategory getPoiCategory() {
		return pc;
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
