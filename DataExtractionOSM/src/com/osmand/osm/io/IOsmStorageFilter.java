package com.osmand.osm.io;

import com.osmand.osm.Entity;
import com.osmand.osm.Entity.EntityId;

public interface IOsmStorageFilter {
	
	public boolean acceptEntityToLoad(OsmBaseStorage storage, EntityId entityId, Entity entity);

}
