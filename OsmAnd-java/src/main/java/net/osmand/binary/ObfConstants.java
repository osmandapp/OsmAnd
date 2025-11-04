package net.osmand.binary;

import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;

import java.util.Locale;
import java.util.Map;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.Relation;
import net.osmand.util.Algorithms;

public class ObfConstants {

	public static final int SHIFT_ID = 6;
	
	public static final int SHIFT_MULTIPOLYGON_IDS = 43;
	public static final int SHIFT_NON_SPLIT_EXISTING_IDS = 41;

	public static final int SHIFT_PROPAGATED_NODE_IDS = 50;
	public static final int SHIFT_PROPAGATED_NODES_BITS = 11;
	public static final long MAX_ID_PROPAGATED_NODES = (1L << SHIFT_PROPAGATED_NODES_BITS) - 1;//2047

	public static final long RELATION_BIT = 1L << (ObfConstants.SHIFT_MULTIPOLYGON_IDS - 1); // 1L << 42
	public static final long PROPAGATE_NODE_BIT = 1L << (ObfConstants.SHIFT_PROPAGATED_NODE_IDS  - 1); // 1L << 49
	public static final long SPLIT_BIT = 1L << (ObfConstants.SHIFT_NON_SPLIT_EXISTING_IDS - 1); // 1L << 40

	public static final int DUPLICATE_SPLIT = 5;

	public static String getOsmUrlForId(MapObject mapObject) {
		EntityType type = getOsmEntityType(mapObject);
		if (type != null) {
			long osmId = getOsmObjectId(mapObject);
			return "https://www.openstreetmap.org/" + type.name().toLowerCase(Locale.US) + "/" + osmId;
		}
		return "";
	}

