package net.osmand.shared.binary

import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.routing.RouteCalculationMode
import net.osmand.shared.routing.RoutePlannerFrontEnd
import net.osmand.shared.routing.RouteRegion
import net.osmand.shared.routing.RouteSegmentPoint
import net.osmand.shared.routing.RoutingConfiguration
import net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits
import net.osmand.shared.routing.RoutingContext
import net.osmand.shared.search.core.SearchPhrase
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Reverse geocoding: the roads near a point, by name, then the streets of those names in the
 * address section and the buildings on them.
 *
 * A copy of `GeocodingUtilities` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS.
 */
class GeocodingUtilities {

	companion object {
		// Location to test parameters https://www.openstreetmap.org/#map=18/53.896473/27.540071 (hno 44)
		// BUG https://www.openstreetmap.org/#map=19/50.9356/13.35348 (hno 26) street is
		const val THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER = 5f
		const val STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS = 250f
		const val STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS = 400f

		const val DISTANCE_STREET_NAME_PROXIMITY_BY_NAME = 15000
		const val DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME = 7500f

		const val THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER = 1.5f
		const val DISTANCE_BUILDING_PROXIMITY = 100f

		@JvmField
		var GEOCODING_POI_MEMORY = 512

		@JvmField
		val DISTANCE_COMPARATOR: Comparator<GeocodingResult> = Comparator { o1, o2 ->
			if (o1.getDistance().toInt() == o2.getDistance().toInt()) {
				o1.getCityDistance().compareTo(o2.getCityDistance())
			} else {
				o1.getDistance().compareTo(o2.getDistance())
			}
		}

		@JvmStatic
		fun buildDefaultContextForPOI(index: BinaryMapIndexReader): RoutingContext {
			val geocoding = BinaryMapIndexReader(index.getFile())
			val memoryLimit = RoutingMemoryLimits(GEOCODING_POI_MEMORY, GEOCODING_POI_MEMORY)
			val config = RoutingConfiguration.getDefault().build("car", memoryLimit)
			return RoutePlannerFrontEnd().buildRoutingContext(config, listOf(geocoding), RouteCalculationMode.NORMAL)
		}
	}

	class GeocodingResult {

		constructor()

		constructor(r: GeocodingResult) {
			this.searchPoint = r.searchPoint
			this.regionFP = r.regionFP
			this.regionLen = r.regionLen
			this.connectionPoint = r.connectionPoint
			this.streetName = r.streetName
			this.point = r.point
			this.building = r.building
			this.city = r.city
			this.street = r.street
		}

		// input
		@JvmField
		var searchPoint: KLatLon? = null

		// 1st step
		@JvmField
		var connectionPoint: KLatLon? = null

		@JvmField
		var regionFP: Long = 0

		@JvmField
		var regionLen: Long = 0

		@JvmField
		var point: RouteSegmentPoint? = null

		@JvmField
		var streetName: String? = null

		// 2nd step, findStreetAndBuildings: the street, the building and the city
		@JvmField
		var building: Building? = null

		@JvmField
		var buildingInterpolation: String? = null

		@JvmField
		var street: Street? = null

		@JvmField
		var city: City? = null

		internal var dist = -1.0
		private var cityDist = -1.0

		fun getLocation(): KLatLon? = connectionPoint

		fun getSortDistance(): Double {
			val dist = getDistance()
			if (dist > 0 && building == null) {
				// add extra distance to match buildings first
				return dist + 50
			}
			return dist
		}

		fun getDistance(): Double {
			val searchPoint = searchPoint
			if (dist == -1.0 && searchPoint != null) {
				val connectionPoint = connectionPoint
				val point = point
				if (connectionPoint != null) {
					dist = KMapUtils.getDistance(connectionPoint, searchPoint)
				} else if (building == null && point != null) {
					// Need distance between searchPoint and nearest RouteSegmentPoint here, to approximate distance from neareest named road
					dist = sqrt(point.distToProj)
				}
			}
			return dist
		}

		fun getBuildingString(): String? {
			val building = building
			if (building != null) {
				return buildingInterpolation ?: building.getName()
			}
			return null
		}

		fun resetDistance() {
			dist = -1.0
			getDistance()
		}

