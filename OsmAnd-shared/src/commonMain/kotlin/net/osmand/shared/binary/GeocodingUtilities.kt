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

		// justification
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
		rp.findRouteSegment(lat, lon, ctx, listR, false, true)
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

	private fun matchStreetName(s1: String?, s2: String?, matchWithCommonWords: Boolean): Boolean {
		if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
			return false
		}
		if (s1 == s2) {
			return true
		}

		// Strip dashes before split to match "NC 42" == "NC-42"
		val undashed1 = s1.replace("-", " ")
		val undashed2 = s2.replace("-", " ")

		// sorted to compare the words whatever their order
		var s1words = prepareStreetName(undashed1, false)
		var s2words = prepareStreetName(undashed2, false)
		s1words.sort()
		s2words.sort()
		if (s1words.isNotEmpty() && s1words == s2words) {
			return true
		}

		if (matchWithCommonWords) {
			s1words = prepareStreetName(undashed1, true)
			s2words = prepareStreetName(undashed2, true)
			s1words.sort()
			s2words.sort()
			return s1words.isNotEmpty() && s1words == s2words
		}

		return false
	}

	fun justifyReverseGeocodingSearch(
		road: GeocodingResult, reader: BinaryMapIndexReader,
		knownMinBuildingDistance: Double, result: ResultMatcher<GeocodingResult>?
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
					override fun publish(obj: MapObject): Boolean {
						if (obj is Street && matchStreetName(road.streetName, obj.getName(), addCommonWordsFinal)) {
							val searchPoint = road.searchPoint!!
							val d = KMapUtils.getDistance(obj.getLocation()!!, searchPoint.latitude, searchPoint.longitude)
							// double check to support old format
							if (d < DISTANCE_STREET_NAME_PROXIMITY_BY_NAME) {
								val rs = GeocodingResult(road)
								rs.street = obj
								// set connection point to sort
								rs.connectionPoint = obj.getLocation()
								rs.city = obj.getCity()
								rs.dist = d
								streetsList.add(rs)
								return true
							}
							return false
						}
						return false
					}

					override fun isCancelled(): Boolean = result != null && result.isCancelled()
				}, longestWord, KStringMatcherMode.CHECK_EQUALS_FROM_SPACE
			)
			val location = road.getLocation()!!
			req.setBBoxRadius(location.latitude, location.longitude, DISTANCE_STREET_NAME_PROXIMITY_BY_NAME)
			reader.searchAddressDataByName(req)
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
				val streetBuildings = loadStreetBuildings(road, reader, street)
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
		val eqStreet = KAlgorithms.stringsEqual(gr1.streetName, gr2.streetName)
		if (eqStreet) {
			var sameObj = false
			val building1 = gr1.building
			val building2 = gr2.building
			if (gr1.city != null && gr2.city != null) {
				if (building1 != null && building2 != null) {
					if (KAlgorithms.stringsEqual(building1.getName(), building2.getName())) {
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
				return if (cityDist1 < cityDist2) {
					-1
				} else {
					1
				}
			}
		}
		return 0
	}

	private fun loadStreetBuildings(road: GeocodingResult, reader: BinaryMapIndexReader, street: GeocodingResult): MutableList<GeocodingResult> {
		val streetBuildings = ArrayList<GeocodingResult>()
		val s = street.street!!
		val searchPoint = road.searchPoint!!
		reader.preloadBuildings(s, null)
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

	fun sortGeocodingResults(list: List<BinaryMapIndexReader>, res: List<GeocodingResult>): MutableList<GeocodingResult> {
		val complete = ArrayList<GeocodingResult>()
		var minBuildingDistance = 0.0
		for (r in res) {
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
				val justified = justifyReverseGeocodingSearch(r, reader, minBuildingDistance, null)
				if (justified.isNotEmpty()) {
					val md = justified[0].getDistance()
					minBuildingDistance = if (minBuildingDistance == 0.0) {
						md
					} else {
						min(md, minBuildingDistance)
					}
					justified[0].dist = -1.0 //clear intermediate cached distance
					complete.addAll(justified)
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
