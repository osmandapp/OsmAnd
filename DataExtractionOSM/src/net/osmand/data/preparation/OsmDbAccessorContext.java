package net.osmand.data.preparation;

import java.sql.SQLException;

import net.osmand.osm.Entity;

public interface OsmDbAccessorContext {
	
	public void loadEntityData(Entity e) throws SQLException;
}