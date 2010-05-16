package com.osmand.data;

import com.osmand.osm.Entity;

public class Building extends MapObject<Entity> {
	
	private Entity e;

	public Building(Entity e){
		this.e = e;
	}
	
	public Building(){}

	@Override
	public Entity getEntity() {
		return e;
	}

}
