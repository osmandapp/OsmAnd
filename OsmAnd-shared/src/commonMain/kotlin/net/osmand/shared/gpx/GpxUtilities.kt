package net.osmand.shared.gpx

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.osmand.shared.data.QuadRect
import net.osmand.shared.gpx.SplitMetric.DistanceSplitMetric
import net.osmand.shared.gpx.SplitMetric.TimeSplitMetric
import net.osmand.shared.io.CommonFile
import net.osmand.shared.util.Algorithms
import net.osmand.shared.util.Algorithms.hash
import net.osmand.shared.util.IProgress
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.MapUtils
import net.osmand.shared.util.PlatformUtil.currentTimeMillis
import net.osmand.shared.util.StringBundle
import net.osmand.shared.util.StringBundleXmlReader
import net.osmand.shared.util.StringBundleXmlWriter
import net.osmand.shared.xml.XmlParserException
import net.osmand.shared.xml.XmlPullParser
import net.osmand.shared.xml.XmlSerializer
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Source
import kotlin.math.round

object GpxUtilities {

	val log = LoggerFactory.getLogger("GpxUtilities")

	const val ICON_NAME_EXTENSION = "icon"
	const val BACKGROUND_TYPE_EXTENSION = "background"
	const val COLOR_NAME_EXTENSION = "color"
	const val PROFILE_TYPE_EXTENSION = "profile"
	const val ADDRESS_EXTENSION = "address"
	const val HIDDEN_EXTENSION = "hidden"

	const val GPXTPX_PREFIX = "gpxtpx:"
	const val OSMAND_EXTENSIONS_PREFIX = "osmand:"
	const val OSM_PREFIX = "osm_tag_"
	const val AMENITY_PREFIX = "amenity_"
	const val AMENITY_ORIGIN_EXTENSION = "amenity_origin"

	const val GAP_PROFILE_TYPE = "gap"
	const val TRKPT_INDEX_EXTENSION = "trkpt_idx"
	const val DEFAULT_ICON_NAME = "special_star"

	const val POINT_ELEVATION = "ele"
	const val POINT_SPEED = "speed"
	const val POINT_BEARING = "bearing"

	const val TRAVEL_GPX_CONVERT_FIRST_LETTER = 'A'
	const val TRAVEL_GPX_CONVERT_FIRST_DIST = 5000
	const val TRAVEL_GPX_CONVERT_MULT_1 = 2
	const val TRAVEL_GPX_CONVERT_MULT_2 = 5

	private const val GPX_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
	private const val GPX_TIME_NO_TIMEZONE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
	private const val GPX_TIME_PATTERN_TZ = "yyyy-MM-dd'T'HH:mm:ssXXX"

	private val SUPPORTED_EXTENSION_TAGS = mapOf(
		"heartrate" to PointAttributes.SENSOR_TAG_HEART_RATE,
		"osmand:hr" to PointAttributes.SENSOR_TAG_HEART_RATE,
		"hr" to PointAttributes.SENSOR_TAG_HEART_RATE,
		"speed_sensor" to PointAttributes.SENSOR_TAG_SPEED,
		"cad" to PointAttributes.SENSOR_TAG_CADENCE,
		"cadence" to PointAttributes.SENSOR_TAG_CADENCE,
		"temp" to PointAttributes.SENSOR_TAG_TEMPERATURE_W,
		"wtemp" to PointAttributes.SENSOR_TAG_TEMPERATURE_W,
		"atemp" to PointAttributes.SENSOR_TAG_TEMPERATURE_A
	)

	const val RADIUS_DIVIDER = 5000
	const val PRIME_MERIDIAN = 179.999991234

	enum class GpxColor(val color: Int) {
		BLACK(0xFF000000.toInt()),
		DARKGRAY(0xFF444444.toInt()),
		GRAY(0xFF888888.toInt()),
		LIGHTGRAY(0xFFCCCCCC.toInt()),
		WHITE(0xFFFFFFFF.toInt()),
		RED(0xFFFF0000.toInt()),
		GREEN(0xFF00FF00.toInt()),
		DARKGREEN(0xFF006400.toInt()),
		BLUE(0xFF0000FF.toInt()),
		YELLOW(0xFFFFFF00.toInt()),
		CYAN(0xFF00FFFF.toInt()),
		MAGENTA(0xFFFF00FF.toInt()),
		AQUA(0xFF00FFFF.toInt()),
		FUCHSIA(0xFFFF00FF.toInt()),
		DARKGREY(0xFF444444.toInt()),
		GREY(0xFF888888.toInt()),
		LIGHTGREY(0xFFCCCCCC.toInt()),
		LIME(0xFF00FF00.toInt()),
		MAROON(0xFF800000.toInt()),
		NAVY(0xFF000080.toInt()),
		OLIVE(0xFF808000.toInt()),
		PURPLE(0xFF800080.toInt()),
		SILVER(0xFFC0C0C0.toInt()),
		TEAL(0xFF008080.toInt());

		companion object {
			fun getColorFromName(name: String): GpxColor? {
				return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
			}
		}
	}

	// "0.00#####"
	fun formatLatLon(number: Double): String {
		val roundedNumber = kotlin.math.round(number * 10000000) / 10000000
		val numberString = roundedNumber.toString()
		val parts = numberString.split('.')
		val integerPart = parts[0]
		var fractionalPart = parts.getOrElse(1) { "00" }
		if (fractionalPart.length < 2) {
			fractionalPart += "0"
		}
		return "$integerPart.$fractionalPart"
	}

	// "#.#"
	fun formatDecimal(number: Double): String {
		val roundedNumber = kotlin.math.round(number * 10) / 10
		val numberString = roundedNumber.toString()
		val parts = numberString.split('.')
		val integerPart = parts[0]
		val fractionalPart = parts.getOrElse(1) { "0" }
		return if (fractionalPart == "0") {
			integerPart
		} else {
			"$integerPart.$fractionalPart"
		}
	}

	interface GpxExtensionsWriter {
		fun writeExtensions(serializer: XmlSerializer)
	}

	interface GpxExtensionsReader {
		@Throws(IOException::class, XmlParserException::class)
		fun readExtensions(res: GpxFile, parser: XmlPullParser): Boolean
	}

	open class GpxExtensions {
		var extensions: MutableMap<String, String>? = null
		var extensionsWriter: GpxExtensionsWriter? = null
		var additionalExtensionsWriter: GpxExtensionsWriter? = null

		fun getExtensionsToRead(): Map<String, String> {
			return extensions ?: emptyMap()
		}

		fun getExtensionsToWrite(): MutableMap<String, String> {
			if (extensions == null) {
				extensions = LinkedHashMap()
			}
			return extensions!!
		}

		fun copyExtensions(e: GpxExtensions) {
			val extensionsToRead = e.getExtensionsToRead()
			if (extensionsToRead.isNotEmpty()) {
				getExtensionsToWrite().putAll(extensionsToRead)
			}
		}

		fun getColor(defColor: Int): Int {
			var clrValue: String? = null
			val extensions = this.extensions
			if (extensions != null) {
				clrValue = extensions[COLOR_NAME_EXTENSION]
				if (clrValue == null) {
					clrValue = extensions["colour"]
				}
				if (clrValue == null) {
					clrValue = extensions["displaycolor"]
				}
				if (clrValue == null) {
					clrValue = extensions["displaycolour"]
				}
			}
			return parseColor(clrValue, defColor)
		}

		fun setColor(color: Int) {
			setColor(Algorithms.colorToString(color))
		}

		fun setColor(color: String) {
			getExtensionsToWrite()[COLOR_NAME_EXTENSION] = color
		}

		fun removeColor() {
			getExtensionsToWrite().remove(COLOR_NAME_EXTENSION)
		}
	}

	fun parseColor(colorString: String?, defColor: Int): Int {
		val color = parseColor(colorString)
		return color ?: defColor
	}

