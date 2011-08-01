package net.osmand.data.preparation;

import java.sql.SQLException;

import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityType;

public interface OsmDbAccessorContext {
	
	public void loadEntityData(Entity e, boolean loadTags) throws SQLException;
}