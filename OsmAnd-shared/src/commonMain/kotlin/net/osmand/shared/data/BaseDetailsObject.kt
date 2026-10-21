package net.osmand.shared.data

import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.TagValuePair
import net.osmand.shared.data.Amenity.Companion.DEFAULT_ELO
import net.osmand.shared.data.Amenity.Companion.WIKIDATA
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiType
import net.osmand.shared.search.core.SearchResult.SearchResultResource
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.jvm.JvmField

/**
 * Objects that stand for one place - an amenity from each of several files, the object the
 * renderer drew, an address - and the amenity made of them all.
 *
 * A copy of `BaseDetailsObject` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. Java also takes public transport stops: it keeps their amenity, matches a drawn stop
 * with a stop nearby, and lists them. Stops are not copied, so neither are those branches.
 */
open class BaseDetailsObject(private val lang: String?) {

	private val osmIds: MutableSet<Long> = HashSet()
	private val wikidataIds: MutableSet<String> = HashSet()
	private val objects: MutableList<Any> = ArrayList()

	private var obfResourceName: String? = null
	private var searchResultResource: SearchResultResource? = null

	@JvmField
	protected var syntheticAmenity = Amenity()
	private var bbox31: IntArray? = null

	private var objectCompleteness = ObjectCompleteness.EMPTY

	private enum class ObjectCompleteness {
		EMPTY,
		COMBINED,
		FULL
	}

	constructor(obj: Any?, lang: String?) : this(if (KAlgorithms.isEmpty(lang)) "en" else lang) {
		addObject(obj)
	}

	constructor(mapObjects: List<MapObject>, lang: String?) : this(if (KAlgorithms.isEmpty(lang)) "en" else lang) {
		var containsAmenity = false
		for (mo in mapObjects) {
			addObjectNoCombine(mo)
			if (mo is Amenity) {
				containsAmenity = true
			}
		}
		combineData()
		if (objects.isNotEmpty()) {
			objectCompleteness = if (containsAmenity) ObjectCompleteness.FULL else ObjectCompleteness.COMBINED
		}
	}

	fun getSyntheticAmenity(): Amenity = syntheticAmenity

	fun getLocation(): KLatLon? = syntheticAmenity.getLocation()

	fun getObjects(): List<Any> = objects

	fun isObjectFull(): Boolean = objectCompleteness == ObjectCompleteness.FULL

	fun isObjectCombined(): Boolean = objectCompleteness == ObjectCompleteness.COMBINED

	fun isObjectEmpty(): Boolean = objectCompleteness == ObjectCompleteness.EMPTY

	fun addObject(obj: Any?): Boolean {
		val added = addObjectNoCombine(obj)
		if (added) {
			combineData()
		}
		return added
	}

	private fun addObjectNoCombine(obj: Any?): Boolean {
		if (bbox31 == null && obj is City) {
			bbox31 = obj.getBbox31()
		} else if (bbox31 == null && obj is Street) {
			val bb = obj.getBboxPoints()
			if (bb != null) {
				bbox31 = intArrayOf(
					KMapUtils.get31TileNumberX(bb.left), KMapUtils.get31TileNumberY(bb.top),
					KMapUtils.get31TileNumberX(bb.right), KMapUtils.get31TileNumberY(bb.bottom)
				)
			}
		}
		if (!isSupportedObjectType(obj)) {
			return false
		}
		if (obj is BaseDetailsObject) {
			for (o in obj.getObjects()) {
				addObject(o)
			}
		} else {
			objects.add(obj!!)

			val osmId = getOsmId(obj)
			val wikidata = getWikidata(obj)

			if (osmId != null && osmId != -1L) {
				osmIds.add(osmId)
			}
			if (!KAlgorithms.isEmpty(wikidata)) {
				wikidataIds.add(wikidata!!)
			}
		}
		return true
	}

	protected open fun getWikidata(obj: Any?): String? {
		if (obj is Amenity) {
			return obj.getWikidata()
		} else if (obj is RenderedObject) {
			return obj.getTagValue(WIKIDATA)
		} else if (obj is MapObject) {
			return obj.getWikidata()
		}
		return null
	}

