package net.osmand.plus.osmedit;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpenstreetmapLocalUtil implements OpenstreetmapUtil {

	public final static Log LOG = PlatformUtil.getLog(OpenstreetmapLocalUtil.class);

	private OsmEditingPlugin plugin;

	public OpenstreetmapLocalUtil(OsmEditingPlugin plugin) {
		this.plugin = plugin;
	}

	private List<OnNodeCommittedListener> listeners = new ArrayList<>();

	public void addNodeCommittedListener(OnNodeCommittedListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeNodeCommittedListener(OnNodeCommittedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public EntityInfo getEntityInfo(long id) {
		return null;
	}
	
	@Override
	public Node commitNodeImpl(OsmPoint.Action action, Node n, EntityInfo info, String comment,
							   boolean closeChangeSet, Set<String> changedTags){
		Node newNode = n;
		if (n.getId() == -1) {
			newNode = new Node(n, Math.min(-2, plugin.getDBPOI().getMinID() - 1)); // generate local id for the created node
		}
		OpenstreetmapPoint p = new OpenstreetmapPoint();
		newNode.setChangedTags(changedTags);
		p.setEntity(newNode);
		p.setAction(action);
		p.setComment(comment);
		if (p.getAction() == OsmPoint.Action.DELETE && newNode.getId() < 0) { //if it is our local poi
			plugin.getDBPOI().deletePOI(p);
		} else {
			plugin.getDBPOI().addOpenstreetmap(p);
		}
		for (OnNodeCommittedListener listener : listeners) {
			listener.onNoteCommitted();
		}
		return newNode;
	}
	
	@Override
	public Node loadNode(Amenity n) {
		PoiType poiType = n.getType().getPoiTypeByKeyName(n.getSubType());
		if(n.getId() % 2 == 1 || poiType == null){
			// that's way id
			return null;
		}
		long nodeId = n.getId() >> 1;

//		EntityId id = new Entity.EntityId(EntityType.NODE, nodeId);
		Node entity = new Node(n.getLocation().getLatitude(),
							   n.getLocation().getLongitude(),
							   nodeId);
		entity.putTagNoLC(EditPoiData.POI_TYPE_TAG, poiType.getTranslation());
		if(poiType.getOsmTag2() != null) {
			entity.putTagNoLC(poiType.getOsmTag2(), poiType.getOsmValue2());
		}
		if(!Algorithms.isEmpty(n.getName())) {
			entity.putTagNoLC(OSMTagKey.NAME.getValue(), n.getName());
		}
		if(!Algorithms.isEmpty(n.getOpeningHours())) {
			entity.putTagNoLC(OSMTagKey.OPENING_HOURS.getValue(), n.getOpeningHours());
		}

		for (Map.Entry<String, String> entry : n.getAdditionalInfo().entrySet()) {
			AbstractPoiType abstractPoi = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(entry.getKey());
			if (abstractPoi != null && abstractPoi instanceof PoiType) {
				PoiType p = (PoiType) abstractPoi;
				if (!p.isNotEditableOsm() && !Algorithms.isEmpty(p.getOsmTag())) {
					entity.putTagNoLC(p.getOsmTag(), entry.getValue());
				}
			}
		}

		// check whether this is node (because id of node could be the same as relation)
		if(entity != null && MapUtils.getDistance(entity.getLatLon(), n.getLocation()) < 50){
			return entity;
		}
		return null;
	}

	@Override
	public void closeChangeSet() {
	}

	public interface OnNodeCommittedListener {
		void onNoteCommitted();
	}
	
}
