package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KLocation
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.synchronized

/**
 * An [AmenityIndexRepository] over one open obf file.
 *
 * A copy of `AmenityIndexRepositoryBinary` in the android app, which stays there; this copy is for
 * iOS. The reader is asked for on every call rather than held, because the platform closes and
 * reopens files as maps are enabled, downloaded or updated - see [ObfReaderSupplier]. While the
 * file is not open every method here is a no-op.
 *
 * What is left behind in android: the poi-category cache that the app keeps per file in its own
 * database. [calculateDeltaSubcategories] - what would go into that cache - is here.
 */
open class BinaryAmenityIndexRepository(
	private val file: KFile,
	private val readerSupplier: ObfReaderSupplier
) : AmenityIndexRepository {

	constructor(path: String, readerSupplier: ObfReaderSupplier) : this(KFile(path), readerSupplier)

	private val lock = KLock()

	override fun close() {
	}

	override fun getFile(): KFile = file

	private fun getOpenReader(): BinaryMapIndexReader? = readerSupplier.getReader()

	/**
	 * The subtypes this file uses that [poiTypes] does not know - a file built with a newer
	 * poi_types.xml than the app ships with.
	 */
	fun calculateDeltaSubcategories(
		region: PoiRegion,
		poiTypes: MapPoiTypes,
		deltaPoiCategories: MutableMap<String, MutableList<String>>
	) {
		val categories = region.categories
		val subCategories = region.subcategories
		for (i in categories.indices) {
			val categoryName = categories[i]
			val poiCategory = poiTypes.getPoiCategoryByName(categoryName)
			var deltaSubCategories: MutableList<String>? = null
			for (subCategory in subCategories[i]) {
				if (poiCategory?.getPoiTypeByKeyName(subCategory) == null) {
					if (deltaSubCategories == null) {
						deltaSubCategories = ArrayList()
					}
					deltaSubCategories.add(subCategory)
				}
			}
			if (deltaSubCategories != null) {
				val existing = deltaPoiCategories[categoryName]
				if (existing != null) {
					existing.addAll(deltaSubCategories)
				} else {
					deltaPoiCategories[categoryName] = deltaSubCategories
				}
			}
		}
	}

	override fun checkContains(latitude: Double, longitude: Double): Boolean {
		val x31 = KMapUtils.get31TileNumberX(longitude)
		val y31 = KMapUtils.get31TileNumberY(latitude)
		val reader = getOpenReader()
		return reader != null && reader.containsPoiData(x31, y31, x31, y31)
	}

	override fun checkContainsInt(top31: Int, left31: Int, bottom31: Int, right31: Int): Boolean {
		val reader = getOpenReader()
		return reader != null && reader.containsPoiData(left31, top31, right31, bottom31)
	}

	fun searchPoiSubTypesByPrefix(query: String): List<PoiSubType> = synchronized(lock) {
		val poiSubTypes = ArrayList<PoiSubType>()
		try {
			getOpenReader()?.let { poiSubTypes.addAll(it.searchPoiSubTypesByPrefix(query)) }
		} catch (e: Exception) {
			log.error("Error searching poiSubTypes", e)
		}
		poiSubTypes
	}

	override fun searchAmenitiesByName(
		x: Int, y: Int, l: Int, t: Int, r: Int, b: Int, query: String,
		resultMatcher: ResultMatcher<Amenity>?
	): List<Amenity> = synchronized(lock) {
		var amenities: List<Amenity> = emptyList()
		val req = SearchRequest.buildSearchPoiRequest(x, y, query, l, r, t, b, null, resultMatcher)
		try {
			getOpenReader()?.let { amenities = it.searchPoiByName(req) }
		} catch (e: Exception) {
			log.error("Error searching amenities", e)
		}
		amenities
	}

	override fun searchAmenities(
		stop: Int, sleft: Int, sbottom: Int, sright: Int, zoom: Int,
		filter: SearchPoiTypeFilter?, additionalFilter: SearchPoiAdditionalFilter?,
		matcher: ResultMatcher<Amenity>?, priorityQueue: KPriorityQueue<Amenity>?,
		priorityQueueLimit: Int
	): List<Amenity> = synchronized(lock) {
		val req = SearchRequest.buildSearchPoiRequest(
			sleft, sright, stop, sbottom, zoom, filter, additionalFilter, matcher
		)
		req.setPriorityQueue(priorityQueue, priorityQueueLimit)
		var result: List<Amenity> = emptyList()
		try {
			getOpenReader()?.let { result = it.searchPoi(req) }
		} catch (e: Exception) {
			log.error("Error searching amenities", e)
		}
		result
	}

	override fun searchAmenitiesOnThePath(
		locations: List<KLocation>, radius: Double,
		filter: SearchPoiTypeFilter?, matcher: ResultMatcher<Amenity>?
	): List<Amenity> = synchronized(lock) {
		var result: List<Amenity> = emptyList()
		val req = SearchRequest.buildSearchPoiRequest(locations, radius, filter, matcher)
		try {
			getOpenReader()?.let { result = it.searchPoi(req) }
		} catch (e: Exception) {
			log.error("Error searching amenities", e)
		}
		result
	}

	override fun getReaderPoiIndexes(): List<PoiRegion> = getOpenReader()?.getPoiIndexes() ?: emptyList()

	override fun searchMapIndex(searchRequest: SearchRequest<BinaryMapDataObject>): Unit = synchronized(lock) {
		val reader = getOpenReader()
		if (reader != null) {
			try {
				reader.searchMapIndex(searchRequest)
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		} else {
			log.error("reader is null")
		}
	}

	override fun searchPoi(searchRequest: SearchRequest<Amenity>): Unit = synchronized(lock) {
		val reader = getOpenReader()
		if (reader != null) {
			try {
				reader.searchPoi(searchRequest)
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		} else {
			log.error("reader is null")
		}
	}

	override fun searchPoiByName(searchRequest: SearchRequest<Amenity>): List<Amenity> = synchronized(lock) {
		val reader = getOpenReader()
		if (reader != null) {
			try {
				return@synchronized reader.searchPoiByName(searchRequest)
			} catch (e: Exception) {
				log.error(e.message, e)
			}
		} else {
			log.error("reader is null")
		}
		emptyList()
	}

	override fun isWorldMap(): Boolean {
		val fileName = getFile().name().lowercase()
		return fileName.startsWith(WORLD_REGION_ID + "_") || fileName.contains("basemap")
	}

	override fun isPoiSectionIntersects(searchRequest: SearchRequest<*>): Boolean {
		for (index in getReaderPoiIndexes()) {
			if (searchRequest.intersects(index.left31, index.top31, index.right31, index.bottom31)) {
				return true
			}
		}
		return false
	}

	override fun toString(): String = getFile().name()

	companion object {
		private val log = LoggerFactory.getLogger("BinaryAmenityIndexRepository")

		/** `WorldRegion.WORLD`, which stays in OsmAnd-java. */
		private const val WORLD_REGION_ID = "world"
	}
}
