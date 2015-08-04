package net.osmand.plus.osmedit;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import android.content.Context;

public class OpenstreetmapLocalUtil implements OpenstreetmapUtil {
	
	private final Context ctx;

	public final static Log log = PlatformUtil.getLog(OpenstreetmapLocalUtil.class);

	private OsmEditingPlugin plugin;

	public OpenstreetmapLocalUtil(OsmEditingPlugin plugin, Context uiContext) {
		this.plugin = plugin;
		this.ctx = uiContext;
	}

	@Override
	public EntityInfo getEntityInfo() {
		return new EntityInfo();
	}
	
	@Override
	public Node commitNodeImpl(OsmPoint.Action action, Node n, EntityInfo info, String comment, boolean closeChangeSet){
		Node newNode = n;
		if (n.getId() == -1) {
			newNode = new Node(n, Math.min(-2, plugin.getDBPOI().getMinID() - 1)); // generate local id for the created node
		}
		OpenstreetmapPoint p = new OpenstreetmapPoint();
		p.setEntity(newNode);
		p.setAction(action);
		p.setComment(comment);
		if (p.getAction() == OsmPoint.Action.DELETE && newNode.getId() < 0) { //if it is our local poi
			plugin.getDBPOI().deletePOI(p);
		} else {
			plugin.getDBPOI().addOpenstreetmap(p);
		}
		return newNode;
	}
	
	@Override
	public Node loadNode(Amenity n) {
		PoiType st = n.getType().getPoiTypeByKeyName(n.getSubType());
		if(n.getId() % 2 == 1 || st == null){
			// that's way id
			return null;
		}
		long nodeId = n.getId() >> 1;

//		EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
		Node entity = new Node(n.getLocation().getLatitude(),
							   n.getLocation().getLongitude(),
							   nodeId);
		entity.putTag(st.getOsmTag(), st.getOsmValue());
		if(st.getOsmTag2() != null) {
			entity.putTag(st.getOsmTag2(), st.getOsmValue2());
		}
		entity.putTag(OSMTagKey.NAME.getValue(), n.getName());
		entity.putTag(OSMTagKey.OPENING_HOURS.getValue(), n.getOpeningHours());
 
		// check whether this is node (because id of node could be the same as relation) 
		if(entity != null && MapUtils.getDistance(entity.getLatLon(), n.getLocation()) < 50){
			return entity;
		}
		return null;
	}

	@Override
	public void closeChangeSet() {
	}
	
}
