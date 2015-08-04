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
	private boolean topVisible;

	
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
	
	public void setTopVisible(boolean topVisible) {
		this.topVisible = topVisible;
	}
	
	public boolean isTopVisible() {
		return topVisible;
	}
	
	public boolean isAdditional() {
		return this instanceof PoiType && ((PoiType) this).isAdditional();
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
