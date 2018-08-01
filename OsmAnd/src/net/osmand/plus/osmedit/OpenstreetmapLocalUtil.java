package net.osmand.plus.osmedit;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.osm.edit.Way;
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
	public Entity commitEntityImpl(OsmPoint.Action action, Entity entity, EntityInfo info, String comment,
	                               boolean closeChangeSet, Set<String> changedTags) {
		Entity newEntity = entity;
		if (entity.getId() == -1) {
			if (entity instanceof Node) {
				newEntity = new Node((Node) entity, Math.min(-2, plugin.getDBPOI().getMinID() - 1));
			} else if (entity instanceof Way) {
				newEntity = new Way(Math.min(-2, plugin.getDBPOI().getMinID() - 1), ((Way) entity).getNodeIds(), entity.getLatitude(), entity.getLongitude());
			} else {
				return null;
			}
		}
		OpenstreetmapPoint p = new OpenstreetmapPoint();
		newEntity.setChangedTags(changedTags);
		p.setEntity(newEntity);
		p.setAction(action);
		p.setComment(comment);
		if (p.getAction() == OsmPoint.Action.DELETE && newEntity.getId() < 0) { //if it is our local poi
			plugin.getDBPOI().deletePOI(p);
		} else {
			plugin.getDBPOI().addOpenstreetmap(p);
		}
		for (OnNodeCommittedListener listener : listeners) {
			listener.onNoteCommitted();
		}
		return newEntity;
	}
	
	@Override
	public Entity loadEntity(Amenity n) {
		PoiType poiType = n.getType().getPoiTypeByKeyName(n.getSubType());
		boolean isWay = n.getId() % 2 == 1; // check if amenity is a way
		if (poiType == null) {
			return null;
		}
		long entityId = n.getId() >> 1;

//		EntityId id = new Entity.EntityId(EntityType.NODE, entityId);
		Entity entity;
		if (isWay) {
			entity = new Way(entityId, null, n.getLocation().getLatitude(), n.getLocation().getLongitude());
		} else {
			entity = new Node(n.getLocation().getLatitude(),
					n.getLocation().getLongitude(),
					entityId);
		}
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
				if (!p.isNotEditableOsm() && !Algorithms.isEmpty(p.getEditOsmTag())) {
					entity.putTagNoLC(p.getEditOsmTag(), entry.getValue());
				}
			}
		}

		// check whether this is node (because id of node could be the same as relation)
		if (entity instanceof Node && MapUtils.getDistance(entity.getLatLon(), n.getLocation()) < 50) {
			return entity;
		} else if (entity instanceof Way) {
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
