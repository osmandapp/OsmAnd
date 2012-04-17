package net.osmand;

import net.osmand.data.Amenity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;
import android.app.Activity;

public interface OpenstreetmapUtil {
	
	public static enum Action {CREATE, MODIFY, DELETE};

	public EntityInfo getEntityInfo();
	
	public Node commitNodeImpl(Action action, Node n, EntityInfo info, String comment);
	
	public Node loadNode(Amenity n);
	
	public void updateNodeInIndexes(Activity ctx, OpenstreetmapUtil.Action action, Node n, Node oldNode);
}
