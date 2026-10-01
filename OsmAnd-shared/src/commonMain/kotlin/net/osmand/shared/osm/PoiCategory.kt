package net.osmand.shared.osm

/**
 * A top level group of poi_types.xml: "shop", "transportation", "tourism". It is both a filter
 * over its own types and the owner of the filters nested inside it.
 *
 * A copy of `PoiCategory` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS.
 */
class PoiCategory(registry: MapPoiTypes, keyName: String, private val regId: Int) :
	PoiFilter(registry, null, keyName, null) {

	private val poiFilters: MutableList<PoiFilter> = ArrayList()
	private var basemapPoi: MutableSet<PoiType>? = null
	private var defaultTag: String? = null

	fun addPoiType(poi: PoiFilter) {
		poiFilters.add(poi)
	}

	fun getPoiFilters(): List<PoiFilter> = poiFilters

	fun getPoiFilterByName(keyName: String): PoiFilter? {
		for (f in poiFilters) {
			if (f.getKeyName() == keyName) {
				return f
			}
		}
		return null
	}

	fun getDefaultTag(): String = defaultTag ?: keyName

	fun setDefaultTag(defaultTag: String?) {
		this.defaultTag = defaultTag
	}

	override fun putTypes(
		acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>
	): MutableMap<PoiCategory, LinkedHashSet<String>?> {
		acceptedTypes[this] = null
		addReferenceTypes(acceptedTypes)
		return acceptedTypes
	}

	fun isAdministrative(): Boolean = keyName == MapPoiTypes.ADMINISTRATIVE_CATEGORY

	fun isWiki(): Boolean = keyName == MapPoiTypes.OSM_WIKI_CATEGORY

	fun isRoutes(): Boolean = keyName == MapPoiTypes.ROUTES

	/** Its position in poi_types.xml, which is the number the obf files store it by. */
	fun ordinal(): Int = regId

	fun addBasemapPoi(pt: PoiType) {
		var basemap = basemapPoi
		if (basemap == null) {
			basemap = HashSet()
			basemapPoi = basemap
		}
		basemap.add(pt)
	}

	fun containsBasemapPoi(pt: PoiType): Boolean = basemapPoi?.contains(pt) == true

	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null || other !is PoiCategory) {
			return false
		}
		return regId == other.regId && keyName == other.keyName && defaultTag == other.defaultTag
	}

	override fun hashCode(): Int {
		var result = 8
		result = 88 * result + keyName.hashCode()
		result = 88 * result + regId
		result = 88 * result + (defaultTag?.hashCode() ?: 0)
		return result
	}

	override fun getParentTypeName(): String = ""
}
