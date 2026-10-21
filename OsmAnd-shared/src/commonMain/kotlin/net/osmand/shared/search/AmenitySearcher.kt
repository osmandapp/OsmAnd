package net.osmand.shared.search

import co.touchlab.stately.collections.ConcurrentMutableMap
import net.osmand.shared.IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Amenity.Companion.WIKIDATA
import net.osmand.shared.data.BaseDetailsObject
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KLocation
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.RenderedObject
import net.osmand.shared.data.Street
import net.osmand.shared.map.WorldRegion
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.edit.EntityType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTIntArrayList

/**
 * The amenities of the obf files the app has open: in a box, around a place, along a path, by
 * name, and the ones that stand for an object the map shows, found by its osm id, its wikidata or
 * its names and made into one [BaseDetailsObject].
 *
 * A copy of `AmenitySearcher` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS. The public transport stops are left out, with the model of public transport.
 */
class AmenitySearcher(private val mapPoiTypes: MapPoiTypes?) {

	class Request {

		internal val latLon: KLatLon?
		internal val osmId: Long?
		internal val type: EntityType?
		internal val wikidata: String?

		internal var names: Collection<String>?
		internal var checkOriginName = false
		internal var tags: Map<String, String>? = null
		internal var mainAmenityType: String? = null

		constructor(mapObject: MapObject) {
			osmId = ObfConstants.getOsmObjectId(mapObject)
			type = ObfConstants.getOsmEntityType(mapObject)
			tags = null
			mainAmenityType = null

			if (mapObject is Amenity) {
				latLon = mapObject.getLocation()
				wikidata = mapObject.getWikidata()
				val otherNames = ArrayList(mapObject.getOtherNames())
				otherNames.add(mapObject.getName())
				names = otherNames
				mainAmenityType = mapObject.getSubType()
			} else if (mapObject is RenderedObject) {
				latLon = mapObject.getLatLon()
				names = mapObject.getOriginalNames()
				wikidata = mapObject.getTagValue(WIKIDATA)
				tags = mapObject.getTags()
			} else if (mapObject is City || mapObject is Street || mapObject is Building) {
				latLon = mapObject.getLocation()
				wikidata = mapObject.getWikidata()
				names = emptyList()
			} else {
				latLon = mapObject.getLocation()
				wikidata = null
				names = mapObject.getOtherNames()
			}
		}

		constructor(mapObject: MapObject, names: List<String>?) : this(mapObject, names, false)

		constructor(mapObject: MapObject, names: List<String>?, checkOriginName: Boolean) : this(mapObject) {
			this.names = names
			this.checkOriginName = checkOriginName
		}

		constructor(names: List<String>?, latLon: KLatLon?, wikiDataId: String?, osmId: Long?, subType: String?) {
			this.type = EntityType.NODE
			this.names = names
			this.latLon = latLon
			this.wikidata = wikiDataId
			this.osmId = osmId
			this.mainAmenityType = subType
		}

		fun getMainAmenityType(): String? = mainAmenityType
	}

	data class Settings(
		val language: () -> String?,
		val transliterate: () -> Boolean,
		val fileVisibility: ((String) -> Boolean)?
	)

	private val amenityRepositories = ConcurrentMutableMap<String, AmenityIndexRepository>()
	private val executor = SerialExecutor()

	fun getAmenityRepositories(includeTravel: Boolean, travelFileVisibility: ((String) -> Boolean)?): List<AmenityIndexRepository> {
		val travelMaps = ArrayList<AmenityIndexRepository>()
		val baseMaps = ArrayList<AmenityIndexRepository>()
		val result = ArrayList<AmenityIndexRepository>()

		val fileNames = KAlgorithms.sortByFileVersions(amenityRepositories.block { ArrayList(it.keys) })

		for (fileName in fileNames) {
			val r = amenityRepositories[fileName]
			if (r != null && fileName.endsWith(BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)) {
				if (includeTravel && (travelFileVisibility == null || travelFileVisibility(fileName))) {
					travelMaps.add(r)
				}
			} else if (r != null && r.isWorldMap()) {
				baseMaps.add(r)
			} else if (r != null) {
				result.add(r)
			}
		}

		result.addAll(baseMaps)
		result.addAll(travelMaps)

		return result
	}

