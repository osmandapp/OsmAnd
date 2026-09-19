package net.osmand.shared.osm

/**
 * A group of poi types inside a category, as poi_types.xml nests them: "shop" holds "bakery",
 * "butcher" and the rest.
 *
 * A copy of `PoiFilter` in OsmAnd-java, which stays there for android and tools; this copy is for
 * iOS.
 */
open class PoiFilter(
	registry: MapPoiTypes,
	private val pc: PoiCategory?,
	keyName: String,
	iconKeyName: String?
) : AbstractPoiType(keyName, registry, iconKeyName) {

	private var poiTypes: MutableList<PoiType> = ArrayList()
	private var map: MutableMap<String, PoiType> = LinkedHashMap()

	fun getPoiCategory(): PoiCategory? = pc

	fun getPoiTypeByKeyName(kn: String): PoiType? = map[kn]

	/**
	 * Adds the types of another filter that this one does not have yet, on a fresh copy of both
	 * lists, so that a filter being read while it is extended does not see a half filled one.
	 */
	fun addExtraPoiTypes(poiTypesToAdd: Map<String, PoiType>) {
		var npoiTypes: MutableList<PoiType>? = null
		var nmap: MutableMap<String, PoiType>? = null
		for (poiType in poiTypesToAdd.values) {
			val keyName = poiType.getKeyName()
			if (!map.containsKey(keyName) && !registry.isTypeForbidden(keyName)) {
				if (npoiTypes == null) {
					npoiTypes = ArrayList(this.poiTypes)
					nmap = LinkedHashMap(map)
				}
				npoiTypes.add(poiType)
				nmap!![keyName] = poiType
			}
		}
		if (npoiTypes != null) {
			poiTypes = npoiTypes
			map = nmap!!
		}
	}

	fun addPoiType(type: PoiType) {
		if (registry.isTypeForbidden(type.getKeyName())) {
			return
		}
		if (!map.containsKey(type.getKeyName())) {
			poiTypes.add(type)
			map[type.getKeyName()] = type
		} else {
			val prev = map[type.getKeyName()]
			if (prev != null && prev.isReference()) {
				poiTypes.remove(prev)
				poiTypes.add(type)
				map[type.getKeyName()] = type
			}
		}
	}

	override fun putTypes(
		acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>
	): MutableMap<PoiCategory, LinkedHashSet<String>?> {
		if (pc != null && !acceptedTypes.containsKey(pc)) {
			acceptedTypes[pc] = LinkedHashSet()
		}
		val set = acceptedTypes[pc]
		for (pt in poiTypes) {
			set?.add(pt.getKeyName())
		}
		addReferenceTypes(acceptedTypes)
		return acceptedTypes
	}

	override fun getParentTypeName(): String = pc?.getTranslation() ?: ""

	/** A type that only points at one in another category still opens that category up. */
	protected fun addReferenceTypes(acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>) {
		for (pt in getPoiTypes()) {
			if (pt.isReference()) {
				val refCat = pt.getReferenceType()?.getCategory() ?: continue
				if (!acceptedTypes.containsKey(refCat)) {
					acceptedTypes[refCat] = LinkedHashSet()
				}
				val ls = acceptedTypes[refCat]
				ls?.add(pt.getKeyName())
			}
		}
	}

	open fun getPoiTypes(): List<PoiType> = poiTypes
}