		fun getCityDistance(): Double {
			val city = city
			val searchPoint = searchPoint
			if (cityDist == -1.0 && city != null && searchPoint != null) {
				cityDist = KMapUtils.getDistance(city.getLocation()!!, searchPoint)
			}
			return cityDist
		}

		override fun toString(): String {
			val bld = StringBuilder()
			val bldStr = getBuildingString()
			if (bldStr != null) {
				bld.append(bldStr)
			}
			val street = street
			if (street != null) {
				bld.append(" str. ").append(street.getName()).append(" city ").append(city!!.getName())
			} else if (streetName != null) {
				bld.append(" str. ").append(streetName)
			} else if (city != null) {
				bld.append(" city ").append(city!!.getName())
			}
			if (getDistance() > 0) {
				bld.append(" dist=").append(getDistance().toInt())
			}
			return bld.toString()
		}
	}

	fun reverseGeocodingSearch(ctx: RoutingContext, lat: Double, lon: Double, allowEmptyNames: Boolean): MutableList<GeocodingResult> {
		val rp = RoutePlannerFrontEnd()
		val lst = ArrayList<GeocodingResult>()
		val listR = ArrayList<RouteSegmentPoint>()
		// we allow duplications to search in both files for boundary regions
		// here we use same code as for normal routing, so we take into account current profile and sort by priority & distance
		val best = rp.findRouteSegment(lat, lon, ctx, listR, false, true)
		if (best != null) {
			best.others = null
			listR.add(0, best)
		}
		var distSquare = 0.0
		val streetNames = HashMap<String, MutableList<RouteRegion>>()
		for (p in listR) {
			val road = p.getRoad()
			val name = if (KAlgorithms.isEmpty(road.getName())) road.getRef("", false, true) else road.getName()
			if (allowEmptyNames || !KAlgorithms.isEmpty(name)) {
				if (distSquare == 0.0 || distSquare > p.distToProj) {
					distSquare = p.distToProj
				}
				val sr = GeocodingResult()
				sr.searchPoint = KLatLon(lat, lon)
				val streetName = name ?: ""
				sr.streetName = streetName
				sr.point = p
				sr.connectionPoint = KLatLon(KMapUtils.get31LatitudeY(p.preciseY), KMapUtils.get31LongitudeX(p.preciseX))
				val region = road.region!!
				sr.regionFP = region.getFilePointer()
				sr.regionLen = region.getLength()
				val plst = streetNames.getOrPut(streetName) { ArrayList() }
				if (!plst.contains(region)) {
					plst.add(region)
					lst.add(sr)
				}
			}
			if (p.distToProj > STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS * STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS &&
				distSquare != 0.0 && p.distToProj > THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER * distSquare) {
				break
			}
			if (p.distToProj > STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS * STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS) {
				break
			}
		}
		lst.sortWith(DISTANCE_COMPARATOR)
		return lst
	}

	private fun prepareStreetName(streetName: String?, includeCommonWords: Boolean): MutableList<String> {
		val words = ArrayList<String>()
		// "Tempelhofer Damm" == "Tempelhofer Damm (Tempelhof-Schöneberg)"
		for (word in KSearchAlgorithms.splitAndNormalize(SearchPhrase.stripBraces(streetName)!!, true)) {
			if (!KAlgorithms.isEmpty(word) && (includeCommonWords || CommonWords.getInstance().getCommonGeocoding(word) == -1)) {
				words.add(word)
			}
		}
		return words // keep original order ("NC 42" - search by "NC" not by "42")
	}

	private fun matchStreetName(s1: String?, s2: String?, matchWithCommonWords: Boolean, cache: StreetsCache?): Boolean {
		if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
			return false
		}
		if (s1 == s2) {
			return true
		}

		var s1words = sortedWords(s1, false, cache)
		if (s1words.isNotEmpty() && s1words == sortedWords(s2, false, cache)) {
			return true
		}

		if (matchWithCommonWords) {
			s1words = sortedWords(s1, true, cache)
			return s1words.isNotEmpty() && s1words == sortedWords(s2, true, cache)
		}