	fun searchAmenities(latLon: KLatLon, settings: Settings): List<Amenity> {
		val rect = KMapUtils.calculateLatLonBbox(latLon.latitude, latLon.longitude, AMENITY_SEARCH_RADIUS)
		return searchAmenities(SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, rect, true, settings.fileVisibility, null)
	}

	fun searchAmenities(
		filter: SearchPoiTypeFilter, rect: KQuadRect, includeTravel: Boolean,
		travelFileVisibility: ((String) -> Boolean)?, matcher: ResultMatcher<Amenity>?
	): List<Amenity> {
		return searchAmenities(filter, rect, includeTravel, travelFileVisibility, matcher, null)
	}

	fun searchWorldMapAmenities(
		filter: SearchPoiTypeFilter, rect: KQuadRect, includeTravel: Boolean,
		travelFileVisibility: ((String) -> Boolean)?, matcher: ResultMatcher<Amenity>?
	): List<Amenity> {
		return searchAmenities(filter, rect, includeTravel, travelFileVisibility, matcher) { it.isWorldMap() }
	}

	private fun searchAmenities(
		filter: SearchPoiTypeFilter, rect: KQuadRect, includeTravel: Boolean,
		travelFileVisibility: ((String) -> Boolean)?, matcher: ResultMatcher<Amenity>?,
		repositoryFilter: ((AmenityIndexRepository) -> Boolean)?
	): List<Amenity> {
		return searchAmenities(
			filter, null, rect.top, rect.left, rect.bottom, rect.right,
			-1, includeTravel, travelFileVisibility, matcher, repositoryFilter, null, -1
		)
	}

	fun searchAmenities(
		filter: SearchPoiTypeFilter, additionalFilter: SearchPoiAdditionalFilter?,
		topLatitude: Double, leftLongitude: Double, bottomLatitude: Double,
		rightLongitude: Double, zoom: Int, includeTravel: Boolean,
		travelFileVisibility: ((String) -> Boolean)?, matcher: ResultMatcher<Amenity>?
	): List<Amenity> {
		return searchAmenities(
			filter, additionalFilter, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
			zoom, includeTravel, travelFileVisibility, matcher, null, null, -1
		)
	}

