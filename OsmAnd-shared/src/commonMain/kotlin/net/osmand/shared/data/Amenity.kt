package net.osmand.shared.data

import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.TagValuePair
import net.osmand.shared.gpx.GpxFile.Companion.XML_COLON
import net.osmand.shared.gpx.GpxUtilities.AMENITY_PREFIX
import net.osmand.shared.gpx.GpxUtilities.ICON_NAME_EXTENSION
import net.osmand.shared.gpx.GpxUtilities.OSM_PREFIX
import net.osmand.shared.osm.AbstractPoiType
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTES_PREFIX
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_ARTICLE
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_ARTICLE_POINT
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_TRACK
import net.osmand.shared.osm.MapPoiTypes.Companion.ROUTE_TRACK_POINT
import net.osmand.shared.osm.MapPoiTypes.Companion.WIKI_LANG
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.wiki.WikiHelper
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A point of interest read out of the poi section of an obf file: what it is, where it is, and
 * every tag the indexer kept for it.
 *
 * A copy of `Amenity` in OsmAnd-java, which stays there for android and tools; this copy is for
 * iOS. `toJSON` and `parseJSON` are left out for the same reason as in [MapObject]. A track stored
 * in an obf file is an amenity too - [isRouteTrack] - which is how the travel code finds one.
 */
class Amenity : MapObject() {

	private var subType: String? = null
	private var type: PoiCategory? = null

	// duplicate for fast access
	private var openingHours: String? = null
	private var additionalInfo: MutableMap<String, String>? = null
	private var routePoint: AmenityRoutePoint? = null // for search on path

	// context menu geometry;
	private var y: KTIntArrayList? = null
	private var x: KTIntArrayList? = null
	private var mapIconName: String? = null
	private var order: Int = 0
	private var tagGroups: MutableMap<Int, List<TagValuePair>>? = null
	private var regionName: String? = null
	private var bbox31: IntArray? = null

	private var wikiIconUrl: String? = null
	private var wikiImageStubUrl: String? = null
	private var travelElo: Int = 0
	private var contentLocales: MutableSet<String>? = null

	fun getOrder(): Int = order

	fun setOrder(order: Int) {
		this.order = order
	}

	/** The administrative areas this amenity falls in, as the poi section groups their tags. */
	fun getTagGroups(): Map<Int, List<TagValuePair>>? = tagGroups

	fun addTagGroup(id: Int, tagValues: List<TagValuePair>) {
		var tagGroups = this.tagGroups
		if (tagGroups == null) {
			tagGroups = HashMap()
			this.tagGroups = tagGroups
		}
		tagGroups[id] = tagValues
	}

	fun setTagGroups(tagGroups: MutableMap<Int, List<TagValuePair>>?) {
		this.tagGroups = tagGroups
	}

	fun setRegionName(regionName: String?) {
		this.regionName = regionName
	}

	fun getRegionName(): String? = regionName

	fun getMapIconName(): String? = mapIconName

	fun setMapIconName(mapIconName: String?) {
		this.mapIconName = mapIconName
	}

	fun getType(): PoiCategory? = type

	fun getSubType(): String? = subType

	fun setType(type: PoiCategory?) {
		this.type = type
	}

	fun setSubType(subType: String?) {
		this.subType = subType
	}

	/** The first of the subtypes, translated; an amenity can carry several separated by ";". */
	fun getMainSubtype(): String? {
		val subtype = getSubType() ?: return null
		val index = subtype.indexOf(';')
		val firstKey = if (index == -1) subtype else subtype.substring(0, index)

		val category = getType()
		val poiType = findPoiType(firstKey, category, MapPoiTypes.getDefault())
		return poiType?.getTranslation()
			?: MapPoiTypes.capitalizeFirstLetterAndLowercase(firstKey.replace('_', ' '))
	}

	fun getSubTypeStr(): String {
		val builder = StringBuilder()

		val subtype = getSubType() ?: return ""
		val category = getType()
		val mapPoiTypes = MapPoiTypes.getDefault()

		for (type in subtype.split(";")) {
			val poiType = findPoiType(type, category, mapPoiTypes)
			if (poiType != null) {
				builder.append(
					if (builder.isEmpty()) poiType.getTranslation()
					else ", " + poiType.getTranslation().lowercase()
				)
			}
		}
		if (builder.isEmpty()) {
			builder.append(MapPoiTypes.capitalizeFirstLetterAndLowercase(subtype.replace('_', ' ')))
		}
		return builder.toString()
	}

	private fun findPoiType(keyName: String, category: PoiCategory?, mapPoiTypes: MapPoiTypes): PoiType? {
		var poiType = category?.getPoiTypeByKeyName(keyName)
		if (poiType == null) {
			// Try to get POI type from another category, but skip non-OSM-types
			val abstractPoiType = mapPoiTypes.getAnyPoiTypeByKey(keyName)
			if (abstractPoiType is PoiType && !abstractPoiType.isNotEditableOsm()) {
				poiType = abstractPoiType
			}
		}
		return poiType
	}

