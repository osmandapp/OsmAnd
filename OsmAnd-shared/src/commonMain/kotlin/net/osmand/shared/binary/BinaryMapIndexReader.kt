package net.osmand.shared.binary

import net.osmand.shared.io.KFile
import net.osmand.shared.routing.RouteDataObject
import net.osmand.shared.routing.RouteRegion
import net.osmand.shared.routing.RouteSubregion
import okio.FileHandle
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * An open obf file, with the index sections it carries located and the routing ones read.
 *
 * A copy of `BinaryMapIndexReader` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS, so a route can be calculated there without the C++ core. It carries the part
 * the routing needs - the structure walk in the constructor, the routing sections, `readInt` and
 * the string table - and skips the map, address, poi, transport and HH sections, which will be
 * read when the searches that use them are copied. The method names are java's, so the two can
 * be compared side by side.
 */
class BinaryMapIndexReader {

	private val handle: FileHandle
	private val file: KFile
	internal val codedIS: CodedInputStream
	private val routeAdapter: BinaryMapRouteReaderAdapter

	private var version: Int = 0
	private var dateCreated: Long = 0
	private var initCorrectly = false

	private val routingIndexes = ArrayList<RouteRegion>()

	/** True when the file carries an HH routing section; the section itself is not read yet. */
	private var hhRoutingIndexes = 0

	constructor(file: KFile) {
		this.file = file
		this.handle = FileSystem.SYSTEM.openReadOnly(file.path().toPath())
		this.codedIS = CodedInputStream(handle)
		this.routeAdapter = BinaryMapRouteReaderAdapter(this)
		init()
	}

	/** Takes a path rather than a [KFile], for java callers that do not have okio on their classpath. */
	constructor(filePath: String) : this(KFile(filePath))

	private fun init() {
		val size = handle.size()
		codedIS.seek(0)
		codedIS.pushLimitLong(size)
		var reachedEnd = false
		while (!reachedEnd) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> reachedEnd = true
				OsmAndStructure.VERSION_FIELD_NUMBER -> version = codedIS.readUInt32()
				OsmAndStructure.DATECREATED_FIELD_NUMBER -> dateCreated = codedIS.readInt64()
				OsmAndStructure.ROUTINGINDEX_FIELD_NUMBER -> {
					val routeReg = RouteRegion()
					routeReg.length = readInt()
					routeReg.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(routeReg.length)
					routeAdapter.readRouteIndex(routeReg)
					codedIS.popLimit(oldLimit)
					routingIndexes.add(routeReg)
					codedIS.seek(routeReg.filePointer + routeReg.length)
				}
				OsmAndStructure.HHROUTINGINDEX_FIELD_NUMBER -> {
					hhRoutingIndexes++
					val length = readInt()
					codedIS.seek(codedIS.getTotalBytesRead() + length)
				}
				OsmAndStructure.MAPINDEX_FIELD_NUMBER, OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER,
				OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER, OsmAndStructure.POIINDEX_FIELD_NUMBER -> {
					val length = readInt()
					codedIS.seek(codedIS.getTotalBytesRead() + length)
				}
				OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER -> {
					val cversion = codedIS.readUInt32()
					initCorrectly = cversion == version
					reachedEnd = true
				}
				else -> skipUnknownField(t)
			}
		}
	}

	fun getFile(): KFile = file

	fun getVersion(): Int = version

	fun getDateCreated(): Long = dateCreated

	fun isInitCorrectly(): Boolean = initCorrectly

	fun getRoutingIndexes(): List<RouteRegion> = routingIndexes

	fun containsRouteData(): Boolean = routingIndexes.size > 0

	fun hasHHRoutingIndexes(): Boolean = hhRoutingIndexes > 0

	fun containsRouteData(left31x: Int, top31y: Int, right31x: Int, bottom31y: Int): Boolean {
		for (ri in routingIndexes) {
			for (r in ri.getSubregions()) {
				if (right31x >= r.left && left31x <= r.right && r.top <= bottom31y && r.bottom >= top31y) {
					return true
				}
			}
		}
		return false
	}

	/**
	 * `OsmAndStructure` frames its index sections with a big endian length: four bytes, or eight
	 * when the high bit of the first is set.
	 */
	fun readInt(): Long {
		var l = (codedIS.readRawByte().toInt() and 0xff).toLong()
		val eightBytes = l > 0x7f
		if (eightBytes) {
			l = l and 0x7f
		}
		l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
		l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
		l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
		if (eightBytes) {
			l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
			l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
			l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
			l = (l shl 8) + (codedIS.readRawByte().toInt() and 0xff)
		}
		return l
	}

	internal fun skipUnknownField(tag: Int) {
		val wireType = CodedInputStream.getTagWireType(tag)
		if (wireType == CodedInputStream.WIRETYPE_FIXED32_LENGTH_DELIMITED) {
			val length = readInt()
			codedIS.skipRawBytes(length)
		} else {
			codedIS.skipField(tag)
		}
	}

	internal fun readStringTable(): List<String> {
		val list = ArrayList<String>()
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return list
				StringTable.S_FIELD_NUMBER -> list.add(codedIS.readString())
				else -> skipUnknownField(t)
			}
		}
	}

	fun searchRouteIndexTree(req: SearchRequest, list: List<RouteSubregion>): List<RouteSubregion> {
		req.numberOfVisitedObjects = 0
		req.numberOfAcceptedObjects = 0
		req.numberOfAcceptedSubtrees = 0
		req.numberOfReadSubtrees = 0
		routeAdapter.initRouteTypesIfNeeded(req, list)
		return routeAdapter.searchRouteRegionTree(req, list, ArrayList())
	}

	fun loadRouteIndexData(toLoad: MutableList<RouteSubregion>, matcher: ResultMatcher<RouteDataObject>) {
		routeAdapter.loadRouteRegionData(toLoad, matcher)
	}

	fun loadRouteIndexData(rs: RouteSubregion): List<RouteDataObject?> = routeAdapter.loadRouteRegionData(rs)

	fun initRouteRegion(routeReg: RouteRegion) {
		routeAdapter.initRouteRegion(routeReg)
	}

	fun close() {
		handle.close()
	}

	/** Field numbers of `OsmAndStructure` in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndStructure {
		const val VERSION_FIELD_NUMBER = 1
		const val TRANSPORTINDEX_FIELD_NUMBER = 4
		const val MAPINDEX_FIELD_NUMBER = 6
		const val ADDRESSINDEX_FIELD_NUMBER = 7
		const val POIINDEX_FIELD_NUMBER = 8
		const val ROUTINGINDEX_FIELD_NUMBER = 9
		const val HHROUTINGINDEX_FIELD_NUMBER = 10
		const val DATECREATED_FIELD_NUMBER = 18
		const val VERSIONCONFIRM_FIELD_NUMBER = 32
	}

	private object StringTable {
		const val S_FIELD_NUMBER = 1
	}
}
