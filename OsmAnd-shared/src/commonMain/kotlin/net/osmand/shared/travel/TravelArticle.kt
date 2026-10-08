package net.osmand.shared.travel

import net.osmand.shared.binary.SearchPoiTypeFilter
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.UrlEncoder
import okio.ByteString.Companion.encodeUtf8
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * One wikivoyage article, or one track stored in an obf file - [TravelGpx] is the second kind.
 * It carries what the poi section said about it and, once it has been built, the gpx itself.
 *
 * A copy of `TravelArticle` in the android app, which stays there; this copy is for iOS. Left out:
 * `Parcelable`, which is how android passes the identifier between screens, and `getTravelBook`,
 * which turns a path into a display name through the app's directory layout.
 */
open class TravelArticle {

	@JvmField
	var file: KFile? = null

	@JvmField
	var title: String? = null

	@JvmField
	var content: String? = null

	@JvmField
	var isPartOf: String? = null

	@JvmField
	var isParentOf: String = ""

	@JvmField
	var lat: Double = Double.NaN

	@JvmField
	var lon: Double = Double.NaN

	@JvmField
	var imageTitle: String? = null

	@JvmField
	var gpxFile: GpxFile? = null

	@JvmField
	var routeId: String? = null

	@JvmField
	var ref: String? = null

	@JvmField
	var routeSource: String = ""

	@JvmField
	var originalId: Long = 0

	@JvmField
	var lang: String? = null

	@JvmField
	var contentsJson: String? = null

	@JvmField
	var aggregatedPartOf: String? = null

	@JvmField
	var description: String? = null

	@JvmField
	var lastModified: Long = 0

	@JvmField
	var gpxFileReading: Boolean = false

	@JvmField
	var gpxFileRead: Boolean = false

	@JvmField
	var routeRadius: Int = -1

	private var bbox31: KQuadRect? = null

	/**
	 * The box the route covers, from the shortlink tiles the indexer wrote for it. It is what tells
	 * the reader which files to open before anything of the route has been read.
	 */
	fun initShortLinkTiles(shortLinkTiles: String) {
		val box = KQuadRect()
		this.bbox31 = box
		for (shortLink in shortLinkTiles.split(",")) {
			val bbox = KMapUtils.decodeShortLinkToQuadRect(shortLink)
			val left = KMapUtils.get31TileNumberX(bbox.left)
			val top = KMapUtils.get31TileNumberY(bbox.top)
			val right = KMapUtils.get31TileNumberX(bbox.right)
			val bottom = KMapUtils.get31TileNumberY(bbox.bottom)
			box.expand(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
		}
	}

	fun getBbox31(): KQuadRect? = bbox31

	fun hasBbox31(): Boolean = bbox31?.hasInitialState() == false

	fun generateIdentifier(): TravelArticleIdentifier = TravelArticleIdentifier(this)

	fun getFile(): KFile? = file

	fun getLastModified(): Long {
		if (lastModified > 0) {
			return lastModified
		}
		return file?.lastModified() ?: 0
	}

	fun getTitle(): String? = title

	fun getDescription(): String? = description

	fun getContent(): String? = content

	fun getIsPartOf(): String? = isPartOf

	fun getLat(): Double = lat

	fun getLon(): Double = lon

	fun getImageTitle(): String? = imageTitle

	fun getGpxFile(): GpxFile? = gpxFile

	fun getRouteId(): String? = routeId

	fun hasOsmRouteId(): Boolean {
		val routeId = getRouteId()
		return routeId != null &&
				(routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX_LEGACY) ||
						routeId.startsWith(Amenity.ROUTE_ID_OSM_PREFIX))
	}

	/** What the article is saved as; never empty, never a path. */
	fun getGpxFileName(): String {
		val gpxFileName = if (!KAlgorithms.isEmpty(title)) title else routeId
		if (gpxFileName != null) {
			return KAlgorithms.sanitizeFileName(gpxFileName)
		}
		log.error("Empty travel article in ${this.file}")
		return "Travel Article File"
	}

	fun getRouteSource(): String = routeSource

	fun getOriginalId(): Long = originalId

	fun getLang(): String? = lang

	fun getContentsJson(): String? = contentsJson

	fun getAggregatedPartOf(): String? = aggregatedPartOf

	/** The innermost and outermost regions the article belongs to, for the subtitle of a card. */
	fun getGeoDescription(): String? {
		val aggregatedPartOf = this.aggregatedPartOf
		if (KAlgorithms.isEmpty(aggregatedPartOf)) {
			return null
		}
		val parts = aggregatedPartOf!!.split(",")
		if (parts.isNotEmpty()) {
			val res = StringBuilder()
			res.append(parts[parts.size - 1])
			if (parts.size > 1) {
				res.append(" • ").append(parts[0])
			}
			return res.toString()
		}
		return null
	}

