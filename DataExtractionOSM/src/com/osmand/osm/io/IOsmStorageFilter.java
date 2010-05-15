package com.osmand.osm.io;

import com.osmand.osm.Entity;

public interface IOsmStorageFilter {
	
	public boolean acceptEntityToLoad(OsmBaseStorage storage, Entity entity);

}
