package net.osmand.util;

import static net.osmand.data.MapObject.AMENITY_ID_RIGHT_SHIFT;
import static net.osmand.router.RouteResultPreparation.SHIFT_ID;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.osm.edit.Entity.EntityType;

import java.util.Locale;
import java.util.Map;

public class OsmUtils {

	public static final int SHIFT_MULTIPOLYGON_IDS = 43;
	public static final int SHIFT_NON_SPLIT_EXISTING_IDS = 41;
	public static final long RELATION_BIT = 1L << SHIFT_MULTIPOLYGON_IDS - 1; //According IndexPoiCreator SHIFT_MULTIPOLYGON_IDS
	public static final long SPLIT_BIT = 1L << SHIFT_NON_SPLIT_EXISTING_IDS - 1; //According IndexVectorMapCreator
	public static final int DUPLICATE_SPLIT = 5; //According IndexPoiCreator DUPLICATE_SPLIT

	public static String getOsmUrlForId(MapObject mapObject) {
		EntityType type = getOsmEntityType(mapObject);
		if (type != null) {
			long osmId = getOsmObjectId(mapObject);
			return "https://www.openstreetmap.org/" + type.name().toLowerCase(Locale.US) + "/" + osmId;
		}
		return "";
	}

	public static long getOsmObjectId(MapObject object) {
		long originalId = -1;
		Long id = object.getId();
		if (id != null) {
			if (object instanceof RenderedObject) {
				id >>= 1;
			}
			if (isShiftedID(id)) {
				originalId = getOsmId(id);
			} else {
				int shift = object instanceof Amenity ? AMENITY_ID_RIGHT_SHIFT : SHIFT_ID;
				originalId = id >> shift;
			}
		}
		return originalId;
	}

	public static EntityType getOsmEntityType(MapObject object) {
		if (isOsmUrlAvailable(object)) {
			Long id = object.getId();
			long originalId = id >> 1;
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

	public static long getOsmId(long id) {
		//According methods assignIdForMultipolygon and genId in IndexPoiCreator
		long clearBits = RELATION_BIT | SPLIT_BIT;
		id = isShiftedID(id) ? (id & ~clearBits) >> DUPLICATE_SPLIT : id;
		return id >> SHIFT_ID;
	}

	public static boolean isShiftedID(long id) {
		return isIdFromRelation(id) || isIdFromSplit(id);
	}

	public static boolean isIdFromRelation(long id) {
		return id > 0 && (id & RELATION_BIT) == RELATION_BIT;
	}

	public static boolean isIdFromSplit(long id) {
		return id > 0 && (id & SPLIT_BIT) == SPLIT_BIT;
	}
}
