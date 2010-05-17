package com.osmand.data;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.OSMSettings.OSMTagKey;

public abstract class MapObject<T extends Entity> implements Comparable<MapObject<T>> {
	
	protected String name = null;
	protected LatLon location = null;
	protected Long id = null;
	// could be null
	protected T entity = null;

	public MapObject(){}
	
	public MapObject(T e){
		entity = e;
	}
	
	public T getEntity(){
		return entity;
	}
	
	public void setEntity(T e){
		entity = e;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getId() {
		if(id != null){
			return id;
		}
		T e = getEntity();
		if(e != null){
			return e.getId();
		}
		return null;
	}
	
	public String getName() {
		if (this.name != null) {
			return this.name;
		}
		Entity e = getEntity();
		if (e != null) {
			String name = getEntity().getTag(OSMTagKey.NAME);
			if (name != null) {
				return name;
			}
			return e.getId() + "";
		} else {
			return "";
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public LatLon getLocation(){
		if(location != null){
			return location;
		}
		return MapUtils.getCenter(getEntity());
	}
	
	public void setLocation(double latitude, double longitude){
		location = new LatLon(latitude, longitude);
	}
	
	@Override
	public int compareTo(MapObject<T> o) {
		return getName().compareTo(o.getName());
	}

}