	fun searchAmenities(
		filter: SearchPoiTypeFilter, additionalFilter: SearchPoiAdditionalFilter?,
		topLatitude: Double, leftLongitude: Double, bottomLatitude: Double,
		rightLongitude: Double, zoom: Int, includeTravel: Boolean,
		travelFileVisibility: ((String) -> Boolean)?, matcher: ResultMatcher<Amenity>?,
		repositoryFilter: ((AmenityIndexRepository) -> Boolean)?,
		comparator: Comparator<Amenity>?, searchResultsLimit: Int
	): List<Amenity> {
		val closedAmenities = HashSet<Long?>()
		var actualAmenities: MutableList<Amenity> = ArrayList()

		val isEmpty = filter.isEmpty()
		val poiTypeFilter = if (isEmpty && additionalFilter != null) null else filter
		// the capacity of a queue is at least one
		require(comparator == null || searchResultsLimit >= 1)
		val priorityQueue = if (comparator != null) KPriorityQueue(searchResultsLimit, comparator) else null
		if (!isEmpty || additionalFilter != null) {
			val top31 = KMapUtils.get31TileNumberY(topLatitude)
			val left31 = KMapUtils.get31TileNumberX(leftLongitude)
			val bottom31 = KMapUtils.get31TileNumberY(bottomLatitude)
			val right31 = KMapUtils.get31TileNumberX(rightLongitude)

			val repos = getAmenityRepositories(includeTravel, travelFileVisibility)

			val allIds = HashSet<Long?>() // live updates filter
			for (repo in repos) {
				if (matcher != null && matcher.isCancelled()) {
					break
				}
				if ((repositoryFilter == null || repositoryFilter(repo))
					&& repo.checkContainsInt(top31, left31, bottom31, right31)
				) {
					val foundAmenities = repo.searchAmenities(
						top31, left31, bottom31, right31,
						zoom, poiTypeFilter, additionalFilter, matcher, priorityQueue, searchResultsLimit
					)

					if (priorityQueue == null) {
						val localIds = HashSet<Long?>()
						for (amenity in foundAmenities) {
							val id = amenity.getId()
							if (amenity.isClosed()) {
								closedAmenities.add(id)
							} else if (!closedAmenities.contains(id) && !allIds.contains(id)) {
								actualAmenities.add(amenity)
								localIds.add(id)
							}
						}
						allIds.addAll(localIds)
					}
				}
			}
		}
		if (priorityQueue != null) {
			actualAmenities = ArrayList(priorityQueue.size())
			while (!priorityQueue.isEmpty()) {
				val am = priorityQueue.poll()!!
				val id = am.getId()
				if (am.isClosed()) {
					closedAmenities.add(id)
				} else if (!closedAmenities.contains(id)) {
					actualAmenities.add(am)
				}
			}
			actualAmenities.reverse()
		}

		return actualAmenities
	}

	fun searchDetailedAmenity(request: Request, settings: Settings): Amenity? {
		val detailed = searchDetailedObject(request, settings, null)
		return detailed?.getSyntheticAmenity()
	}

	fun searchDetailedObject(obj: Any?, settings: Settings): BaseDetailsObject? {
		var request: Request? = null
		if (obj is Request) {
			return searchDetailedObject(obj, settings, null)
		} else if (obj is MapObject) {
			request = Request(obj)
		} else if (obj is BaseDetailsObject) {
			if (obj.isObjectFull()) {
				completeGeometry(obj, obj.getObjects()[0])
				return obj
			}
			var searched: BaseDetailsObject? = null
			if (obj.getObjects().isNotEmpty()) {
				val first = obj.getObjects()[0]
				searched = searchDetailedObject(first, settings)
			}
			if (searched != null) {
				return searched
			} else if (obj.isObjectCombined()) {
				completeGeometry(obj, obj.getObjects()[0])
				return obj
			}
		}
		var detailsObject: BaseDetailsObject? = null
		if (request != null) {
			detailsObject = searchDetailedObject(request, settings, null)
		}
		completeGeometry(detailsObject, obj)
		return detailsObject
	}

	fun searchDetailedObject(request: Request, settings: Settings, matcher: ResultMatcher<Amenity>?): BaseDetailsObject? {
		val latLon = request.latLon
		if (latLon != null) {
			val searchRadius = if (request.type == EntityType.RELATION) AMENITY_SEARCH_RADIUS_FOR_RELATION else AMENITY_SEARCH_RADIUS
			val rect = KMapUtils.calculateLatLonBbox(latLon.latitude, latLon.longitude, searchRadius)

			val amenities = searchAmenities(SearchRequest.ACCEPT_ALL_POI_TYPE_FILTER, rect, true, settings.fileVisibility, matcher)

			val filtered = filterAmenities(amenities, request, settings)
			val type = request.getMainAmenityType()
			if (type != null) {
				filtered.sortWith { a1, a2 ->
					val m1 = a1.getSubType()!! == type
					val m2 = a2.getSubType()!! == type
					if (m1 == m2) {
						0
					} else if (m1) {
						-1
					} else {
						1
					}
				}
			}
			if (!KAlgorithms.isEmpty(filtered)) {
				return BaseDetailsObject(filtered, settings.language())
			}
		}
		return null
	}

