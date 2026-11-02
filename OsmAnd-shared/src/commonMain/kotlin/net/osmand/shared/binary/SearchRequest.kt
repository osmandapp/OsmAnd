package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KLocation
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.routing.RouteDataObject
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongArrayList
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * What a search asks the reader for and where its results go: a bounding box in 31 coordinates or a
 * path to search along, what to match, and how much of it to keep.
 *
 * Copy of `BinaryMapIndexReader.SearchRequest` in OsmAnd-java, which is one class for every kind of
 * search. The poi type filters and the read statistics come with the sections they belong to;
 * everything a search needs to say where and what to look for is here.
 *
 * Not thread safe: one request belongs to one search.
 */
class SearchRequest<T> {

	private var searchResults: MutableList<T> = mutableListOf()
	private var land: Boolean = false
	private var ocean: Boolean = false

	private var resultMatcher: ResultMatcher<T>? = null
	private var rawDataCollector: ResultMatcher<T>? = null

	// 31 zoom tiles
	// common variables
	@JvmField
	var x: Int = 0

	@JvmField
	var y: Int = 0

	@JvmField
	var left: Int = 0

	@JvmField
	var right: Int = 0

	@JvmField
	var top: Int = 0

	@JvmField
	var bottom: Int = 0

	private var searchBoxes: Collection<KQuadRect>? = null

	@JvmField
	var zoom: Int = 15

	@JvmField
	var limit: Int = -1

	// search on the path
	/** Tile of zoom 16 to the pairs of points, always an even count, where the path crosses it. */
	@JvmField
	var tiles: KTLongObjectMap<MutableList<KLocation>>? = null

	@JvmField
	var radius: Double = -1.0

	@JvmField
	var nameQuery: String? = null

	@JvmField
	var matcherMode: KStringMatcherMode = KStringMatcherMode.CHECK_STARTS_FROM_SPACE

	/** Map search only: which type numbers are worth reading an object for. */
	@JvmField
	var searchFilter: SearchFilter? = null

	/** Poi search only: which kinds of amenity to keep. */
	@JvmField
	var poiTypeFilter: SearchPoiTypeFilter? = null

	/** Poi search only: which values of a top index attribute to keep. */
	@JvmField
	var poiAdditionalFilter: SearchPoiAdditionalFilter? = null

	// cache information
	@JvmField
	val cacheCoordinates = KTIntArrayList()

	@JvmField
	val cacheTypes = KTIntArrayList()

	@JvmField
	val cacheIdsA = KTLongArrayList()

	@JvmField
	val cacheIdsB = KTLongArrayList()

	@JvmField
	val cacheIdsC = KTLongArrayList()

	// TRACE INFO
	@JvmField
	var log: Boolean = true

	@JvmField
	var numberOfVisitedObjects: Int = 0

	@JvmField
	var numberOfAcceptedObjects: Int = 0

	@JvmField
	var numberOfReadSubtrees: Int = 0

	@JvmField
	var numberOfAcceptedSubtrees: Int = 0

	private var interrupted: Boolean = false

	private var priorityQueue: KPriorityQueue<T & Any>? = null
	private var priorityQueueLimit: Int = 0

	fun getTileHashOnPath(lat: Double, lon: Double): Long {
		val x = KMapUtils.getTileNumberX(ZOOM_TO_SEARCH_POI.toDouble(), lon).toInt().toLong()
		val y = KMapUtils.getTileNumberY(ZOOM_TO_SEARCH_POI.toDouble(), lat).toInt().toLong()
		return (x shl ZOOM_TO_SEARCH_POI) or y
	}

	fun setBBoxRadius(lat: Double, lon: Double, radiusMeters: Int) {
		val dx = KMapUtils.getTileNumberX(16.0, lon)
		val half16t = KMapUtils.getDistance(
			lat, KMapUtils.getLongitudeFromTile(16.0, dx.toInt() + 0.5),
			lat, KMapUtils.getLongitudeFromTile(16.0, dx.toInt().toDouble())
		)
		val cf31 = (radiusMeters.toDouble() / (half16t * 2)) * (1 shl 15)
		y = KMapUtils.get31TileNumberY(lat)
		x = KMapUtils.get31TileNumberX(lon)
		left = (x - cf31).toInt()
		right = (x + cf31).toInt()
		top = (y - cf31).toInt()
		bottom = (y + cf31).toInt()
	}

