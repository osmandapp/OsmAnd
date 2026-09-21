package net.osmand.shared.binary

import net.osmand.shared.data.KQuadRect
import net.osmand.shared.routing.HHRouteDataStructure
import net.osmand.shared.routing.HHRouteRegionPointsCtx
import net.osmand.shared.routing.HHRoutingContext
import net.osmand.shared.routing.NetworkDBPoint
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * A block of a region's edge section: the edges of the vertices with file ids in
 * `[idRangeStart, idRangeStart + idRangeLength)` for one profile, either directly or split into
 * [sublist] blocks read on first use.
 *
 * A copy of `BinaryHHRouteReaderAdapter.HHRouteBlockSegments` in OsmAnd-java.
 */
class HHRouteBlockSegments {

	@JvmField
	var idRangeStart: Int = 0

	@JvmField
	var idRangeLength: Int = 0

	@JvmField
	var profileId: Int = 0

	@JvmField
	var length: Long = 0

	@JvmField
	var filePointer: Long = 0

	@JvmField
	var sublist: MutableList<HHRouteBlockSegments>? = null
}

/**
 * A node of the tree the vertices of a region are stored in, with its bounds in 31 coordinates.
 *
 * A copy of `BinaryHHRouteReaderAdapter.HHRoutePointsBox` in OsmAnd-java.
 */
class HHRoutePointsBox {

	@JvmField
	var length: Long = 0

	@JvmField
	var filePointer: Long = 0

	@JvmField
	var left: Int = 0

	@JvmField
	var right: Int = 0

	@JvmField
	var bottom: Int = 0

	@JvmField
	var top: Int = 0

	fun getLatLonBox(): KQuadRect {
		val q = KQuadRect()
		q.left = KMapUtils.get31LongitudeX(left)
		q.right = KMapUtils.get31LongitudeX(right)
		q.top = KMapUtils.get31LatitudeY(top)
		q.bottom = KMapUtils.get31LatitudeY(bottom)
		return q
	}

	fun contains(x: Int, y: Int): Boolean = x >= left && x <= right && y >= top && y <= bottom
}

/**
 * An HH routing section of an obf file: the hub graph of one vehicle profile, built at [edition]
 * for each of the parameter sets in [profileParams] (the empty string is the profile without
 * parameters). [top] is the root of the vertex tree and gives the section its bounds; the
 * [segments] headers are read together with the vertices, not when the file is opened.
 *
 * A copy of `BinaryHHRouteReaderAdapter.HHRouteRegion` in OsmAnd-java.
 */
open class HHRouteRegion : BinaryIndexPart() {

	@JvmField
	var edition: Long = 0

	@JvmField
	var profile: String? = null

	@JvmField
	var profileParams: MutableList<String> = ArrayList()

	@JvmField
	var top: HHRoutePointsBox? = null

	@JvmField
	var encodingRules: MutableList<TagValuePair> = ArrayList()

	/** not stored in cache */
	@JvmField
	var segments: MutableList<HHRouteBlockSegments>? = null

	override fun getPartName(): String = "Highway routing "

	override fun getName(): String? = profile

	override fun getFieldNumber(): Int = BinaryMapIndexReader.HHROUTINGINDEX_FIELD_NUMBER

	open fun getLatLonBbox(): KQuadRect {
		return top?.getLatLonBox() ?: KQuadRect()
	}
}

/**
 * Reads the HH routing section of an obf file: the header when the file is opened, all vertices
 * of a region when a route is calculated over it, and the edges of one vertex when the search
 * reaches it.
 *
 * A copy of `BinaryHHRouteReaderAdapter` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. Java reads the vertices into a `Class<T>` chosen by the caller,
 * which is how the tools plug in their point subclasses; here they are always [NetworkDBPoint].
 * The two `bytes` fields of a vertex's edges go through a [PointSegments] pair rather than the
 * protobuf message java builds them into.
 */
class BinaryHHRouteReaderAdapter internal constructor(private val map: BinaryMapIndexReader) {

	private val codedIS: CodedInputStream = map.codedIS

	private fun skipUnknownField(t: Int) {
		map.skipUnknownField(t)
	}

	private fun readInt(): Long = map.readInt()