	fun filterAmenities(amenities: Collection<Amenity>, request: Request, settings: Settings): MutableList<Amenity> {
		var filtered: MutableList<Amenity> = ArrayList()
		val latLon = request.latLon
		if (latLon != null) {
			val osmId = request.osmId
			val wikidata = request.wikidata
			if (osmId!! > 0 || wikidata != null) {
				filtered = filterByOsmIdOrWikidata(amenities, osmId, latLon, wikidata)
			}
			val names = request.names
			val checkOriginName = request.checkOriginName
			if (KAlgorithms.isEmpty(filtered) && !KAlgorithms.isEmpty(names)) {
				val amenity = findByName(amenities, names!!, latLon, settings, checkOriginName)
				if (amenity != null) {
					filtered = filterByOsmIdOrWikidata(
						amenities, amenity.getOsmId()!!, amenity.getLocation(), amenity.getWikidata()
					)
				}
			}
			val tags = request.tags
			if (KAlgorithms.isEmpty(filtered) && !KAlgorithms.isEmpty(tags)) {
				filtered = filterByLatLonAndType(amenities, latLon, tags!!)
			}
		}
		return filtered
	}

	private fun filterByOsmIdOrWikidata(amenities: Collection<Amenity>, id: Long, point: KLatLon?, wikidata: String?): MutableList<Amenity> {
		val result = ArrayList<Amenity>()
		var minDist = AMENITY_SEARCH_RADIUS_FOR_RELATION * 4.0
		for (amenity in amenities) {
			val initAmenityId = amenity.getId()
			if (initAmenityId != null) {
				val wiki = amenity.getWikidata()
				val wikiEqual = wiki != null && wiki == wikidata
				val amenityOsmId = amenity.getOsmId()
				val idEqual = amenityOsmId != null && amenityOsmId == id
				if ((idEqual || wikiEqual) && !amenity.isClosed()) {
					val dist = KMapUtils.getDistance(amenity.getLocation()!!, point!!)
					if (dist < minDist) {
						result.add(0, amenity) // to the top
						minDist = dist
					} else {
						result.add(amenity)
					}
				}
			}
		}
		return result
	}

	private fun filterByLatLonAndType(amenities: Collection<Amenity>, point: KLatLon, tags: Map<String, String>): MutableList<Amenity> {
		val result = ArrayList<Amenity>()
		for (amenity in amenities) {
			if (amenity.getLocation()!! == point) {
				val type = amenity.getSubType()
				for (v in tags.values) {
					if (type!! == v) {
						result.add(amenity)
						break
					}
				}
				break
			}
		}
		return result
	}

	private fun findByName(
		amenities: Collection<Amenity>, names: Collection<String>,
		searchLatLon: KLatLon, settings: Settings, checkOriginName: Boolean
	): Amenity? {
		if (!KAlgorithms.isEmpty(names) && !KAlgorithms.isEmpty(amenities)) {
			return amenities.sortedBy { KMapUtils.getDistance(it.getLocation()!!, searchLatLon) }
				.firstOrNull { !it.isClosed() && namesMatcher(it, names, settings, false, checkOriginName) }
				?: amenities.firstOrNull {
					!it.isClosed() && it.isRoutePoint() && it.getName().isEmpty()
							&& it.getAdditionalInfo("route_id").let { travelRouteId -> travelRouteId != null && names.contains(travelRouteId) }
				}
				?: amenities.firstOrNull { namesMatcher(it, names, settings, true, checkOriginName) }
		}
		return null
	}

