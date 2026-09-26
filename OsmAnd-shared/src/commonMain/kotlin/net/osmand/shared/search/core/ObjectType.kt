package net.osmand.shared.search.core

import kotlin.jvm.JvmStatic

/**
 * What a search result is: an address, a poi, a location or an object of the app.
 *
 * A copy of `ObjectType` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS.
 */
enum class ObjectType(private val hasLocation: Boolean) {
	// ADDRESS
	CITY(true), VILLAGE(true), BOUNDARY(true), POSTCODE(true), STREET(true), HOUSE(true), STREET_INTERSECTION(true),
	// POI
	POI_TYPE(false), POI(true),
	// LOCATION
	LOCATION(true), PARTIAL_LOCATION(false),
	// UI OBJECTS
	FAVORITE(true), FAVORITE_GROUP(false), WPT(true), RECENT_OBJ(true),
	GPX_TRACK(false), ROUTE(false), MAP_MARKER(true), INDEX_ITEM(false),

	// ONLINE SEARCH
	ONLINE_SEARCH(true),

	REGION(true),

	SEARCH_STARTED(false),
	FILTER_FINISHED(false),
	SEARCH_FINISHED(false),
	SEARCH_API_FINISHED(false),
	SEARCH_API_REGION_FINISHED(false),
	UNKNOWN_NAME_FILTER(false);

	fun hasLocation(): Boolean = hasLocation

	companion object {
		@JvmStatic
		fun isAddress(t: ObjectType?): Boolean {
			return t == CITY || t == VILLAGE || t == BOUNDARY || t == POSTCODE || t == STREET || t == HOUSE || t == STREET_INTERSECTION
		}

		@JvmStatic
		fun isTopVisible(t: ObjectType?): Boolean {
			return t == POI_TYPE || t == FAVORITE || t == FAVORITE_GROUP || t == WPT || t == GPX_TRACK || t == LOCATION || t == PARTIAL_LOCATION || t == INDEX_ITEM
		}

		@JvmStatic
		fun getExclusiveSearchType(t: ObjectType?): ObjectType? {
			if (t == FAVORITE_GROUP) {
				return FAVORITE
			}
			return null
		}

		@JvmStatic
		fun getTypeWeight(t: ObjectType?): Int {
			if (t == null) {
				return 1
			}
			return when (t) {
				HOUSE -> 4
				STREET -> 3
				STREET_INTERSECTION, CITY, VILLAGE, POSTCODE -> 2
				BOUNDARY, POI -> 1
				else -> 1
			}
		}
	}
}
