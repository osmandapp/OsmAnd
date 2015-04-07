package net.osmand.osm;

import java.util.LinkedHashSet;
import java.util.Map;

public abstract class AbstractPoiType {

	protected final String keyName;
	protected final MapPoiTypes registry;
	
	public AbstractPoiType(String keyName, MapPoiTypes registry) {
		this.keyName = keyName;
		this.registry = registry;
	}

	public String getKeyName() {
		return keyName;
	}
	
	public String getTranslation() {
		return registry.getTranslation(this);
	}

	public abstract Map<PoiCategory, LinkedHashSet<String>> putTypes(Map<PoiCategory, LinkedHashSet<String>> acceptedTypes);
	
	
}