	open fun getPointFilterString(): String = MapPoiTypes.ROUTE_ARTICLE_POINT

	open fun getMainFilterString(): String = MapPoiTypes.ROUTE_ARTICLE

	/** One point of a wikivoyage article, with the tags that article points are written with. */
	open fun createWptPt(amenity: Amenity, lang: String?): WptPt {
		val wptPt = WptPt()
		wptPt.name = (amenity.getName())
		wptPt.lat = (amenity.getLocation()!!.latitude)
		wptPt.lon = (amenity.getLocation()!!.longitude)
		wptPt.desc = (amenity.getDescription(lang))
		wptPt.link = (Link(amenity.getSite() ?: ""))
		val colorId = amenity.getColor()
		if (colorId != null) {
			wptPt.setColor(defaultColor(colorId))
		}
		val iconName = amenity.getGpxIcon()
		if (iconName != null) {
			wptPt.setIconName(iconName)
		}
		val category = amenity.getTagSuffix("category_")
		if (category != null) {
			wptPt.category = (KAlgorithms.capitalizeFirstLetter(category))
		}
		for (key in amenity.getAdditionalInfoKeys()) {
			if (!WikivoyageOSMTags.contains(key)) {
				continue
			}
			val amenityAdditionalInfo = amenity.getAdditionalInfo(key)
			if (amenityAdditionalInfo != null) {
				wptPt.getExtensionsToWrite()[key] = amenityAdditionalInfo
			}
		}
		return wptPt
	}

	open fun getAnalysis(): GpxTrackAnalysis? = null

	/** Accepts one subtype, which is how a search is narrowed to the points of one article. */
	fun getSearchFilter(filterSubcategory: String): SearchPoiTypeFilter {
		return object : SearchPoiTypeFilter {
			override fun accept(type: PoiCategory?, subcategory: String): Boolean =
				subcategory == filterSubcategory

			override fun isEmpty(): Boolean = false
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null || this::class != other::class) {
			return false
		}
		other as TravelArticle
		return TravelArticleIdentifier.areLatLonEqual(other.lat, other.lon, lat, lon) &&
				file == other.file &&
				KAlgorithms.stringsEqual(routeId, other.routeId) &&
				KAlgorithms.stringsEqual(routeSource, other.routeSource)
	}

	override fun hashCode(): Int = KAlgorithms.hash(file, lat, lon, routeId, routeSource)

	companion object {
		private val log = LoggerFactory.getLogger("TravelArticle")

		private const val IMAGE_ROOT_URL = "https://upload.wikimedia.org/wikipedia/commons/"
		private const val THUMB_PREFIX = "330px-"
		private const val REGULAR_PREFIX = "1280px-" // 1280, 1024, 800

		const val TRAVEL_GPX_DEFAULT_SEARCH_RADIUS: Int = 50 * 1000

		/**
		 * Where wikimedia keeps the picture of that name: the first one and two characters of the
		 * md5 of the title are the two directories it lives under.
		 */
		@JvmStatic
		fun getImageUrl(imageTitle: String, thumbnail: Boolean): String {
			var title = imageTitle.replace(" ", "_")
			title = UrlEncoder.decode(title)
			val hash = getHash(title)
			title = UrlEncoder.encode(title)
			val prefix = if (thumbnail) THUMB_PREFIX else REGULAR_PREFIX
			val suffix = if (title.endsWith(".svg")) ".png" else ""
			return IMAGE_ROOT_URL + "thumb/" + hash[0] + "/" + hash[1] + "/" + title + "/" +
					prefix + title + suffix
		}

		private fun getHash(s: String): Array<String> {
			val md5 = s.encodeUtf8().md5().hex()
			return arrayOf(md5.substring(0, 1), md5.substring(0, 2))
		}

		/**
		 * The colour of that name in OsmAnd's own palette, or 0. A copy of `DefaultColors` in the
		 * android app, which keeps the names for the interface as well; these values are written
		 * into gpx files, so they are frozen.
		 */
		internal fun defaultColor(id: String): Int {
			return when (id) {
				"darkyellow" -> 0xffeecc22.toInt()
				"red" -> 0xffd00d0d.toInt()
				"orange" -> 0xffff5020.toInt()
				"yellow" -> 0xffeeee10.toInt()
				"lightgreen" -> 0xff88e030.toInt()
				"green" -> 0xff00842b.toInt()
				"lightblue" -> 0xff10c0f0.toInt()
				"blue" -> 0xff1010a0.toInt()
				"purple" -> 0xffa71de1.toInt()
				"pink" -> 0xffe044bb.toInt()
				"brown" -> 0xff8e2512.toInt()
				"black" -> 0xff000001.toInt()
				else -> 0
			}
		}
	}
}