	fun setBBox(x31: Int, y31: Int, left: Int, top: Int, right: Int, bottom: Int) {
		x = x31
		y = y31
		this.left = left
		this.right = right
		this.top = top
		this.bottom = bottom
	}

	fun publish(obj: T): Boolean {
		val matcher = resultMatcher
		if (matcher == null || matcher.publish(obj)) {
			val queue = priorityQueue
			if (queue != null && obj != null) {
				queue.add(obj)
				if (queue.size() > priorityQueueLimit) {
					queue.poll()
				}
			} else {
				searchResults.add(obj)
			}
			return true
		}
		return false
	}

	fun collectRawData(obj: T) {
		rawDataCollector?.publish(obj)
	}

	fun publishOceanTile(ocean: Boolean) {
		if (ocean) {
			this.ocean = true
		} else {
			this.land = true
		}
	}

	fun getSearchResults(): MutableList<T> = searchResults

	fun setResultMatcher(resultMatcher: ResultMatcher<T>?) {
		this.resultMatcher = resultMatcher
	}

	fun getResultMatcher(): ResultMatcher<T>? = resultMatcher

	fun setRawDataCollector(rawDataCollector: ResultMatcher<T>?) {
		this.rawDataCollector = rawDataCollector
	}

	fun setInterrupted(interrupted: Boolean) {
		this.interrupted = interrupted
	}

	fun limitExceeded(): Boolean = limit != -1 && searchResults.size > limit

	fun setLimit(limit: Int) {
		this.limit = limit
	}

	fun isCancelled(): Boolean {
		if (interrupted) {
			return true
		}
		return resultMatcher?.isCancelled() == true
	}

	fun isOcean(): Boolean = ocean

	fun isLand(): Boolean = land

	fun intersects(l: Int, t: Int, r: Int, b: Int): Boolean =
		r >= left && l <= right && t <= bottom && b >= top

	fun contains(l: Int, t: Int, r: Int, b: Int): Boolean =
		r <= right && l >= left && b <= bottom && t >= top

	fun getLeft(): Int = left

	fun getRight(): Int = right

	fun getBottom(): Int = bottom

	fun getTop(): Int = top

	fun getZoom(): Int = zoom

	fun clearSearchResults() {
		// recreate whole list to allow GC collect old data
		searchResults = mutableListOf()
		cacheCoordinates.clear()
		cacheTypes.clear()
		cacheIdsA.clear()
		cacheIdsB.clear()
		cacheIdsC.clear()
		land = false
		ocean = false
		numberOfVisitedObjects = 0
		numberOfAcceptedObjects = 0
		numberOfReadSubtrees = 0
		numberOfAcceptedSubtrees = 0
	}

	fun isBboxSpecified(): Boolean = left != 0 || right != 0

	fun hasSearchBoxes(): Boolean = searchBoxes != null

	fun containsSearchBox(left: Int, top: Int, right: Int, bottom: Int): Boolean {
		val boxes = searchBoxes
		if (KAlgorithms.isEmpty(boxes)) {
			return false
		}
		val searchRect = KQuadRect(
			left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble()
		)
		for (box in boxes!!) {
			if (searchRect.contains(box)) {
				return true
			}
		}
		return false
	}

	fun clearSearchBoxes() {
		searchBoxes = null
	}

	fun setSearchBoxes(searchBoxes: Collection<KQuadRect>?) {
		this.searchBoxes = searchBoxes
	}

	fun setMatcherMode(mode: KStringMatcherMode) {
		matcherMode = mode
	}