		return false
	}

	private fun sortedWords(name: String, includeCommonWords: Boolean, cache: StreetsCache?): List<String> {
		val key = if (includeCommonWords) "+$name" else "-$name"
		val cached = cache?.words?.get(key)
		if (cached != null) {
			return cached
		}
		// Strip dashes before split to match "NC 42" == "NC-42"
		val words = prepareStreetName(name.replace("-", " "), includeCommonWords)
		// the sort only brings both word lists to one order before equals(), plain string order is enough
		words.sort()
		cache?.words?.put(key, words)
		return words
	}

	// the street with the name of the road in the address index and the buildings on it within
	// DISTANCE_BUILDING_PROXIMITY of the point: buildings first, the street after them, the road
	// itself when no street matches
	fun findStreetAndBuildings(
		road: GeocodingResult, reader: BinaryMapIndexReader,
		knownMinBuildingDistance: Double, result: ResultMatcher<GeocodingResult>?
	): MutableList<GeocodingResult> = findStreetAndBuildings(road, reader, knownMinBuildingDistance, result, null)

	// the roads of one lookup lie within a few hundred metres of the point and many share the query word
	// ("calle"): the name index is searched once per word around the point, and every street has its
	// buildings loaded once
	private class StreetsCache {

		companion object {
			const val MARGIN = 1000
		}

		private data class StreetsKey(val reader: BinaryMapIndexReader, val radius: Int, val word: String)

		private val streets = HashMap<StreetsKey, List<Street>>()

		// by identity: a street equals any street of its id
		private val withBuildings = ArrayList<Street>()

		val words = HashMap<String, List<String>>()

		fun getStreets(
			reader: BinaryMapIndexReader, word: String, radius: Int, point: KLatLon,
			result: ResultMatcher<GeocodingResult>?
		): List<Street> {
			val key = StreetsKey(reader, radius, word)
			val cached = streets[key]
			if (cached != null) {
				return cached
			}
			val found = ArrayList<Street>()
			val req = SearchRequest.buildAddressByNameRequest(
				object : ResultMatcher<MapObject> {
					override fun publish(obj: MapObject): Boolean {
						if (obj is Street) {
							found.add(obj)
						}
						return false
					}

					override fun isCancelled(): Boolean = result != null && result.isCancelled()
				}, word, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE
			)
			req.setBBoxRadius(point.latitude, point.longitude, radius + MARGIN)
			reader.searchAddressDataByName(req)
			streets[key] = found
			return found
		}

		fun addWithBuildings(street: Street): Boolean {
			if (withBuildings.any { it === street }) {
				return false
			}
			withBuildings.add(street)
			return true
		}
	}

	private fun findStreetAndBuildings(
		road: GeocodingResult, reader: BinaryMapIndexReader,
		knownMinBuildingDistance: Double, result: ResultMatcher<GeocodingResult>?, cache: StreetsCache?
	): MutableList<GeocodingResult> {
		var knownMinBuildingDistance = knownMinBuildingDistance
		val streetsList = ArrayList<GeocodingResult>()

		var streetNamesUsed = prepareStreetName(road.streetName, false)

		var addCommonWords = false
		for (word in streetNamesUsed) {
			if (KSearchAlgorithms.isNumber2Letters(word)) {
				addCommonWords = true // 1-я Цэнтральная вуліца
				break
			}
		}

		if (streetNamesUsed.isEmpty() || addCommonWords) {
			streetNamesUsed = prepareStreetName(road.streetName, true)
			addCommonWords = true
		}

		val addCommonWordsFinal = addCommonWords
		if (streetNamesUsed.isNotEmpty()) {
			var longestWord = ""
			for (i in streetNamesUsed.indices) {
				val s = streetNamesUsed[i]
				if (s.length > longestWord.length) {
					longestWord = s
				}
			}
			val req = SearchRequest.buildAddressByNameRequest(
				object : ResultMatcher<MapObject> {
					override fun publish(obj: MapObject): Boolean =
						obj is Street && addStreet(road, obj, addCommonWordsFinal, streetsList, null)

					override fun isCancelled(): Boolean = result != null && result.isCancelled()
				}, longestWord, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE
			)
			val location = road.getLocation()!!
			req.setBBoxRadius(location.latitude, location.longitude, DISTANCE_STREET_NAME_PROXIMITY_BY_NAME)
			if (cache == null) {
				reader.searchAddressDataByName(req)
			} else {
				// the same box test the name index does, on the zoom 16 tile of the street
				for (street in cache.getStreets(reader, longestWord, DISTANCE_STREET_NAME_PROXIMITY_BY_NAME, road.searchPoint!!, result)) {
					val streetLocation = street.getLocation()!!
					val x16 = (KMapUtils.get31TileNumberX(streetLocation.longitude) shr 15) shl 15
					val y16 = (KMapUtils.get31TileNumberY(streetLocation.latitude) shr 15) shl 15
					if (req.contains(x16, y16, x16, y16)) {
						addStreet(road, street, addCommonWordsFinal, streetsList, cache)
					}
				}
			}
		}

		val res = ArrayList<GeocodingResult>()
		if (streetsList.size == 0) {
			res.add(road)
		} else {
			streetsList.sortWith(DISTANCE_COMPARATOR)
			var streetDistance = 0.0
			var isBuildingFound = knownMinBuildingDistance > 0
			for (street in streetsList) {
				if (streetDistance == 0.0) {
					streetDistance = street.getDistance()
				} else if (isBuildingFound && street.getDistance() > streetDistance + DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME) {
					continue
				}
				street.resetDistance() //reset to road projection
				street.connectionPoint = road.connectionPoint
				val streetBuildings = loadStreetBuildings(road, reader, street, cache)
				streetBuildings.sortWith(DISTANCE_COMPARATOR)
				if (streetBuildings.size > 0) {
					val it = streetBuildings.iterator()
					if (knownMinBuildingDistance == 0.0) {
						val firstBld = it.next()
						knownMinBuildingDistance = firstBld.getDistance()
						isBuildingFound = true
						res.add(firstBld)
					}
					while (it.hasNext()) {
						val nextBld = it.next()
						if (nextBld.getDistance() > knownMinBuildingDistance * THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER) {
							break
						}
						res.add(nextBld)
					}
				}
				res.add(street)
			}
		}
		res.sortWith(DISTANCE_COMPARATOR)
		return res
	}

	fun filterDuplicateRegionResults(res: MutableList<GeocodingResult>) {
		res.sortWith(DISTANCE_COMPARATOR)
		// filter duplicate city results (when building is in both regions on boundary)
		var i = 0
		while (i < res.size - 1) {
			val cmp = cmpResult(res[i], res[i + 1])
			if (cmp > 0) {
				res.removeAt(i)
			} else if (cmp < 0) {
				res.removeAt(i + 1)
			} else {
				// nothing to delete
				i++
			}
		}
	}

	private fun cmpResult(gr1: GeocodingResult, gr2: GeocodingResult): Int {
		// the same street, or the same house number on it, from two maps or from two cities of one map;
		// the street name too (not only the name of the road it was found for), the nearer one on equal cities
		val street1 = gr1.street
		val street2 = gr2.street
		val eqStreet = KAlgorithms.stringsEqual(gr1.streetName, gr2.streetName) && street1 != null
				&& street2 != null && KAlgorithms.stringsEqual(street1.getName(), street2.getName())
		if (eqStreet) {
			var sameObj = false
			val building1 = gr1.building
			val building2 = gr2.building
			if (gr1.city != null && gr2.city != null) {
				if (building1 != null && building2 != null) {
					if (KAlgorithms.stringsEqual(gr1.getBuildingString(), gr2.getBuildingString())) {
						// same building
						sameObj = true
					}
				} else if (building1 == null && building2 == null) {
					// same street
					sameObj = true
				}
			}
			if (sameObj) {
				val cityDist1 = KMapUtils.getDistance(gr1.searchPoint!!, gr1.city!!.getLocation()!!)
				val cityDist2 = KMapUtils.getDistance(gr2.searchPoint!!, gr2.city!!.getLocation()!!)
				return if (cityDist1 <= cityDist2) {
					-1
				} else {
					1
				}
			}
		}
		return 0
	}

	private fun loadStreetBuildings(
		road: GeocodingResult, reader: BinaryMapIndexReader, street: GeocodingResult, cache: StreetsCache?
	): MutableList<GeocodingResult> {
		val streetBuildings = ArrayList<GeocodingResult>()
		val s = street.street!!
		val searchPoint = road.searchPoint!!
		if (cache == null || cache.addWithBuildings(s)) {
			reader.preloadBuildings(s, null)
		}
		for (b in s.getBuildings()) {
			val latLon2 = b.getLatLon2()
			val location = b.getLocation()!!
			if (latLon2 != null) {
				val slat = location.latitude
				val slon = location.longitude
				val tolat = latLon2.latitude
				val tolon = latLon2.longitude
				val coeff = KMapUtils.getProjectionCoeff(searchPoint.latitude, searchPoint.longitude, slat, slon, tolat, tolon)
				val plat = slat + (tolat - slat) * coeff
				val plon = slon + (tolon - slon) * coeff
				if (KMapUtils.getDistance(searchPoint, plat, plon) < DISTANCE_BUILDING_PROXIMITY) {
					val bld = GeocodingResult(street)
					bld.building = b
					//bld.connectionPoint = b.getLocation();
					bld.connectionPoint = KLatLon(plat, plon)
					streetBuildings.add(bld)
					val nm = b.getInterpolationName(coeff)
					if (!KAlgorithms.isEmpty(nm)) {
						bld.buildingInterpolation = nm
					}
				}
			} else if (KMapUtils.getDistance(location, searchPoint) < DISTANCE_BUILDING_PROXIMITY) {
				val bld = GeocodingResult(street)
				bld.building = b
				bld.connectionPoint = location
				streetBuildings.add(bld)
			}
		}
		return streetBuildings
	}

	private fun addStreet(
		road: GeocodingResult, street: Street, matchWithCommonWords: Boolean,
		streetsList: MutableList<GeocodingResult>, cache: StreetsCache?
	): Boolean {
		// "<Segré>" holds the houses of a place without a street, it is not a street named after the place
		if (street.getName().startsWith("<")) {
			return false
		}
		if (!matchStreetName(road.streetName, street.getName(), matchWithCommonWords, cache)) {
			return false
		}
		val searchPoint = road.searchPoint!!
		val d = KMapUtils.getDistance(street.getLocation()!!, searchPoint.latitude, searchPoint.longitude)
		// double check to support old format
		if (d >= DISTANCE_STREET_NAME_PROXIMITY_BY_NAME) {
			return false
		}
		val rs = GeocodingResult(road)
		rs.street = street
		// set connection point to sort
		rs.connectionPoint = street.getLocation()
		rs.city = street.getCity()
		rs.dist = d
		streetsList.add(rs)
		return true
	}

	fun findAddresses(list: List<BinaryMapIndexReader>, res: List<GeocodingResult>): MutableList<GeocodingResult> =
		findAddresses(list, res, null)

	// the addresses of the roads found by reverseGeocodingSearch: the street and the buildings of
	// every road, duplicates from neighbouring maps and buildings far behind the nearest dropped,
	// nearest first; stops early when the matcher is cancelled
	fun findAddresses(
		list: List<BinaryMapIndexReader>, res: List<GeocodingResult>, cancel: ResultMatcher<GeocodingResult>?
	): MutableList<GeocodingResult> {
		val complete = ArrayList<GeocodingResult>()
		var minBuildingDistance = 0.0
		val cache = StreetsCache()
		for (r in res) {
			if (cancel != null && cancel.isCancelled()) {
				break
			}
			var reader: BinaryMapIndexReader? = null
			for (b in list) {
				for (rb in b.getRoutingIndexes()) {
					if (r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
						reader = b
						break
					}
				}
				if (reader != null) {
					break
				}
			}
			if (reader != null) {
				val streetAndBuildings = findStreetAndBuildings(r, reader, minBuildingDistance, cancel, cache)
				if (streetAndBuildings.isNotEmpty()) {
					val md = streetAndBuildings[0].getDistance()
					minBuildingDistance = if (minBuildingDistance == 0.0) {
						md
					} else {
						min(md, minBuildingDistance)
					}
					streetAndBuildings[0].dist = -1.0 //clear intermediate cached distance
					complete.addAll(streetAndBuildings)
				}
			} else {
				complete.add(r)
			}
		}
		filterDuplicateRegionResults(complete)
		val it = complete.iterator()
		while (it.hasNext()) {
			val r = it.next()
			if (r.building != null && r.getDistance() > minBuildingDistance * THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER) {
				it.remove()
			}
		}
		complete.sortWith(DISTANCE_COMPARATOR)
		return complete
	}
}
