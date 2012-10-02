package net.osmand.osm;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.osmand.osm.OSMSettings.OSMTagKey;


public abstract class Entity {
	public enum EntityType {
		NODE,
		WAY,
		RELATION,
		WAY_BOUNDARY;
		
		public static EntityType valueOf(Entity e){
			if(e instanceof Node){
				return NODE;
			} else if(e instanceof Way){
				return WAY;
			} else if(e instanceof Relation){
				return RELATION;
			}
			return null;
		}
	}
	
	public static class EntityId {
		private final EntityType type;
		private final Long id;
		
		
		public EntityId(EntityType type, Long id){
			this.type = type;
			this.id = id;
		}
		
		public static EntityId valueOf(Entity e){
			return new EntityId(EntityType.valueOf(e), e.getId());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}
		
		@Override
		public String toString() {
			return type + " " + id; //$NON-NLS-1$
		}
		
		public EntityType getType() {
			return type;
		}
		
		public Long getId() {
			return id;
		}

		public String getOsmUrl() {
			final String browseUrl = "http://www.openstreetmap.org/browse/";
			if (type == EntityType.NODE)
				return browseUrl + "node/" + id;
			if (type == EntityType.WAY)
				return browseUrl + "way/" + id;
			return null;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EntityId other = (EntityId) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
		
	}
	
	// lazy initializing
	private Map<String, String> tags = null;
	private final long id;
	private boolean dataLoaded;
	
	public Entity(long id) {
		this.id = id;
	}

	public Entity(Entity copy, long id) {
		this.id = id;
		for (String t : copy.getTagKeySet()) {
			putTag(t, copy.getTag(t));
		}
		this.dataLoaded = copy.dataLoaded;
	}

	public long getId() {
		return id;
	}
	
	public String removeTag(String key){
		return tags.remove(key);
	}
	
	public void removeTags(String[] keys){
		if (tags != null){
			for (String key : keys){
				tags.remove(key);
			}
		}
	}
	
	public String putTag(String key, String value){
		if(tags == null){
			tags = new LinkedHashMap<String, String>();
		}
		return tags.put(key, value);
	}
	
	public String getTag(OSMTagKey key){
		return getTag(key.getValue());
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
	
	public abstract void initializeLinks(Map<EntityId, Entity> entities);
	
	
	/**
	 * @return middle point for entity
	 */
	public abstract LatLon getLatLon();
	
	
	public boolean isVirtual(){
		return id < 0;
	}
	
	public String getOsmUrl() {
		return EntityId.valueOf(this).getOsmUrl();
	}

	@Override
	public String toString() {
		return EntityId.valueOf(this).toString();
	}
	@Override
	public int hashCode() {
		if (id < 0) {
			return System.identityHashCode(this);
		}
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
		// virtual are not equal
		if(id < 0){
			return false;
		}
		return true;
	}

	public Set<String> getIsInNames() {
		String values = getTag(OSMTagKey.IS_IN);
		if (values == null) {
			return Collections.emptySet();
		}
		if (values.indexOf(';') != -1) {
			String[] splitted = values.split(";");
			Set<String> set = new HashSet<String>(splitted.length);
			for (int i = 0; i < splitted.length; i++) {
				set.add(splitted[i].trim());
			}
			return set;
		}
		return Collections.singleton(values.trim());
	}

	public void entityDataLoaded() {
		this.dataLoaded = true;
	}
	
	public boolean isDataLoaded() {
		return dataLoaded;
	}
}
