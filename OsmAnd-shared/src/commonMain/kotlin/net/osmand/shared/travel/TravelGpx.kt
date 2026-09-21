package net.osmand.shared.travel

import net.osmand.shared.data.Amenity
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxUtilities.PointsGroup.Companion.OBF_POINTS_GROUPS_CATEGORY
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.jvm.JvmField

/**
 * A track stored in an obf file rather than written by hand: what the poi section said about it,
 * and the gpx once it has been built out of the map section.
 *
 * A copy of `TravelGpx` in the android app, which stays there; this copy is for iOS. The `url`,
 * `url_text` and `wpt_extra_tags` tag names live on `TravelObfHelper` in java, which arrives with
 * the next step; they are here because this is what reads them.
 */
class TravelGpx : TravelArticle {

	@JvmField
	var user: String? = null

	@JvmField
	var activityType: String = ""

	@JvmField
	var totalDistance: Float = 0f

	@JvmField
	var diffElevationUp: Double = 0.0

	@JvmField
	var diffElevationDown: Double = 0.0

	@JvmField
	var maxElevation: Double = Double.NaN

	@JvmField
	var minElevation: Double = Double.NaN

	@JvmField
	var avgElevation: Double = 0.0

	@JvmField
	var isSuperRoute: Boolean = false

	private var amenity: Amenity? = null
	private var amenitySubType: String? = null
	private var amenityRegionName: String? = null

	constructor()