	fun parseColor(colorString: String?): Int? {
		if (!Algorithms.isEmpty(colorString)) {
			if (colorString!![0] == '#') {
				return try {
					Algorithms.parseColor(colorString)
				} catch (e: IllegalArgumentException) {
					log.error("Error parse color", e)
					null
				}
			} else {
				val gpxColor = GpxColor.getColorFromName(colorString)
				if (gpxColor != null) {
					return gpxColor.color
				}
			}
		}
		return null
	}

	class WptPt : GpxExtensions {
		var firstPoint = false
		var lastPoint = false
		var lat: Double = 0.0
		var lon: Double = 0.0
		var name: String? = null
		var link: String? = null
		var category: String? = null
		var desc: String? = null
		var comment: String? = null
		var time: Long = 0
		var ele = Double.NaN
		var speed = 0.0
		var hdop = Double.NaN
		var heading = Float.NaN
		var bearing = Float.NaN
		var deleted = false
		var speedColor = 0
		var altitudeColor = 0
		var slopeColor = 0
		var colourARGB = 0
		var distance = 0.0

		constructor()

		constructor(wptPt: WptPt) {
			lat = wptPt.lat
			lon = wptPt.lon
			name = wptPt.name
			link = wptPt.link
			category = wptPt.category
			desc = wptPt.desc
			comment = wptPt.comment
			time = wptPt.time
			ele = wptPt.ele
			speed = wptPt.speed
			hdop = wptPt.hdop
			heading = wptPt.heading
			deleted = wptPt.deleted
			speedColor = wptPt.speedColor
			altitudeColor = wptPt.altitudeColor
			slopeColor = wptPt.slopeColor
			colourARGB = wptPt.colourARGB
			distance = wptPt.distance
			getExtensionsToWrite().putAll(wptPt.getExtensionsToWrite())
		}

		fun getColor(): Int {
			return getColor(0)
		}

		fun getLatitude(): Double {
			return lat
		}

		fun getLongitude(): Double {
			return lon
		}

		constructor(lat: Double, lon: Double) {
			this.lat = lat
			this.lon = lon
		}

		constructor(
			lat: Double,
			lon: Double,
			time: Long,
			ele: Double,
			speed: Double,
			hdop: Double
		) : this(lat, lon, time, ele, speed, hdop, Float.NaN)

		constructor(
			lat: Double,
			lon: Double,
			time: Long,
			ele: Double,
			speed: Double,
			hdop: Double,
			heading: Float
		) {
			this.lat = lat
			this.lon = lon
			this.time = time
			this.ele = ele
			this.speed = speed
			this.hdop = hdop
			this.heading = heading
		}

		constructor(
			lat: Double,
			lon: Double,
			desc: String,
			name: String,
			category: String,
			color: String,
			icon: String,
			background: String
		) {
			this.lat = lat
			this.lon = lon
			this.desc = desc
			this.name = name
			this.category = category
			setColor(color)
			setIconName(icon)
			setBackgroundType(background)
		}

		fun isVisible(): Boolean {
			return true
		}

		fun getIconName(): String? {
			return getExtensionsToRead()[ICON_NAME_EXTENSION]
		}

		fun getIconNameOrDefault(): String {
			var iconName = getIconName()
			if (iconName == null) {
				iconName = DEFAULT_ICON_NAME
			}
			return iconName
		}

		fun setIconName(iconName: String) {
			getExtensionsToWrite()[ICON_NAME_EXTENSION] = iconName
		}

		fun getAmenityOriginName(): String? {
			val extensionsToRead = getExtensionsToRead()
			var amenityOrigin = extensionsToRead[AMENITY_ORIGIN_EXTENSION]
			val comment = this.comment
			if (amenityOrigin == null && comment != null && comment.startsWith("Amenity")) {
				amenityOrigin = comment
			}
			return amenityOrigin
		}

		fun setAmenityOriginName(originName: String) {
			getExtensionsToWrite()[AMENITY_ORIGIN_EXTENSION] = originName
		}

		fun getBackgroundType(): String? {
			return getExtensionsToRead()[BACKGROUND_TYPE_EXTENSION]
		}

		fun setBackgroundType(backType: String) {
			getExtensionsToWrite()[BACKGROUND_TYPE_EXTENSION] = backType
		}

		fun getProfileType(): String? {
			return getExtensionsToRead()[PROFILE_TYPE_EXTENSION]
		}

		fun getAddress(): String? {
			return getExtensionsToRead()[ADDRESS_EXTENSION]
		}

		fun setAddress(address: String?) {
			if (Algorithms.isBlank(address)) {
				getExtensionsToWrite().remove(ADDRESS_EXTENSION)
			} else {
				getExtensionsToWrite()[ADDRESS_EXTENSION] = address!!
			}
		}

		fun setHidden(hidden: String) {
			getExtensionsToWrite()[HIDDEN_EXTENSION] = hidden
		}

		fun setProfileType(profileType: String) {
			getExtensionsToWrite()[PROFILE_TYPE_EXTENSION] = profileType
		}

		fun hasProfile(): Boolean {
			val profileType = getProfileType()
			return profileType != null && GAP_PROFILE_TYPE != profileType
		}

		fun isGap(): Boolean {
			val profileType = getProfileType()
			return GAP_PROFILE_TYPE == profileType
		}

		fun setGap() {
			setProfileType(GAP_PROFILE_TYPE)
		}

		fun removeProfileType() {
			getExtensionsToWrite().remove(PROFILE_TYPE_EXTENSION)
		}

		fun getTrkPtIndex(): Int {
			return try {
				getExtensionsToRead()[TRKPT_INDEX_EXTENSION]?.toInt() ?: -1
			} catch (e: NumberFormatException) {
				-1
			}
		}

		fun setTrkPtIndex(index: Int) {
			getExtensionsToWrite()[TRKPT_INDEX_EXTENSION] = index.toString()
		}

		override fun hashCode(): Int {
			return hash(name, category, desc, comment, lat, lon)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other == null || other !is WptPt) return false
			return other.name == name &&
					other.category == category &&
					other.lat == lat && other.lon == lon &&
					other.desc == desc
		}

		fun hasLocation(): Boolean {
			return lat != 0.0 && lon != 0.0
		}

		companion object {
			fun createAdjustedPoint(
				lat: Double,
				lon: Double,
				description: String,
				name: String,
				category: String,
				color: Int,
				iconName: String?,
				backgroundType: String?,
				amenityOriginName: String?,
				amenityExtensions: Map<String, String>?
			): WptPt {
				val latAdjusted = formatLatLon(lat).toDouble()
				val lonAdjusted = formatLatLon(lon).toDouble()
				val point = WptPt(
					latAdjusted,
					lonAdjusted,
					currentTimeMillis(),
					Double.NaN,
					0.0,
					Double.NaN
				)
				point.name = name
				point.category = category
				point.desc = description

				if (color != 0) {
					point.setColor(color)
				}
				if (iconName != null) {
					point.setIconName(iconName)
				}
				if (backgroundType != null) {
					point.setBackgroundType(backgroundType)
				}
				if (amenityOriginName != null) {
					point.setAmenityOriginName(amenityOriginName)
				}
				if (amenityExtensions != null) {
					point.getExtensionsToWrite().putAll(amenityExtensions)
				}
				return point
			}
		}

