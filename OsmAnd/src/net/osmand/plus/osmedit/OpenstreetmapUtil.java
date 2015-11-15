package net.osmand.plus.osmedit;

import net.osmand.data.Amenity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;

public interface OpenstreetmapUtil {
	
	public EntityInfo getEntityInfo(long id);
	
	public Node commitNodeImpl(OsmPoint.Action action, Node n, EntityInfo info, String comment, boolean closeChangeSet);
	
	public void closeChangeSet();
	
	public Node loadNode(Amenity n);
}