	fun getOpeningHours(): String? = openingHours

	fun getAdditionalInfo(key: String): String? {
		val additionalInfo = this.additionalInfo ?: return null
		var str = additionalInfo[key]
		if (str == null && key.contains(":")) {
			str = additionalInfo[key.replace(":", XML_COLON)] // try content_-_uk after content:uk
		}
		if (str != null) {
			str = unzipContent(str)
		}
		return str
	}

	fun hasAdditionalInfo(): Boolean = !KAlgorithms.isEmpty(additionalInfo)

	// this method should be used carefully
	private fun getInternalAdditionalInfoMap(): Map<String, String> = additionalInfo ?: emptyMap()

	fun getAdditionalInfoValues(excludeZipped: Boolean): Collection<String> {
		val additionalInfo = this.additionalInfo ?: return emptyList()
		var zipped = false
		for (v in additionalInfo.values) {
			if (isContentZipped(v)) {
				zipped = true
				break
			}
		}
		if (zipped) {
			val r = ArrayList<String>(additionalInfo.size)
			for (str in additionalInfo.values) {
				if (excludeZipped && isContentZipped(str)) {
					// left out on purpose
				} else {
					unzipContent(str)?.let { r.add(it) }
				}
			}
			return r
		} else {
			return additionalInfo.values
		}
	}

	fun getAdditionalInfoKeys(): Collection<String> = additionalInfo?.keys ?: emptyList()

	fun setAdditionalInfo(additionalInfo: Map<String, String>?) {
		this.additionalInfo = null
		openingHours = null
		if (additionalInfo != null) {
			for (e in additionalInfo.entries) {
				setAdditionalInfo(e.key, e.value)
			}
		}
	}

	fun setRoutePoint(routePoint: AmenityRoutePoint?) {
		this.routePoint = routePoint
	}

	fun getRoutePoint(): AmenityRoutePoint? = routePoint

	fun setAdditionalInfo(tag: String, value: String) {
		if ("name" == tag) {
			setName(value)
		} else if (isNameLangTag(tag)) {
			setName(tag.substring("name:".length), value)
		} else {
			var additionalInfo = this.additionalInfo
			if (additionalInfo == null) {
				additionalInfo = LinkedHashMap()
				this.additionalInfo = additionalInfo
			}
			additionalInfo[tag] = value

			if (OPENING_HOURS == tag) {
				openingHours = unzipContent(value)
			}
		}
	}

	fun copyAdditionalInfo(amenity: Amenity, overwrite: Boolean) {
		copyAdditionalInfo(amenity.getInternalAdditionalInfoMap(), overwrite)
	}

	fun copyAdditionalInfo(map: Map<String, String>, overwrite: Boolean) {
		val additionalInfo = this.additionalInfo
		if (overwrite || additionalInfo == null) {
			setAdditionalInfo(map)
		} else {
			for (entry in map.entries) {
				if (!additionalInfo.containsKey(entry.key)) {
					setAdditionalInfo(entry.key, entry.value)
				}
			}
		}
	}