	fun initRegionAndLoadPoints(reg: HHRouteRegion, mapId: Short): KTLongObjectMap<NetworkDBPoint> {
		codedIS.seek(reg.filePointer)
		val oldLimit = codedIS.pushLimitLong(reg.length)
		val mp = KTLongObjectMap<NetworkDBPoint>()
		val segments = ArrayList<HHRouteBlockSegments>()
		reg.segments = segments
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					codedIS.popLimit(oldLimit)
					return mp
				}
				OsmAndHHRoutingIndex.TAGVALUESTABLE_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val old = codedIS.pushLimitLong(length.toLong())
					val st = map.readStringTable()
					for (s in st) {
						val i = s.indexOf('=')
						if (i > 0) {
							reg.encodingRules.add(TagValuePair(s.substring(0, i), s.substring(i + 1), -1))
						}
					}
					codedIS.popLimit(old)
				}
				OsmAndHHRoutingIndex.POINTBOXES_FIELD_NUMBER -> readPointBox(reg, mapId, mp, null)
				OsmAndHHRoutingIndex.POINTSEGMENTS_FIELD_NUMBER -> segments.add(readRegionSegmentHeader())
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readHHIndex(region: HHRouteRegion, fullInit: Boolean) {
		region.profileParams.clear()
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndHHRoutingIndex.EDITION_FIELD_NUMBER -> region.edition = codedIS.readInt64()
				OsmAndHHRoutingIndex.PROFILE_FIELD_NUMBER -> region.profile = codedIS.readString()
				OsmAndHHRoutingIndex.PROFILEPARAMS_FIELD_NUMBER -> region.profileParams.add(codedIS.readString())
				OsmAndHHRoutingIndex.POINTBOXES_FIELD_NUMBER -> region.top = readPointBox(region, 0, null, null)
				OsmAndHHRoutingIndex.POINTSEGMENTS_FIELD_NUMBER -> codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
				else -> skipUnknownField(t)
			}
		}
	}

	/** With [mp] null only the bounds of the box are read, as java does when it is given no point class. */
	private fun readPointBox(
		reg: HHRouteRegion, mapId: Short, mp: KTLongObjectMap<NetworkDBPoint>?, parent: HHRoutePointsBox?
	): HHRoutePointsBox {
		val box = HHRoutePointsBox()
		box.length = readInt()
		box.filePointer = codedIS.getTotalBytesRead()
		val oldLimit = codedIS.pushLimitLong(box.length)
		while (true) {
			if (mp == null && box.bottom != 0 && box.top != 0 && box.right != 0 && box.left != 0) {
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					codedIS.popLimit(oldLimit)
					return box
				}
				OsmAndHHRoutingIndex.HHRoutePointsBox.BOTTOM_FIELD_NUMBER ->
					box.bottom = codedIS.readSInt32() + (parent?.bottom ?: 0)
				OsmAndHHRoutingIndex.HHRoutePointsBox.TOP_FIELD_NUMBER ->
					box.top = codedIS.readSInt32() + (parent?.top ?: 0)
				OsmAndHHRoutingIndex.HHRoutePointsBox.RIGHT_FIELD_NUMBER ->
					box.right = codedIS.readSInt32() + (parent?.right ?: 0)
				OsmAndHHRoutingIndex.HHRoutePointsBox.LEFT_FIELD_NUMBER ->
					box.left = codedIS.readSInt32() + (parent?.left ?: 0)
				OsmAndHHRoutingIndex.HHRoutePointsBox.BOXES_FIELD_NUMBER -> {
					if (mp == null) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					} else {
						readPointBox(reg, mapId, mp, box)
					}
				}
				OsmAndHHRoutingIndex.HHRoutePointsBox.POINTS_FIELD_NUMBER -> {
					if (mp == null) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					} else {
						readPoint(reg, mapId, mp, box.left, box.top)
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	private fun readPoint(
		reg: HHRouteRegion, mapId: Short, mp: KTLongObjectMap<NetworkDBPoint>, dx: Int, dy: Int
	): NetworkDBPoint {
		val pnt = NetworkDBPoint()
		pnt.mapId = mapId
		val size = codedIS.readRawVarint32()
		val oldLimit = codedIS.pushLimitLong(size.toLong())
		var dualIdPoint = -1
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					codedIS.popLimit(oldLimit)
					mp.put(pnt.index.toLong(), pnt)
					if (dualIdPoint >= 0) {
						val dual = mp[dualIdPoint.toLong()]
						if (dual != null) {
							pnt.dualPoint = dual
							dual.dualPoint = pnt
							dual.endX = pnt.startX
							dual.endY = pnt.startY
							pnt.endX = dual.startX
							pnt.endY = dual.startY
						}
					}
					return pnt
				}
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.ID_FIELD_NUMBER -> pnt.fileId = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.DX_FIELD_NUMBER -> {
					pnt.startX = codedIS.readSInt32() + dx
					pnt.endX = pnt.startX
				}
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.DY_FIELD_NUMBER -> {
					pnt.startY = codedIS.readSInt32() + dy
					pnt.endY = pnt.startY
				}
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.GLOBALID_FIELD_NUMBER -> pnt.index = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.TAGVALUEIDS_FIELD_NUMBER -> {
					val sz = codedIS.readRawVarint32()
					val old = codedIS.pushLimitLong(sz.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						val tvId = codedIS.readInt32()
						if (tvId < reg.encodingRules.size) {
							val tagValuePair = reg.encodingRules[tvId]
							var tagValues = pnt.tagValues
							if (tagValues == null) {
								tagValues = ArrayList()
								pnt.tagValues = tagValues
							}
							tagValues.add(tagValuePair)
						}
					}
					codedIS.popLimit(old)
				}
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.ROADID_FIELD_NUMBER -> pnt.roadId = codedIS.readInt64()
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.ROADSTARTENDINDEX_FIELD_NUMBER -> {
					val v = codedIS.readInt32()
					pnt.start = (v shr 1).toShort()
					pnt.end = (pnt.start + (if (v % 2 == 1) 1 else -1)).toShort()
				}
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.CLUSTERID_FIELD_NUMBER -> pnt.clusterId = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.PARTIALIND_FIELD_NUMBER -> pnt.incomplete = codedIS.readInt32() > 0
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.DUALPOINTID_FIELD_NUMBER -> dualIdPoint = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteNetworkPoint.DUALCLUSTERID_FIELD_NUMBER -> codedIS.readInt32()
				else -> skipUnknownField(t)
			}
		}
	}

	private fun readRegionSegmentHeader(): HHRouteBlockSegments {
		val block = HHRouteBlockSegments()
		block.length = readInt()
		block.filePointer = codedIS.getTotalBytesRead()
		val oldLimit = codedIS.pushLimitLong(block.length)
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					codedIS.popLimit(oldLimit)
					return block
				}
				OsmAndHHRoutingIndex.HHRouteBlockSegments.IDRANGELENGTH_FIELD_NUMBER -> block.idRangeLength = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteBlockSegments.IDRANGESTART_FIELD_NUMBER -> block.idRangeStart = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteBlockSegments.PROFILEID_FIELD_NUMBER -> block.profileId = codedIS.readInt32()
				OsmAndHHRoutingIndex.HHRouteBlockSegments.INNERBLOCKS_FIELD_NUMBER,
				OsmAndHHRoutingIndex.HHRouteBlockSegments.POINTSEGMENTS_FIELD_NUMBER ->
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
				else -> skipUnknownField(t)
			}
		}
	}

	private fun loadNetworkSegmentPoint(
		ctx: HHRoutingContext, reg: HHRouteRegionPointsCtx, block: HHRouteBlockSegments, searchInd: Int, reverse: Boolean
	): Int {
		val sublist = block.sublist
		if (sublist != null) {
			for (s in sublist) {
				if (checkId(searchInd, s)) {
					return loadNetworkSegmentPoint(ctx, reg, s, searchInd, reverse)
				}
			}
			return 0
		}
		if (codedIS.getTotalBytesRead() != block.filePointer) {
			codedIS.seek(block.filePointer)
		}
		var loaded = 0
		val oldLimit = codedIS.pushLimitLong(block.length)
		var ind = 0
		try {
			while (true) {
				val t = codedIS.readTag()
				when (CodedInputStream.getTagFieldNumber(t)) {
					0 -> return loaded
					OsmAndHHRoutingIndex.HHRouteBlockSegments.IDRANGELENGTH_FIELD_NUMBER -> block.idRangeLength = codedIS.readInt32()
					OsmAndHHRoutingIndex.HHRouteBlockSegments.IDRANGESTART_FIELD_NUMBER -> block.idRangeStart = codedIS.readInt32()
					OsmAndHHRoutingIndex.HHRouteBlockSegments.PROFILEID_FIELD_NUMBER -> block.profileId = codedIS.readInt32()
					OsmAndHHRoutingIndex.HHRouteBlockSegments.INNERBLOCKS_FIELD_NUMBER -> {
						if (!checkId(searchInd, block)) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						} else {
							// read all sublist
							var list = block.sublist
							if (list == null) {
								list = ArrayList()
								block.sublist = list
							}
							val child = HHRouteBlockSegments()
							child.length = readInt()
							child.filePointer = codedIS.getTotalBytesRead()
							val olLimit = codedIS.pushLimitLong(child.length)
							loaded += loadNetworkSegmentPoint(ctx, reg, child, searchInd, reverse)
							codedIS.popLimit(olLimit)
							list.add(child)
						}
					}
					OsmAndHHRoutingIndex.HHRouteBlockSegments.POINTSEGMENTS_FIELD_NUMBER -> {
						if (!checkId(searchInd, block)) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						} else {
							val pntFileId = (ind++) + block.idRangeStart
							val point = reg.getPoint(pntFileId)
							val len = codedIS.readRawVarint32()
							val olLimit = codedIS.pushLimitLong(len.toLong())
							val s = readSegments()
							codedIS.popLimit(olLimit)
							if (point != null) {
								// not used from this file
								HHRouteDataStructure.setSegments(ctx, point, s.segmentsIn, s.segmentsOut)
								loaded += (point.connected(true)?.size ?: 0) + (point.connected(false)?.size ?: 0)
							}
						}
					}
					else -> skipUnknownField(t)
				}
			}
		} finally {
			codedIS.popLimit(oldLimit)
		}
	}

	private class PointSegments {
		var segmentsIn: ByteArray? = null
		var segmentsOut: ByteArray? = null
	}

	private fun readSegments(): PointSegments {
		val s = PointSegments()
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return s
				OsmAndHHRoutingIndex.HHRoutePointSegments.SEGMENTSIN_FIELD_NUMBER -> s.segmentsIn = codedIS.readBytes()
				OsmAndHHRoutingIndex.HHRoutePointSegments.SEGMENTSOUT_FIELD_NUMBER -> s.segmentsOut = codedIS.readBytes()
				else -> skipUnknownField(t)
			}
		}
	}

	private fun checkId(id: Int, s: HHRouteBlockSegments): Boolean {
		return s.idRangeStart <= id && s.idRangeStart + s.idRangeLength > id
	}

	fun loadNetworkSegmentPoint(ctx: HHRoutingContext, reg: HHRouteRegionPointsCtx, point: NetworkDBPoint, reverse: Boolean): Int {
		if (point.connected(reverse) != null) {
			return 0
		}
		val fileRegion = reg.getFileRegion()
		val segments = fileRegion.segments ?: throw IllegalStateException("Points of ${fileRegion.profile} were not loaded")
		for (s in segments) {
			if (s.profileId == reg.getRoutingProfile() && checkId(point.fileId, s)) {
				return loadNetworkSegmentPoint(ctx, reg, s, point.fileId, reverse)
			}
		}
		return 0
	}

	companion object {

		/**
		 * Segment headers are read together with the points, so a region of another reader over the same file
		 * that didn't read points can take them. Inner blocks (sublist) are read lazily from its own file.
		 */
		@JvmStatic
		fun copySegmentHeaders(src: HHRouteRegion, dst: HHRouteRegion) {
			val from = src.segments
			if (dst.segments != null || from == null) {
				return
			}
			val segments = ArrayList<HHRouteBlockSegments>()
			for (s in from) {
				val block = HHRouteBlockSegments()
				block.idRangeStart = s.idRangeStart
				block.idRangeLength = s.idRangeLength
				block.profileId = s.profileId
				block.length = s.length
				block.filePointer = s.filePointer
				segments.add(block)
			}
			dst.segments = segments
		}
	}

	/** Field numbers of `OsmAndHHRoutingIndex` in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndHHRoutingIndex {
		const val EDITION_FIELD_NUMBER = 1
		const val PROFILE_FIELD_NUMBER = 2
		const val PROFILEPARAMS_FIELD_NUMBER = 3
		const val TAGVALUESTABLE_FIELD_NUMBER = 4
		const val POINTBOXES_FIELD_NUMBER = 5
		const val POINTSEGMENTS_FIELD_NUMBER = 6

		object HHRoutePointsBox {
			const val LEFT_FIELD_NUMBER = 2
			const val RIGHT_FIELD_NUMBER = 3
			const val TOP_FIELD_NUMBER = 4
			const val BOTTOM_FIELD_NUMBER = 5
			const val BOXES_FIELD_NUMBER = 6
			const val POINTS_FIELD_NUMBER = 7
		}

		object HHRouteNetworkPoint {
			const val ID_FIELD_NUMBER = 1
			const val DX_FIELD_NUMBER = 2
			const val DY_FIELD_NUMBER = 3
			const val GLOBALID_FIELD_NUMBER = 4
			const val ROADID_FIELD_NUMBER = 5
			const val ROADSTARTENDINDEX_FIELD_NUMBER = 6
			const val CLUSTERID_FIELD_NUMBER = 7
			const val DUALPOINTID_FIELD_NUMBER = 8
			const val DUALCLUSTERID_FIELD_NUMBER = 9
			const val PARTIALIND_FIELD_NUMBER = 11
			const val TAGVALUEIDS_FIELD_NUMBER = 12
		}

		object HHRouteBlockSegments {
			const val IDRANGESTART_FIELD_NUMBER = 1
			const val IDRANGELENGTH_FIELD_NUMBER = 2
			const val PROFILEID_FIELD_NUMBER = 3
			const val POINTSEGMENTS_FIELD_NUMBER = 4
			const val INNERBLOCKS_FIELD_NUMBER = 6
		}

		object HHRoutePointSegments {
			const val SEGMENTSIN_FIELD_NUMBER = 2
			const val SEGMENTSOUT_FIELD_NUMBER = 3
		}
	}
}