	fun setPriorityQueue(priorityQueue: KPriorityQueue<T & Any>?, priorityQueueLimit: Int) {
		this.priorityQueue = priorityQueue
		this.priorityQueueLimit = priorityQueueLimit
	}

	companion object {
		const val ZOOM_TO_SEARCH_POI: Int = 16

		/** Keeps every amenity, and says it is not empty so that the boxes are still checked. */
		@JvmField
		val ACCEPT_ALL_POI_TYPE_FILTER: SearchPoiTypeFilter = object : SearchPoiTypeFilter {
			override fun isEmpty(): Boolean = false

			override fun accept(type: PoiCategory?, subcategory: String): Boolean = true
		}

		@JvmStatic
		@JvmOverloads
		fun buildSearchRequest(
			sleft: Int,
			sright: Int,
			stop: Int,
			sbottom: Int,
			zoom: Int,
			searchFilter: SearchFilter?,
			resultMatcher: ResultMatcher<BinaryMapDataObject>? = null
		): SearchRequest<BinaryMapDataObject> {
			val request = SearchRequest<BinaryMapDataObject>()
			request.left = sleft
			request.right = sright
			request.top = stop
			request.bottom = sbottom
			request.zoom = zoom
			request.searchFilter = searchFilter
			request.resultMatcher = resultMatcher
			return request
		}

		/**
		 * A search along [route] - the points of a path - out to [radius] metres either side. The
		 * path is cut into zoom 16 tiles, each holding the pairs of points that cross it, so that
		 * an amenity is only measured against the stretch of path near it.
		 */
		@JvmStatic
		fun buildSearchPoiRequest(
			route: List<KLocation>,
			radius: Double,
			poiTypeFilter: SearchPoiTypeFilter?,
			resultMatcher: ResultMatcher<Amenity>?
		): SearchRequest<Amenity> {
			val request = SearchRequest<Amenity>()
			val coeff = (radius / KMapUtils.getTileDistanceWidth(ZOOM_TO_SEARCH_POI.toDouble())).toFloat()
			val zooms = KTLongObjectMap<MutableList<KLocation>>()
			for (i in 1 until route.size) {
				val cr = route[i]
				val pr = route[i - 1]
				val tx = KMapUtils.getTileNumberX(ZOOM_TO_SEARCH_POI.toDouble(), cr.longitude)
				val ty = KMapUtils.getTileNumberY(ZOOM_TO_SEARCH_POI.toDouble(), cr.latitude)
				val px = KMapUtils.getTileNumberX(ZOOM_TO_SEARCH_POI.toDouble(), pr.longitude)
				val py = KMapUtils.getTileNumberY(ZOOM_TO_SEARCH_POI.toDouble(), pr.latitude)
				val topLeftX = min(tx, px) - coeff
				val topLeftY = min(ty, py) - coeff
				val bottomRightX = max(tx, px) + coeff
				val bottomRightY = max(ty, py) + coeff
				var x = topLeftX.toInt()
				while (x <= bottomRightX) {
					var y = topLeftY.toInt()
					while (y <= bottomRightY) {
						val hash = (x.toLong() shl ZOOM_TO_SEARCH_POI) + y
						var ll = zooms[hash]
						if (ll == null) {
							ll = ArrayList()
							zooms.put(hash, ll)
						}
						ll.add(pr)
						ll.add(cr)
						y++
					}
					x++
				}
			}
			var sleft = Int.MAX_VALUE
			var sright = 0
			var stop = Int.MAX_VALUE
			var sbottom = 0
			for (vl in zooms.keys()) {
				val x = (vl shr ZOOM_TO_SEARCH_POI) shl (31 - ZOOM_TO_SEARCH_POI)
				val y = (vl and ((1L shl ZOOM_TO_SEARCH_POI) - 1)) shl (31 - ZOOM_TO_SEARCH_POI)
				sleft = min(x, sleft.toLong()).toInt()
				stop = min(y, stop.toLong()).toInt()
				sbottom = max(y, sbottom.toLong()).toInt()
				sright = max(x, sright.toLong()).toInt()
			}
			request.radius = radius
			request.left = sleft
			request.zoom = -1
			request.right = sright
			request.top = stop
			request.bottom = sbottom
			request.tiles = zooms
			request.poiTypeFilter = poiTypeFilter
			request.resultMatcher = resultMatcher
			return request
		}

		@JvmStatic
		@JvmOverloads
		fun buildSearchPoiRequest(
			sleft: Int,
			sright: Int,
			stop: Int,
			sbottom: Int,
			zoom: Int,
			poiTypeFilter: SearchPoiTypeFilter?,
			poiTopIndexAdditionalFilter: SearchPoiAdditionalFilter? = null,
			matcher: ResultMatcher<Amenity>? = null
		): SearchRequest<Amenity> {
			val request = SearchRequest<Amenity>()
			request.left = sleft
			request.right = sright
			request.top = stop
			request.bottom = sbottom
			request.zoom = zoom
			request.poiTypeFilter = poiTypeFilter
			request.poiAdditionalFilter = poiTopIndexAdditionalFilter
			request.resultMatcher = matcher
			return request
		}

		@JvmStatic
		@JvmOverloads
		fun buildSearchPoiRequest(
			latLon: KLatLon,
			radius: Int,
			zoom: Int,
			poiTypeFilter: SearchPoiTypeFilter?,
			matcher: ResultMatcher<Amenity>? = null
		): SearchRequest<Amenity> {
			val request = SearchRequest<Amenity>()
			request.setBBoxRadius(latLon.latitude, latLon.longitude, radius)
			request.zoom = zoom
			request.poiTypeFilter = poiTypeFilter
			request.resultMatcher = matcher
			return request
		}

		@JvmStatic
		@JvmOverloads
		fun buildSearchPoiRequest(
			x: Int,
			y: Int,
			nameFilter: String,
			sleft: Int,
			sright: Int,
			stop: Int,
			sbottom: Int,
			poiTypeFilter: SearchPoiTypeFilter? = null,
			resultMatcher: ResultMatcher<Amenity>? = null,
			rawDataCollector: ResultMatcher<Amenity>? = null
		): SearchRequest<Amenity> {
			val request = SearchRequest<Amenity>()
			request.x = x
			request.y = y
			request.left = sleft
			request.right = sright
			request.top = stop
			request.bottom = sbottom
			request.poiTypeFilter = poiTypeFilter
			request.resultMatcher = resultMatcher
			request.rawDataCollector = rawDataCollector
			request.nameQuery = nameFilter.trim()
			return request
		}

		@JvmStatic
		fun <T> buildAddressRequest(resultMatcher: ResultMatcher<T>?): SearchRequest<T> {
			val request = SearchRequest<T>()
			request.resultMatcher = resultMatcher
			return request
		}

		@JvmStatic
		fun <T> buildAddressByNameRequest(
			resultMatcher: ResultMatcher<T>?, nameRequest: String, matcherMode: KStringMatcherMode
		): SearchRequest<T> = buildAddressByNameRequest(resultMatcher, null, nameRequest, matcherMode)

		@JvmStatic
		fun <T> buildAddressByNameRequest(
			resultMatcher: ResultMatcher<T>?, rawDataCollector: ResultMatcher<T>?, nameRequest: String,
			matcherMode: KStringMatcherMode
		): SearchRequest<T> {
			val request = SearchRequest<T>()
			request.resultMatcher = resultMatcher
			request.rawDataCollector = rawDataCollector
			request.nameQuery = nameRequest.trim()
			request.matcherMode = matcherMode
			return request
		}

		@JvmStatic
		@JvmOverloads
		fun buildSearchRouteRequest(
			sleft: Int,
			sright: Int,
			stop: Int,
			sbottom: Int,
			matcher: ResultMatcher<RouteDataObject>? = null
		): SearchRequest<RouteDataObject> {
			val request = SearchRequest<RouteDataObject>()
			request.left = sleft
			request.right = sright
			request.top = stop
			request.bottom = sbottom
			request.resultMatcher = matcher
			return request
		}
	}
}
