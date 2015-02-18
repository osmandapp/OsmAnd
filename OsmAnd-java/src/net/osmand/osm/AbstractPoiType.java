package net.osmand.osm;

public class AbstractPoiType {

	protected final String keyName;
	protected final MapPoiTypes registry;
	
	
	
	public AbstractPoiType(String keyName, MapPoiTypes registry) {
		this.keyName = keyName;
		this.registry = registry;
	}

	public String getName() {
		return keyName;
	}
	
	public String getKeyName() {
		return keyName;
	}
	
	public String getTranslation() {
		return registry.getTranslation(this);
	}
	
	
}
