package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.data.MapObject
import net.osmand.shared.routing.RouteDataObject
import net.osmand.shared.util.KAlgorithms

/**
 * How an osm id is packed into an obf id, and which tags the indexer treats as names or as ids.
 *
 * A copy of the part of `ObfConstants` in OsmAnd-java that a device needs; the rest of it - the osm
 * url helpers, `EntityType` and everything taking a `RenderedObject` or an `Entity` - belongs to
 * the editing and indexing code and stays there.
 *
 * The packing is what the map creator does on the way in: a node id is shifted left by one, a way
 * id by one with the low bit set, and a relation id is shifted further and marked with
 * [RELATION_BIT]. Reading an object back gives the packed id, so anything that wants the osm id -
 * an url, a route id, a comparison against osm data - has to undo it here.
 */
object ObfConstants {

	const val SHIFT_ID: Int = 6

	const val SHIFT_MULTIPOLYGON_IDS: Int = 43
	const val SHIFT_NON_SPLIT_EXISTING_IDS: Int = 41

	const val SHIFT_PROPAGATED_NODE_IDS: Int = 50
	const val SHIFT_PROPAGATED_NODES_BITS: Int = 11
	const val MAX_ID_PROPAGATED_NODES: Long = (1L shl SHIFT_PROPAGATED_NODES_BITS) - 1 // 2047

	const val RELATION_BIT: Long = 1L shl (SHIFT_MULTIPOLYGON_IDS - 1) // 1L << 42
	const val PROPAGATE_NODE_BIT: Long = 1L shl (SHIFT_PROPAGATED_NODE_IDS - 1) // 1L << 49
	const val SPLIT_BIT: Long = 1L shl (SHIFT_NON_SPLIT_EXISTING_IDS - 1) // 1L << 40

	const val DUPLICATE_SPLIT: Int = 5

	/** The osm id behind a route id of the form `O123456`, or its legacy form `OSM123456`. */
	fun getOsmIdFromPrefixedRouteId(routeId: String): Long {
		var osmId = 0L
		if (routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX)) {
			osmId = KAlgorithms.parseLongSilently(routeId.replace(Amenity.ROUTE_ID_OSM_PREFIX, ""), 0) // ^O
		} else if (routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX_LEGACY)) {
			osmId = KAlgorithms.parseLongSilently(routeId.replace(Amenity.ROUTE_ID_OSM_PREFIX_LEGACY, ""), 0) // ^OSM
		}
		return osmId
	}

	fun getOsmObjectId(obj: MapObject): Long {
		var originalId = -1L
		val id = obj.getId()
		if (id != null) {
			originalId = getOsmIdFromMapObjectId(id)
		}
		return originalId
	}

	// Doesn't work correctly for some TransportStop (stop.getId() = 16055353830 results in osmId 8027676915 which does not exist)
	// https://www.openstreetmap.org/node/988560310
	fun getOsmIdFromMapObjectId(id: Long): Long {
		val originalId: Long
		if (isIdFromPropagatedNode(id)) {
			val shifted = id and PROPAGATE_NODE_BIT.inv()
			originalId = shifted shr SHIFT_PROPAGATED_NODES_BITS
		} else {
			originalId = if (isShiftedID(id)) {
				getOsmId(id)
			} else {
				id shr MapObject.AMENITY_ID_RIGHT_SHIFT
			}
		}
		return originalId
	}

	fun getOsmIdFromBinaryMapObjectId(id: Long): Long {
		// BinaryMapDataObject object
		return getOsmId(id shr 1)
	}

	fun getOsmObjectId(obj: RouteDataObject?): Long {
		if (obj == null) {
			return 0
		}
		return getOsmId(obj.getId())
	}

	fun getOsmObjectId(obj: BinaryMapDataObject?): Long {
		if (obj == null) {
			return 0
		}
		return getOsmId(obj.getId() shr 1)
	}

	fun isOsmUrlAvailable(obj: MapObject): Boolean {
		val id = obj.getId()
		return id != null && id > 0
	}

	private fun getOsmId(id: Long): Long {
		// According methods assignIdForMultipolygon and genId in IndexPoiCreator
		val clearBits = RELATION_BIT or SPLIT_BIT
		val cleaned = if (isShiftedID(id)) (id and clearBits.inv()) shr DUPLICATE_SPLIT else id
		return cleaned shr SHIFT_ID
	}

	private fun isShiftedID(id: Long): Boolean = isIdFromRelation(id) || isIdFromSplit(id)

	private fun isIdFromRelation(id: Long): Boolean = id > 0 && (id and RELATION_BIT) == RELATION_BIT

	private fun isIdFromPropagatedNode(id: Long): Boolean =
		id > 0 && (id and PROPAGATE_NODE_BIT) == PROPAGATE_NODE_BIT

	private fun isIdFromSplit(id: Long): Boolean = id > 0 && (id and SPLIT_BIT) == SPLIT_BIT

	fun isTagNonIndexedForSearchAsName(tag: String): Boolean = tag == "ref"

	fun isTagIndexedForSearchAsName(tag: String?): Boolean {
		if (tag != null) {
			// search related but not direct
			if (tag.startsWith(Amenity.ROUTE_NAME) || tag == Amenity.SHIELD_STUB_NAME) {
				return false
			}
			// some popular tags ignored as name
			if (tag.startsWith("tiger:") || tag.startsWith("noname") ||
				tag.startsWith("name:" + MapObject.NAME_ETYMOLOGY_ATTR) ||
				tag.startsWith("artist_name") ||
				tag.startsWith("addr:") // housename, street name
			) {
				return false
			}
			return tag.contains("name") || tag.contains("brand")
		}
		return false
	}

	fun isTagIndexedForSearchAsId(tag: String?): Boolean {
		if (tag != null) {
			return tag == Amenity.WIKIDATA || tag == Amenity.ROUTE_ID
		}
		return false
	}

	fun isTagIndexedAsSearchRelated(tag: String?): Boolean {
		if (tag != null) {
			return tag == Amenity.ROUTE_MEMBERS_IDS || tag == Amenity.ROUTE_NAME
		}
		return false
	}
}
