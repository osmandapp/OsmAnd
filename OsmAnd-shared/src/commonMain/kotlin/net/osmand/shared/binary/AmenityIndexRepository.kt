package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLocation
import net.osmand.shared.io.KFile
import net.osmand.shared.util.collections.KPriorityQueue

/**
 * One obf file as a searchable source of amenities. What holds the file open, and when it is
 * closed and reopened, is the platform's business; everything that searches amenities goes
 * through this.
 *
 * A copy of `net.osmand.search.core.AmenityIndexRepository`, which stays in OsmAnd-java.
 */
interface AmenityIndexRepository {

	fun close()

	fun checkContains(latitude: Double, longitude: Double): Boolean

	fun checkContainsInt(top31: Int, left31: Int, bottom31: Int, right31: Int): Boolean

	/** Amenities in the box, nothing cached. */
	fun searchAmenities(
		stop: Int, sleft: Int, sbottom: Int, sright: Int, zoom: Int,
		filter: SearchPoiTypeFilter?, additionalFilter: SearchPoiAdditionalFilter?,
		matcher: ResultMatcher<Amenity>?, priorityQueue: KPriorityQueue<Amenity>?,
		priorityQueueLimit: Int
	): List<Amenity>

	fun searchAmenitiesOnThePath(
		locations: List<KLocation>, radius: Double,
		filter: SearchPoiTypeFilter?, matcher: ResultMatcher<Amenity>?
	): List<Amenity>

	fun getFile(): KFile

	fun isWorldMap(): Boolean

	fun getReaderPoiIndexes(): List<PoiRegion>

	fun searchMapIndex(searchRequest: SearchRequest<BinaryMapDataObject>)

	fun searchPoi(searchRequest: SearchRequest<Amenity>)

	fun searchPoiByName(searchRequest: SearchRequest<Amenity>): List<Amenity>

	fun isPoiSectionIntersects(searchRequest: SearchRequest<*>): Boolean

	/** Whether a map search would reach this file at all, answered without reading it. */
	fun isMapSectionIntersects(searchRequest: SearchRequest<*>): Boolean

	fun searchAmenitiesByName(
		x: Int, y: Int, l: Int, t: Int, r: Int, b: Int, query: String,
		resultMatcher: ResultMatcher<Amenity>?
	): List<Amenity>
}