	private fun namesMatcher(
		amenity: Amenity, matchList: Collection<String>, settings: Settings,
		matchAllLanguagesAndAltNames: Boolean, checkOriginName: Boolean
	): Boolean {
		val lang = settings.language()
		val transliterate = settings.transliterate()

		val poiSimpleFormat = Amenity.getPoiStringWithoutType(amenity, lang, transliterate)
		if (poiSimpleFormat != null && matchList.contains(poiSimpleFormat)) {
			return true
		}

		val amenityName = amenity.getName(lang, transliterate)
		if (!KAlgorithms.isEmpty(amenityName)) {
			for (match in matchList) {
				if (match.endsWith(amenityName)) {
					return true
				}
			}
		}

		if (mapPoiTypes != null) {
			val subType = amenity.getSubType()
			val st = if (subType == null) null else mapPoiTypes.getAnyPoiTypeByKey(subType)
			val poiTypeName = if (st != null) st.getTranslation() else subType
			if (poiTypeName != null && matchList.contains(poiTypeName)) {
				return true
			}
		}

		if (matchAllLanguagesAndAltNames) {
			val altNames = amenity.getAltNamesMap().values
			for (name in altNames) {
				if (matchList.contains(name)) {
					return true
				}
			}
			val primaryNames = amenity.getNamesMap(true).values
			for (name in primaryNames) {
				if (matchList.contains(name)) {
					return true
				}
			}
			val typeName = amenity.getSubTypeStr()
			if (!KAlgorithms.isEmpty(typeName)) {
				for (name in altNames) {
					if (matchList.contains("$typeName $name")) {
						return true
					}
				}
				for (name in primaryNames) {
					if (matchList.contains("$typeName $name")) {
						return true
					}
				}
			}
		}
		if (checkOriginName) {
			if (matchList.contains(amenity.toStringEn())) {
				return true
			}
		}
		return false
	}

	fun mergeAmenities(amenities: List<Amenity>?, settings: Settings): List<Amenity>? {
		if (amenities == null || amenities.size < 2) {
			return amenities
		}
		val lang = settings.language()
		val result = ArrayList<Amenity>(amenities.size)
		val osmMap = HashMap<Long, Amenity>(amenities.size)
		val wikiMap = HashMap<String, Amenity>(amenities.size)
		val redirects = Redirects()

		for (amenity in amenities) {
			if (amenity.isRouteTrack()) {
				result.add(amenity)
				continue
			}
			var osmId = amenity.getOsmId()
			osmId = if (osmId != null && osmId < 0) null else osmId

			var wikidata = amenity.getWikidata()
			wikidata = if (KAlgorithms.isEmpty(wikidata)) null else wikidata

			val byOsm = redirects.resolve(if (osmId != null) osmMap[osmId] else null)
			val byWiki = redirects.resolve(if (wikidata != null) wikiMap[wikidata] else null)

			if (byOsm == null && byWiki == null) {
				result.add(amenity)
				if (osmId != null) {
					osmMap[osmId] = amenity
				}
				if (wikidata != null) {
					wikiMap[wikidata] = amenity
				}
			} else {
				val target = byOsm ?: byWiki!!
				if (byOsm != null && byWiki != null && byOsm !== byWiki) {
					BaseDetailsObject.mergeAmenityData(byOsm, byWiki, lang, false)
					redirects.put(byWiki, byOsm)
				}
				BaseDetailsObject.mergeAmenityData(target, amenity, lang, false)

				if (osmId != null) {
					osmMap[osmId] = target
				}
				if (wikidata != null) {
					wikiMap[wikidata] = target
				}
			}
		}
		if (redirects.isEmpty()) {
			return result
		}
		val compact = ArrayList<Amenity>(result.size - redirects.size())
		for (a in result) {
			if (!redirects.containsKey(a)) {
				compact.add(a)
			}
		}
		return compact
	}

	/** Amenities merged into others, by identity: an amenity's equality is its id and name. */
	private class Redirects {
		private val from = ArrayList<Amenity>()
		private val to = ArrayList<Amenity>()

		fun isEmpty(): Boolean = from.isEmpty()

		fun size(): Int = from.size

		fun containsKey(amenity: Amenity): Boolean = indexOf(amenity) >= 0