		fun updatePoint(pt: WptPt) {
			lat = formatLatLon(pt.lat).toDouble()
			lon = formatLatLon(pt.lon).toDouble()
			time = currentTimeMillis()
			desc = pt.desc
			name = pt.name
			category = pt.category

			val extensions = pt.getExtensionsToRead()
			val color = extensions[COLOR_NAME_EXTENSION]
			if (color != null) {
				setColor(color)
			}
			val iconName = extensions[ICON_NAME_EXTENSION]
			if (iconName != null) {
				setIconName(iconName)
			}
			val backgroundType = extensions[BACKGROUND_TYPE_EXTENSION]
			if (backgroundType != null) {
				setBackgroundType(backgroundType)
			}
			val address = extensions[ADDRESS_EXTENSION]
			if (address != null) {
				setAddress(address)
			}
			val hidden = extensions[HIDDEN_EXTENSION]
			if (hidden != null) {
				setHidden(hidden)
			}
		}
	}

	class TrkSegment : GpxExtensions() {
		var name: String? = null
		var generalSegment = false
		var points = mutableListOf<WptPt>()
		var renderer: Any? = null
		var routeSegments = mutableListOf<RouteSegment>()
		var routeTypes = mutableListOf<RouteType>()

		fun hasRoute(): Boolean {
			return routeSegments.isNotEmpty() && routeTypes.isNotEmpty()
		}

		fun splitByDistance(meters: Double, joinSegments: Boolean): List<GpxTrackAnalysis> {
			return split(DistanceSplitMetric(), TimeSplitMetric(), meters, joinSegments)
		}

		fun splitByTime(seconds: Int, joinSegments: Boolean): List<GpxTrackAnalysis> {
			return split(TimeSplitMetric(), DistanceSplitMetric(), seconds.toDouble(), joinSegments)
		}

		private fun split(
			metric: SplitMetric,
			secondaryMetric: SplitMetric,
			metricLimit: Double,
			joinSegments: Boolean
		): List<GpxTrackAnalysis> {
			val splitSegments = mutableListOf<SplitSegment>()
			SplitMetric.splitSegment(
				metric,
				secondaryMetric,
				metricLimit,
				splitSegments,
				this,
				joinSegments
			)
			return convert(splitSegments)
		}
	}

	class Track : GpxExtensions() {
		var name: String? = null
		var desc: String? = null
		var segments = mutableListOf<TrkSegment>()
		var generalTrack = false
	}

	class Route : GpxExtensions() {
		var name: String? = null
		var desc: String? = null
		var points = mutableListOf<WptPt>()
	}

	class Metadata : GpxExtensions {
		var name: String? = null
		var desc: String? = null
		var link: String? = null
		var keywords: String? = null
		var time: Long = 0
		var author: Author? = null
		var copyright: Copyright? = null
		var bounds: Bounds? = null

		constructor()

		constructor(source: Metadata) {
			name = source.name
			desc = source.desc
			link = source.link
			keywords = source.keywords
			time = source.time
			val sourceAuthor = source.author
			if (sourceAuthor != null) {
				author = Author(sourceAuthor)
			}
			val sourceCopyright = source.copyright
			if (sourceCopyright != null) {
				copyright = Copyright(sourceCopyright)
			}
			val sourceBounds = source.bounds
			if (sourceBounds != null) {
				bounds = Bounds(sourceBounds)
			}
			copyExtensions(source)
		}

		fun getArticleTitle(): String? {
			return getExtensionsToRead()["article_title"]
		}

		fun getArticleLang(): String? {
			return getExtensionsToRead()["article_lang"]
		}

		fun getDescription(): String? {
			return desc
		}

		fun readDescription() {
			val readDescription = getExtensionsToWrite().remove("desc")
			if (!Algorithms.isEmpty(readDescription)) {
				desc = if (Algorithms.isEmpty(desc)) {
					readDescription
				} else {
					"$desc; $readDescription"
				}
			}
		}
	}

	class Author : GpxExtensions {
		var name: String? = null
		var email: String? = null
		var link: String? = null

		constructor()

		constructor(author: Author) {
			name = author.name
			email = author.email
			link = author.link
			copyExtensions(author)
		}
	}

	class Copyright : GpxExtensions {
		var author: String? = null
		var year: String? = null
		var license: String? = null

		constructor()

		constructor(copyright: Copyright) {
			author = copyright.author
			year = copyright.year
			license = copyright.license
			copyExtensions(copyright)
		}
	}

	class Bounds : GpxExtensions {
		var minlat: Double = 0.0
		var minlon: Double = 0.0
		var maxlat: Double = 0.0
		var maxlon: Double = 0.0

		constructor()

		constructor(source: Bounds) {
			minlat = source.minlat
			minlon = source.minlon
			maxlat = source.maxlat
			maxlon = source.maxlon
			copyExtensions(source)
		}
	}

	class RouteSegment {
		var id: String? = null
		var length: String? = null
		var startTrackPointIndex: String? = null
		var segmentTime: String? = null
		var speed: String? = null
		var turnType: String? = null
		var turnLanes: String? = null
		var turnAngle: String? = null
		var skipTurn: String? = null
		var types: String? = null
		var pointTypes: String? = null
		var names: String? = null

		companion object {
			const val START_TRKPT_IDX_ATTR = "startTrkptIdx"

			fun fromStringBundle(bundle: StringBundle): RouteSegment {
				val s = RouteSegment()
				s.id = bundle.getString("id", null)
				s.length = bundle.getString("length", null)
				s.startTrackPointIndex = bundle.getString(START_TRKPT_IDX_ATTR, null)
				s.segmentTime = bundle.getString("segmentTime", null)
				s.speed = bundle.getString("speed", null)
				s.turnType = bundle.getString("turnType", null)
				s.turnLanes = bundle.getString("turnLanes", null)
				s.turnAngle = bundle.getString("turnAngle", null)
				s.skipTurn = bundle.getString("skipTurn", null)
				s.types = bundle.getString("types", null)
				s.pointTypes = bundle.getString("pointTypes", null)
				s.names = bundle.getString("names", null)
				return s
			}
		}

		fun toStringBundle(): StringBundle {
			val bundle = StringBundle()
			bundle.putString("id", id)
			bundle.putString("length", length)
			bundle.putString(START_TRKPT_IDX_ATTR, startTrackPointIndex)
			bundle.putString("segmentTime", segmentTime)
			bundle.putString("speed", speed)
			bundle.putString("turnType", turnType)
			bundle.putString("turnLanes", turnLanes)
			bundle.putString("turnAngle", turnAngle)
			bundle.putString("skipTurn", skipTurn)
			bundle.putString("types", types)
			bundle.putString("pointTypes", pointTypes)
			bundle.putString("names", names)
			return bundle
		}
	}

	class RouteType {
		var tag: String? = null
		var value: String? = null

		companion object {
			fun fromStringBundle(bundle: StringBundle): RouteType {
				val t = RouteType()
				t.tag = bundle.getString("t", null)
				t.value = bundle.getString("v", null)
				return t
			}
		}

		fun toStringBundle(): StringBundle {
			val bundle = StringBundle()
			bundle.putString("t", tag)
			bundle.putString("v", value)
			return bundle
		}
	}

	class PointsGroup(var name: String) {
		var iconName: String? = null
		var backgroundType: String? = null
		var points = mutableListOf<WptPt>()
		var color: Int = 0
		var hidden = false

		constructor(name: String, iconName: String?, backgroundType: String?, color: Int) : this(
			name
		) {
			this.iconName = iconName
			this.backgroundType = backgroundType
			this.color = color
		}

		constructor(point: WptPt) : this(point.category ?: "") {
			color = point.getColor()
			iconName = point.getIconName()
			backgroundType = point.getBackgroundType()
		}

		fun isHidden(): Boolean {
			return hidden
		}

		override fun hashCode(): Int {
			return hash(name, iconName, backgroundType, color, points, hidden)
		}

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other == null || other !is PointsGroup) return false
			return color == other.color &&
					hidden == other.hidden &&
					name == other.name &&
					iconName == other.iconName &&
					backgroundType == other.backgroundType &&
					points == other.points
		}

		fun toStringBundle(): StringBundle {
			val bundle = StringBundle()
			bundle.putString("name", name)
			if (color != 0) {
				bundle.putString("color", Algorithms.colorToString(color))
			}
			if (!Algorithms.isEmpty(iconName)) {
				bundle.putString(ICON_NAME_EXTENSION, iconName)
			}
			if (!Algorithms.isEmpty(backgroundType)) {
				bundle.putString(BACKGROUND_TYPE_EXTENSION, backgroundType)
			}
			if (isHidden()) {
				bundle.putBoolean(HIDDEN_EXTENSION, true)
			}
			return bundle
		}

		companion object {
			fun parsePointsGroupAttributes(parser: XmlPullParser): PointsGroup {
				val name = parser.getAttributeValue("", "name")
				val category = PointsGroup(name ?: "")
				category.color = parseColor(parser.getAttributeValue("", "color"), 0)
				category.iconName = parser.getAttributeValue("", ICON_NAME_EXTENSION)
				category.backgroundType = parser.getAttributeValue("", BACKGROUND_TYPE_EXTENSION)
				category.hidden = parser.getAttributeValue("", HIDDEN_EXTENSION).toBoolean()
				return category
			}
		}
	}

	private fun convert(splitSegments: List<SplitSegment>): List<GpxTrackAnalysis> {
		val list = mutableListOf<GpxTrackAnalysis>()
		for (segment in splitSegments) {
			val analysis = GpxTrackAnalysis()
			analysis.prepareInformation(0, null, segment)
			list.add(analysis)
		}
		return list
	}

	fun calculateBounds(pts: List<WptPt>): QuadRect {
		val trackBounds = QuadRect(
			Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
			Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
		)
		updateBounds(trackBounds, pts, 0)
		return trackBounds
	}

	fun calculateTrackBounds(segments: List<TrkSegment>): QuadRect {
		val trackBounds = QuadRect(
			Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
			Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
		)
		var updated = false
		for (segment in segments) {
			if (segment.points.isNotEmpty()) {
				updateBounds(trackBounds, segment.points, 0)
				updated = true
			}
		}
		return if (updated) trackBounds else QuadRect()
	}

	fun updateBounds(trackBounds: QuadRect, pts: List<WptPt>, startIndex: Int) {
		for (i in startIndex until pts.size) {
			val pt = pts[i]
			trackBounds.right = maxOf(trackBounds.right, pt.lon)
			trackBounds.left = minOf(trackBounds.left, pt.lon)
			trackBounds.top = maxOf(trackBounds.top, pt.lat)
			trackBounds.bottom = minOf(trackBounds.bottom, pt.lat)
		}
	}

	fun calculateTrackPoints(segments: List<TrkSegment>): Int {
		return segments.sumOf { it.points.size }
	}

	fun updateQR(q: QuadRect, p: WptPt, defLat: Double, defLon: Double) {
		if (q.left == defLon && q.top == defLat &&
			q.right == defLon && q.bottom == defLat
		) {
			q.left = p.getLongitude()
			q.right = p.getLongitude()
			q.top = p.getLatitude()
			q.bottom = p.getLatitude()
		} else {
			q.left = minOf(q.left, p.getLongitude())
			q.right = maxOf(q.right, p.getLongitude())
			q.top = maxOf(q.top, p.getLatitude())
			q.bottom = minOf(q.bottom, p.getLatitude())
		}
	}

	fun asString(file: GpxFile): String {
		val writer = Buffer()
		writeGpx(writer, file, null)
		return writer.toString()
	}

	fun writeGpxFile(fout: CommonFile, file: GpxFile): Exception? {
		var output: Sink? = null
		return try {
			fout.createDirectories()
			output = fout.sink()
			if (Algorithms.isEmpty(file.path)) {
				file.path = (if (fout.isAbsolute()) fout.path() else
					fout.absolutePath())
			}
			writeGpx(output, file, null)
		} catch (e: Exception) {
			log.error("Failed to write gpx '$fout.path()'", e)
			e
		} finally {
			output?.close()
		}
	}

	fun writeGpx(output: Sink, file: GpxFile, progress: IProgress?): Exception? {
		progress?.startWork(file.getItemsToWriteSize())
		return try {
			val serializer = XmlSerializer()
			serializer.setOutput(output)
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
			serializer.startDocument("UTF-8", true)
			serializer.startTag(null, "gpx")
			serializer.attribute(null, "version", "1.1")
			val author = file.author
			if (author != null) {
				serializer.attribute(null, "creator", author)
			}
			serializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1")
			serializer.attribute(null, "xmlns:osmand", "https://osmand.net")
			serializer.attribute(
				null,
				"xmlns:gpxtpx",
				"http://www.garmin.com/xmlschemas/TrackPointExtension/v1"
			)
			serializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
			serializer.attribute(
				null,
				"xsi:schemaLocation",
				"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"
			)

			assignPointsGroupsExtensionWriter(file)
			writeMetadata(serializer, file, progress)
			writePoints(serializer, file, progress)
			writeRoutes(serializer, file, progress)
			writeTracks(serializer, file, progress)
			writeExtensions(serializer, file, progress)

			serializer.endTag(null, "gpx")
			serializer.endDocument()
			serializer.flush()
			null
		} catch (e: Exception) {
			log.error("Failed to write gpx", e)
			e
		}
	}

	fun createNetworkRouteExtensionWriter(networkRouteTags: Map<String, String>): GpxExtensionsWriter {
		return object : GpxExtensionsWriter {
			override fun writeExtensions(serializer: XmlSerializer) {
				val bundle = StringBundle()
				val tagsBundle = StringBundle()
				tagsBundle.putString("type", networkRouteTags["type"])
				for ((key, value) in networkRouteTags) {
					tagsBundle.putString(key, value)
				}
				val routeKeyBundle = mutableListOf(tagsBundle)
				bundle.putBundleList(
					"network_route",
					OSMAND_EXTENSIONS_PREFIX + "route_key",
					routeKeyBundle
				)
				val bundleWriter = StringBundleXmlWriter(bundle, serializer)
				bundleWriter.writeBundle()
			}
		}
	}

	private fun assignPointsGroupsExtensionWriter(gpxFile: GpxFile) {
		if (!Algorithms.isEmpty(gpxFile.pointsGroups) && gpxFile.extensionsWriter == null) {
			gpxFile.extensionsWriter = object : GpxExtensionsWriter {
				override fun writeExtensions(serializer: XmlSerializer) {
					val bundle = StringBundle()
					val categoriesBundle = mutableListOf<StringBundle>()
					for (group in gpxFile.pointsGroups.values) {
						categoriesBundle.add(group.toStringBundle())
					}
					bundle.putBundleList("points_groups", "group", categoriesBundle)
					val bundleWriter = StringBundleXmlWriter(bundle, serializer)
					bundleWriter.writeBundle()
				}
			}
		}
	}

	private fun writeMetadata(serializer: XmlSerializer, file: GpxFile, progress: IProgress?) {
		val defName = file.metadata.name
		val trackName = if (!Algorithms.isEmpty(defName)) defName else getFilename(file.path)
		serializer.startTag(null, "metadata")
		writeNotNullText(serializer, "name", trackName)
		writeNotNullText(serializer, "desc", file.metadata.desc)
		val author = file.metadata.author
		if (author != null) {
			serializer.startTag(null, "author")
			writeAuthor(serializer, author)
			serializer.endTag(null, "author")
		}
		val copyright = file.metadata.copyright
		if (copyright != null) {
			serializer.startTag(null, "copyright")
			writeCopyright(serializer, copyright)
			serializer.endTag(null, "copyright")
		}
		writeNotNullTextWithAttribute(serializer, "link", "href", file.metadata.link)
		if (file.metadata.time != 0L) {
			writeNotNullText(serializer, "time", formatTime(file.metadata.time))
		}
		writeNotNullText(serializer, "keywords", file.metadata.keywords)
		val bounds = file.metadata.bounds
		if (bounds != null) {
			writeBounds(serializer, bounds)
		}
		writeExtensions(serializer, file.metadata, null)
		progress?.progress(1)
		serializer.endTag(null, "metadata")
	}

	private fun writePoints(serializer: XmlSerializer, file: GpxFile, progress: IProgress?) {
		for (l in file.getPointsList()) {
			serializer.startTag(null, "wpt")
			writeWpt(serializer, l, progress)
			serializer.endTag(null, "wpt")
		}
	}

	private fun writeRoutes(serializer: XmlSerializer, file: GpxFile, progress: IProgress?) {
		for (route in file.routes) {
			serializer.startTag(null, "rte")
			writeNotNullText(serializer, "name", route.name)
			writeNotNullText(serializer, "desc", route.desc)
			for (p in route.points) {
				serializer.startTag(null, "rtept")
				writeWpt(serializer, p, progress)
				serializer.endTag(null, "rtept")
			}
			writeExtensions(serializer, route, null)
			serializer.endTag(null, "rte")
		}
	}

	private fun writeTracks(serializer: XmlSerializer, file: GpxFile, progress: IProgress?) {
		for (track in file.tracks) {
			if (!track.generalTrack) {
				serializer.startTag(null, "trk")
				writeNotNullText(serializer, "name", track.name)
				writeNotNullText(serializer, "desc", track.desc)
				for (segment in track.segments) {
					serializer.startTag(null, "trkseg")
					writeNotNullText(serializer, "name", segment.name)
					for (p in segment.points) {
						serializer.startTag(null, "trkpt")
						writeWpt(serializer, p, progress)
						serializer.endTag(null, "trkpt")
					}
					assignRouteExtensionWriter(segment)
					writeExtensions(serializer, segment, null)
					serializer.endTag(null, "trkseg")
				}
				writeExtensions(serializer, track, null)
				serializer.endTag(null, "trk")
			}
		}
	}

	private fun assignRouteExtensionWriter(segment: TrkSegment) {
		if (segment.hasRoute() && segment.extensionsWriter == null) {
			segment.extensionsWriter = object : GpxExtensionsWriter {
				override fun writeExtensions(serializer: XmlSerializer) {
					val bundle = StringBundle()
					val segmentsBundle = mutableListOf<StringBundle>()
					for (routeSegment in segment.routeSegments) {
						segmentsBundle.add(routeSegment.toStringBundle())
					}
					bundle.putBundleList("route", "segment", segmentsBundle)
					val typesBundle = mutableListOf<StringBundle>()
					for (routeType in segment.routeTypes) {
						typesBundle.add(routeType.toStringBundle())
					}
					bundle.putBundleList("types", "type", typesBundle)
					val bundleWriter = StringBundleXmlWriter(bundle, serializer)
					bundleWriter.writeBundle()
				}
			}
		}
	}

	private fun getFilename(path: String?): String? {
		var newPath = path
		if (newPath != null) {
			var i = newPath.lastIndexOf('/')
			if (i > 0) {
				newPath = newPath.substring(i + 1)
			}
			i = newPath.lastIndexOf('.')
			if (i > 0) {
				newPath = newPath.substring(0, i)
			}
		}
		return newPath
	}

	private fun writeNotNullTextWithAttribute(
		serializer: XmlSerializer,
		tag: String,
		attribute: String,
		value: String?
	) {
		if (value != null) {
			serializer.startTag(null, tag)
			serializer.attribute(null, attribute, value)
			serializer.endTag(null, tag)
		}
	}

	fun writeNotNullText(serializer: XmlSerializer, tag: String, value: String?) {
		if (value != null) {
			serializer.startTag(null, tag)
			serializer.text(value)
			serializer.endTag(null, tag)
		}
	}

	private fun writeExtensions(serializer: XmlSerializer, p: GpxExtensions, progress: IProgress?) {
		writeExtensions(serializer, p.getExtensionsToRead(), p, progress)
	}

	private fun writeExtensions(
		serializer: XmlSerializer,
		extensions: Map<String, String>?,
		p: GpxExtensions,
		progress: IProgress?
	) {
		val extensionsWriter = p.extensionsWriter
		val additionalExtensionsWriter = p.additionalExtensionsWriter
		val hasExtensions = !extensions.isNullOrEmpty()
		if (hasExtensions || extensionsWriter != null) {
			serializer.startTag(null, "extensions")
			if (hasExtensions) {
				for ((key, value) in extensions!!) {
					writeNotNullText(serializer, getOsmandTagKey(key, value), value)
				}
			}
			if (additionalExtensionsWriter != null) {
				serializer.startTag(null, "gpxtpx:TrackPointExtension")
				additionalExtensionsWriter.writeExtensions(serializer)
				serializer.endTag(null, "gpxtpx:TrackPointExtension")
			}
			extensionsWriter?.writeExtensions(serializer)
			serializer.endTag(null, "extensions")
			progress?.progress(1)
		}
	}

	private fun writeWpt(serializer: XmlSerializer, p: WptPt, progress: IProgress?) {
		serializer.attribute(null, "lat", formatLatLon(p.lat))
		serializer.attribute(null, "lon", formatLatLon(p.lon))
		if (!p.ele.isNaN()) {
			writeNotNullText(serializer, POINT_ELEVATION, formatDecimal(p.ele))
		}
		if (p.time != 0L) {
			writeNotNullText(serializer, "time", formatTime(p.time))
		}
		writeNotNullText(serializer, "name", p.name)
		writeNotNullText(serializer, "desc", p.desc)
		writeNotNullTextWithAttribute(serializer, "link", "href", p.link)
		writeNotNullText(serializer, "type", p.category)
		writeNotNullText(serializer, "cmt", p.comment)
		if (!p.hdop.isNaN()) {
			writeNotNullText(serializer, "hdop", formatDecimal(p.hdop))
		}
		if (p.speed > 0) {
			p.getExtensionsToWrite()[POINT_SPEED] = formatDecimal(p.speed)
		}
		if (!p.heading.isNaN()) {
			p.getExtensionsToWrite()["heading"] = round(p.heading).toString()
		}
		val extensions = p.getExtensionsToRead().toMutableMap()
		if (serializer.getName() != "rtept") {
			extensions.remove(PROFILE_TYPE_EXTENSION)
			extensions.remove(TRKPT_INDEX_EXTENSION)
		} else {
			val profile = extensions[PROFILE_TYPE_EXTENSION]
			if (GAP_PROFILE_TYPE == profile) {
				extensions.remove(PROFILE_TYPE_EXTENSION)
			}
		}
		assignExtensionWriter(p, extensions)
		writeExtensions(serializer, null, p, null)
		progress?.progress(1)
	}

	fun assignExtensionWriter(wptPt: WptPt, pluginsExtensions: Map<String, String>) {
		if (wptPt.extensionsWriter == null) {
			val regularExtensions = HashMap<String, String>()
			val gpxtpxExtensions = HashMap<String, String>()
			for ((key, value) in pluginsExtensions) {
				if (key.startsWith(GPXTPX_PREFIX)) {
					gpxtpxExtensions[key] = value
				} else {
					regularExtensions[key] = value
				}
			}
			wptPt.extensionsWriter = createExtensionsWriter(regularExtensions, true)
			if (gpxtpxExtensions.isNotEmpty()) {
				wptPt.additionalExtensionsWriter = createExtensionsWriter(gpxtpxExtensions, false)
			}
		}
	}

	private fun createExtensionsWriter(
		extensions: Map<String, String>,
		addOsmandPrefix: Boolean
	): GpxExtensionsWriter {
		return object : GpxExtensionsWriter {
			override fun writeExtensions(serializer: XmlSerializer) {
				for ((key, value) in extensions) {
					try {
						writeNotNullText(
							serializer,
							if (addOsmandPrefix) getOsmandTagKey(key, value) else key,
							value
						)
					} catch (e: IOException) {
						log.error("Error writeExtensions", e)
					}
				}
			}
		}
	}

	private fun getOsmandTagKey(key: String, value: String): String {
		var newKey = key
		if (newKey.startsWith(OSMAND_EXTENSIONS_PREFIX)) {
			newKey = newKey.replace(OSMAND_EXTENSIONS_PREFIX, "")
		}
		newKey = newKey.replace(":", "_-_")
		return OSMAND_EXTENSIONS_PREFIX + newKey
	}

	private fun writeAuthor(serializer: XmlSerializer, author: Author) {
		writeNotNullText(serializer, "name", author.name)
		if (author.email != null && author.email!!.contains("@")) {
			val idAndDomain = author.email!!.split("@")
			if (idAndDomain.size == 2 && idAndDomain[0].isNotEmpty() && idAndDomain[1].isNotEmpty()) {
				serializer.startTag(null, "email")
				serializer.attribute(null, "id", idAndDomain[0])
				serializer.attribute(null, "domain", idAndDomain[1])
				serializer.endTag(null, "email")
			}
		}
		writeNotNullTextWithAttribute(serializer, "link", "href", author.link)
	}

	private fun writeCopyright(serializer: XmlSerializer, copyright: Copyright) {
		val author = copyright.author
		if (author != null) {
			serializer.attribute(null, "author", author)
		}
		writeNotNullText(serializer, "year", copyright.year)
		writeNotNullText(serializer, "license", copyright.license)
	}

	private fun writeBounds(serializer: XmlSerializer, bounds: Bounds) {
		serializer.startTag(null, "bounds")
		serializer.attribute(null, "minlat", formatLatLon(bounds.minlat))
		serializer.attribute(null, "minlon", formatLatLon(bounds.minlon))
		serializer.attribute(null, "maxlat", formatLatLon(bounds.maxlat))
		serializer.attribute(null, "maxlon", formatLatLon(bounds.maxlon))
		serializer.endTag(null, "bounds")
	}

	@Throws(XmlParserException::class, IOException::class)
	fun readText(parser: XmlPullParser, key: String): String? {
		var tok: Int
		var text: StringBuilder? = null
		while (parser.next().also { tok = it } != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG && parser.getName() == key) {
				break
			} else if (tok == XmlPullParser.TEXT) {
				if (text == null) {
					text = StringBuilder()
				}
				text.append(parser.getText())
			}
		}
		return text?.toString()
	}

	@Throws(XmlParserException::class, IOException::class)
	fun readTextMap(parser: XmlPullParser, key: String): Map<String, String> {
		var tok: Int
		var text: StringBuilder? = null
		val result: MutableMap<String, String> = HashMap()
		while (parser.next().also { tok = it } != XmlPullParser.END_DOCUMENT) {
			if (tok == XmlPullParser.END_TAG) {
				val tag = parser.getName()
				if (tag != null && text != null && text.toString().trim().isNotEmpty()) {
					result[tag] = text.toString()
				}
				if (tag == key) {
					break
				}
				text = null
			} else if (tok == XmlPullParser.START_TAG) {
				text = null
			} else if (tok == XmlPullParser.TEXT) {
				if (text == null) {
					text = StringBuilder()
				}
				text.append(parser.getText())
			}
		}
		return result
	}

	fun formatTime(time: Long): String {
		val format = getTimeFormatter()
		return format.format(Instant.fromEpochMilliseconds(time).toLocalDateTime(TimeZone.UTC))
	}

	fun parseTime(text: String): Long {
		return parseTime(text, getTimeFormatterTZ())
	}

	fun parseTime(text: String, format: DateTimeFormat<LocalDateTime>): Long {
		var time: Long = 0
		try {
			time = flexibleGpxTimeParser(text, format)
		} catch (e: Exception) {
			try {
				time = getTimeNoTimeZoneFormatter().parse(text).toInstant(TimeZone.UTC)
					.toEpochMilliseconds()
			} catch (e: Exception) {
				log.error("Failed to parse date $text", e)
			}
		}
		return time
	}

	@Throws(Exception::class)
	private fun flexibleGpxTimeParser(
		timeStr: String,
		parser: DateTimeFormat<LocalDateTime>
	): Long {
		var text = timeStr
		var ms = 0.0
		val isIndex = text.indexOf('.')
		if (isIndex > 0) {
			var esIndex = isIndex + 1
			while (esIndex < text.length && text[esIndex].isDigit()) {
				esIndex++
			}
			ms = ("0" + text.substring(isIndex, esIndex)).toDouble()
			text = text.substring(0, isIndex) + text.substring(esIndex)
		}
		return parser.parse(text).toInstant(TimeZone.UTC)
			.toEpochMilliseconds() + (ms * 1000).toLong()
	}

	fun getCreationTime(gpxFile: GpxFile?): Long {
		var time: Long = 0
		if (gpxFile != null) {
			time = if (gpxFile.metadata.time > 0) {
				gpxFile.metadata.time
			} else {
				gpxFile.getLastPointTime()
			}
			if (time == 0L) {
				time = gpxFile.modifiedTime
			}
		}
		if (time == 0L) {
			time = currentTimeMillis()
		}
		return time
	}

	private fun getTimeFormatter(): DateTimeFormat<LocalDateTime> {
		@OptIn(FormatStringsInDatetimeFormats::class)
		return LocalDateTime.Format {
			byUnicodePattern(GPX_TIME_PATTERN)
		}
	}

	private fun getTimeNoTimeZoneFormatter(): DateTimeFormat<LocalDateTime> {
		@OptIn(FormatStringsInDatetimeFormats::class)
		return LocalDateTime.Format {
			byUnicodePattern(GPX_TIME_NO_TIMEZONE_PATTERN)
		}
	}

	private fun getTimeFormatterTZ(): DateTimeFormat<LocalDateTime> {
		@OptIn(FormatStringsInDatetimeFormats::class)
		return LocalDateTime.Format {
			byUnicodePattern(GPX_TIME_PATTERN_TZ)
		}
	}

	fun loadGpxFile(file: CommonFile): GpxFile {
		return loadGpxFile(file, null, true)
	}

	fun loadGpxFile(
		file: CommonFile,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile {
		var source: Source? = null
		return try {
			source = file.source()
			val gpxFile = loadGpxFile(source, extensionsReader, addGeneralTrack)
			gpxFile.path = file.absolutePath()
			gpxFile.modifiedTime = file.lastModified()
			gpxFile.pointsModifiedTime = gpxFile.modifiedTime
			if (gpxFile.error != null) {
				log.info("Error reading gpx ${gpxFile.path}")
			}
			gpxFile
		} catch (e: IOException) {
			val gpxFile = GpxFile(null)
			gpxFile.path = file.absolutePath()
			log.error("Error reading gpx ${gpxFile.path}", e)
			gpxFile.error = e
			gpxFile
		} finally {
			source?.close()
		}
	}

	fun loadGpxFile(source: Source): GpxFile {
		return loadGpxFile(source, null, true)
	}

	fun loadGpxFile(
		source: Source,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile {
		val gpxFile = GpxFile(null)
		gpxFile.metadata.time = 0
		try {
			val parser = XmlPullParser()
			parser.setInput(source, "UTF-8")
			val routeTrack = Track()
			val routeTrackSegment = TrkSegment()
			routeTrack.segments.add(routeTrackSegment)
			val parserState = ArrayDeque<GpxExtensions>()
			var firstSegment: TrkSegment? = null
			var extensionReadMode = false
			var routePointExtension = false
			val routeSegments = mutableListOf<RouteSegment>()
			val routeTypes = mutableListOf<RouteType>()
			val pointsGroups = mutableListOf<PointsGroup>()
			var routeExtension = false
			var typesExtension = false
			var pointsGroupsExtension = false
			var networkRoute = false
			parserState.add(gpxFile)
			var tok: Int
			while (parser.next().also { tok = it } != XmlPullParser.END_DOCUMENT) {
				if (tok == XmlPullParser.START_TAG) {
					val parse = parserState.lastOrNull()
					val tag = parser.getName() ?: ""
					if (extensionReadMode && parse != null && !routePointExtension) {
						val tagName = tag.lowercase()
						when {
							routeExtension && tagName == "segment" -> {
								val segment = parseRouteSegmentAttributes(parser)
								routeSegments.add(segment)
							}

							typesExtension && tagName == "type" -> {
								val type = parseRouteTypeAttributes(parser)
								routeTypes.add(type)
							}

							pointsGroupsExtension && tagName == "group" -> {
								val pointsGroup = PointsGroup.parsePointsGroupAttributes(parser)
								pointsGroups.add(pointsGroup)
							}

							networkRoute && tagName == "route_key" -> {
								gpxFile.addRouteKeyTags(parseRouteKeyAttributes(parser))
							}

							tagName == "routepointextension" -> {
								routePointExtension = true
								if (parse is WptPt) {
									parse.getExtensionsToWrite()["offset"] =
										routeTrackSegment.points.size.toString()
								}
							}

							tagName == "route" -> routeExtension = true
							tagName == "types" -> typesExtension = true
							tagName == "points_groups" -> pointsGroupsExtension = true
							tagName == "network_route" -> networkRoute = true
							else -> {
								if (extensionsReader == null || !extensionsReader.readExtensions(
										gpxFile,
										parser
									)
								) {
									val values = readTextMap(parser, tag)
									if (values.isNotEmpty()) {
										for ((t, value) in values) {
											val supportedTag =
												getExtensionsSupportedTag(t.lowercase())
											parse.getExtensionsToWrite()[supportedTag] = value
											if (parse is WptPt) {
												when (tag) {
													POINT_SPEED -> {
														try {
															parse.speed = value.toDouble()
														} catch (e: NumberFormatException) {
															println(e.message)
														}
													}

													POINT_BEARING -> {
														try {
															parse.bearing = value.toFloat()
														} catch (ignored: NumberFormatException) {
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} else if (parse != null && tag == "extensions") {
						extensionReadMode = true
					} else if (routePointExtension) {
						if (tag == "rpt") {
							val wptPt = parseWptAttributes(parser)
							routeTrackSegment.points.add(wptPt)
							parserState.add(wptPt)
						}
					} else {
						when (parse) {
							is GpxFile -> {
								when (tag) {
									"gpx" -> parse.author = parser.getAttributeValue("", "creator")
									"metadata" -> {
										val metadata = Metadata()
										parse.metadata = metadata
										parserState.add(metadata)
									}

									"trk" -> {
										val track = Track()
										parse.tracks.add(track)
										parserState.add(track)
									}

									"rte" -> {
										val route = Route()
										parse.routes.add(route)
										parserState.add(route)
									}

									"wpt" -> {
										val wptPt = parseWptAttributes(parser)
										parse.addParsedPoint(wptPt)
										parserState.add(wptPt)
									}
								}
							}

							is Metadata -> {
								when (tag) {
									"name" -> parse.name = readText(parser, "name")
									"desc" -> parse.desc = readText(parser, "desc")
									"author" -> {
										val author = Author()
										author.name = parser.getText()
										parse.author = author
										parserState.add(author)
									}

									"copyright" -> {
										val copyright = Copyright()
										copyright.license = parser.getText()
										copyright.author = parser.getAttributeValue("", "author")
										parse.copyright = copyright
										parserState.add(copyright)
									}

									"link" -> parse.link = parser.getAttributeValue("", "href")
									"time" -> {
										val text = readText(parser, "time")
										parse.time = parseTime(text!!)
									}

									"keywords" -> parse.keywords = readText(parser, "keywords")
									"bounds" -> {
										val bounds = parseBoundsAttributes(parser)
										parse.bounds = bounds
										parserState.add(bounds)
									}
								}
							}

							is Author -> {
								when (tag) {
									"name" -> parse.name = readText(parser, "name")
									"email" -> {
										val id = parser.getAttributeValue("", "id")
										val domain = parser.getAttributeValue("", "domain")
										if (!Algorithms.isEmpty(id) && !Algorithms.isEmpty(domain)) {
											parse.email = "$id@$domain"
										}
									}

									"link" -> parse.link = parser.getAttributeValue("", "href")
								}
							}

							is Copyright -> {
								when (tag) {
									"year" -> parse.year = readText(parser, "year")
									"license" -> parse.license = readText(parser, "license")
								}
							}

							is Route -> {
								when (tag) {
									"name" -> parse.name = readText(parser, "name")
									"desc" -> parse.desc = readText(parser, "desc")
									"rtept" -> {
										val wptPt = parseWptAttributes(parser)
										parse.points.add(wptPt)
										parserState.add(wptPt)
									}
								}
							}

							is Track -> {
								when (tag) {
									"name" -> parse.name = readText(parser, "name")
									"desc" -> parse.desc = readText(parser, "desc")
									"trkseg" -> {
										val trkSeg = TrkSegment()
										parse.segments.add(trkSeg)
										parserState.add(trkSeg)
									}

									"trkpt", "rpt" -> {
										val wptPt = parseWptAttributes(parser)
										if (parse.segments.isEmpty()) {
											parse.segments.add(TrkSegment())
										}
										parse.segments.last().points.add(wptPt)
										parserState.add(wptPt)
									}
								}
							}

							is TrkSegment -> {
								when (tag) {
									"name" -> parse.name = readText(parser, "name")
									"trkpt", "rpt" -> {
										val wptPt = parseWptAttributes(parser)
										parse.points.add(wptPt)
										parserState.add(wptPt)
									}

									"csvattributes" -> {
										val segmentPoints = readText(parser, "csvattributes")
										val pointsArr = segmentPoints?.split("\n")?.toTypedArray()
										for (i in pointsArr?.indices!!) {
											val pointAttrs = pointsArr[i].split(",").toTypedArray()
											try {
												if (pointAttrs.size > 1) {
													val wptPt = WptPt()
													wptPt.lon = pointAttrs[0].toDouble()
													wptPt.lat = pointAttrs[1].toDouble()
													parse.points.add(wptPt)
													if (pointAttrs.size > 2) {
														wptPt.ele = pointAttrs[2].toDouble()
													}
												}
											} catch (_: NumberFormatException) {
											}
										}
									}
								}
							}

							is WptPt -> {
								when (tag) {
									"name" -> parse.name = readText(parser, "name")
									"desc" -> parse.desc = readText(parser, "desc")
									"cmt" -> parse.comment = readText(parser, "cmt")
									POINT_SPEED -> {
										try {
											val value = readText(parser, POINT_SPEED)
											if (!value.isNullOrEmpty()) {
												parse.speed = value.toDouble()
												parse.getExtensionsToWrite()[POINT_SPEED] = value
											}
										} catch (_: NumberFormatException) {
										}
									}

									"link" -> parse.link = parser.getAttributeValue("", "href")
									"category" -> parse.category = readText(parser, "category")
									"type" -> {
										if (parse.category == null) {
											parse.category = readText(parser, "type")
										}
									}

									POINT_ELEVATION -> {
										val text = readText(parser, POINT_ELEVATION)
										if (text != null) {
											try {
												parse.ele = text.toDouble()
											} catch (_: NumberFormatException) {
											}
										}
									}

									"hdop" -> {
										val text = readText(parser, "hdop")
										if (text != null) {
											try {
												parse.hdop = text.toDouble()
											} catch (_: NumberFormatException) {
											}
										}
									}

									"time" -> {
										val text = readText(parser, "time")
										parse.time = parseTime(text!!)
									}
								}
							}
						}
					}
				} else if (tok == XmlPullParser.END_TAG) {
					val parse = parserState.lastOrNull()
					val tag = parser.getName()

					if (tag.equals("routepointextension", ignoreCase = true)) {
						routePointExtension = false
					}
					if (parse != null && tag == "extensions") {
						extensionReadMode = false
					}
					if (extensionReadMode && tag == "route") {
						routeExtension = false
						continue
					}
					if (extensionReadMode && tag == "types") {
						typesExtension = false
						continue
					}
					if (extensionReadMode && tag == "network_route") {
						networkRoute = false
						continue
					}

					when (tag) {
						"metadata" -> {
							val pop = parserState.removeLast() as Metadata
							pop.readDescription()
						}

						"author" -> {
							if (parse is Author) {
								parserState.removeLast()
							}
						}

						"copyright" -> {
							if (parse is Copyright) {
								parserState.removeLast()
							}
						}

						"bounds" -> {
							if (parse is Bounds) {
								parserState.removeLast()
							}
						}

						"trkpt" -> {
							val pop = parserState.removeLast()
						}

						"wpt" -> {
							val pop = parserState.removeLast()
						}

						"rtept" -> {
							val pop = parserState.removeLast()
						}

						"trk" -> {
							val pop = parserState.removeLast()
						}

						"rte" -> {
							val pop = parserState.removeLast()
						}

						"trkseg" -> {
							val pop = parserState.removeLast()
							if (pop is TrkSegment) {
								pop.routeSegments = routeSegments
								pop.routeTypes = routeTypes
								routeSegments.clear()
								routeTypes.clear()
								if (firstSegment == null) {
									firstSegment = pop
								}
							}
						}

						"rpt" -> {
							val pop = parserState.removeLast()
						}
					}
				}
			}
			if (routeTrackSegment.points.isNotEmpty()) {
				gpxFile.tracks.add(routeTrack)
			}
			if (routeSegments.isNotEmpty() && routeTypes.isNotEmpty() && firstSegment != null) {
				firstSegment.routeSegments = routeSegments
				firstSegment.routeTypes = routeTypes
			}
			if (pointsGroups.isNotEmpty() || !gpxFile.isPointsEmpty()) {
				gpxFile.pointsGroups.putAll(mergePointsGroups(pointsGroups, gpxFile.getPointsList()))
			}
			if (addGeneralTrack) {
				gpxFile.addGeneralTrack()
			}
			if (gpxFile.metadata.time == 0L) {
				gpxFile.metadata.time = getCreationTime(gpxFile)
			}
		} catch (e: Exception) {
			gpxFile.error = e
		}

		return gpxFile
	}

	private fun getExtensionsSupportedTag(tag: String): String {
		val supportedTag = SUPPORTED_EXTENSION_TAGS[tag]
		return supportedTag ?: tag
	}

	private fun parseRouteKeyAttributes(parser: XmlPullParser): Map<String, String> {
		val networkRouteKeyTags: MutableMap<String, String> = LinkedHashMap()
		val reader = StringBundleXmlReader(parser)
		reader.readBundle()
		val bundle = reader.getBundle()
		if (!bundle.isEmpty()) {
			for (item in bundle.getMap().values) {
				if (item.type == StringBundle.ItemType.STRING) {
					networkRouteKeyTags[item.name] = item.value as String
				}
			}
		}
		return networkRouteKeyTags
	}

	private fun mergePointsGroups(
		groups: List<PointsGroup>,
		points: List<WptPt>
	): Map<String, PointsGroup> {
		val pointsGroups: MutableMap<String, PointsGroup> = LinkedHashMap()
		for (category in groups) {
			pointsGroups[category.name] = category
		}
		for (point in points) {
			val categoryName = point.category ?: ""
			var pointsGroup = pointsGroups[categoryName]
			if (pointsGroup == null) {
				pointsGroup = PointsGroup(point)
				pointsGroups[categoryName] = pointsGroup
			}
			val color = point.getColor()
			if (pointsGroup.color == 0 && color != 0) {
				pointsGroup.color = color
			}
			val iconName = point.getIconName()
			if (Algorithms.isEmpty(pointsGroup.iconName) && !Algorithms.isEmpty(iconName)) {
				pointsGroup.iconName = iconName
			}
			val backgroundType = point.getBackgroundType()
			if (Algorithms.isEmpty(pointsGroup.backgroundType) && !Algorithms.isEmpty(backgroundType)) {
				pointsGroup.backgroundType = backgroundType
			}
			pointsGroup.points.add(point)
		}
		return pointsGroups
	}

	private fun parseWptAttributes(parser: XmlPullParser): WptPt {
		val wpt = WptPt()
		try {
			val latStr = parser.getAttributeValue("", "lat")
			val lonStr = parser.getAttributeValue("", "lon")
			if (!latStr.isNullOrEmpty() && !lonStr.isNullOrEmpty()) {
				wpt.lat = latStr.toDouble()
				wpt.lon = lonStr.toDouble()
			}
		} catch (_: NumberFormatException) {
		}
		return wpt
	}

	private fun parseRouteSegmentAttributes(parser: XmlPullParser): RouteSegment {
		val segment = RouteSegment()
		segment.id = parser.getAttributeValue("", "id")
		segment.length = parser.getAttributeValue("", "length")
		segment.startTrackPointIndex =
			parser.getAttributeValue("", RouteSegment.START_TRKPT_IDX_ATTR)
		segment.segmentTime = parser.getAttributeValue("", "segmentTime")
		segment.speed = parser.getAttributeValue("", "speed")
		segment.turnType = parser.getAttributeValue("", "turnType")
		segment.turnLanes = parser.getAttributeValue("", "turnLanes")
		segment.turnAngle = parser.getAttributeValue("", "turnAngle")
		segment.skipTurn = parser.getAttributeValue("", "skipTurn")
		segment.types = parser.getAttributeValue("", "types")
		segment.pointTypes = parser.getAttributeValue("", "pointTypes")
		segment.names = parser.getAttributeValue("", "names")
		return segment
	}

	private fun parseRouteTypeAttributes(parser: XmlPullParser): RouteType {
		val type = RouteType()
		type.tag = parser.getAttributeValue("", "t")
		type.value = parser.getAttributeValue("", "v")
		return type
	}

	private fun parseBoundsAttributes(parser: XmlPullParser): Bounds {
		val bounds = Bounds()
		try {
			var minlat = parser.getAttributeValue("", "minlat")
			var minlon = parser.getAttributeValue("", "minlon")
			var maxlat = parser.getAttributeValue("", "maxlat")
			var maxlon = parser.getAttributeValue("", "maxlon")
			if (minlat == null) {
				minlat = parser.getAttributeValue("", "minLat")
			}
			if (minlon == null) {
				minlon = parser.getAttributeValue("", "minLon")
			}
			if (maxlat == null) {
				maxlat = parser.getAttributeValue("", "maxLat")
			}
			if (maxlon == null) {
				maxlon = parser.getAttributeValue("", "maxLon")
			}
			if (minlat != null) {
				bounds.minlat = minlat.toDouble()
			}
			if (minlon != null) {
				bounds.minlon = minlon.toDouble()
			}
			if (maxlat != null) {
				bounds.maxlat = maxlat.toDouble()
			}
			if (maxlon != null) {
				bounds.maxlon = maxlon.toDouble()
			}
		} catch (_: NumberFormatException) {
		}
		return bounds
	}

	fun mergeGpxFileInto(to: GpxFile, from: GpxFile?) {
		if (from == null) {
			return
		}
		if (from.showCurrentTrack) {
			to.showCurrentTrack = true
		}
		if (!from.isPointsEmpty()) {
			to.addPoints(from.getPointsList())
		}
		to.tracks.addAll(from.tracks)
		to.routes.addAll(from.routes)
		if (from.error != null) {
			to.error = from.error
		}
	}

	fun projectionOnPrimeMeridian(previous: WptPt, next: WptPt): WptPt {
		val lat = MapUtils.getProjection(
			0.0,
			0.0,
			previous.lat,
			previous.lon,
			next.lat,
			next.lon
		).latitude
		val lon = if (previous.lon < 0) -PRIME_MERIDIAN else PRIME_MERIDIAN
		val projectionCoeff =
			MapUtils.getProjectionCoeff(0.0, 0.0, previous.lat, previous.lon, next.lat, next.lon)
		val time = (previous.time + (next.time - previous.time) * projectionCoeff).toLong()
		val ele =
			if (previous.ele.isNaN() && next.ele.isNaN()) Double.NaN else previous.ele + (next.ele - previous.ele) * projectionCoeff
		val speed = previous.speed + (next.speed - previous.speed) * projectionCoeff
		return WptPt(lat, lon, time, ele, speed, Double.NaN)
	}
}
