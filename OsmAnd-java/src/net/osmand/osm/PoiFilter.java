package net.osmand.osm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PoiFilter extends AbstractPoiType {
	
	private PoiCategory pc; 
	private List<PoiType> poiTypes = new ArrayList<PoiType>();
	private Map<String, PoiType> map = new LinkedHashMap<String, PoiType>();

	public PoiFilter(MapPoiTypes registry, PoiCategory pc, String keyName){
		super(keyName, registry);
		this.pc = pc;
	}
	
	public PoiCategory getPoiCategory() {
		return pc;
	}
	
	public PoiType getPoiTypeByKeyName(String kn) {
		return map.get(kn);
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
