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
	private final String iconKeyName;

	public PoiFilter(MapPoiTypes registry, PoiCategory pc, String keyName, String iconKeyName) {
		super(keyName, registry);
		this.iconKeyName = iconKeyName;
		this.pc = pc;
	}

	public PoiCategory getPoiCategory() {
		return pc;
	}

	public PoiType getPoiTypeByKeyName(String kn) {
		return map.get(kn);
	}


	public void addExtraPoiTypes(Map<String, PoiType> poiTypesToAdd) {
		List<PoiType> npoiTypes = null;
		Map<String, PoiType> nmap = null;
		for (PoiType poiType : poiTypesToAdd.values()) {
			String keyName = poiType.getKeyName();
			if (!map.containsKey(keyName) && !registry.isTypeForbidden(keyName)) {
				if (npoiTypes == null) {
					npoiTypes = new ArrayList<PoiType>(this.poiTypes);
					nmap = new LinkedHashMap<>(map);
				}
				npoiTypes.add(poiType);
				nmap.put(keyName, poiType);
			}
		}
		if (npoiTypes != null) {
			poiTypes = npoiTypes;
			map = nmap;
		}
	}

	public void addPoiType(PoiType type) {
		if (registry.isTypeForbidden(type.keyName)) {
			return;
		}
		if (!map.containsKey(type.getKeyName())) {
			poiTypes.add(type);
			map.put(type.getKeyName(), type);
		} else {
			PoiType prev = map.get(type.getKeyName());
			if (prev.isReference()) {
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

	public String getIconKeyName() {
		return formatKeyName(iconKeyName != null ? iconKeyName : getKeyName());
	}

	public List<PoiType> getPoiTypes() {
		return poiTypes;
	}
}
