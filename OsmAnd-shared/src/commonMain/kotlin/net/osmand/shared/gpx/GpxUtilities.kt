package net.osmand.shared.gpx

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import net.osmand.shared.KException
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.primitives.Author
import net.osmand.shared.gpx.primitives.Bounds
import net.osmand.shared.gpx.primitives.Copyright
import net.osmand.shared.gpx.primitives.GpxExtensions
import net.osmand.shared.gpx.primitives.Metadata
import net.osmand.shared.gpx.primitives.Route
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.util.IProgress
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KAlgorithms.hash
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.StringBundle
import net.osmand.shared.util.StringBundleWriter
import net.osmand.shared.util.StringBundleXmlReader
import net.osmand.shared.util.StringBundleXmlWriter
import net.osmand.shared.xml.XmlParserException
import net.osmand.shared.xml.XmlPullParser
import net.osmand.shared.xml.XmlSerializer
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Source
import okio.buffer
import kotlin.math.round


object GpxUtilities {

	val log = LoggerFactory.getLogger("GpxUtilities")

	const val ICON_NAME_EXTENSION = "icon"
	const val BACKGROUND_TYPE_EXTENSION = "background"
	const val COLOR_NAME_EXTENSION = "color"
	const val PROFILE_TYPE_EXTENSION = "profile"
	const val ADDRESS_EXTENSION = "address"
	const val HIDDEN_EXTENSION = "hidden"
	const val POINT_TYPE_EXTENSION = "point_type"

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

	fun parseColor(colorString: String?, defColor: Int?): Int? {
		val color = parseColor(colorString)
		return color ?: defColor
	}

