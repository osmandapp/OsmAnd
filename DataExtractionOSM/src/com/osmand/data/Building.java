package com.osmand.data;

import com.osmand.osm.Entity;

public class Building extends MapObject<Entity> {
	
	private final Entity e;

	public Building(Entity e){
		this.e = e;
	}

	@Override
	public Entity getEntity() {
		return e;
	}

}
