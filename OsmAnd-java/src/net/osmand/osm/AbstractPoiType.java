package net.osmand.osm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public abstract class AbstractPoiType {

	protected final String keyName;
	protected final MapPoiTypes registry;
	private List<PoiType> poiAdditionals = null;
	
	public AbstractPoiType(String keyName, MapPoiTypes registry) {
		this.keyName = keyName;
		this.registry = registry;
	}

	public String getKeyName() {
		return keyName;
	}
	
	public String getIconKeyName() {
		return getKeyName().replace(':', '_');
	}
	
	
	public String getTranslation() {
		return registry.getTranslation(this);
	}
	
	public void addPoiAdditional(PoiType tp) {
		if(poiAdditionals == null) {
			poiAdditionals = new ArrayList<PoiType>();
		}
		poiAdditionals.add(tp);
	}
	
	public List<PoiType> getPoiAdditionals() {
		if(poiAdditionals == null) {
			return Collections.emptyList();
		}
		return poiAdditionals;
	}

	public abstract Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes);
	
	
}
