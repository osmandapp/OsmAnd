package net.osmand.plus.osmedit;

import android.support.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;

import java.util.Set;

public interface OpenstreetmapUtil {
	
	EntityInfo getEntityInfo(long id);
	
	Node commitNodeImpl(OsmPoint.Action action, Node n, EntityInfo info, String comment, boolean closeChangeSet, @Nullable Set<String> changedTags);
	
	void closeChangeSet();
	
	Node loadNode(Amenity n);
}