		fun put(amenity: Amenity, target: Amenity) {
			val i = indexOf(amenity)
			if (i >= 0) {
				to[i] = target
			} else {
				from.add(amenity)
				to.add(target)
			}
		}

		fun resolve(amenity: Amenity?): Amenity? {
			var a = amenity
			while (a != null) {
				val i = indexOf(a)
				if (i < 0) {
					break
				}
				a = to[i]
			}
			return a
		}

		private fun indexOf(amenity: Amenity): Int {
			for (i in from.indices) {
				if (from[i] === amenity) {
					return i
				}
			}
			return -1
		}
	}

	fun addAmenityRepository(fileName: String, repository: AmenityIndexRepository) {
		amenityRepositories[fileName] = repository
	}

	/** The repositories as they are now, safe to walk while others are added and removed. */
	fun getAmenityRepositories(): Collection<AmenityIndexRepository> = amenityRepositories.block { ArrayList(it.values) }

	fun getAmenityRepository(fileName: String): AmenityIndexRepository? = amenityRepositories[fileName]

	fun removeAmenityRepository(fileName: String) {
		amenityRepositories.remove(fileName)
	}

	fun clearAmenityRepositories() {
		amenityRepositories.clear()
	}

	fun searchAmenitiesOnThePath(
		locations: List<KLocation>?, radius: Double, filter: SearchPoiTypeFilter, matcher: ResultMatcher<Amenity>?
	): List<Amenity> {
		val amenities = ArrayList<Amenity>()

		if (locations != null && locations.isNotEmpty()) {
			val repos = ArrayList<AmenityIndexRepository>()
			var topLatitude = locations[0].latitude
			var bottomLatitude = locations[0].latitude
			var leftLongitude = locations[0].longitude
			var rightLongitude = locations[0].longitude
			for (l in locations) {
				topLatitude = maxOf(topLatitude, l.latitude)
				bottomLatitude = minOf(bottomLatitude, l.latitude)
				leftLongitude = minOf(leftLongitude, l.longitude)
				rightLongitude = maxOf(rightLongitude, l.longitude)
			}
			if (!filter.isEmpty()) {
				for (index in getAmenityRepositories()) {
					if (index.checkContainsInt(
							KMapUtils.get31TileNumberY(topLatitude),
							KMapUtils.get31TileNumberX(leftLongitude),
							KMapUtils.get31TileNumberY(bottomLatitude),
							KMapUtils.get31TileNumberX(rightLongitude)
						)
					) {
						repos.add(index)
					}
				}
				if (repos.isNotEmpty()) {
					for (r in repos) {
						val res = r.searchAmenitiesOnThePath(locations, radius, filter, matcher)
						amenities.addAll(res)
					}
				}
			}
		}

		return amenities
	}

	private fun searchRouteByName(multipleSearch: String, mode: KStringMatcherMode, matcher: ResultMatcher<Amenity>?): List<Amenity> {
		val result = ArrayList<Amenity>()
		val req = SearchRequest.buildSearchPoiRequest(
			0, 0, multipleSearch, 0, Int.MAX_VALUE, 0, Int.MAX_VALUE, null, matcher
		)
		req.setMatcherMode(mode)
		for (index in getAmenityRepositories(false, null)) {
			val amenities = index.searchPoiByName(req)
			if (!KAlgorithms.isEmpty(amenities)) {
				result.addAll(amenities)
			}
		}
		return result
	}

	fun searchRoutePartOf(routeId: String): List<Amenity> {
		val matcher = object : ResultMatcher<Amenity> {
			override fun publish(obj: Amenity): Boolean {
				val members = obj.getAdditionalInfo(Amenity.ROUTE_MEMBERS_IDS)
				if (members != null) {
					val ids = HashSet(WorldRegion.splitLikeJava(members, " "))
					return ids.contains(routeId)
				}
				return false
			}

			override fun isCancelled(): Boolean = false
		}
		return searchRouteByName(routeId, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE, matcher)
	}

