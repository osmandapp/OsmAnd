package net.osmand.shared.binary

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.jvm.JvmField
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * One object of a map section: its points in 31 coordinates, the numbers of its tags, and its
 * names. The numbers mean nothing without [mapIndex], which holds the table that decodes them.
 *
 * A copy of `BinaryMapDataObject` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. Two differences from java. `toString` prints the raw id, because turning it
 * back into an osm id belongs to `ObfConstants`, which is not copied yet. And where java hands
 * back a null it read - a name the string table had nothing for, a type its section has no rule
 * for - this answers "" or leaves the entry out instead of throwing; the reader fills names and
 * their order together, so no object read out of a file gets there.
 */
class BinaryMapDataObject {

	@JvmField
	var coordinates: IntArray? = null

	@JvmField
	var polygonInnerCoordinates: Array<IntArray>? = null

	@JvmField
	var area: Boolean = false

	@JvmField
	var types: IntArray? = null

	@JvmField
	var additionalTypes: IntArray? = null

	@JvmField
	var objectType: Int = POINT_RULES

	@JvmField
	var labelX: Int = 0

	@JvmField
	var labelY: Int = 0

	@JvmField
	var objectNames: KTIntObjectMap<String>? = null

	@JvmField
	var namesOrder: KTIntArrayList? = null

	@JvmField
	var id: Long = 0

	@JvmField
	var mapIndex: MapIndex? = null

	constructor()

	constructor(
		id: Long, coordinates: IntArray?, polygonInnerCoordinates: Array<IntArray>?,
		objectType: Int, area: Boolean, types: IntArray?, additionalTypes: IntArray?,
		labelX: Int, labelY: Int
	) {
		this.polygonInnerCoordinates = polygonInnerCoordinates
		this.coordinates = coordinates
		this.additionalTypes = additionalTypes
		this.types = types
		this.id = id
		this.objectType = objectType
		this.area = area
		this.labelX = labelX
		this.labelY = labelY
	}

	fun setCoordinates(coordinates: IntArray?) {
		this.coordinates = coordinates
	}

	fun getName(): String {
		val names = objectNames ?: return ""
		return names[mapIndex?.nameEncodingType ?: 0] ?: ""
	}

	fun getObjectNames(): KTIntObjectMap<String>? = objectNames

	/** The names in the order the file lists them, which is the order they should be shown in. */
	fun getOrderedObjectNames(): Map<Int, String>? {
		val order = namesOrder ?: return null
		val lm = LinkedHashMap<Int, String>()
		for (i in 0 until order.size()) {
			val nm = order[i]
			val name = objectNames?.get(nm)
			if (name != null) {
				lm[nm] = name
			}
		}
		return lm
	}

	fun putObjectName(type: Int, name: String) {
		if (objectNames == null) {
			objectNames = KTIntObjectMap()
			namesOrder = KTIntArrayList()
		}
		objectNames!!.put(type, name)
		namesOrder!!.add(type)
	}

	fun getPolygonInnerCoordinates(): Array<IntArray>? = polygonInnerCoordinates

	fun getTypes(): IntArray? = types

	fun containsType(cachedType: Int): Boolean {
		val types = this.types
		if (cachedType != -1 && types != null) {
			for (type in types) {
				if (type == cachedType) {
					return true
				}
			}
		}
		return false
	}

	fun containsAdditionalType(cachedType: Int): Boolean {
		val additionalTypes = this.additionalTypes
		if (cachedType != -1 && additionalTypes != null) {
			for (type in additionalTypes) {
				if (type == cachedType) {
					return true
				}
			}
		}
		return false
	}

	fun getNameByType(type: Int): String? {
		if (type != -1) {
			return objectNames?.get(type)
		}
		return null
	}

	fun getAdditionalTypes(): IntArray? = additionalTypes

	fun isArea(): Boolean = area

	fun isCycle(): Boolean {
		val coordinates = this.coordinates
		if (coordinates == null || coordinates.size < 2) {
			return false
		}
		return coordinates[0] == coordinates[coordinates.size - 2] &&
				coordinates[1] == coordinates[coordinates.size - 1]
	}

	fun setArea(area: Boolean) {
		this.area = area
	}

	fun getId(): Long = id

	fun setId(id: Long) {
		this.id = id
	}

	fun setTypes(types: IntArray?) {
		this.types = types
	}

	/** 1 for a bridge, -1 for a tunnel, 0 for anything at ground level. */
	fun getSimpleLayer(): Int {
		val mapIndex = this.mapIndex
		val additionalTypes = this.additionalTypes
		if (mapIndex != null && additionalTypes != null) {
			for (type in additionalTypes) {
				if (mapIndex.positiveLayers.contains(type)) {
					return 1
				} else if (mapIndex.negativeLayers.contains(type)) {
					return -1
				}
			}
		}
		return 0
	}

	fun getNamesOrder(): KTIntArrayList? = namesOrder

