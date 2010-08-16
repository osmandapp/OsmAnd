package net.osmand.osm.io;

import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;

public interface IOsmStorageFilter {
	
	public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity);

}
