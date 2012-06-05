package net.osmand.plus.osmedit;

import net.osmand.data.Amenity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import android.app.Activity;

public interface OpenstreetmapUtil {
	
	public EntityInfo getEntityInfo();
	
	public Node commitNodeImpl(OsmPoint.Action action, Node n, EntityInfo info, String comment);
	
	public Node loadNode(Amenity n);
	
	public void updateNodeInIndexes(Activity ctx, OsmPoint.Action action, Node n, Node oldNode);
}