	fun getMapIndex(): MapIndex? = mapIndex

	fun setMapIndex(mapIndex: MapIndex?) {
		this.mapIndex = mapIndex
	}

	fun getPointsLength(): Int {
		val coordinates = this.coordinates ?: return 0
		return coordinates.size / 2
	}

	fun getPoint31YTile(ind: Int): Int = coordinates!![2 * ind + 1]

	fun getPoint31XTile(ind: Int): Int = coordinates!![2 * ind]

	/**
	 * Whether [thatObj] is the same object, read from another file: same id and shape, and the same
	 * tags once both sides are decoded through their own sections. A [coordinatesPrecision] above
	 * zero allows the two shapes to differ by that much, for maps generated at different detail.
	 */
	fun compareBinary(thatObj: BinaryMapDataObject, coordinatesPrecision: Int): Boolean {
		if (this.objectType == thatObj.objectType &&
			this.id == thatObj.id &&
			this.area == thatObj.area &&
			compareCoordinates(this.coordinates, thatObj.coordinates, coordinatesPrecision)
		) {
			val mapIndex = this.mapIndex ?: throw IllegalStateException("Illegal binary object: $id")
			val thatMapIndex = thatObj.mapIndex
				?: throw IllegalStateException("Illegal binary object: ${thatObj.id}")

			var equals = true
			if (equals) {
				val inner = polygonInnerCoordinates
				val thatInner = thatObj.polygonInnerCoordinates
				if (inner == null || thatInner == null) {
					equals = inner === thatInner
				} else if (inner.size != thatInner.size) {
					equals = false
				} else {
					var i = 0
					while (i < inner.size && equals) {
						equals = compareCoordinates(inner[i], thatInner[i], coordinatesPrecision)
						i++
					}
				}
			}

			if (equals) {
				val types = this.types
				val thatTypes = thatObj.types
				if (types == null || thatTypes == null) {
					equals = types === thatTypes
				} else if (types.size != thatTypes.size) {
					equals = false
				} else {
					var i = 0
					while (i < types.size && equals) {
						val o = mapIndex.decodeType(types[i])
						val s = thatMapIndex.decodeType(thatTypes[i])
						equals = o == s && equals
						i++
					}
				}
			}
			if (equals) {
				val additionalTypes = this.additionalTypes
				val thatAdditionalTypes = thatObj.additionalTypes
				if (additionalTypes == null || thatAdditionalTypes == null) {
					equals = additionalTypes === thatAdditionalTypes
				} else if (additionalTypes.size != thatAdditionalTypes.size) {
					equals = false
				} else {
					var i = 0
					while (i < additionalTypes.size && equals) {
						val o = mapIndex.decodeType(additionalTypes[i])
						val s = thatMapIndex.decodeType(thatAdditionalTypes[i])
						equals = o == s
						i++
					}
				}
			}
			if (equals) {
				val namesOrder = this.namesOrder
				val thatNamesOrder = thatObj.namesOrder
				if (namesOrder == null || thatNamesOrder == null) {
					equals = namesOrder === thatNamesOrder
				} else if (namesOrder.size() != thatNamesOrder.size()) {
					equals = false
				} else {
					var i = 0
					while (i < namesOrder.size() && equals) {
						val o = mapIndex.decodeType(namesOrder[i])
						val s = thatMapIndex.decodeType(thatNamesOrder[i])
						equals = o == s
						i++
					}
				}
			}
			if (equals) {
				// here we know that name indexes are equal & it is enough to check the value sets
				val objectNames = this.objectNames
				val thatObjectNames = thatObj.objectNames
				if (objectNames == null || thatObjectNames == null) {
					equals = objectNames === thatObjectNames
				} else if (objectNames.size() != thatObjectNames.size()) {
					equals = false
				} else {
					val namesOrder = this.namesOrder
					val thatNamesOrder = thatObj.namesOrder
					var i = 0
					while (namesOrder != null && thatNamesOrder != null && i < namesOrder.size() && equals) {
						val o = objectNames[namesOrder[i]]
						val s = thatObjectNames[thatNamesOrder[i]]
						equals = o == s
						i++
					}
				}
			}

			return equals
		}
		return false
	}

	fun isLabelSpecified(): Boolean =
		(labelX != 0 || labelY != 0) && (coordinates?.size ?: 0) > 0

	fun getLabelX(): Int {
		var sum = 0L
		val coordinates = this.coordinates!!
		val labelShift = 31 - BinaryMapIndexReader.LABEL_ZOOM_ENCODE
		val len = coordinates.size / 2
		for (i in 0 until len) {
			sum += coordinates[2 * i]
		}
		val l = ((sum shr BinaryMapIndexReader.SHIFT_COORDINATES) / len).toInt()
		val average = l shl (BinaryMapIndexReader.SHIFT_COORDINATES - labelShift)
		return (average + this.labelX) shl labelShift
	}