	/** Everything the poi section knows about the track, before any of its geometry is read. */
	constructor(amenity: Amenity) {
		this.amenity = amenity
		amenitySubType = amenity.getSubType()
		amenityRegionName = amenity.getRegionName()
		val enTitle = amenity.getName("en")
		title = if (KAlgorithms.isEmpty(title)) amenity.getName() else enTitle
		lat = amenity.getLocation()!!.latitude
		lon = amenity.getLocation()!!.longitude
		description = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.DESCRIPTION))
		routeId = KAlgorithms.emptyIfNull(amenity.getTagContent(Amenity.ROUTE_ID))
		user = KAlgorithms.emptyIfNull(amenity.getTagContent(USER))
		activityType = KAlgorithms.emptyIfNull(amenity.getTagContent(ROUTE_ACTIVITY_TYPE))
		ref = KAlgorithms.emptyIfNull(amenity.getRef())
		totalDistance = KAlgorithms.parseFloatSilently(amenity.getTagContent(DISTANCE), 0f)
		diffElevationUp = KAlgorithms.parseDoubleSilently(amenity.getTagContent(DIFF_ELEVATION_UP), 0.0)
		diffElevationDown = KAlgorithms.parseDoubleSilently(amenity.getTagContent(DIFF_ELEVATION_DOWN), 0.0)
		minElevation = KAlgorithms.parseDoubleSilently(amenity.getTagContent(MIN_ELEVATION), 0.0)
		avgElevation = KAlgorithms.parseDoubleSilently(amenity.getTagContent(AVG_ELEVATION), 0.0)
		maxElevation = KAlgorithms.parseDoubleSilently(amenity.getTagContent(MAX_ELEVATION), 0.0)
		val radius = amenity.getTagContent(ROUTE_BBOX_RADIUS)
		if (radius != null) {
			routeRadius = KMapUtils.convertCharToDist(
				radius[0], GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_LETTER,
				GpxUtilities.TRAVEL_GPX_CONVERT_FIRST_DIST, GpxUtilities.TRAVEL_GPX_CONVERT_MULT_1,
				GpxUtilities.TRAVEL_GPX_CONVERT_MULT_2
			)
		} else if (!KAlgorithms.isEmpty(routeId)) {
			routeRadius = TRAVEL_GPX_DEFAULT_SEARCH_RADIUS
		}
		val shortLinkTiles = amenity.getTagContent(ROUTE_SHORTLINK_TILES)
		if (shortLinkTiles != null) {
			initShortLinkTiles(shortLinkTiles)
		}
		if (activityType.isEmpty()) {
			for (key in amenity.getAdditionalInfoKeys()) {
				if (key.startsWith(ROUTE_ACTIVITY_TYPE)) {
					activityType = amenity.getTagContent(key) ?: ""
				}
			}
		}
		if (!KAlgorithms.isEmpty(amenity.getAdditionalInfo(Amenity.ROUTE_MEMBERS_IDS))) {
			isSuperRoute = true
		}
	}

	/**
	 * The numbers shown for the track. When the gpx has altitudes they are measured from it, and
	 * otherwise taken from what the indexer wrote into the poi section.
	 */
	override fun getAnalysis(): GpxTrackAnalysis? {
		var analysis = GpxTrackAnalysis()
		val gpxFile = this.gpxFile
		if (gpxFile != null && gpxFile.hasAltitude()) {
			analysis = gpxFile.getAnalysis(0)
		} else {
			analysis.diffElevationDown = diffElevationDown
			analysis.diffElevationUp = diffElevationUp
			analysis.maxElevation = maxElevation
			analysis.minElevation = minElevation
			analysis.totalDistance = totalDistance
			analysis.totalDistanceWithoutGaps = totalDistance
			analysis.avgElevation = avgElevation

			if (!maxElevation.isNaN() || !minElevation.isNaN()) {
				analysis.setHasData(GpxUtilities.POINT_ELEVATION, true)
			}
		}
		return analysis
	}

	/** One point of a track, with every tag the indexer kept for it carried over as an extension. */
	override fun createWptPt(amenity: Amenity, lang: String?): WptPt {
		val wptPt = WptPt()
		wptPt.name = (amenity.getName())
		wptPt.lat = (amenity.getLocation()!!.latitude)
		wptPt.lon = (amenity.getLocation()!!.longitude)

		val wptPtExtensions = wptPt.getExtensionsToWrite()
		for (entry in amenity.getNamesMap(true)) {
			wptPtExtensions["name:" + entry.key] = entry.value
		}

		var linkHref: String? = null
		var linkText: String? = null

		for (obfTag in amenity.getAdditionalInfoKeys()) {
			val value = amenity.getAdditionalInfo(obfTag)
			if (!KAlgorithms.isEmpty(value)) {
				if (OBF_POINTS_GROUPS_CATEGORY == obfTag) {
					wptPt.category = (value)
				} else if ("name" == obfTag) {
					wptPt.name = (value)
				} else if ("description" == obfTag) {
					wptPt.desc = (value)
				} else if ("note" == obfTag) {
					wptPt.comment = (value)
				} else if (TAG_URL == obfTag) {
					linkHref = value
				} else if (TAG_URL_TEXT == obfTag) {
					linkText = value
				} else if ("colour" == obfTag && amenity.getAdditionalInfoKeys().contains("color")) {
					// ignore "colour" if "color" exists
				} else if (WPT_EXTRA_TAGS == obfTag) {
					wptPtExtensions.putAll(parseExtraTags(value!!))
				} else if (!doNotSaveWptTags.contains(obfTag)) {
					wptPtExtensions[obfTag] = value!!
				}
			}
		}

		if (linkHref != null || linkText != null) {
			wptPt.link = (Link(linkHref ?: "", linkText, null)) // nullable href/text
		}

		return wptPt
	}

	override fun getPointFilterString(): String = MapPoiTypes.ROUTE_TRACK_POINT

	override fun getMainFilterString(): String =
		MapPoiTypes.ROUTE_TRACK // considered together with ROUTES_PREFIX

	fun getAmenitySubType(): String? = amenitySubType

	fun getAmenityRegionName(): String? = amenityRegionName

	/** The activity the route was written for, out of a subtype such as `routes_hiking`. */
	fun getRouteType(): String? {
		val amenitySubType = this.amenitySubType
		if (amenitySubType != null) {
			for (subType in amenitySubType.split(";")) {
				if (subType.startsWith(MapPoiTypes.ROUTES_PREFIX)) {
					return subType.replace(MapPoiTypes.ROUTES_PREFIX, "")
				}
			}
		}
		return null
	}

	fun getAmenity(): Amenity? = amenity

	companion object {
		private val log = LoggerFactory.getLogger("TravelGpx")

		const val DISTANCE: String = "distance"
		const val MAX_ELEVATION: String = GpxUtilities.MAX_ELEVATION
		const val MIN_ELEVATION: String = GpxUtilities.MIN_ELEVATION
		const val AVG_ELEVATION: String = GpxUtilities.AVG_ELEVATION
		const val DIFF_ELEVATION_UP: String = GpxUtilities.DIFF_ELEVATION_UP
		const val DIFF_ELEVATION_DOWN: String = GpxUtilities.DIFF_ELEVATION_DOWN
		const val START_ELEVATION: String = "start_ele"
		const val ELE_GRAPH: String = "ele_graph"
		const val ROUTE_BBOX_RADIUS: String = "route_bbox_radius"
		const val ROUTE_SHORTLINK_TILES: String = "route_shortlink_tiles"
		const val ROUTE_SEGMENT_INDEX: String = "route_segment_index"
		const val USER: String = "user"
		const val ROUTE_TYPE: String = "route_type"
		const val ROUTE_ACTIVITY_TYPE: String = "route_activity_type"
		const val TRAVEL_MAP_TO_POI_TAG: String = "route_id"

		/** Java keeps these on `TravelObfHelper`; they arrive with it in the next step. */
		const val TAG_URL: String = "url"
		const val TAG_URL_TEXT: String = "url_text"
		const val WPT_EXTRA_TAGS: String = "wpt_extra_tags"

		private val doNotSaveWptTags = setOf("route_id", "route_name")

		private val json = Json { isLenient = true; ignoreUnknownKeys = true }

		/**
		 * The `wpt_extra_tags` value, which the indexer writes as a flat json object. Java reads it
		 * with Gson, which turns every value into a string; a value that is not an object at all
		 * makes Gson throw, where this leaves the point without its extra tags.
		 */
		private fun parseExtraTags(value: String): Map<String, String> {
			return try {
				val parsed = json.parseToJsonElement(value)
				if (parsed !is JsonObject) {
					return emptyMap()
				}
				val tags = LinkedHashMap<String, String>()
				for (entry in parsed) {
					val element = entry.value
					if (element is JsonPrimitive) {
						tags[entry.key] = element.content
					}
				}
				tags
			} catch (e: Exception) {
				log.error("Can't read $WPT_EXTRA_TAGS: $value", e)
				emptyMap()
			}
		}
	}
}