	fun searchRouteMembers(multipleSearch: String): Map<String, List<Amenity>?> {
		val ids = HashSet(WorldRegion.splitLikeJava(multipleSearch, " "))
		val matcher = object : ResultMatcher<Amenity> {
			override fun publish(obj: Amenity): Boolean {
				val routeId = obj.getAdditionalInfo(Amenity.ROUTE_ID)
				return routeId != null && ids.contains(routeId)
			}

			override fun isCancelled(): Boolean = false
		}

		val map = HashMap<String, MutableList<Amenity>?>()
		val result = searchRouteByName(multipleSearch, KStringMatcherMode.MULTISEARCH, matcher)
		for (am in result) {
			val routeId = am.getAdditionalInfo(Amenity.ROUTE_ID)!!
			val amenities = map.getOrPut(routeId) { ArrayList() }!!
			amenities.add(am)
		}
		for (id in ids) {
			if (!map.containsKey(id)) {
				map[id] = null
			}
		}
		return map
	}

	fun searchAmenitiesByName(
		searchQuery: String,
		topLatitude: Double, leftLongitude: Double, bottomLatitude: Double, rightLongitude: Double,
		lat: Double, lon: Double, matcher: ResultMatcher<Amenity>?
	): List<Amenity> {
		val amenities = ArrayList<Amenity>()
		val list = ArrayList<AmenityIndexRepository>()
		val left = KMapUtils.get31TileNumberX(leftLongitude)
		val top = KMapUtils.get31TileNumberY(topLatitude)
		val right = KMapUtils.get31TileNumberX(rightLongitude)
		val bottom = KMapUtils.get31TileNumberY(bottomLatitude)
		for (index in getAmenityRepositories(false, null)) {
			if (matcher != null && matcher.isCancelled()) {
				break
			}
			if (index.checkContainsInt(top, left, bottom, right)) {
				if (index.checkContains(lat, lon)) {
					list.add(0, index)
				} else {
					list.add(index)
				}
			}
		}

		for (index in list) {
			if (matcher != null && matcher.isCancelled()) {
				break
			}
			val result = index.searchAmenitiesByName(
				KMapUtils.get31TileNumberX(lon), KMapUtils.get31TileNumberY(lat),
				left, top, right, bottom,
				searchQuery, matcher
			)
			amenities.addAll(result)
		}

		return amenities
	}

	private fun searchBinaryMapDataForAmenity(amenity: Amenity, limit: Int): List<BinaryMapDataObject> {
		val osmId = ObfConstants.getOsmObjectId(amenity)
		val checkId = osmId > 0
		val wikidata = amenity.getWikidata()
		val checkWikidata = !KAlgorithms.isEmpty(wikidata)
		val routeId = amenity.getRouteId()
		val checkRouteId = !KAlgorithms.isEmpty(routeId)

		val matcher = object : ResultMatcher<BinaryMapDataObject> {
			override fun publish(obj: BinaryMapDataObject): Boolean {
				if (checkId && osmId == ObfConstants.getOsmObjectId(obj)) {
					return true
				}
				if (checkWikidata) {
					val names = obj.getObjectNames()
					return names != null && !names.isEmpty() && names.values().contains(wikidata!!)
				}
				if (checkRouteId) {
					val names = obj.getObjectNames()
					return names != null && !names.isEmpty() && names.values().contains(routeId!!)
				}
				return false
			}

			override fun isCancelled(): Boolean = false
		}
		return searchBinaryMapDataObjects(amenity.getLocation()!!, matcher, limit)
	}

