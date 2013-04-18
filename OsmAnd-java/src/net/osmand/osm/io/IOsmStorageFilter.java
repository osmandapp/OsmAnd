package net.osmand.osm.io;

import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityId;

public interface IOsmStorageFilter {
	
	public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity);

}
