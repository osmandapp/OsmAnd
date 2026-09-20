package net.osmand.shared.binary

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.jvm.JvmField

/**
 * A map section of an obf file: the table that turns a tag and a value into a number and back, and
 * the r-tree roots, one per zoom range.
 *
 * A copy of `BinaryMapIndexReader.MapIndex` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. The encoding table is what makes a [BinaryMapDataObject] readable:
 * the objects carry numbers, and only the section that wrote them knows what they mean, which is
 * why every object keeps the index it came from.
 *
 * The layer sets hold a handful of ids each, so they are plain sets of boxed ints rather than a
 * primitive set copied in from trove.
 */
class MapIndex : BinaryIndexPart() {

	private val roots = ArrayList<MapRoot>()

	/** tag -> value -> id; the value of a rule without one is null, as it is in the file. */
	internal val encodingRules = HashMap<String?, HashMap<String?, Int>>()

	@JvmField
	val decodingRules = KTIntObjectMap<TagValuePair>()

	@JvmField
	var nameEncodingType: Int = 0

	@JvmField
	var nameEnEncodingType: Int = -1

	@JvmField
	var refEncodingType: Int = -1

	@JvmField
	var coastlineEncodingType: Int = -1

	@JvmField
	var coastlineBrokenEncodingType: Int = -1

	@JvmField
	var landEncodingType: Int = -1

	@JvmField
	var onewayAttribute: Int = -1

	@JvmField
	var onewayReverseAttribute: Int = -1

	@JvmField
	val positiveLayers = HashSet<Int>()

	@JvmField
	val negativeLayers = HashSet<Int>()

	@JvmField
	var encodingRulesSizeBytes: Int = 0

	// to speed up comparision
	private var referenceMapIndex: MapIndex? = null

	fun getRule(t: String?, v: String?): Int? = encodingRules[t]?.get(v)

	fun getRule(tv: TagValuePair): Int? = encodingRules[tv.tag]?.get(tv.value)

	/** Centre of the coarsest root, which is the one that covers the whole section. */
	fun getCenterLatLon(): KLatLon? {
		if (roots.size == 0) {
			return null
		}
		val mapRoot = roots[roots.size - 1]
		val cy = (KMapUtils.get31LatitudeY(mapRoot.getBottom()) + KMapUtils.get31LatitudeY(mapRoot.getTop())) / 2
		val cx = (KMapUtils.get31LongitudeX(mapRoot.getLeft()) + KMapUtils.get31LongitudeX(mapRoot.getRight())) / 2
		return KLatLon(cy, cx)
	}

	fun getRoots(): MutableList<MapRoot> = roots

	fun decodeType(type: Int): TagValuePair? = decodingRules[type]

	/** Adds the two rules that the file does not carry but the renderer expects to exist. */
	fun finishInitializingTags() {
		var free = decodingRules.size()
		coastlineBrokenEncodingType = free++
		initMapEncodingRule(0, coastlineBrokenEncodingType, "natural", "coastline_broken")
		if (landEncodingType == -1) {
			landEncodingType = free++
			initMapEncodingRule(0, landEncodingType, "natural", "land")
		}
	}

	fun isRegisteredRule(id: Int): Boolean = decodingRules.containsKey(id)

	fun initMapEncodingRule(type: Int, id: Int, tag: String?, value: String?) {
		if (!encodingRules.containsKey(tag)) {
			encodingRules[tag] = HashMap()
		}
		encodingRules[tag]!![value] = id
		if (!decodingRules.containsKey(id)) {
			decodingRules.put(id, TagValuePair(tag, value, type))
		}

		if ("name" == tag) {
			nameEncodingType = id
		} else if ("natural" == tag && "coastline" == value) {
			coastlineEncodingType = id
		} else if ("natural" == tag && "land" == value) {
			landEncodingType = id
		} else if ("oneway" == tag && "yes" == value) {
			onewayAttribute = id
		} else if ("oneway" == tag && "-1" == value) {
			onewayReverseAttribute = id
		} else if ("ref" == tag) {
			refEncodingType = id
		} else if ("name:en" == tag) {
			nameEnEncodingType = id
		} else if ("tunnel" == tag) {
			negativeLayers.add(id)
		} else if ("bridge" == tag) {
			positiveLayers.add(id)
		} else if ("layer" == tag) {
			if (value != null && value != "0" && value.isNotEmpty()) {
				if (value.startsWith("-")) {
					negativeLayers.add(id)
				} else {
					positiveLayers.add(id)
				}
			}
		}
	}

	fun isBaseMap(): Boolean = name?.lowercase()?.contains(BASEMAP_NAME) == true

	override fun getPartName(): String = "Map"

	override fun getFieldNumber(): Int = BinaryMapIndexReader.MAPINDEX_FIELD_NUMBER

	/**
	 * Returns [o] restated in this section's numbers, so that objects of several files can be
	 * compared and drawn together. An empty index takes the other one's table over instead, and
	 * then hands its objects back untouched.
	 */
	fun adoptMapObject(o: BinaryMapDataObject): BinaryMapDataObject {
		val source = o.mapIndex
		if (source == this || source == referenceMapIndex) {
			return o
		}
		if (encodingRules.isEmpty() && source != null) {
			encodingRules.putAll(source.encodingRules)
			source.decodingRules.forEach { key, value -> decodingRules.put(key, value) }
			referenceMapIndex = source
			return o
		}
		val types = KTIntArrayList()
		val additionalTypes = KTIntArrayList()
		if (o.types != null) {
			for (type in o.types!!) {
				val tp = source?.decodeType(type)
				val r = if (tp == null) null else getRule(tp)
				if (r != null) {
					types.add(r)
				} else {
					val nid = decodingRules.size() + 1
					initMapEncodingRule(tp?.additionalAttribute ?: 0, nid, tp?.tag, tp?.value)
					types.add(nid)
				}
			}
		}
		if (o.additionalTypes != null) {
			for (type in o.additionalTypes!!) {
				val tp = source?.decodeType(type)
				val r = if (tp == null) null else getRule(tp)
				if (r != null) {
					additionalTypes.add(r)
				} else {
					val nid = decodingRules.size() + 1
					initMapEncodingRule(tp?.additionalAttribute ?: 0, nid, tp?.tag, tp?.value)
					additionalTypes.add(nid)
				}
			}
		}

		val bm = BinaryMapDataObject(
			o.id, o.coordinates, o.polygonInnerCoordinates, o.objectType, o.area,
			types.toArray(), if (additionalTypes.isEmpty()) null else additionalTypes.toArray(),
			o.labelX, o.labelY
		)
		val order = o.namesOrder
		if (order != null) {
			val names = KTIntObjectMap<String>()
			val newOrder = KTIntArrayList()
			for (i in 0 until order.size()) {
				val nameType = order[i]
				val name = o.objectNames?.get(nameType)
				val tp = source?.decodeType(nameType)
				var nameKeyId = if (tp == null) null else getRule(tp)
				if (nameKeyId == null) {
					nameKeyId = decodingRules.size() + 1
					initMapEncodingRule(tp?.additionalAttribute ?: 0, nameKeyId, tp?.tag, tp?.value)
					additionalTypes.add(nameKeyId)
				}
				if (name != null) {
					names.put(nameKeyId, name)
				}
				newOrder.add(nameKeyId)
			}
			bm.objectNames = names
			bm.namesOrder = newOrder
		}
		return bm
	}

	companion object {
		private const val BASEMAP_NAME = "basemap"
	}
}