	private fun searchBinaryMapDataObjects(
		latLon: KLatLon, matcher: ResultMatcher<BinaryMapDataObject>?, limit: Int
	): List<BinaryMapDataObject> {
		val list = ArrayList<BinaryMapDataObject>()

		val y = KMapUtils.get31TileNumberY(latLon.latitude)
		val x = KMapUtils.get31TileNumberX(latLon.longitude)

		val request = SearchRequest.buildSearchRequest(x, x + 1, y, y + 1, 15, null, object : ResultMatcher<BinaryMapDataObject> {
			override fun publish(obj: BinaryMapDataObject): Boolean {
				if (obj.isDeleted()) {
					return false
				}
				if (matcher == null || matcher.publish(obj)) {
					list.add(obj)
					return true
				}
				return false
			}

			override fun isCancelled(): Boolean =
				matcher != null && matcher.isCancelled() || limit != -1 && list.size == limit
		})

		for (repository in getAmenityRepositories(false, null)) {
			if (matcher != null && matcher.isCancelled()) {
				break
			}
			if (repository.isPoiSectionIntersects(request)) {
				repository.searchMapIndex(request)
			}
		}
		return list
	}

	fun searchDetailedAmenityAsync(request: Request, settings: Settings, callbackWithAmenity: (Amenity?) -> Boolean) {
		executor.submit {
			val amenity = searchDetailedAmenity(request, settings)
			callbackWithAmenity(amenity)
		}
	}

	fun searchBaseDetailedObjectAsync(
		renderedObject: RenderedObject, settings: Settings,
		callback: (BaseDetailsObject?) -> Boolean, matcher: ResultMatcher<Amenity>?
	) {
		val latLon = renderedObject.getLatLon()
		if (latLon == null) {
			callback(null)
			return
		}
		executor.submit {
			val request = Request(renderedObject)
			val detailsObject = searchDetailedObject(request, settings, matcher)
			if (detailsObject != null) {
				detailsObject.addObject(renderedObject)

				val amenity = detailsObject.getSyntheticAmenity()
				if (detailsObject.getPointsLength() < renderedObject.getX().size()) {
					amenity.setX(renderedObject.getX())
					amenity.setY(renderedObject.getY())
				}
			}
			callback(detailsObject)
		}
	}

	fun searchDetailedObjectAsync(obj: Any?, settings: Settings, callback: (Any?) -> Boolean) {
		executor.submit {
			val fetched = searchDetailedObject(obj, settings)
			callback(fetched ?: obj)
		}
	}

	private fun completeGeometry(detailsObject: BaseDetailsObject?, obj: Any?) {
		if (detailsObject == null) {
			return
		}
		var xx: KTIntArrayList? = null
		var yy: KTIntArrayList? = null
		if (obj is Amenity) {
			xx = obj.getX()
			yy = obj.getY()
		}
		if (obj is RenderedObject) {
			xx = obj.getX()
			yy = obj.getY()
		}
		if (obj is BaseDetailsObject) {
			xx = obj.getSyntheticAmenity().getX()
			yy = obj.getSyntheticAmenity().getY()
		}
		if (xx != null && yy != null && !xx.isEmpty()) {
			detailsObject.setX(xx)
			detailsObject.setY(yy)
		} else {
			val dataObjects = searchBinaryMapDataForAmenity(detailsObject.getSyntheticAmenity(), 1)
			for (dataObject in dataObjects) {
				if (copyCoordinates(detailsObject, dataObject)) {
					break
				}
			}
		}
	}

	private fun copyCoordinates(detailsObject: BaseDetailsObject, mapObject: BinaryMapDataObject): Boolean {
		val pointsLength = mapObject.getPointsLength()
		if (pointsLength > 2) {
			detailsObject.clearGeometry()
			for (i in 0 until pointsLength) {
				detailsObject.addX(mapObject.getPoint31XTile(i))
				detailsObject.addY(mapObject.getPoint31YTile(i))
			}
		}
		return pointsLength > 0
	}

	companion object {
		const val AMENITY_SEARCH_RADIUS = 50
		const val AMENITY_SEARCH_RADIUS_FOR_RELATION = 500
	}
}