	/** Everything the amenity carries, grouped by what poi_types.xml says each tag is. */
	fun printNamesAndAdditional(): StringBuilder {
		val s = StringBuilder()
		val additionals = LinkedHashMap<String, String>()
		val poiType = LinkedHashMap<String, String>()
		val text = LinkedHashMap<String, String>()
		val additionalInfo = this.additionalInfo
		if (additionalInfo != null) {
			for (e in additionalInfo.entries) {
				var key = e.key
				val value = e.value
				var pt: AbstractPoiType? = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key)
				if (pt == null && !KAlgorithms.isEmpty(value) && value.length < 50) {
					pt = MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(key + "_" + value)
				}
				if (pt != null) {
					additionals[key] = value
				} else {
					val pt2 = MapPoiTypes.getDefault().getPoiTypeByKey(key)
					if (pt2 != null) {
						val cat = pt2.getCategory()?.getKeyName() ?: ""
						if (poiType.containsKey(cat)) {
							key = poiType[cat] + ";" + key
						}
						poiType[cat] = key
					} else {
						text[key] = value
					}
				}
			}
		}
		if (poiType.isNotEmpty()) {
			s.append(" [ ")
			printNames("", poiType, s)
			s.append(" ] ")
		}
		if (additionals.isNotEmpty()) {
			s.append(" poi_additional:[ ")
			printNames("", additionals, s)
			s.append(" ] ")
		}
		if (text.isNotEmpty()) {
			s.append(" non_default_poi_xml:[ ")
			printNames("", text, s)
			s.append(" ] ")
		}
		printNames(" name:", getNamesMap(true), s)
		return s
	}

	private fun printNames(prefix: String, stringMap: Map<String, String>, s: StringBuilder) {
		for (e in stringMap.entries) {
			if (e.value.startsWith(" gz ")) {
				s.append(prefix).append(e.key).append("='gzip ...' ")
			} else {
				s.append(prefix).append(e.key).append("='").append(e.value).append("' ")
			}
		}
	}

	override fun toStringEn(): String =
		super.toStringEn() + ": " + type?.getKeyName() + ":" + subType

	override fun toString(): String = type?.getKeyName() + ": " + subType + " " + getName()

	fun getSite(): String? = getAdditionalInfo(WEBSITE)

	fun getStreetName(): String? = getAdditionalInfo(ADDR_STREET)

	fun getHousenumber(): String? = getAdditionalInfo(ADDR_HOUSENUMBER)

	fun setSite(site: String) {
		setAdditionalInfo(WEBSITE, site)
	}

	fun getPhone(): String? = getAdditionalInfo(PHONE)

	fun setPhone(phone: String) {
		setAdditionalInfo(PHONE, phone)
	}

	fun getColor(): String? = getAdditionalInfo(COLOR)

	fun getGpxIcon(): String? {
		val wikiVoyageIcon = getAdditionalInfo(GPX_ICON)
		val travelGpxIcon = getAdditionalInfo(ICON_NAME_EXTENSION)
		return if (KAlgorithms.isEmpty(wikiVoyageIcon)) travelGpxIcon else wikiVoyageIcon
	}

	/** Which language of [tag] this amenity actually has content in, preferring [lang]. */
	fun getContentLanguage(tag: String, lang: String?, defLang: String?): String? {
		if (lang != null) {
			val translateName = getAdditionalInfo("$tag:$lang")
			if (!KAlgorithms.isEmpty(translateName)) {
				return lang
			}
		}
		val plainContent = getAdditionalInfo(tag)
		if (!KAlgorithms.isEmpty(plainContent)) {
			return defLang
		}
		val enName = getAdditionalInfo("$tag:en")
		if (!KAlgorithms.isEmpty(enName)) {
			return "en"
		}
		var maxLen = 0
		var lng = defLang
		for (nm in getAdditionalInfoKeys()) {
			if (nm.startsWith("$tag:")) {
				val key = nm.substring(tag.length + 1)
				val cnt = getAdditionalInfo("$tag:$key")
				if (!KAlgorithms.isEmpty(cnt) && cnt!!.length > maxLen) {
					maxLen = cnt.length
					lng = key
				}
			}
		}
		return lng
	}

	fun getSupportedContentLocales(): Set<String> {
		val contentLocales = this.contentLocales
		if (contentLocales != null) {
			return contentLocales
		}
		return sortedSet(getNames(CONTENT, "en") + getNames(DESCRIPTION, "en"))
	}

	fun updateContentLocales(locales: Set<String>) {
		val current = contentLocales ?: emptySet()
		contentLocales = sortedSet(current + locales)
	}

	fun getNames(tag: String, defTag: String): List<String> {
		val l = ArrayList<String>()
		for (nm in getAdditionalInfoKeys()) {
			if (nm.startsWith("$tag:")) {
				l.add(nm.substring(tag.length + 1))
			} else if (nm == tag) {
				l.add(defTag)
			}
		}
		return l
	}

	fun getAltNamesMap(): Map<String, String> {
		val names = HashMap<String, String>()
		val additionalInfo = this.additionalInfo ?: return names
		for (nm in getAdditionalInfoKeys()) {
			val name = additionalInfo[nm] ?: continue
			if (nm.startsWith(ALT_NAME_WITH_LANG_PREFIX)) {
				names[nm.substring(ALT_NAME_WITH_LANG_PREFIX.length)] = name
			}
		}
		return names
	}

	fun getTagSuffix(tagPrefix: String): String? {
		for (infoTag in getAdditionalInfoKeys()) {
			if (infoTag.startsWith(tagPrefix)) {
				if (infoTag.length > tagPrefix.length) {
					return infoTag.substring(tagPrefix.length)
				}
			}
		}
		return null
	}

	fun getTagContent(tag: String): String? = getTagContent(tag, null)

	fun getTagContent(tag: String, lang: String?): String? {
		val translateName = getStrictTagContent(tag, lang)
		if (translateName != null) {
			return translateName
		}
		for (nm in getAdditionalInfoKeys()) {
			if (nm.startsWith("$tag:") || nm.startsWith(tag + XML_COLON)) {
				return getAdditionalInfo(nm)
			}
		}
		return null
	}

	fun getRef(): String? = getAdditionalInfo(REF)

	fun getRouteId(): String? = getAdditionalInfo(ROUTE_ID)

	fun getWikiPhoto(): String? = getAdditionalInfo(WIKI_PHOTO)

	fun setWikiPhoto(wikiPhoto: String) {
		setAdditionalInfo(WIKI_PHOTO, wikiPhoto)
	}

	fun getWikiCategory(): String? = getAdditionalInfo(WIKI_CATEGORY)

	fun setWikiCategory(wikiCategory: String) {
		setAdditionalInfo(WIKI_CATEGORY, wikiCategory)
	}

	fun getTravelTopic(): String? = getAdditionalInfo(TRAVEL_TOPIC)

	fun setTravelTopic(travelTopic: String) {
		setAdditionalInfo(TRAVEL_TOPIC, travelTopic)
	}

	fun getTravelElo(): String? = getAdditionalInfo(TRAVEL_ELO)

	override fun getWikidata(): String? = getAdditionalInfo(WIKIDATA)

	fun getTravelEloNumber(): Int {
		if (travelElo > 0) {
			return travelElo
		}
		travelElo = KAlgorithms.parseIntSilently(getTravelElo(), DEFAULT_ELO)
		return travelElo
	}

	fun setTravelEloNumber(elo: Int) {
		travelElo = elo
	}

	fun getWikiIconUrl(): String? {
		if (wikiIconUrl == null) {
			obtainWikiUrls()
		}
		return wikiIconUrl
	}

	fun setWikiIconUrl(wikiIconUrl: String?) {
		this.wikiIconUrl = wikiIconUrl
	}

	fun getWikiImageStubUrl(): String? {
		if (wikiImageStubUrl == null) {
			obtainWikiUrls()
		}
		return wikiImageStubUrl
	}

	fun setWikiImageStubUrl(wikiImageStubUrl: String?) {
		this.wikiImageStubUrl = wikiImageStubUrl
	}

	fun getOsmandPoiKey(): String? = getAdditionalInfo(OSMAND_POI_KEY)

	private fun obtainWikiUrls() {
		val wikiPhoto = getWikiPhoto()
		if (!KAlgorithms.isEmpty(wikiPhoto)) {
			val wikiImage = WikiHelper.getImageData(wikiPhoto!!)
			setWikiIconUrl(wikiImage.imageIconUrl)
			setWikiImageStubUrl(wikiImage.imageStubUrl)
		}
	}

	fun hasOsmRouteId(): Boolean {
		val routeId = getRouteId()
		return routeId != null &&
				(routeId.startsWith(ROUTE_ID_OSM_PREFIX_LEGACY) || routeId.startsWith(ROUTE_ID_OSM_PREFIX))
	}

	/** What a track built out of this amenity is saved as; never empty, never a path. */
	fun getGpxFileName(lang: String?): String? {
		val gpxFileName = if (lang != null) getName(lang) else getEnName(true)
		if (!KAlgorithms.isEmpty(gpxFileName)) {
			return KAlgorithms.sanitizeFileName(gpxFileName)
		}
		if (!KAlgorithms.isEmpty(getRouteId())) {
			return getRouteId()
		}
		if (!KAlgorithms.isEmpty(getSubType())) {
			return getType()?.getKeyName() + " " + getSubType()
		}
		return getType()?.getKeyName()
	}

	fun getStrictTagContent(tag: String, lang: String?): String? {
		if (lang != null) {
			val translateName = getAdditionalInfo("$tag:$lang")
			if (!KAlgorithms.isEmpty(translateName)) {
				return translateName
			}
		}
		val plainName = getAdditionalInfo(tag)
		if (!KAlgorithms.isEmpty(plainName)) {
			return plainName
		}
		val enName = getAdditionalInfo("$tag:en")
		if (!KAlgorithms.isEmpty(enName)) {
			return enName
		}
		return null
	}

	fun getDescription(lang: String?): String? {
		val info = getTagContent(DESCRIPTION, lang)
		if (!KAlgorithms.isEmpty(info)) {
			return info
		}
		return getTagContent(CONTENT, lang)
	}

	fun setDescription(description: String) {
		setAdditionalInfo(DESCRIPTION, description)
	}

	fun setOpeningHours(openingHours: String) {
		setAdditionalInfo(OPENING_HOURS, openingHours)
	}

	fun comparePoi(thatObj: Amenity?): Boolean {
		return this.compareObject(thatObj) &&
				this.type?.getKeyName() == thatObj?.type?.getKeyName() &&
				this.subType == thatObj?.subType &&
				this.additionalInfo == thatObj?.additionalInfo
	}

	/** Equal, and outlining the same shape - two amenities of one object in two files are not. */
	fun strictEquals(other: Any?): Boolean {
		if (equals(other) && other is Amenity) {
			val x = this.x
			val otherX = other.x
			if (x != null && otherX != null && x.size() == otherX.size()) {
				for (i in 0 until x.size()) {
					if (x[i] != otherX[i] || getY()[i] != other.getY()[i]) {
						return false
					}
				}
				return true
			} else {
				return x == null && otherX == null
			}
		}
		return false
	}

	override fun compareTo(other: MapObject): Int {
		val cmp = super.compareTo(other)
		if (cmp == 0 && other is Amenity) {
			var kn = (other.getType()?.getKeyName() ?: "").compareTo(getType()?.getKeyName() ?: "")
			if (kn == 0) {
				kn = (other.getSubType() ?: "").compareTo(getSubType() ?: "")
			}
			return kn
		}
		return cmp
	}

	override fun equals(other: Any?): Boolean {
		val res = super.equals(other)
		if (res && other is Amenity) {
			return KAlgorithms.stringsEqual(other.getType()?.getKeyName(), getType()?.getKeyName()) &&
					KAlgorithms.stringsEqual(other.getSubType(), getSubType())
		}
		return res
	}

	override fun hashCode(): Int = super.hashCode()

	fun getY(): KTIntArrayList {
		var y = this.y
		if (y == null) {
			y = KTIntArrayList()
			this.y = y
		}
		return y
	}

	fun getX(): KTIntArrayList {
		var x = this.x
		if (x == null) {
			x = KTIntArrayList()
			this.x = x
		}
		return x
	}

	/** Deleted by a live update: the object is still in the file, marked as gone. */
	fun isClosed(): Boolean = OSM_DELETE_VALUE == getAdditionalInfo(OSM_DELETE_TAG)

	fun isPrivateAccess(): Boolean = PRIVATE_VALUE == getTagContent(ACCESS_PRIVATE_TAG)

	/** A track stored in an obf file: it has a route subtype, a geometry and a route id. */
	fun isRouteTrack(): Boolean {
		val subType = this.subType ?: return false
		val hasRouteTrackSubtype = subType.startsWith(ROUTES_PREFIX) ||
				subType.contains(";$ROUTES_PREFIX") || subType == ROUTE_TRACK
		val hasGeometry = additionalInfo?.containsKey(ROUTE_BBOX_RADIUS) == true
		return hasRouteTrackSubtype && hasGeometry && !KAlgorithms.isEmpty(getRouteId())
	}

	fun isRoutePoint(): Boolean =
		subType != null && (subType == ROUTE_TRACK_POINT || subType == ROUTE_ARTICLE_POINT)

	fun isRouteArticle(): Boolean = KAlgorithms.stringsEqual(ROUTE_ARTICLE, subType)

	fun isSuperRoute(): Boolean = additionalInfo?.containsKey(ROUTE_MEMBERS_IDS) == true

	fun getAmenityExtensions(): Map<String, String> =
		getAmenityExtensions(MapPoiTypes.getDefault(), true)

	fun getAmenityExtensions(mapPoiTypes: MapPoiTypes, addPrefixes: Boolean): Map<String, String> =
		getAmenityExtensions(mapPoiTypes, addPrefixes, false, null)

	/** The amenity flattened into gpx extensions, which is how a track built from it carries it. */
	fun getAmenityExtensions(
		mapPoiTypes: MapPoiTypes, addPrefixes: Boolean, excludeWikiContent: Boolean,
		preferredLang: String?
	): Map<String, String> {
		val result = HashMap<String, String>()
		val categories = HashMap<String, MutableList<PoiType>>()

		val name = this.name
		if (name != null) {
			result[if (addPrefixes) AMENITY_PREFIX + NAME else NAME] = name
		}
		val subType = this.subType
		if (subType != null) {
			result[if (addPrefixes) AMENITY_PREFIX + SUBTYPE else SUBTYPE] = subType
		}
		val type = this.type
		if (type != null) {
			result[if (addPrefixes) AMENITY_PREFIX + TYPE else TYPE] = type.getKeyName()
		}
		val openingHours = this.openingHours
		if (openingHours != null) {
			result[if (addPrefixes) AMENITY_PREFIX + OPENING_HOURS else OPENING_HOURS] = openingHours
		}
		if (hasAdditionalInfo()) {
			result.putAll(
				getAdditionalInfoAndCollectCategories(
					mapPoiTypes, categories, addPrefixes, excludeWikiContent, preferredLang
				)
			)

			// join collected tags by category into one string
			for (entry in categories.entries) {
				val key = COLLAPSABLE_PREFIX + entry.key
				val categoryTypes = entry.value
				if (categoryTypes.isNotEmpty()) {
					val builder = StringBuilder()
					for (poiType in categoryTypes) {
						if (builder.isNotEmpty()) {
							builder.append(SEPARATOR)
						}
						builder.append(poiType.getKeyName())
					}
					result[key] = builder.toString()
				}
			}
		}
		return result
	}

	fun getAdditionalInfoAndCollectCategories(
		mapPoiTypes: MapPoiTypes, categories: MutableMap<String, MutableList<PoiType>>?,
		addPrefixes: Boolean, excludeWikiContent: Boolean, preferredLang: String?
	): Map<String, String> {
		val result = HashMap<String, String>()
		val hasDefaultShortDescription = getInternalAdditionalInfoMap().containsKey(SHORT_DESCRIPTION)
		for (originalKey in getAdditionalInfoKeys()) {
			var key = originalKey
			if (excludeWikiContent && isWikiContentTag(key, preferredLang, hasDefaultShortDescription)) {
				continue
			}
			val value = getAdditionalInfo(key) ?: continue
			val poiType = getPoiType(mapPoiTypes, key, value)
			if (poiType != null && poiType.isFilterOnly()) {
				continue
			}
			if (poiType != null && !poiType.isText()) {
				if (categories != null) {
					val category = poiType.getPoiAdditionalCategory()
					if (!KAlgorithms.isEmpty(category)) {
						val types = categories.getOrPut(category!!) { ArrayList() }
						types.add(poiType)
						continue
					}
				}
			}
			// save all other values to separate lines
			if (key.endsWith(OPENING_HOURS)) {
				continue
			}
			if (!HIDING_EXTENSIONS_AMENITY_TAGS.contains(key) && addPrefixes) {
				key = OSM_PREFIX + key
			}
			result[key] = value
		}
		return result
	}

	private fun getPoiType(mapPoiTypes: MapPoiTypes, key: String, value: String?): PoiType? {
		var abstractPoiType = mapPoiTypes.getAnyPoiAdditionalTypeByKey(key)
		if (abstractPoiType == null && !isContentZipped(value)) {
			abstractPoiType = mapPoiTypes.getAnyPoiAdditionalTypeByKey(key + "_" + value)
		}
		if (abstractPoiType is PoiType) {
			return abstractPoiType
		}
		return null
	}

	/** The translated name of the additional type whose value is [alternateName], or it unchanged. */
	fun getTranslation(mapPoiTypes: MapPoiTypes, alternateName: String): String {
		for (key in getAdditionalInfoKeys()) {
			val value = getAdditionalInfo(key)
			if (value != null && value == alternateName) {
				val poiType = getPoiType(mapPoiTypes, key, value)
				if (poiType != null && !poiType.isText()) {
					return poiType.getTranslation()
				}
			}
		}
		return alternateName
	}

	/** The settlements this amenity is inside, as one string; districts and postcodes are left out. */
	fun getCityFromTagGroups(lang: String?): String? {
		val tagGroups = this.tagGroups
		if (KAlgorithms.isEmpty(tagGroups)) {
			return null
		}
		var singleName: String? = null
		var singleType: CityType? = null
		val nameLangTag = "name:$lang"
		var names: MutableMap<CityType, String>? = null

		for (entry in tagGroups!!.entries) {
			var type: CityType? = null
			var translated: String? = null
			var nonTranslated: String? = null

			val tagValues = entry.value
			for (tagValue in tagValues) {
				val tag = tagValue.tag ?: continue
				if (tag.endsWith(nameLangTag)) {
					translated = tagValue.value
				} else if (tag.endsWith("name")) {
					nonTranslated = tagValue.value
				} else if (tag == "place") {
					type = CityType.valueFromString(tagValue.value)
				}
			}
			val name = if (KAlgorithms.isEmpty(translated)) nonTranslated else translated
			if (!KAlgorithms.isEmpty(name) && isCityTypeAccept(type)) {
				if (names != null) {
					names[type!!] = name!!
				} else if (singleType == null) {
					singleType = type
					singleName = name
				} else if (singleType == type) {
					singleName = name
				} else {
					names = LinkedHashMap()
					names[singleType] = singleName!!
					names[type!!] = name!!
				}
			}
		}
		if (names == null) {
			return if (singleType == null) "" else singleName
		}
		val result = StringBuilder()
		// java keeps these in an EnumMap, so they come out in the order the enum declares
		for (type in CityType.entries) {
			val name = names[type] ?: continue
			if (result.isNotEmpty()) {
				result.append(", ")
			}
			result.append(name)
		}
		return result.toString()
	}

	private fun isCityTypeAccept(type: CityType?): Boolean {
		if (type == null) {
			return false
		}
		return type.storedAsSeparateAdminEntity()
	}

	/** The outline of the object this amenity stands for, when the file kept one. */
	fun getPolygon(): List<KLatLon> {
		val res = ArrayList<KLatLon>()
		if (x == null) {
			return res
		}
		for (i in 0 until getX().size()) {
			val x = getX()[i]
			val y = getY()[i]
			res.add(KLatLon(KMapUtils.get31LatitudeY(y), KMapUtils.get31LongitudeX(x)))
		}
		return res
	}

	fun setX(x: KTIntArrayList?) {
		this.x = x
	}

	fun setY(y: KTIntArrayList?) {
		this.y = y
	}

	fun setBbox31(bbox31: IntArray?) {
		this.bbox31 = bbox31
	}

	override fun getBbox31(): IntArray? = bbox31

	fun getRouteActivityType(): String {
		if (!isRouteTrack() && !isSuperRoute()) {
			return ""
		}
		val additionalInfo = this.additionalInfo ?: return ""
		for (entry in additionalInfo.entries) {
			if (entry.key.startsWith(ROUTE_ACTIVITY_TYPE + "_")) {
				return MapPoiTypes.getDefault().getAnyPoiAdditionalTypeByKey(entry.key)?.getTranslation() ?: ""
			}
		}
		return ""
	}

	fun getOsmId(): Long? {
		getId() ?: return null
		return ObfConstants.getOsmObjectId(this)
	}

	/** Every tag of the amenity spelled as osm spells it, for editing and for gpx extensions. */
	fun getOsmTags(): Map<String, String> {
		val result = LinkedHashMap<String, String>()

		val amenityTags = LinkedHashMap<String, String>()
		for (amenityTag in getAdditionalInfoKeys()) {
			getAdditionalInfo(amenityTag)?.let { amenityTags[amenityTag] = it }
		}

		val amenityName = getName()
		if (!KAlgorithms.isEmpty(amenityName)) {
			result[NAME] = amenityName
		}

		val category = getType()
		val subTypesList = getSubType()

		if (subTypesList != null && category != null) {
			for (subType in subTypesList.split(";")) {
				val type = category.getPoiTypeByKeyName(subType)
				if (type != null) {
					result.putAll(type.getOsmTagsValues())
					for (additional in type.getPoiAdditionals()) {
						if (amenityTags.remove(additional.getKeyName()) != null) {
							result.putAll(additional.getOsmTagsValues())
						}
					}
				}
			}
		}

		result.putAll(amenityTags) // unresolved residues

		return result
	}

	/** The icon poi_types.xml gives this subtype, or the one an additional attribute overrides it with. */
	fun getIcon(): String? {
		val type = this.type ?: return null
		val subType = this.subType ?: return null
		val pt = type.getPoiTypeByKeyName(subType) ?: return null
		var icon = pt.getOriginalIconName()
		if (icon == null && pt.getPoiAdditionals().isNotEmpty()) {
			for (ad in pt.getPoiAdditionals()) {
				val headerIcon = ad.getOriginalIconName()
				if (headerIcon != null && getAdditionalInfo(ad.getKeyName()) != null) {
					icon = headerIcon
					break
				}
			}
		}
		return icon
	}

	companion object {
		const val WEBSITE: String = "website"
		const val URL: String = "url"
		const val PHONE: String = "phone"
		const val MOBILE: String = "mobile"
		const val BRAND: String = "brand"
		const val OPERATOR: String = "operator"
		const val DESCRIPTION: String = "description"
		const val SHORT_DESCRIPTION: String = "short_description"
		const val ROUTE: String = "route"
		const val OPENING_HOURS: String = "opening_hours"
		const val NOTE: String = "note"
		const val POPULATION: String = "population"
		const val WIDTH: String = "width"
		const val HEIGHT: String = "height"
		const val DISTANCE: String = "distance"
		const val SERVICE_TIMES: String = "service_times"
		const val COLLECTION_TIMES: String = "collection_times"
		const val CONTENT: String = "content"
		const val CUISINE: String = "cuisine"
		const val WIKIPEDIA: String = "wikipedia"
		const val WIKIDATA: String = "wikidata"
		const val WIKIMEDIA_COMMONS: String = "wikimedia_commons"
		const val MAPILLARY: String = "mapillary"
		const val DISH: String = "dish"
		const val REF: String = "ref"
		const val OSM_DELETE_VALUE: String = "delete"
		const val OSM_DELETE_TAG: String = "osmand_change"
		const val PRIVATE_VALUE: String = "private"
		const val ACCESS_PRIVATE_TAG: String = "access_private"
		const val IMAGE_TITLE: String = "image_title"
		const val IS_PART: String = "is_part"
		const val IS_PARENT_OF: String = "is_parent_of"
		const val IS_AGGR_PART: String = "is_aggr_part"
		const val CONTENT_JSON: String = "content_json"
		const val ROUTE_ID: String = "route_id"
		const val ROUTE_ID_OSM_PREFIX_LEGACY: String = "OSM" // non-indexed
		const val ROUTE_ID_OSM_PREFIX: String = "O" // indexed in POI-section
		const val ROUTE_SOURCE: String = "route_source"
		const val ROUTE_NAME: String = "route_name"
		const val SHIELD_STUB_NAME: String = "shield_stub_name"
		const val ROUTE_ACTIVITY_TYPE: String = "route_activity_type"
		const val WIKI_PHOTO: String = "wiki_photo"
		const val WIKI_CATEGORY: String = "wiki_category"
		const val TRAVEL_TOPIC: String = "travel_topic"
		const val TRAVEL_ELO: String = "travel_elo"
		const val OSMAND_POI_KEY: String = "osmand_poi_key"
		const val COLOR: String = "color"
		const val LANG_YES: String = "lang_yes"
		const val GPX_ICON: String = "gpx_icon"
		const val TYPE: String = "type"
		const val SUBTYPE: String = "subtype"
		const val NAME: String = "name"
		const val SEPARATOR: String = ";"
		const val ALT_NAME_WITH_LANG_PREFIX: String = "alt_name:"
		const val COLLAPSABLE_PREFIX: String = "collapsable_"
		const val ROUTE_MEMBERS_IDS: String = "route_members_ids"
		const val ROUTE_BBOX_RADIUS: String = "route_bbox_radius"
		@JvmField
		val HIDING_EXTENSIONS_AMENITY_TAGS: List<String> = listOf(PHONE, WEBSITE)
		const val DEFAULT_ELO: Int = 900
		const val ADDR_STREET: String = "addr_street"
		const val ADDR_HOUSENUMBER: String = "addr_housenumber"
		const val DIFF_ELE_DOWN: String = "diff_ele_down"
		const val DIFF_ELE_UP: String = "diff_ele_up"
		const val ALT_NAME_TAG: String = "short"

		/** Wikivoyage content, which a track built out of an amenity does not carry over. */
		@JvmStatic
		fun isWikiContentTag(
			key: String, preferredLang: String?, hasDefaultShortDescription: Boolean
		): Boolean {
			if (isContentTag(key) || WIKI_CATEGORY == key ||
				hasLangSuffix(key, WIKI_LANG) || hasLangSuffix(key, LANG_YES)
			) {
				return true
			}
			if (KAlgorithms.isEmpty(preferredLang) || !hasLangSuffix(key, SHORT_DESCRIPTION)) {
				return false
			}
			if (isTagWithLang(key, SHORT_DESCRIPTION, preferredLang!!)) {
				return false
			}
			if (isTagWithLang(key, SHORT_DESCRIPTION, "en")) {
				// without the unsuffixed tag this is the only English copy left
				return hasDefaultShortDescription
			}
			return true
		}

		private fun isTagWithLang(key: String, tag: String, lang: String): Boolean =
			key == "$tag:$lang" || key == tag + XML_COLON + lang

		@JvmStatic
		fun removeWikiContentTags(
			extensions: Map<String, String>, preferredLang: String?
		): Map<String, String> {
			val hasDefaultShortDescription = extensions.containsKey(SHORT_DESCRIPTION) ||
					extensions.containsKey(OSM_PREFIX + SHORT_DESCRIPTION)
			val result = LinkedHashMap<String, String>()
			for (entry in extensions.entries) {
				if (!isWikiContentTag(stripOsmPrefix(entry.key), preferredLang, hasDefaultShortDescription)) {
					result[entry.key] = entry.value
				}
			}
			return result
		}

		private fun isContentTag(key: String): Boolean =
			CONTENT == key || CONTENT_JSON == key ||
					hasLangSuffix(key, CONTENT) || hasLangSuffix(key, CONTENT_JSON)

		private fun hasLangSuffix(key: String, tag: String): Boolean =
			KCollectionUtils.startsWithAny(key, "$tag:", tag + XML_COLON)

		private fun stripOsmPrefix(key: String): String =
			if (key.startsWith(OSM_PREFIX)) key.substring(OSM_PREFIX.length) else key

		@JvmStatic
		fun getPoiStringWithoutType(amenity: Amenity, locale: String?, transliterate: Boolean): String? {
			val typeName = amenity.getMainSubtype()
			var localName = amenity.getName(locale, transliterate)
			if (typeName != null && localName.contains(typeName)) {
				// type is contained in name e.g.
				// localName = "Bakery the Corner"
				// type = "Bakery"
				// no need to repeat this
				return localName
			}
			if (KAlgorithms.isEmpty(localName) && amenity.isRouteTrack()) {
				localName = amenity.getAdditionalInfo(ROUTE_ID) ?: ""
			}
			if (KAlgorithms.isEmpty(localName)) {
				return typeName
			}
			return "$typeName $localName"
		}

		/** Java keeps these in a `TreeSet`; common Kotlin has none, so they are sorted by hand. */
		private fun sortedSet(values: Collection<String>): MutableSet<String> {
			val sorted = LinkedHashSet<String>()
			sorted.addAll(values.distinct().sorted())
			return sorted
		}
	}
}
