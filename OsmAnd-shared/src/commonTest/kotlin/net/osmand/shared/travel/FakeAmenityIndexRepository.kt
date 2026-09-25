package net.osmand.shared.travel

import net.osmand.shared.binary.AmenityIndexRepository
import net.osmand.shared.binary.BinaryMapDataObject
import net.osmand.shared.binary.PoiRegion
import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.binary.SearchRequest
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLocation
import net.osmand.shared.io.KFile
import net.osmand.shared.util.collections.KPriorityQueue

/**
 * One obf file made of lists instead of bytes, so that the helper and the gpx builder can be
 * tested without one. It applies the type filter and the name query of a request and then hands
 * every candidate over; the box and the tree are the reader's business and are covered against
 * real files by the compat tests.
 */
class FakeAmenityIndexRepository(
	private val file: KFile,
	private val amenities: List<Amenity> = emptyList(),
	private val mapObjects: List<BinaryMapDataObject> = emptyList(),
	private val regions: List<PoiRegion> = listOf(wholeWorldRegion()),
	private val worldMap: Boolean = false,
	private val coversMapSection: Boolean = true
) {

	var poiSearches: Int = 0
		private set

	var nameSearches: Int = 0
		private set

	var mapSearches: Int = 0
		private set

	/** The repository itself, kept apart so the counters above stay readable from a test. */
	val repository: AmenityIndexRepository = object : AmenityIndexRepository {

		override fun close() {}

		override fun checkContains(latitude: Double, longitude: Double): Boolean = true

		override fun checkContainsInt(top31: Int, left31: Int, bottom31: Int, right31: Int): Boolean = true

		override fun searchAmenities(
			stop: Int, sleft: Int, sbottom: Int, sright: Int, zoom: Int,
			filter: SearchPoiTypeFilter?, additionalFilter: SearchPoiAdditionalFilter?,
			matcher: ResultMatcher<Amenity>?, priorityQueue: KPriorityQueue<Amenity>?,
			priorityQueueLimit: Int
		): List<Amenity> = emptyList()

		override fun searchAmenitiesOnThePath(
			locations: List<KLocation>, radius: Double,
			filter: SearchPoiTypeFilter?, matcher: ResultMatcher<Amenity>?
		): List<Amenity> = emptyList()

		override fun getFile(): KFile = file

		override fun isWorldMap(): Boolean = worldMap

		override fun getReaderPoiIndexes(): List<PoiRegion> = regions

		override fun searchMapIndex(searchRequest: SearchRequest<BinaryMapDataObject>) {
			mapSearches++
			for (obj in mapObjects) {
				if (searchRequest.isCancelled()) {
					return
				}
				searchRequest.publish(obj)
			}
		}

		override fun searchPoi(searchRequest: SearchRequest<Amenity>) {
			poiSearches++
			publishAmenities(searchRequest, matchByName = false)
		}

		override fun searchPoiByName(searchRequest: SearchRequest<Amenity>): List<Amenity> {
			nameSearches++
			publishAmenities(searchRequest, matchByName = true)
			return searchRequest.getSearchResults()
		}

		override fun isMapSectionIntersects(searchRequest: SearchRequest<*>): Boolean = coversMapSection

		override fun isPoiSectionIntersects(searchRequest: SearchRequest<*>): Boolean = true

		override fun searchAmenitiesByName(
			x: Int, y: Int, l: Int, t: Int, r: Int, b: Int, query: String,
			resultMatcher: ResultMatcher<Amenity>?
		): List<Amenity> = emptyList()
	}

	/**
	 * A name search only reaches the amenities whose name or route_id starts with the query, the
	 * way the poi name index would; a box search reaches all of them.
	 */
	private fun publishAmenities(req: SearchRequest<Amenity>, matchByName: Boolean) {
		val query = req.nameQuery
		for (amenity in amenities) {
			if (req.isCancelled()) {
				return
			}
			val subType = amenity.getSubType() ?: continue
			// the reader asks the filter about one subtype at a time and joins the accepted ones
			val filter = req.poiTypeFilter
			if (filter != null && subType.split(";").none { filter.accept(amenity.getType(), it) }) {
				continue
			}
			if (matchByName && !query.isNullOrEmpty() && !namesOf(amenity).any {
					it.lowercase().startsWith(query.lowercase())
				}) {
				continue
			}
			req.publish(amenity)
		}
	}

	private fun namesOf(amenity: Amenity): List<String> {
		val names = ArrayList<String>()
		names.add(amenity.getName())
		names.addAll(amenity.getNamesMap(true).values)
		amenity.getRouteId()?.let { names.add(it) }
		// a travel obf indexes the points of a route under the route's name too
		amenity.getAdditionalInfo("route_name")?.let { names.add(it) }
		return names
	}

	companion object {

		fun wholeWorldRegion(): PoiRegion {
			val region = PoiRegion()
			region.left31 = 0
			region.top31 = 0
			region.right31 = Int.MAX_VALUE
			region.bottom31 = Int.MAX_VALUE
			return region
		}

		fun region(name: String): PoiRegion {
			val region = wholeWorldRegion()
			region.name = name
			return region
		}
	}
}
