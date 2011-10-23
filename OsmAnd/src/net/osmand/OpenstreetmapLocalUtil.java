package net.osmand;

import java.util.Map;

import net.osmand.OpenstreetmapPoint;
import net.osmand.data.Amenity;
import net.osmand.data.AmenityType;
import net.osmand.osm.Entity;
import net.osmand.osm.Entity.EntityId;
import net.osmand.osm.Entity.EntityType;
import net.osmand.osm.EntityInfo;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.MapUtils;
import net.osmand.osm.Node;
import net.osmand.osm.OSMSettings.OSMTagKey;
import net.osmand.plus.R;
import net.osmand.plus.OpenstreetmapsDbHelper;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.util.Xml;
import android.widget.Toast;

public class OpenstreetmapLocalUtil implements OpenstreetmapUtil {
	
	private final Context ctx;
	private final OpenstreetmapsDbHelper db;

	public final static Log log = LogUtil.getLog(OpenstreetmapLocalUtil.class);

	public OpenstreetmapLocalUtil(Context uiContext){
		this.ctx = uiContext;
		this.db = new OpenstreetmapsDbHelper(ctx);
	}

	public EntityInfo getEntityInfo() {
		return new EntityInfo();
	}
	
	public boolean commitNodeImpl(Action action, Node n, EntityInfo info, String comment){
		OpenstreetmapPoint p = new OpenstreetmapPoint();
		p.setEntity(n);
		p.setAction(action);
		p.setComment(comment);
		return db.addOpenstreetmap(p);
	}
	
	public Node loadNode(Amenity n) {
		if(n.getId() % 2 == 1){
			// that's way id
			return null;
		}
		long nodeId = n.getId() >> 1;

		EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
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
