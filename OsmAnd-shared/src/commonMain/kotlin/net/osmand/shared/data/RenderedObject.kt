package net.osmand.shared.data

import net.osmand.shared.binary.ObfConstants
import net.osmand.shared.data.Amenity.Companion.ROUTE_ID
import net.osmand.shared.data.Amenity.Companion.WIKIDATA
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * An object the map renderer drew: its tags, its geometry in 31 tiles and where its label went.
 *
 * A copy of `NativeLibrary.RenderedObject` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. Java nests it in `NativeLibrary`; here it is a model of its own.
 */
open class RenderedObject : MapObject() {

	private val tags: MutableMap<String, String> = LinkedHashMap()
	private var bbox = KQuadRect()
	private val x = KTIntArrayList()
	private val y = KTIntArrayList()
	private var iconRes: String? = null
	private var order = 0
	private var visible = false
	private var drawOnPath = false
	private var labelLatLon: KLatLon? = null
	private var labelX = 0
	private var labelY = 0
	private var isPolygon = false

	fun getTags(): MutableMap<String, String> = tags

	fun getTagValue(tag: String): String? = getTags()[tag]

	fun isText(): Boolean = getName().isNotEmpty()

	fun getOrder(): Int = order

	fun setLabelLatLon(labelLatLon: KLatLon?) {
		this.labelLatLon = labelLatLon
	}

	fun getLabelLatLon(): KLatLon? = labelLatLon

	fun setOrder(order: Int) {
		this.order = order
	}

	fun addLocation(x: Int, y: Int) {
		this.x.add(x)
		this.y.add(y)
	}

	fun getX(): KTIntArrayList = x

	fun getIconRes(): String? = iconRes

	fun setIconRes(iconRes: String?) {
		this.iconRes = iconRes
	}

	fun setVisible(visible: Boolean) {
		this.visible = visible
	}

	fun isVisible(): Boolean = visible

	fun setDrawOnPath(drawOnPath: Boolean) {
		this.drawOnPath = drawOnPath
	}

	fun isDrawOnPath(): Boolean = drawOnPath

	fun getY(): KTIntArrayList = y

	fun setBbox(left: Int, top: Int, right: Int, bottom: Int) {
		bbox = KQuadRect(left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())
	}

	fun getBbox(): KQuadRect = bbox

	fun setNativeId(id: Long) {
		setId(id)
	}

	fun putTag(t: String, v: String) {
		tags[t] = v
	}

	fun getLabelX(): Int = labelX

	fun getLabelY(): Int = labelY

	fun setLabelX(labelX: Int) {
		this.labelX = labelX
	}

	fun setLabelY(labelY: Int) {
		this.labelY = labelY
	}

	fun markAsPolygon(isPolygon: Boolean) {
		this.isPolygon = isPolygon
	}

	fun isPolygon(): Boolean = isPolygon

	fun isSimplePoint(): Boolean = x.size() == 1 && y.size() == 1

	fun getOriginalNames(): List<String> {
		val names = ArrayList<String>()
		val name = this.name
		if (!KAlgorithms.isEmpty(name)) {
			names.add(name!!)
		}
		for ((key, value) in tags) {
			if ((key.startsWith("name:") || key == "name") && value.isNotEmpty()) {
				names.add(value)
			}
		}
		return names
	}

	fun getRouteID(): String? {
		for ((key, value) in getTags()) {
			if (ROUTE_ID == key) {
				return value
			}
		}
		return null
	}

	override fun toString(): String {
		var s = (this::class.simpleName ?: "") + " " + name
		val link = ObfConstants.getOsmUrlForId(this)
		val tags = ObfConstants.getPrintTags(this)
		s += if (s.contains(link)) "" else " $link"
		s += if (s.contains(tags)) "" else " $tags"
		return s
	}

	override fun toStringEn(): String {
		var s = "MapObject $name "
		s += "${ObfConstants.getOsmEntityType(this)}/"
		s += ObfConstants.getOsmObjectId(this)
		if (this.getTags().containsKey(WIKIDATA)) {
			s += " " + this.getTags()[WIKIDATA]
		}
		return s
	}

	fun getPolygon(): List<KLatLon> {
		val res = ArrayList<KLatLon>()
		for (i in 0 until this.x.size()) {
			val x = this.x[i]
			val y = this.y[i]
			res.add(KLatLon(KMapUtils.get31LatitudeY(y), KMapUtils.get31LongitudeX(x)))
		}
		return res
	}

	fun getRectLatLon(): KQuadRect? {
		if (x.size() == 0) {
			return null
		}
		var left = x[0]
		var right = left
		var top = y[0]
		var bottom = top
		for (i in 0 until x.size()) {
			val x = this.x[i]
			val y = this.y[i]
			left = min(left, x)
			right = max(right, x)
			top = min(top, y)
			bottom = max(bottom, y)
		}
		return KQuadRect(
			KMapUtils.get31LongitudeX(left), KMapUtils.get31LatitudeY(top),
			KMapUtils.get31LongitudeX(right), KMapUtils.get31LatitudeY(bottom)
		)
	}

	fun getLatLon(): KLatLon? {
		var latLon = getLabelLatLon()
		if (latLon == null && getLabelX() != 0) {
			latLon = KLatLon(KMapUtils.get31LatitudeY(getLabelY()), KMapUtils.get31LongitudeX(getLabelX()))
		}
		val rect = getRectLatLon()
		if (latLon == null && rect != null) {
			latLon = KLatLon(rect.centerY(), rect.centerX())
		}
		return latLon
	}
}
