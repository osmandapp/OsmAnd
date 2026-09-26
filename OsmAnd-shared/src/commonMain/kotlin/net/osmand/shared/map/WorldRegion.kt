package net.osmand.shared.map

import net.osmand.shared.IndexConstants
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A region of the world as the downloads know it: a continent, a country or a part of one, with
 * its names, the maps it can be downloaded as, and its boundary. [OsmandRegions] reads them all
 * out of `regions.ocbf` and hangs them into one tree under [WORLD].
 *
 * A copy of `WorldRegion` in OsmAnd-java, which stays there for android and tools; this copy is for
 * iOS. It is not `Serializable`.
 */
open class WorldRegion(regionFullName: String, downloadName: String?) {

	constructor(id: String) : this(id, null)

	class RegionParams {
		@JvmField
		internal var regionLeftHandDriving: String? = null
		@JvmField
		internal var regionLang: String? = null
		@JvmField
		internal var regionMetric: String? = null
		@JvmField
		internal var regionRoadSigns: String? = null
		@JvmField
		internal var wikiLink: String? = null
		@JvmField
		internal var population: String? = null

		fun getRegionLeftHandDriving(): String? = regionLeftHandDriving

		fun getRegionLang(): String? = regionLang

		fun getRegionMetric(): String? = regionMetric

		fun getRegionRoadSigns(): String? = regionRoadSigns

		fun getWikiLink(): String? = wikiLink

		fun getPopulation(): String? = population
	}

	// Hierarchy
	@JvmField
	internal var superregion: WorldRegion? = null
	@JvmField
	internal val subregions: MutableList<WorldRegion> = ArrayList()

	// filled by osmand regions
	@JvmField
	internal val params = RegionParams()
	@JvmField
	internal val regionFullName: String = regionFullName
	@JvmField
	internal var regionParentFullName: String? = null
	@JvmField
	internal var regionName: String? = null
	@JvmField
	internal var regionNameEn: String? = null
	@JvmField
	internal var regionNameLocale: String? = null
	@JvmField
	internal var regionSearchText: String? = null
	@JvmField
	internal val regionDownloadName: String? = downloadName
	@JvmField
	internal var regionMapDownload: Boolean = false
	@JvmField
	internal var regionRoadsDownload: Boolean = false
	@JvmField
	internal var regionJoinMapDownload: Boolean = false
	@JvmField
	internal var regionJoinRoadsDownload: Boolean = false
	@JvmField
	internal var regionCenter: KLatLon? = null
	@JvmField
	internal var boundingBox: KQuadRect? = null

	/** The biggest polygon of the region (CountryOcbfGeneration), lat, lon, lat, lon... */
	@JvmField
	internal var polygon: FloatArray? = null

	/** All the inclusions and exclusions. */
	@JvmField
	internal val additionalPolygons: MutableList<FloatArray> = ArrayList()

	fun isRegionMapDownload(): Boolean = regionMapDownload

	fun isRegionRoadsDownload(): Boolean = regionRoadsDownload

	fun isRegionJoinMapDownload(): Boolean = regionJoinMapDownload

	fun isRegionJoinRoadsDownload(): Boolean = regionJoinRoadsDownload

	fun getLocaleName(): String {
		val regionNameLocale = this.regionNameLocale
		if (!regionNameLocale.isNullOrEmpty()) {
			return regionNameLocale
		}
		val regionNameEn = this.regionNameEn
		if (!regionNameEn.isNullOrEmpty()) {
			return regionNameEn
		}
		val regionName = this.regionName
		if (!regionName.isNullOrEmpty()) {
			return regionName
		}

		return capitalize(regionFullName.replace('_', ' '))
	}

	fun getRegionDownloadName(): String? = regionDownloadName

	fun getRegionDownloadNameLC(): String? = regionDownloadName?.lowercase()

	fun getParams(): RegionParams = params

	fun getRegionCenter(): KLatLon? = regionCenter

	fun getRegionSearchText(): String? = regionSearchText

	fun getSuperregion(): WorldRegion? = superregion

	fun getSuperRegions(): List<WorldRegion> = getSuperRegions(null)

	fun getSuperRegions(baseRegion: WorldRegion?): List<WorldRegion> {
		val regions = ArrayList<WorldRegion>()
		collectSuperRegions(regions, superregion, baseRegion)
		return regions
	}

