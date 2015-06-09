package net.osmand.osm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
		if(!map.containsKey(type.getKeyName())) {
			poiTypes.add(type);
			map.put(type.getKeyName(), type);
		} else {
			PoiType prev = map.get(type.getKeyName());
			if(prev.isReference()) {
				poiTypes.remove(prev);
				poiTypes.add(type);
				map.put(type.getKeyName(), type);
			}
		}
	}
	
	public Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
		if (!acceptedTypes.containsKey(pc)) {
			acceptedTypes.put(pc, new LinkedHashSet<String>());
		}
		LinkedHashSet<String> set = acceptedTypes.get(pc);
		for (PoiType pt : poiTypes) {
			set.add(pt.getKeyName());
		}
		addReferenceTypes(acceptedTypes);
		return acceptedTypes;
	}

	protected void addReferenceTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes) {
		for (PoiType pt : getPoiTypes()) {
			if (pt.isReference()) {
				PoiCategory refCat = pt.getReferenceType().getCategory();
				if (!acceptedTypes.containsKey(refCat)) {
					acceptedTypes.put(refCat, new LinkedHashSet<String>());
				}
				LinkedHashSet<String> ls = acceptedTypes.get(refCat);
				if (ls != null) {
					ls.add(pt.getKeyName());
				}
			}
		}
	}
	
	public List<PoiType> getPoiTypes() {
		return poiTypes;
	}
	
}
