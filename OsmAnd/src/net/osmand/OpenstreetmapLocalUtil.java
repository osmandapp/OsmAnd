package net.osmand;

import java.util.Map;

import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.plus.OpenstreetmapsDbHelper;

import org.apache.commons.logging.Log;

import android.content.Context;

public class OpenstreetmapLocalUtil extends AbstractOpenstreetmapUtil {
	
	private final Context ctx;
	private final OpenstreetmapsDbHelper db;
	//temporal IDs for not yet uploaded new POIs
	private long nextid;

	public final static Log log = LogUtil.getLog(OpenstreetmapLocalUtil.class);

	public OpenstreetmapLocalUtil(Context uiContext){
		this.ctx = uiContext;
		this.db = new OpenstreetmapsDbHelper(ctx);
		this.nextid = Math.min(-2,db.getMinID());
	}

	@Override
	public EntityInfo getEntityInfo() {
		return new EntityInfo();
	}
	
	@Override
	public Node commitNodeImpl(Action action, Node n, EntityInfo info, String comment){
		Node newNode = n;
		if (n.getId() == -1) {
			newNode = new Node(n,--nextid); //generate local id for the created node
		}
		OpenstreetmapPoint p = new OpenstreetmapPoint();
		p.setEntity(newNode);
		p.setAction(action);
		p.setComment(comment);
		if (p.getAction() == Action.DELETE && newNode.getId() < 0) { //if it is our local poi
			db.deleteAllPOIModifications(p.getId());
		} else {
			db.addOpenstreetmap(p);
		}
		return newNode;
	}
	
	@Override
	public Node loadNode(Amenity n) {
		if(n.getId() % 2 == 1){
			// that's way id
			return null;
		}
		long nodeId = n.getId() >> 1;

//		EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
		Node entity = new Node(n.getLocation().getLatitude(),
							   n.getLocation().getLongitude(),
							   nodeId);

		Map<AmenityType, Map<String, String>> typeNameToTagVal = MapRenderingTypes.getDefault().getAmenityTypeNameToTagVal();
		AmenityType type = n.getType();
		String tag = type.getDefaultTag();
		String subType = n.getSubType();
		String val = subType;
		if (typeNameToTagVal.containsKey(type)) {
			Map<String, String> map = typeNameToTagVal.get(type);
			if (map.containsKey(subType)) {
				String res = map.get(subType);
				if (res != null) {
					int i = res.indexOf(' ');
					if (i != -1) {
						tag = res.substring(0, i);
						val = res.substring(i + 1);
					} else {
						tag = res;
					}
				}
			}
		}
		entity.putTag(tag, val);
		entity.putTag(OSMTagKey.NAME.getValue(), n.getName());
		entity.putTag(OSMTagKey.OPENING_HOURS.getValue(), n.getOpeningHours());
 
		// check whether this is node (because id of node could be the same as relation) 
		if(entity != null && MapUtils.getDistance(entity.getLatLon(), n.getLocation()) < 50){
			return entity;
		}
		return null;
	}
	
}
