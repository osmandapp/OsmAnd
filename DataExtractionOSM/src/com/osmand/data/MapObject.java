package com.osmand.data;

import java.text.Collator;

import com.osmand.osm.Entity;
import com.osmand.osm.LatLon;
import com.osmand.osm.MapUtils;
import com.osmand.osm.OSMSettings.OSMTagKey;

public abstract class MapObject implements Comparable<MapObject> {
	
	protected String name = null;
	protected String enName = null;
	protected LatLon location = null;
	protected Long id = null;

	public MapObject(){}
	
	public MapObject(Entity e){
		setEntity(e);
	}
	
	
	public void setEntity(Entity e){
		this.id = e.getId();
		if(this.name == null){
			this.name = e.getTag(OSMTagKey.NAME);
		}
		if(this.location == null){
			this.location = MapUtils.getCenter(e);
		}
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
		if (id != null) {
			return id + "";
		} else {
			return "";
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getEnName() {
		return enName;
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

}
