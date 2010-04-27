package com.osmand.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Entity {
	// lazy initializing
	private Map<String, String> tags = null;
	private final long id;
	
	public Entity(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
	public String putTag(String key, String value){
		if(tags == null){
			tags = new LinkedHashMap<String, String>();
		}
		return tags.put(key, value);
	}
	
	public String getTag(String key){
		if(tags == null){
			return null;
		}
		return tags.get(key);
	}
	
	public Map<String, String> getTags() {
		if(tags == null){
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(tags);
	}
	

	public Collection<String> getTagKeySet(){
		if(tags == null){
			return Collections.emptyList();
		}
		return tags.keySet();
	}
	
	public abstract void initializeLinks(Map<Long, Entity> entities);
	
	
	/**
	 * @return middle point for entity
	 */
	public abstract LatLon getLatLon();
	

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entity other = (Entity) obj;
		if (id != other.id)
			return false;
		return true;
	}
}