	fun getLabelY(): Int {
		var sum = 0L
		val coordinates = this.coordinates!!
		val labelShift = 31 - BinaryMapIndexReader.LABEL_ZOOM_ENCODE
		val len = coordinates.size / 2
		for (i in 0 until len) {
			sum += coordinates[2 * i + 1]
		}
		val l = ((sum shr BinaryMapIndexReader.SHIFT_COORDINATES) / len).toInt()
		val average = l shl (BinaryMapIndexReader.SHIFT_COORDINATES - labelShift)
		return (average + this.labelY) shl labelShift
	}

	fun getLabelLatLon(): KLatLon =
		KLatLon(KMapUtils.get31LatitudeY(getLabelY()), KMapUtils.get31LongitudeX(getLabelX()))

	fun getCoordinates(): IntArray? = coordinates

	fun getObjectType(): Int = objectType

	fun getTagValue(tag: String): String {
		val mapIndex = this.mapIndex ?: return ""
		val names = objectNames ?: return ""
		for (key in names.keys()) {
			val tp = mapIndex.decodeType(key)
			if (tp != null && tp.tag == tag) {
				return names[key] ?: ""
			}
		}
		return ""
	}

	fun getAdditionalTagValue(tag: String): String {
		val mapIndex = this.mapIndex ?: return ""
		val additionalTypes = this.additionalTypes ?: return ""
		for (type in additionalTypes) {
			val tp = mapIndex.decodeType(type)
			if (tp != null && tag == tp.tag) {
				return tp.value ?: ""
			}
		}
		return ""
	}

	override fun toString(): String {
		val obj = when (objectType) {
			LINE_RULES -> "Line"
			POLYGON_RULES -> "Polygon"
			else -> "Point"
		}
		return "$obj $id"
	}

	fun isDeleted(): Boolean {
		val mapIndex = this.mapIndex
		val types = this.types
		if (mapIndex != null && types != null && types.isNotEmpty()) {
			for (t in types) {
				val tvp = mapIndex.decodeType(t)
				if (tvp != null && tvp.tag == "osmand_change") {
					return true
				}
			}
		}
		return false
	}

	companion object {
		const val SHIFT_ID: Int = 7

		// the three kinds of rendering rule an object can be drawn by, as RenderingRulesStorage
		// in OsmAnd-java numbers them
		const val POINT_RULES: Int = 1
		const val LINE_RULES: Int = 2
		const val POLYGON_RULES: Int = 3

		private fun compareCoordinates(
			coordinates: IntArray?, coordinates2: IntArray?, precision: Int
		): Boolean {
			if (precision == 0) {
				return coordinates.contentEquals(coordinates2)
			}
			if (coordinates == null || coordinates2 == null) {
				return coordinates === coordinates2
			}
			val cd = simplify(coordinates, precision)
			val cd2 = simplify(coordinates2, precision)
			return cd == cd2
		}

		/** Douglas-Peucker: the points that carry the shape at [precision], in order. */
		private fun simplify(c: IntArray, precision: Int): KTIntArrayList {
			val len = c.size / 2
			val lt = KTIntArrayList(len * 3)
			for (i in 0 until len) {
				lt.add(0)
				lt.add(c[i * 2])
				lt.add(c[i * 2 + 1])
			}
			lt[0] = 1
			lt[(len - 1) * 3] = 1
			simplifyLine(lt, precision, 0, len - 1)

			val res = KTIntArrayList(len * 2)
			for (i in 0 until len) {
				if (lt[i * 3] == 1) {
					res.add(lt[i * 3 + 1])
					res.add(lt[i * 3 + 2])
				}
			}
			return res
		}

		private fun orthogonalDistance(x: Int, y: Int, x1: Int, y1: Int, x2: Int, y2: Int): Double {
			val a = (x - x1).toLong()
			val b = (y - y1).toLong()
			val c = (x2 - x1).toLong()
			val d = (y2 - y1).toLong()
			return abs(a * d - c * b) / sqrt((c * c + d * d).toDouble())
		}

		private fun simplifyLine(lt: KTIntArrayList, precision: Int, start: Int, end: Int) {
			if (start == end - 1) {
				return
			}
			val x = lt[start * 3 + 1]
			val y = lt[start * 3 + 2]
			val ex = lt[end * 3 + 1]
			val ey = lt[end * 3 + 2]
			var maxDistance = 0.0
			var maxK = -1
			for (k in start + 1 until end) {
				val ld = orthogonalDistance(lt[k * 3 + 1], lt[k * 3 + 2], x, y, ex, ey)
				if (maxK == -1 || maxDistance < ld) {
					maxK = k
					maxDistance = ld
				}
			}
			if (maxDistance < precision) {
				return
			}
			lt[maxK * 3] = 1 // keep point
			simplifyLine(lt, precision, start, maxK)
			simplifyLine(lt, precision, maxK, end)
		}
	}
}
