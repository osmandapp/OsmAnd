package net.osmand.plus.plugins.osmedit.helpers;

import static net.osmand.osm.edit.Entity.POI_TYPE_TAG;

import androidx.annotation.NonNull;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.PlatformUtil;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.osm.edit.Way;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint;
import net.osmand.plus.plugins.osmedit.data.OsmPoint.Action;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OpenstreetmapLocalUtil implements OpenstreetmapUtil {

	private static final Log LOG = PlatformUtil.getLog(OpenstreetmapLocalUtil.class);

	private final OsmEditingPlugin plugin;
	private final List<OnNodeCommittedListener> listeners = new ArrayList<>();

	public OpenstreetmapLocalUtil(@NonNull OsmEditingPlugin plugin) {
		this.plugin = plugin;
	}

	public void addNodeCommittedListener(@NonNull OnNodeCommittedListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeNodeCommittedListener(@NonNull OnNodeCommittedListener listener) {
		listeners.remove(listener);
	}

	@Override
	public Entity commitEntityImpl(@NonNull Action action, Entity entity, EntityInfo info, String comment,
	                               boolean closeChangeSet, Set<String> changedTags) {
		Entity newEntity = entity;
		OpenstreetmapsDbHelper dbHelper = plugin.getDBPOI();
		if (entity.getId() == -1) {
			long minID = dbHelper.getMinID();
			if (entity instanceof Node) {
				newEntity = new Node((Node) entity, Math.min(-2, minID - 1));
			} else if (entity instanceof Way) {
				newEntity = new Way(Math.min(-2, minID - 1), ((Way) entity).getNodeIds(), entity.getLatitude(), entity.getLongitude());
			} else {
				return null;
			}
		}
		OpenstreetmapPoint point = new OpenstreetmapPoint();
		newEntity.setChangedTags(changedTags);
		point.setEntity(newEntity);
		point.setAction(action);
		point.setComment(comment);
		if (point.getAction() == OsmPoint.Action.DELETE && newEntity.getId() < 0) { //if it is our local poi
			dbHelper.deletePOI(point);
		} else {
			dbHelper.addOpenstreetmap(point);
		}
		for (OnNodeCommittedListener listener : listeners) {
			listener.onNoteCommitted();
		}
		return newEntity;
	}

	@Override
	public Entity loadEntity(@NonNull MapObject mapObject) {
		EntityType type = ObfConstants.getOsmEntityType(mapObject);
		if (type == null || type == EntityType.RELATION) {
			return null;
		}
		boolean isWay = type == EntityType.WAY;
		long entityId = ObfConstants.getOsmObjectId(mapObject);

		Amenity amenity = null;
		if (mapObject instanceof Amenity) {
			amenity = (Amenity) mapObject;
		}
		PoiType poiType = null;
		if (amenity != null) {
			poiType = amenity.getType().getPoiTypeByKeyName(amenity.getSubType());
		}
		if (poiType == null && mapObject instanceof Amenity) {
			return null;
		}

		Entity entity;
		LatLon loc = mapObject.getLocation();
		if (loc == null) {
			if (mapObject instanceof RenderedObject) {
				loc = ((RenderedObject) mapObject).getLabelLatLon();
			} else if (mapObject instanceof Building) {
				loc = ((Building) mapObject).getLatLon2();
			}
		}
		if (loc == null) {
			return null;
		}
		if (isWay) {
			entity = new Way(entityId, null, loc.getLatitude(), loc.getLongitude());
		} else {
			entity = new Node(loc.getLatitude(), loc.getLongitude(), entityId);
		}
		if (poiType != null) {
			entity.putTagNoLC(POI_TYPE_TAG, poiType.getTranslation());
			if (poiType.getOsmTag2() != null) {
				entity.putTagNoLC(poiType.getOsmTag2(), poiType.getOsmValue2());
			}
			if (poiType.getEditOsmTag2() != null) {
				entity.putTagNoLC(poiType.getEditOsmTag2(), poiType.getEditOsmValue2());
			}
		}
		String name = mapObject.getName();
		if (!Algorithms.isEmpty(name)) {
			if (amenity == null || !Algorithms.stringsEqual(amenity.getRef(), name)
							&& !amenity.getSubType().endsWith("_ref")) {
				entity.putTagNoLC(OSMTagKey.NAME.getValue(), name);
			}
		}
		if (amenity != null) {
			if (!Algorithms.isEmpty(amenity.getOpeningHours())) {
				entity.putTagNoLC(OSMTagKey.OPENING_HOURS.getValue(), amenity.getOpeningHours());
			}
			for (String key : amenity.getAdditionalInfoKeys()) {
				AbstractPoiType abstractPoi = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key);
				if (abstractPoi instanceof PoiType) {
					PoiType p = (PoiType) abstractPoi;
					if (!p.isNotEditableOsm() && !Algorithms.isEmpty(p.getEditOsmTag())) {
						entity.putTagNoLC(p.getEditOsmTag(), amenity.getAdditionalInfo(key));
					}
				}
			}
		}

		// check whether this is node (because id of node could be the same as relation)
		if (entity instanceof Node && MapUtils.getDistance(entity.getLatLon(), loc) < 50) {
			return entity;
		} else if (entity instanceof Way) {
			return entity;
		}
		return null;
	}

	public interface OnNodeCommittedListener {
		void onNoteCommitted();
	}
}