	public static long getOsmIdFromPrefixedRouteId(String routeId) {
		long osmId = 0;
		if (routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX)) {
			osmId = Algorithms.parseLongSilently(routeId.replace(Amenity.ROUTE_ID_OSM_PREFIX, ""), 0); // ^O
		} else if (routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX_LEGACY)) {
			osmId = Algorithms.parseLongSilently(routeId.replace(Amenity.ROUTE_ID_OSM_PREFIX_LEGACY, ""), 0); // ^OSM
		}
		return osmId;
	}
	
	
	public static long createMapObjectIdFromOsmAndEntity(Entity e) {
		// Way / Node with shifts , Relation without
		boolean isPropagated = e.getId() > PROPAGATE_NODE_BIT;
		if (isPropagated) {
			return e.getId();
		}
		long cleanId = e instanceof Relation ? e.getId() : (e.getId() >> SHIFT_ID);
		return createMapObjectIdFromCleanOsmId(cleanId, EntityType.valueOf(e));
	}

	public static long createMapObjectIdFromCleanOsmId(long osmId, EntityType type) {
		if (type == null) {
			return osmId;
		}
		return switch (type) {
			case NODE -> osmId << AMENITY_ID_RIGHT_SHIFT;
			case WAY -> (osmId << AMENITY_ID_RIGHT_SHIFT) + 1;
			case RELATION -> RELATION_BIT + ((osmId << SHIFT_ID) << DUPLICATE_SPLIT);
			default -> osmId;
		};
	}

	public static long getOsmObjectId(MapObject object) {
		long originalId = -1;
		Long id = object.getId();
		if (id != null) {
			if (object instanceof RenderedObject) {
				id = shiftRenderedObjectId(id);
			}
			originalId = getOsmIdFromMapObjectId(id);
		}
		return originalId;
	}

	private static long shiftRenderedObjectId(long id) {
		return isIdFromPropagatedNode(id >> 1) ? id >> 1 : id >> SHIFT_ID;
	}

	// Use getOsmObjectId(MapObject) for RenderedObject
	public static long getOsmIdFromMapObjectId(long id) {
		long originalId;
		if (isIdFromPropagatedNode(id)) {
			long shifted = id & ~PROPAGATE_NODE_BIT;
			originalId = shifted >> ObfConstants.SHIFT_PROPAGATED_NODES_BITS;
		} else {
			if (isShiftedID(id)) {
				originalId = getOsmId(id);
			} else {
				originalId = id >> AMENITY_ID_RIGHT_SHIFT;
			}
		}
		return originalId; 
	}
	
	public static long getOsmIdFromBinaryMapObjectId(long id) {
		// BinaryMapDataObject object
		return getOsmId(id >> 1); 
	}

	public static long getOsmObjectId(RouteDataObject object) {
		if (object == null) {
			return 0;
		}
		return getOsmId(object.getId());
	}
	
	public static long getOsmObjectId(BinaryMapDataObject object) {
		if (object == null) {
			return 0;
		}
		return getOsmId(object.getId() >> 1);
	}

	public static EntityType getOsmEntityType(MapObject object) {
		if (isOsmUrlAvailable(object)) {
			Long id = object.getId();
			long originalId = id >> 1;
			if (object instanceof RenderedObject && isIdFromPropagatedNode(originalId)) {
				return EntityType.WAY;
			}
			if (isIdFromPropagatedNode(id)) {
				return EntityType.WAY;
			}
			long relationShift = 1L << 41;
			if (originalId > relationShift) {
				return EntityType.RELATION;
			} else {
				return id % 2 == MapObject.WAY_MODULO_REMAINDER ? EntityType.WAY : EntityType.NODE;
			}
		}
		return null;
	}

	public static String getPrintTags(RenderedObject renderedObject) {
		StringBuilder s = new StringBuilder();
		for (Map.Entry<String, String> entry : renderedObject.getTags().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			boolean keyEmpty = Algorithms.isEmpty(key);
			boolean valueEmpty = Algorithms.isEmpty(value);
			boolean bothPresent = !keyEmpty && !valueEmpty;
			boolean anyPresent = !keyEmpty || !valueEmpty;
			if (!keyEmpty) {
				s.append(key);
			}
			if (bothPresent) {
				s.append(":");
			}
			if (!valueEmpty) {
				s.append(value);
			}
			if (anyPresent) {
				s.append(" ");
			}
		}
		return s.toString().trim();
	}

	public static boolean isOsmUrlAvailable(MapObject object) {
		Long id = object.getId();
		return id != null && id > 0;
	}

	public static EntityType getOsmEntityType(BinaryMapDataObject object) {
		if(isIdFromRelation(object.getId())) {
			return EntityType.RELATION;
		}
		if(object.getId() % 2 == 0) {
			return EntityType.NODE;
		}
		return EntityType.WAY;
	}
	
	private static long getOsmId(long id) {
		//According methods assignIdForMultipolygon and genId in IndexPoiCreator
		long clearBits = RELATION_BIT | SPLIT_BIT;
		id = isShiftedID(id) ? (id & ~clearBits) >> DUPLICATE_SPLIT : id;
		return id >> SHIFT_ID;
	}

	private static boolean isShiftedID(long id) {
		return isIdFromRelation(id) || isIdFromSplit(id);
	}

	private static boolean isIdFromRelation(long id) {
		return id > 0 && (id & RELATION_BIT) == RELATION_BIT;
	}

	private static boolean isIdFromPropagatedNode(long id) {
		return id > 0 && (id & PROPAGATE_NODE_BIT) == PROPAGATE_NODE_BIT;
	}

	private static boolean isIdFromSplit(long id) {
		return id > 0 && (id & SPLIT_BIT) == SPLIT_BIT;
	}

	public static boolean isTagIndexedForSearchAsName(String tag) {
		if (tag != null) {
			return tag.contains("name") || tag.contains("brand");
		}
		return false;
	}
	
	public static boolean isTagIndexedForSearchAsId(String tag) {
		if (tag != null) {
			return tag.equals(Amenity.WIKIDATA) || tag.equals(Amenity.ROUTE_ID);
		}
		return false;
	}

}
