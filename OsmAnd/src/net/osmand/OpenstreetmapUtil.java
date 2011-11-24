package net.osmand;

import net.osmand.data.Amenity;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.Node;

public interface OpenstreetmapUtil {
	
	public static enum Action {CREATE, MODIFY, DELETE};

	public EntityInfo getEntityInfo();
	
	public boolean commitNodeImpl(Action action, Node n, EntityInfo info, String comment);
	
	public Node loadNode(Amenity n);
}