	fun parseColor(colorString: String?): Int? {
		if (!KAlgorithms.isEmpty(colorString)) {
			if (colorString!![0] == '#') {
				return try {
					KAlgorithms.parseColor(colorString)
				} catch (e: IllegalArgumentException) {
					log.error("Error parse color", e)
					null
				}
			} else {
				val gpxColor = GpxColor.getColorFromName(colorString)
				if (gpxColor != null) {
					return gpxColor.color
				} else {
					try {
						return colorString.toInt()
					} catch (_: NumberFormatException) {
					}
				}
			}
		}
		return null
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
				bundle.putString("color", KAlgorithms.colorToString(color))
			}
			if (!KAlgorithms.isEmpty(iconName)) {
				bundle.putString(ICON_NAME_EXTENSION, iconName)
			}
			if (!KAlgorithms.isEmpty(backgroundType)) {
				bundle.putString(BACKGROUND_TYPE_EXTENSION, backgroundType)
			}
			if (isHidden()) {
				bundle.putBoolean(HIDDEN_EXTENSION, true)
			}
			return bundle
		}

		companion object {
			const val OBF_POINTS_GROUPS_DELIMITER = "~~~"
			const val OBF_POINTS_GROUPS_PREFIX = "points_groups_"
			const val OBF_POINTS_GROUPS_NAMES = "points_groups_names"
			const val OBF_POINTS_GROUPS_ICONS = "points_groups_icons"
			const val OBF_POINTS_GROUPS_COLORS = "points_groups_colors"
			const val OBF_POINTS_GROUPS_BACKGROUNDS = "points_groups_backgrounds"
			const val OBF_POINTS_GROUPS_CATEGORY = "points_groups_category" // optional category of OBF-GPX point

			fun parsePointsGroupAttributes(parser: XmlPullParser): PointsGroup {
				val name = parser.getAttributeValue("", "name")
				val category = PointsGroup(name ?: "")
				category.color = parseColor(parser.getAttributeValue("", "color"), 0)!!
				category.iconName = parser.getAttributeValue("", ICON_NAME_EXTENSION)
				category.backgroundType = parser.getAttributeValue("", BACKGROUND_TYPE_EXTENSION)
				category.hidden = parser.getAttributeValue("", HIDDEN_EXTENSION).toBoolean()
				return category
			}
		}
	}

	fun convert(splitSegments: List<SplitSegment>): List<GpxTrackAnalysis> {
		val list = mutableListOf<GpxTrackAnalysis>()
		for (segment in splitSegments) {
			val analysis = GpxTrackAnalysis()
			analysis.prepareInformation(0, null, segment)
			list.add(analysis)
		}
		return list
	}

	fun calculateBounds(pts: List<WptPt>): KQuadRect {
		val trackBounds = KQuadRect(
			Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
			Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
		)
		updateBounds(trackBounds, pts, 0)
		return trackBounds
	}

	fun calculateTrackBounds(segments: List<TrkSegment>): KQuadRect {
		val trackBounds = KQuadRect(
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
		return if (updated) trackBounds else KQuadRect()
	}

	fun updateBounds(trackBounds: KQuadRect, pts: List<WptPt>, startIndex: Int) {
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

	fun updateQR(q: KQuadRect, p: WptPt, defLat: Double, defLon: Double) {
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

	fun asString(gpxFile: GpxFile): String {
		val writer = Buffer()
		writeGpx(null, writer, gpxFile, null)
		return writer.toString()
	}

	fun writeGpxFile(file: KFile, gpxFile: GpxFile): KException? {
		return try {
			file.getParentFile()?.createDirectories()
			if (KAlgorithms.isEmpty(gpxFile.path)) {
				gpxFile.path = if (file.isAbsolute()) file.path() else file.absolutePath()
			}
			writeGpx(file, null, gpxFile, null)
		} catch (e: KException) {
			log.error("Failed to write gpx '$file.path()'", e)
			e
		}
	}

	fun writeGpx(file: KFile?, stream: Sink?, gpxFile: GpxFile, progress: IProgress?): KException? {
		progress?.startWork(gpxFile.getItemsToWriteSize())
		return try {
			val serializer = XmlSerializer()
			if (file != null) {
				serializer.setOutput(file)
			} else if (stream != null) {
				serializer.setOutput(stream.buffer())
			} else {
				throw KException("Output file or stream is not defined")
			}
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
			serializer.startDocument("UTF-8", true)
			serializer.startTag(null, "gpx")
			serializer.attribute(null, "version", "1.1")
			val author = gpxFile.author
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

			assignPointsGroupsExtensionWriter(gpxFile)
			assignNetworkRouteExtensionWriter(gpxFile)
			writeMetadata(serializer, gpxFile, progress)
			writePoints(serializer, gpxFile, progress)
			writeRoutes(serializer, gpxFile, progress)
			writeTracks(serializer, gpxFile, progress)
			writeExtensions(serializer, gpxFile, progress)

			serializer.endTag(null, "gpx")
			serializer.endDocument()
			serializer.close()
			null
		} catch (e: Exception) {
			log.error("Failed to write gpx", e)
			KException(e.message, e)
		}
	}

	private fun assignNetworkRouteExtensionWriter(gpxFile: GpxFile) {
		if (!KAlgorithms.isEmpty(gpxFile.networkRouteKeyTags)) {
			gpxFile.setExtensionsWriter("network_route", object : GpxExtensionsWriter {
				override fun writeExtensions(serializer: XmlSerializer) {
					val bundle = StringBundle()
					val tagsBundle = StringBundle()
					tagsBundle.putString("type", gpxFile.networkRouteKeyTags.get("type"))
					for ((key, value) in gpxFile.networkRouteKeyTags) {
						tagsBundle.putString(key, value)
					}
					val routeKeyBundle = mutableListOf<StringBundle>()
					routeKeyBundle.add(tagsBundle)
					bundle.putBundleList(
						"network_route",
						OSMAND_EXTENSIONS_PREFIX + "route_key",
						routeKeyBundle
					)
					val bundleWriter: StringBundleWriter = StringBundleXmlWriter(bundle, serializer)
					bundleWriter.writeBundle()
				}
			})
		} else {
			gpxFile.removeExtensionsWriter("network_route")
		}
	}

	private fun assignPointsGroupsExtensionWriter(gpxFile: GpxFile) {
		if (!KAlgorithms.isEmpty(gpxFile.pointsGroups)) {
			gpxFile.setExtensionsWriter("points_groups", object : GpxExtensionsWriter {
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
			})
		} else {
			gpxFile.removeExtensionsWriter("points_groups");
		}
	}

	private fun writeMetadata(serializer: XmlSerializer, file: GpxFile, progress: IProgress?) {
		val defName = file.metadata.name
		val trackName = if (!KAlgorithms.isEmpty(defName)) defName else getFilename(file.path)
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
			writeWpt(serializer, l, progress, file)
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
				writeWpt(serializer, p, progress, file)
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
						writeWpt(serializer, p, progress, file)
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
		if (segment.hasRoute() && segment.getExtensionsWriter("route") == null) {
			segment.setExtensionsWriter("route", object : GpxExtensionsWriter {
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
			})
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
		val extensionsWriters = p.getExtensionsWritersToWrite()
		val hasExtensions = !extensions.isNullOrEmpty()
		val hasExtensionWriters = !KAlgorithms.isEmpty(extensionsWriters)
		if (hasExtensions || hasExtensionWriters) {
			serializer.startTag(null, "extensions")
			if (hasExtensions) {
				for ((key, value) in extensions!!) {
					writeNotNullText(serializer, getOsmandTagKey(key, value), value)
				}
			}
			if (hasExtensionWriters) {
				for (writer in extensionsWriters.values) {
					writer.writeExtensions(serializer)
				}
			}
			serializer.endTag(null, "extensions")
			progress?.progress(1)
		}
	}

	private fun writeWpt(serializer: XmlSerializer, p: WptPt, progress: IProgress?, file: GpxFile) {
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
		if (p.category != null && file.pointsGroups[p.category] != null) {
			val pointsGroup = file.pointsGroups[p.category]!!
			if (p.getColor() == pointsGroup.color) {
				extensions.remove(COLOR_NAME_EXTENSION)
			}
			if (p.getIconName() == pointsGroup.iconName) {
				extensions.remove(ICON_NAME_EXTENSION)
			}
			if (p.getBackgroundType() == pointsGroup.backgroundType) {
				extensions.remove(BACKGROUND_TYPE_EXTENSION)
			}
		}
		assignExtensionWriter(p, extensions)
		writeExtensions(serializer, null, p, null)
		progress?.progress(1)
	}

	fun assignExtensionWriter(wptPt: WptPt, extensions: Map<String, String>) {
		val regularExtensions = HashMap<String, String>()
		val gpxtpxExtensions = HashMap<String, String>()
		for ((key, value) in extensions) {
			if (key.startsWith(GPXTPX_PREFIX)) {
				gpxtpxExtensions[key] = value
			} else {
				regularExtensions[key] = value
			}
			wptPt.getDeferredExtensionsToWrite()[key] = value
		}
		if (regularExtensions.isNotEmpty()) {
			wptPt.setExtensionsWriter("extensions", createExtensionsWriter(regularExtensions, true))
		}
		if (gpxtpxExtensions.isNotEmpty()) {
			wptPt.setExtensionsWriter("gpxtpx:TrackPointExtension", createGpxTpxExtensionsWriter(gpxtpxExtensions, false))
		}
	}

	private fun createExtensionsWriter(
		extensions: Map<String, String>,
		addOsmandPrefix: Boolean
	): GpxExtensionsWriter {
		return object : GpxExtensionsWriter {
			override fun writeExtensions(serializer: XmlSerializer) {
				writeExtensionsWithPrefix(serializer, extensions, addOsmandPrefix);
			}
		}
	}

	private fun createGpxTpxExtensionsWriter(extensions: Map<String, String>, addOsmandPrefix: Boolean): GpxExtensionsWriter {
		return object : GpxExtensionsWriter {
			override fun writeExtensions(serializer: XmlSerializer) {
				try {
					serializer.startTag(null, "gpxtpx:TrackPointExtension")
					writeExtensionsWithPrefix(serializer, extensions, addOsmandPrefix)
					serializer.endTag(null, "gpxtpx:TrackPointExtension")
				} catch (e: IOException) {
					log.error("Error create GpxTpxExtensions", e)
				}
			}
		}
	}

	private fun writeExtensionsWithPrefix(serializer: XmlSerializer, extensions: Map<String, String>, addOsmandPrefix: Boolean) {
		for ((key, value) in extensions.entries) {
			try {
				writeNotNullText(serializer, if (addOsmandPrefix) getOsmandTagKey(key, value) else key, value)
			} catch (e: IOException) {
				log.error("Error write ExtensionsWithPrefix", e)
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

	private fun parseTime(text: String, format: DateTimeFormat<DateTimeComponents>): Long {
		var time: Long = 0
		try {
			time = flexibleGpxTimeParser(text, format)
		} catch (e: Exception) {
			try {
				time = getTimeNoTimeZoneFormatter().parse(text).toInstantUsingOffset()
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
		parser: DateTimeFormat<DateTimeComponents>
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
		return parser.parse(text).toInstantUsingOffset()
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

	@OptIn(FormatStringsInDatetimeFormats::class)
	private fun getTimeNoTimeZoneFormatter(): DateTimeFormat<DateTimeComponents> {
		return DateTimeComponents.Format {
			byUnicodePattern(GPX_TIME_NO_TIMEZONE_PATTERN)
		}
	}

	@OptIn(FormatStringsInDatetimeFormats::class)
	private fun getTimeFormatterTZ(): DateTimeFormat<DateTimeComponents> {
		return DateTimeComponents.Format {
			byUnicodePattern(GPX_TIME_PATTERN_TZ)
		}
	}

	fun loadGpxFile(file: KFile): GpxFile {
		return loadGpxFile(file, null, true)
	}

	fun loadGpxFile(
		file: KFile,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile {
		return try {
			val gpxFile = loadGpxFile(file, null, extensionsReader, addGeneralTrack)
			gpxFile.path = file.absolutePath()
			gpxFile.modifiedTime = file.lastModified()
			gpxFile.pointsModifiedTime = gpxFile.modifiedTime
			if (gpxFile.error != null) {
				log.info("Error reading gpx ${gpxFile.path}: ${gpxFile.error!!.message}")
			}
			gpxFile
		} catch (e: IOException) {
			val gpxFile = GpxFile(null)
			gpxFile.path = file.absolutePath()
			log.error("Error reading gpx ${gpxFile.path}", e)
			gpxFile.error = KException(e.message, e)
			gpxFile
		}
	}

	fun loadGpxFile(source: Source): GpxFile {
		return loadGpxFile(null, source, null, true)
	}

	fun loadGpxFile(
		file: KFile?,
		source: Source?,
		extensionsReader: GpxExtensionsReader?,
		addGeneralTrack: Boolean
	): GpxFile {
		val gpxFile = GpxFile(null)
		gpxFile.metadata.time = 0
		var parser: XmlPullParser? = null
		try {
			parser = XmlPullParser()
			if (file != null) {
				parser.setInput(file, "UTF-8")
			} else if (source != null) {
				parser.setInput(source.buffer(), "UTF-8")
			} else {
				throw KException("Input file or source is not defined")
			}
			val routeTrack = Track()
			val routeTrackSegment = TrkSegment()
			routeTrack.segments.add(routeTrackSegment)
			val parserState = ArrayDeque<GpxExtensions>()
			var firstSegment: TrkSegment? = null
			var extensionReadMode = false
			var routePointExtension = false
			var routeSegments = mutableListOf<RouteSegment>()
			var routeTypes = mutableListOf<RouteType>()
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
								gpxFile.networkRouteKeyTags.putAll(parseRouteKeyAttributes(parser))
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
										if (!KAlgorithms.isEmpty(id) && !KAlgorithms.isEmpty(domain)) {
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
								routeSegments = mutableListOf()
								routeTypes = mutableListOf()
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
			gpxFile.error = KException(e.message, e)
		} finally {
			parser?.close()
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
			} else if (pointsGroup.color != 0 && color == 0) {
				point.setColor(pointsGroup.color)
			}
			val iconName = point.getIconName()
			if (KAlgorithms.isEmpty(pointsGroup.iconName) && !KAlgorithms.isEmpty(iconName)) {
				pointsGroup.iconName = iconName
			} else if (!KAlgorithms.isEmpty(pointsGroup.iconName) && KAlgorithms.isEmpty(iconName)) {
				point.setIconName(pointsGroup.iconName)
			}
			val backgroundType = point.getBackgroundType()
			if (KAlgorithms.isEmpty(pointsGroup.backgroundType) && !KAlgorithms.isEmpty(backgroundType)) {
				pointsGroup.backgroundType = backgroundType
			} else if (!KAlgorithms.isEmpty(pointsGroup.backgroundType) && KAlgorithms.isEmpty(backgroundType)) {
				point.setBackgroundType(pointsGroup.backgroundType)
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
		val lat = KMapUtils.getProjection(
			0.0,
			0.0,
			previous.lat,
			previous.lon,
			next.lat,
			next.lon
		).latitude
		val lon = if (previous.lon < 0) -PRIME_MERIDIAN else PRIME_MERIDIAN
		val projectionCoeff =
			KMapUtils.getProjectionCoeff(0.0, 0.0, previous.lat, previous.lon, next.lat, next.lon)
		val time = (previous.time + (next.time - previous.time) * projectionCoeff).toLong()
		val ele =
			if (previous.ele.isNaN() && next.ele.isNaN()) Double.NaN else previous.ele + (next.ele - previous.ele) * projectionCoeff
		val speed = previous.speed + (next.speed - previous.speed) * projectionCoeff
		return WptPt(lat, lon, time, ele, speed, Double.NaN)
	}

	fun interpolateEmptyElevationWpts(pts: List<WptPt>) {
		var i = 0
		while (i < pts.size) {
			var processedPoints = 0
			if (pts[i].ele.isNaN()) {
				val startIndex = i
				var prevValidIndex = -1
				var nextValidIndex = -1
				var prevValidElevation = Double.NaN
				var nextValidElevation = Double.NaN
				for (j in startIndex - 1 downTo 0) {
					val ele: Double = pts[j].ele
					if (!ele.isNaN()) {
						prevValidElevation = ele
						prevValidIndex = j
						break
					}
				}
				for (j in startIndex + 1 until pts.size) {
					val ele: Double = pts[j].ele
					if (!ele.isNaN()) {
						nextValidElevation = ele
						nextValidIndex = j
						break
					}
				}
				if (prevValidIndex == -1 && nextValidIndex == -1) {
					return  // no elevation at all
				}
				if (prevValidIndex == -1 || nextValidIndex == -1) {
					// outermost section without interpolation
					for (j in startIndex until pts.size) {
						val pt:WptPt = pts[j]
						if (pt.ele.isNaN()) {
							pt.ele = if (startIndex == 0) nextValidElevation else prevValidElevation
							processedPoints++
						} else {
							break
						}
					}
				} else {
					// inner section
					var totalDistance = 0.0
					val distanceArray = DoubleArray(nextValidIndex - prevValidIndex)
					for (j in prevValidIndex until nextValidIndex) {
						val thisPt: WptPt = pts[j]
						val nextPt: WptPt = pts[j + 1]
						val distance: Double = KMapUtils.getDistance(
							thisPt.lat,
							thisPt.lon,
							nextPt.lat,
							nextPt.lon)
						distanceArray[j - prevValidIndex] = distance
						totalDistance += distance
					}
					val deltaElevation: Double = pts[nextValidIndex].ele - pts[prevValidIndex].ele
					var j = startIndex
					while (totalDistance > 0 && j < nextValidIndex) {
						val currentDistance = distanceArray[j - startIndex]
						val increaseElevation = deltaElevation * (currentDistance / totalDistance)
						pts[j].ele = pts[j - 1].ele + increaseElevation
						processedPoints++
						j++
					}
				}
			}
			i += if (processedPoints > 0) processedPoints else 1
		}
	}

}