	private fun collectSuperRegions(regions: MutableList<WorldRegion>, region: WorldRegion?, baseRegion: WorldRegion?) {
		if (region != null && (baseRegion == null || region != baseRegion)) {
			regions.add(region)
			collectSuperRegions(regions, region.getSuperregion(), baseRegion)
		}
	}

	fun getCountryRegion(): WorldRegion? {
		if (isContinent()) {
			return null
		}
		var region: WorldRegion? = this
		while (region != null) {
			val parent = region.getSuperregion()
			// The current region is a country when its parent is a continent, or the world itself
			// for the countries that are not placed under any continent
			if (parent != null && (parent.isContinent() || WORLD == parent.getRegionId())) {
				return region
			}
			region = parent
		}
		// If we reached the top without finding a country, return null
		return null
	}

	fun getSubregions(): MutableList<WorldRegion> = subregions

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false
		other as WorldRegion
		return regionFullName.equals(other.regionFullName, ignoreCase = true)
	}

	override fun hashCode(): Int = regionFullName.hashCode()

	fun getRegionId(): String = regionFullName

	private fun capitalize(s: String): String {
		val words = splitLikeJava(s, " ")
		if (words.isNotEmpty() && words[0].isNotEmpty()) {
			val sb = StringBuilder()
			sb.append(KAlgorithms.capitalizeFirstLetterAndLowercase(words[0]))
			for (i in 1 until words.size) {
				sb.append(" ")
				sb.append(KAlgorithms.capitalizeFirstLetterAndLowercase(words[i]))
			}
			return sb.toString()
		} else {
			return s
		}
	}

	fun addSubregion(rd: WorldRegion) {
		subregions.add(rd)
		rd.superregion = this
	}

	fun getLevel(): Int {
		var res = 0
		var parent = superregion
		while (parent != null) {
			parent = parent.superregion
			res++
		}
		return res
	}

	fun containsRegion(another: WorldRegion): Boolean {
		// Firstly check rectangles for greater efficiency
		if (!containsBoundingBox(another.boundingBox)) {
			return false
		}

		// Secondly check whole polygons
		if (!containsPolygon(another.polygon)) {
			return false
		}

		// Finally check inner point; a region with a polygon always has a centre
		val center = another.regionCenter
		if (center != null && another.containsPoint(center)) {
			return containsPoint(center)
		} else {
			// in this case we should find real inner point and check it
		}
		return true
	}

	fun containsBoundingBox(rectangle: KQuadRect?): Boolean {
		val boundingBox = this.boundingBox
		return boundingBox != null && rectangle != null && boundingBox.contains(rectangle)
	}

	private fun containsPolygon(another: FloatArray?): Boolean {
		val polygon = this.polygon
		return polygon != null && another != null && KAlgorithms.isFirstPolygonInsideSecond(another, polygon)
	}

	/**
	 * Some regions - the continents and the ones that only join their parts, like Great Britain -
	 * are not stored with a boundary of their own, so a point can only be looked up in their
	 * subregions.
	 */
	fun hasBoundaries(): Boolean {
		val boundingBox = this.boundingBox
		return boundingBox != null && boundingBox.width() != 0.0 && boundingBox.height() != 0.0
	}

	fun containsPoint(latLon: KLatLon): Boolean {
		var intersections = 0
		val polygon = this.polygon
		if (polygon != null) {
			val lat = latLon.latitude.toFloat()
			val lon = latLon.longitude.toFloat()
			if (KAlgorithms.isPointInsidePolygon(lat, lon, polygon)) {
				intersections++
			}
			for (additional in additionalPolygons) {
				if (KAlgorithms.isPointInsidePolygon(lat, lon, additional)) {
					if (++intersections % 2 == 0) {
						break // optimize
					}
				}
			}
		}
		return intersections % 2 == 1
	}

	fun isContinent(): Boolean {
		val superregion = this.superregion
		if (superregion != null) {
			val superRegionId = superregion.getRegionId()
			val thisRegionId = getRegionId()
			return WORLD == superRegionId && RUSSIA_REGION_ID != thisRegionId
		}
		return false
	}

	fun getObfFileName(): String = getObfFileName(regionDownloadName)

	fun getRoadObfFileName(): String = getRoadObfFileName(regionDownloadName)

	fun getBoundingBox(): KQuadRect? = boundingBox

	fun getPolygons(): List<FloatArray> {
		val polygons = ArrayList<FloatArray>()
		polygon?.let { polygons.add(it) }
		polygons.addAll(additionalPolygons)
		return polygons
	}

	fun getAllPolygonsBounds(): List<KQuadRect> {
		val allBounds = ArrayList<KQuadRect>()
		polygon?.let { allBounds.add(calculateBoundingBox(it)) }
		for (poly in additionalPolygons) {
			allBounds.add(calculateBoundingBox(poly))
		}
		return allBounds
	}

	private fun calculateBoundingBox(polygon: FloatArray): KQuadRect {
		val bounds = KQuadRect()
		var i = 0
		while (i < polygon.size) {
			val y = polygon[i].toDouble() // latitude
			val x = polygon[i + 1].toDouble() // longitude
			bounds.expand(x, y, x, y)
			i += 2
		}
		return bounds
	}

	override fun toString(): String = getRegionId()

	companion object {
		const val WORLD_BASEMAP = "world_basemap"
		const val WORLD_BASEMAP_MINI = "world_basemap_mini"
		const val ANTARCTICA_REGION_ID = "antarctica"
		const val AFRICA_REGION_ID = "africa"
		const val ASIA_REGION_ID = "asia"
		const val AUSTRALIA_AND_OCEANIA_REGION_ID = "australia-oceania-all"
		const val CENTRAL_AMERICA_REGION_ID = "centralamerica"
		const val EUROPE_REGION_ID = "europe"
		const val NORTH_AMERICA_REGION_ID = "northamerica"
		const val RUSSIA_REGION_ID = "russia"
		const val JAPAN_REGION_ID = "japan_asia"
		const val GERMANY_REGION_ID = "europe_germany"
		const val FRANCE_REGION_ID = "europe_france"
		const val SOUTH_AMERICA_REGION_ID = "southamerica"
		const val WORLD = "world"
		const val UNITED_KINGDOM_REGION_ID = "europe_gb"

		@JvmStatic
		fun removeDuplicates(regions: List<WorldRegion>): List<WorldRegion> {
			val copy = ArrayList(regions)
			val duplicates = HashSet<WorldRegion>()
			for (i in 0 until copy.size - 1) {
				val r1 = copy[i]
				for (j in i + 1 until copy.size) {
					val r2 = copy[j]
					if (r1.containsRegion(r2)) {
						duplicates.add(r2)
					} else if (r2.containsRegion(r1)) {
						duplicates.add(r1)
					}
				}
			}
			copy.removeAll(duplicates)
			return copy
		}

		@JvmStatic
		fun getObfFileName(regionDownloadName: String?): String =
			KAlgorithms.capitalizeFirstLetterAndLowercase(regionDownloadName) + IndexConstants.BINARY_MAP_INDEX_EXT

		@JvmStatic
		fun getRoadObfFileName(regionDownloadName: String?): String =
			KAlgorithms.capitalizeFirstLetterAndLowercase(regionDownloadName) + ".road" + IndexConstants.BINARY_MAP_INDEX_EXT

		@JvmStatic
		fun getRegionDownloadName(obfFileName: String): String {
			val obfExt = IndexConstants.BINARY_MAP_INDEX_EXT
			val roadObfExt = ".road" + IndexConstants.BINARY_MAP_INDEX_EXT
			return if (obfFileName.endsWith(roadObfExt)) {
				obfFileName.lowercase().substring(0, obfFileName.length - roadObfExt.length)
			} else if (obfFileName.endsWith(obfExt)) {
				obfFileName.lowercase().substring(0, obfFileName.length - obfExt.length)
			} else {
				obfFileName.lowercase()
			}
		}

		/**
		 * java's `String.split`: trailing empty parts are dropped, except that a string the
		 * delimiter is not in is one part, itself. Kotlin's `split` keeps every part.
		 */
		internal fun splitLikeJava(s: String, delimiter: String): List<String> {
			val parts = s.split(delimiter)
			return if (parts.size == 1) parts else parts.dropLastWhile { it.isEmpty() }
		}
	}
}
