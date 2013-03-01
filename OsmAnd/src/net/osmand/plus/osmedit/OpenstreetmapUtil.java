package net.osmand.plus.osmedit;

import net.osmand.data.Amenity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import android.app.Activity;

public interface OpenstreetmapUtil {
	
	public EntityInfo getEntityInfo();
	
	public Node commitNodeImpl(OsmPoint.Action action, Node n, EntityInfo info, String comment, boolean closeChangeSet);
	
	public void closeChangeSet();
	
	public Node loadNode(Amenity n);
	
	public void updateNodeInIndexes(Activity ctx, OsmPoint.Action action, Node n, Node oldNode);
}
