package net.osmand.data.preparation;

import java.sql.SQLException;

import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityType;

public interface OsmDbAccessorContext {
	
	public void loadEntityTags(EntityType type, Entity e) throws SQLException;

	public void loadEntityData(Entity e, boolean loadTags) throws SQLException;
}