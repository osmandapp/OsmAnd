package com.osmand.data;

import java.text.Collator;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.Entity.EntityId;
import com.osmand.osm.Entity.EntityType;
import com.osmand.osm.OSMSettings.OSMTagKey;

public abstract class MapObject implements Comparable<MapObject> {
	
	protected String name = null;
	protected String enName = null;
	protected LatLon location = null;
	protected Long id = null;
	protected EntityType type = null;

	public MapObject(){}
	
	public MapObject(Entity e){
		setEntity(e);
	}
	
	
	public void setEntity(Entity e){
		this.id = e.getId();
		this.type = EntityType.valueOf(e);
		if(this.name == null){
			this.name = e.getTag(OSMTagKey.NAME);
		}
		if(this.enName == null){
			this.enName = e.getTag(OSMTagKey.NAME_EN);
		}
		if(this.location == null){
			this.location = MapUtils.getCenter(e);
		}
	}
	
	public EntityId getEntityId(){
		EntityType t = type;
		if(t == null){
			t = EntityType.NODE;
		}
		return new EntityId(t, id);
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getId() {
		if(id != null){
			return id;
		}
		return null;
	}
	
	public String getName(boolean en){
		if(en){
			return getEnName();
		} else {
			return getName();
		}
	}
	
	public String getName() {
		if (this.name != null) {
			return this.name;
		}
		return ""; //$NON-NLS-1$
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getEnName() {
		if(this.enName != null){
			return this.enName;
		}
		return ""; //$NON-NLS-1$
	}
	
	public void setEnName(String enName) {
		this.enName = enName;
	}
	
	public LatLon getLocation(){
		return location;
	}
	
	public void setLocation(double latitude, double longitude){
		location = new LatLon(latitude, longitude);
	}
	
	@Override
	public int compareTo(MapObject o) {
		return Collator.getInstance().compare(getName(), o.getName());
	}
	
	public void doDataPreparation() {
		
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name +"("+id+")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