	private fun getOsmId(obj: Any?): Long? {
		if (obj is Amenity) {
			return obj.getOsmId()
		}
		if (obj is MapObject) {
			return ObfConstants.getOsmObjectId(obj)
		}
		return null
	}

	open fun overlapsWith(obj: Any?): Boolean {
		if (obj is BaseDetailsObject) {
			// a group collected by the renderer keeps its ids in the sets, not in a single object
			return osmIds.any { it in obj.osmIds } || wikidataIds.any { it in obj.wikidataIds }
		}
		val osmId = getOsmId(obj)
		val wikidata = getWikidata(obj)

		val osmIdEqual = osmId != null && osmId != -1L && osmIds.contains(osmId)
		val wikidataEqual = !KAlgorithms.isEmpty(wikidata) && wikidataIds.contains(wikidata!!)

		return osmIdEqual || wikidataEqual
	}

	fun merge(obj: Any?) {
		if (obj is BaseDetailsObject) {
			merge(obj)
		}
		if (obj is RenderedObject) {
			merge(obj)
		}
	}

	private fun merge(other: BaseDetailsObject) {
		osmIds.addAll(other.osmIds)
		wikidataIds.addAll(other.wikidataIds)
		objects.addAll(other.getObjects())
	}

	private fun merge(renderedObject: RenderedObject) {
		osmIds.add(ObfConstants.getOsmObjectId(renderedObject))
		val wikidata = renderedObject.getTagValue(WIKIDATA)
		if (!KAlgorithms.isEmpty(wikidata)) {
			wikidataIds.add(wikidata!!)
		}
		objects.add(renderedObject)
	}

	private fun combineData() {
		syntheticAmenity = Amenity()
		syntheticAmenity.setBbox31(bbox31)
		sortObjects()
		for (obj in objects) {
			mergeObject(obj, objects.size == 1)
		}
		if (this.objectCompleteness.ordinal < ObjectCompleteness.FULL.ordinal) {
			this.objectCompleteness =
				if (syntheticAmenity.getType() == null) ObjectCompleteness.EMPTY else ObjectCompleteness.COMBINED
		}
		if (syntheticAmenity.getType() == null) {
			syntheticAmenity.setType(MapPoiTypes.getDefault().getUserDefinedCategory())
			syntheticAmenity.setSubType("")
			this.objectCompleteness = ObjectCompleteness.EMPTY
		}
	}

	protected open fun mergeObject(obj: Any, isSingleObject: Boolean) {
		if (obj is Amenity) {
			processAmenity(obj, isSingleObject)
		} else if (obj is RenderedObject) {
			val type = ObfConstants.getOsmEntityType(obj)
			if (type != null) {
				val osmId = ObfConstants.getOsmObjectId(obj)
				val objectId = ObfConstants.createMapObjectIdFromCleanOsmId(osmId, type)

				if (syntheticAmenity.getId() == null && objectId > 0) {
					syntheticAmenity.setId(objectId)
				}
			}
			if (syntheticAmenity.getType() == null) {
				val amenity = convertRenderedObjectToAmenity(obj, MapPoiTypes.getDefault())
				syntheticAmenity.setType(amenity.getType())
				syntheticAmenity.setSubType(amenity.getSubType())
				syntheticAmenity.copyAdditionalInfo(obj.getTags(), false)
			}
			syntheticAmenity.copyNames(obj)
			if (syntheticAmenity.getLocation() == null) {
				syntheticAmenity.setLocation(obj.getLocation())
			}
			if (syntheticAmenity.getLocation() == null) {
				syntheticAmenity.setLocation(obj.getLabelLatLon())
			}
			processPolygonCoordinates(obj.getX(), obj.getY())
		}
	}

	protected open fun processId(obj: MapObject) {
		processId(syntheticAmenity, obj)
	}

	protected open fun processAmenity(amenity: Amenity, isSingleObject: Boolean) {
		mergeAmenityData(syntheticAmenity, amenity, lang, isSingleObject)
	}

	private fun processPolygonCoordinates(x: KTIntArrayList, y: KTIntArrayList) {
		processPolygonCoordinates(syntheticAmenity, x, y)
	}

	fun processPolygonCoordinates(obj: Any?) {
		if (obj is Amenity) {
			processPolygonCoordinates(obj.getX(), obj.getY())
		}
		if (obj is RenderedObject) {
			processPolygonCoordinates(obj.getX(), obj.getY())
		}
	}

	private fun sortObjects() {
		sortObjectsByLang()
		sortObjectsByResourceType()
		sortObjectsByClass()
	}

	private fun sortObjectsByLang() {
		objects.sortWith { o1, o2 ->
			val l1 = getLangForTravel(o1)
			val l2 = getLangForTravel(o2)

			val preferred1 = KAlgorithms.stringsEqual(l1, lang)
			val preferred2 = KAlgorithms.stringsEqual(l2, lang)
			if (preferred1 == preferred2) {
				0
			} else if (preferred1) {
				-1
			} else {
				1
			}
		}
	}

	private fun sortObjectsByResourceType() {
		objects.sortWith { o1, o2 ->
			val ord1 = getResourceType(o1).ordinal
			val ord2 = getResourceType(o2).ordinal
			if (ord1 != ord2) {
				if (ord2 > ord1) -1 else 1
			} else {
				0
			}
		}
	}

	private fun sortObjectsByClass() {
		objects.sortWith { o1, o2 ->
			val ord1 = getClassOrder(o1)
			val ord2 = getClassOrder(o2)
			if (ord1 != ord2) {
				if (ord2 > ord1) -1 else 1
			} else {
				0
			}
		}
	}

	fun setObfResourceName(obfName: String?) {
		obfResourceName = obfName
	}

	fun getResourceType(): SearchResultResource {
		var resource = searchResultResource
		if (resource == null) {
			resource = findObfType(obfResourceName, syntheticAmenity)
			searchResultResource = resource
		}
		return resource
	}

	fun getLang(): String? = lang

	fun setMapIconName(mapIconName: String?) {
		this.syntheticAmenity.setMapIconName(mapIconName)
	}

	fun setX(x: KTIntArrayList) {
		this.syntheticAmenity.getX().addAll(x)
	}

	fun setY(y: KTIntArrayList) {
		this.syntheticAmenity.getY().addAll(y)
	}

	fun addX(x: Int) {
		this.syntheticAmenity.getX().add(x)
	}

	fun addY(y: Int) {
		this.syntheticAmenity.getY().add(y)
	}

	fun hasGeometry(): Boolean = !this.syntheticAmenity.getX().isEmpty() && !this.syntheticAmenity.getY().isEmpty()

	fun getPointsLength(): Int = this.syntheticAmenity.getX().size()

	fun clearGeometry() {
		this.syntheticAmenity.getY().clear()
		this.syntheticAmenity.getX().clear()
	}

	protected open fun isSupportedObjectType(obj: Any?): Boolean {
		return obj is Amenity || obj is RenderedObject || obj is City || obj is Street || obj is Building
				|| obj is BaseDetailsObject
	}

	fun getAmenities(): List<Amenity> {
		val amenities = ArrayList<Amenity>()
		for (obj in objects) {
			if (obj is Amenity) {
				amenities.add(obj)
			}
		}
		return amenities
	}

	fun getRenderedObjects(): List<RenderedObject> {
		val renderedObjects = ArrayList<RenderedObject>()
		for (obj in objects) {
			if (obj is RenderedObject) {
				renderedObjects.add(obj)
			}
		}
		return renderedObjects
	}

	override fun toString(): String = getSyntheticAmenity().toString()

	fun getAddressObject(): MapObject? {
		for (obj in objects) {
			if (obj is Building) {
				return obj
			}
			if (obj is Street) {
				return obj
			}
			if (obj is City) {
				return obj
			}
		}
		return null
	}

	companion object {

		protected fun processId(syntheticAmenity: Amenity, obj: MapObject) {
			if (syntheticAmenity.getId() == null && ObfConstants.isOsmUrlAvailable(obj)) {
				syntheticAmenity.setId(obj.getId())
			}
		}

		private fun updateAmenitySubTypes(amenity: Amenity, subTypesToAdd: String?) {
			if (KAlgorithms.isEmpty(subTypesToAdd)) {
				return
			}
			if (amenity.getSubType() == null) {
				amenity.setSubType(subTypesToAdd)
			} else {
				for (subType in WorldRegion.splitLikeJava(subTypesToAdd!!, ";")) {
					var isSubTypeUnique = true
					for (s in WorldRegion.splitLikeJava(amenity.getSubType()!!, ";")) {
						if (s == subType) {
							isSubTypeUnique = false
							break
						}
					}
					if (isSubTypeUnique) {
						amenity.setSubType(amenity.getSubType() + ";" + subType)
					}
				}
			}
		}

		fun mergeAmenityData(syntheticAmenity: Amenity, amenity: Amenity, lang: String?, isSingleObject: Boolean) {
			processId(syntheticAmenity, amenity)

			val location = amenity.getLocation()
			if (syntheticAmenity.getLocation() == null && location != null) {
				syntheticAmenity.setLocation(location)
			}
			val type = amenity.getType()
			val poiTypes = MapPoiTypes.getDefault()
			if (type != null && (syntheticAmenity.getType() == null
						|| (poiTypes.isOtherCategory(syntheticAmenity.getType()) && !poiTypes.isOtherCategory(type)))
			) {
				// a placeholder, e.g. a marker of an address result, must not shadow a real category
				syntheticAmenity.setType(type)
			}
			val subType = amenity.getSubType()
			if (subType != null && !KAlgorithms.stringsEqual(subType, syntheticAmenity.getSubType())) {
				updateAmenitySubTypes(syntheticAmenity, subType)
			}
			val mapIconName = amenity.getMapIconName()
			if (syntheticAmenity.getMapIconName() == null && mapIconName != null) {
				syntheticAmenity.setMapIconName(mapIconName)
			}
			val regionName = amenity.getRegionName()
			if (syntheticAmenity.getRegionName() == null && regionName != null) {
				syntheticAmenity.setRegionName(regionName)
			}
			val groups: Map<Int, List<TagValuePair>>? = amenity.getTagGroups()
			if (syntheticAmenity.getTagGroups() == null && groups != null) {
				syntheticAmenity.setTagGroups(HashMap(groups))
			}
			val travelElo = amenity.getTravelEloNumber()
			if (syntheticAmenity.getTravelEloNumber() == DEFAULT_ELO && travelElo != DEFAULT_ELO) {
				syntheticAmenity.setTravelEloNumber(travelElo)
			}
			syntheticAmenity.copyNames(amenity)
			val shouldCopyAdditionalInfo = getResourceType(amenity) != SearchResultResource.TRAVEL
					|| getLangForTravel(amenity) == lang // avoid articles in another language
			if (isSingleObject || shouldCopyAdditionalInfo) {
				syntheticAmenity.copyAdditionalInfo(amenity, false)
			}
			processPolygonCoordinates(syntheticAmenity, amenity.getX(), amenity.getY())

			val contentLocales = amenity.getSupportedContentLocales()
			if (!KAlgorithms.isEmpty(contentLocales)) {
				syntheticAmenity.updateContentLocales(contentLocales)
			}
		}

		private fun processPolygonCoordinates(syntheticAmenity: Amenity, x: KTIntArrayList, y: KTIntArrayList) {
			if (syntheticAmenity.getX().isEmpty() && !x.isEmpty()) {
				syntheticAmenity.getX().addAll(x)
			}
			if (syntheticAmenity.getY().isEmpty() && !y.isEmpty()) {
				syntheticAmenity.getY().addAll(y)
			}
		}

		private fun findObfType(obfResourceName: String?, amenity: Amenity): SearchResultResource {
			if (obfResourceName != null && obfResourceName.contains("basemap")) {
				return SearchResultResource.BASEMAP
			}
			if (obfResourceName != null && (obfResourceName.contains("travel") || obfResourceName.contains("wikivoyage"))) {
				return SearchResultResource.TRAVEL
			}
			if (amenity.getType()!!.isWiki()) {
				return SearchResultResource.WIKIPEDIA
			}
			return SearchResultResource.DETAILED
		}

		private fun getResourceType(obj: Any?): SearchResultResource {
			if (obj is BaseDetailsObject) {
				return obj.getResourceType()
			}
			if (obj is Amenity) {
				return findObfType(obj.getRegionName(), obj)
			}
			return SearchResultResource.DETAILED
		}

		fun getLangForTravel(obj: Any?): String {
			var amenity: Amenity? = null
			if (obj is Amenity) {
				amenity = obj
			}
			if (obj is BaseDetailsObject) {
				amenity = obj.syntheticAmenity
			}
			if (amenity != null && getResourceType(obj) == SearchResultResource.TRAVEL) {
				val lang = amenity.getTagSuffix(Amenity.LANG_YES + ":")
				if (lang != null) {
					return lang
				}
			}
			return "en"
		}

		private fun getClassOrder(obj: Any?): Int {
			if (obj is BaseDetailsObject) {
				return 1
			}
			if (obj is Amenity) {
				return 2
			}
			if (obj is RenderedObject) {
				return 4
			}
			return 5
		}

		fun convertRenderedObjectToAmenity(renderedObject: RenderedObject, mapPoiTypes: MapPoiTypes): Amenity {
			val am = Amenity()
			am.setType(mapPoiTypes.getOtherPoiCategory())
			am.setSubType("")
			val poiTranslator = mapPoiTypes.getPoiTranslator()
			var pt: PoiType? = null
			var otherPt: PoiType? = null
			var subtype: String? = null
			val additionalInfo: MutableMap<String, String> = LinkedHashMap()
			for ((tag, value) in renderedObject.getTags()) {
				if (tag == "name") {
					am.setName(value)
					continue
				}
				if (tag.startsWith("name:")) {
					am.setName(tag.substring("name:".length), value)
					continue
				}
				if (tag == "amenity") {
					if (pt != null) {
						otherPt = pt
					}
					pt = mapPoiTypes.getPoiTypeByKey(value)
				} else {
					var poiType = mapPoiTypes.getPoiTypeByKey(tag + "_" + value)
					if (poiType == null) {
						poiType = mapPoiTypes.getPoiTypeByKey(tag)
					}
					if (poiType != null) {
						otherPt = if (pt != null) poiType else otherPt
						subtype = if (pt == null) value else subtype
						pt = pt ?: poiType
					}
				}
				if (KAlgorithms.isEmpty(value) && otherPt == null) {
					otherPt = mapPoiTypes.getPoiTypeByKey(tag)
				}
				if (otherPt == null) {
					val poiType = mapPoiTypes.getPoiTypeByKey(value)
					if (poiType != null && poiType.getOsmTag()!! == tag) {
						otherPt = poiType
					}
				}
				if (!KAlgorithms.isEmpty(value)) {
					val translate = poiTranslator!!.getTranslation(tag + "_" + value)
					val translate2 = poiTranslator.getTranslation(value)
					if (translate != null && translate2 != null) {
						additionalInfo[translate] = translate2
					} else {
						additionalInfo[tag] = value
					}
				}
			}
			if (pt != null) {
				am.setType(pt.getCategory())
			} else if (otherPt != null) {
				am.setType(otherPt.getCategory())
				am.setSubType(otherPt.getKeyName())
			}
			if (subtype != null) {
				am.setSubType(subtype)
			}
			val type = ObfConstants.getOsmEntityType(renderedObject)
			if (type != null) {
				val osmId = ObfConstants.getOsmObjectId(renderedObject)
				val objectId = ObfConstants.createMapObjectIdFromCleanOsmId(osmId, type)
				am.setId(objectId)
			}
			am.setAdditionalInfo(additionalInfo)
			am.setX(renderedObject.getX())
			am.setY(renderedObject.getY())
			return am
		}
	}
}
