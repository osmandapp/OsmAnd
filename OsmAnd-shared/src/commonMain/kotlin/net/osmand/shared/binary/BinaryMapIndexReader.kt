package net.osmand.shared.binary

import net.osmand.shared.io.KFile
import net.osmand.shared.routing.HHRouteRegionPointsCtx
import net.osmand.shared.routing.HHRoutingContext
import net.osmand.shared.routing.NetworkDBPoint
import net.osmand.shared.routing.RouteDataObject
import net.osmand.shared.routing.RouteRegion
import net.osmand.shared.routing.RouteSubregion
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.collections.KTLongObjectMap
import okio.FileHandle
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.max
import kotlin.math.min

/**
 * An open obf file, with the index sections it carries located and the routing ones read.
 *
 * A copy of `BinaryMapIndexReader` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS, so a route can be calculated and a map read there without the C++ core. It
 * carries the structure walk in the constructor, the routing and HH sections, the map section,
 * `readInt` and the string table, and skips the address, poi and transport sections, which will
 * be read when the searches that use them are copied. The method names are java's, so the two can
 * be compared side by side.
 *
 * The read statistics java collects behind `READ_STATS` are left out: they serve the obf
 * inspection tools, which stay in OsmAnd-java.
 */
class BinaryMapIndexReader {

	private val handle: FileHandle
	private val file: KFile
	internal val codedIS: CodedInputStream
	private val routeAdapter: BinaryMapRouteReaderAdapter
	private val hhAdapter: BinaryHHRouteReaderAdapter

	private var version: Int = 0
	private var dateCreated: Long = 0
	private var initCorrectly = false

	private val mapIndexes = ArrayList<MapIndex>()
	private val routingIndexes = ArrayList<RouteRegion>()
	private val hhIndexes = ArrayList<HHRouteRegion>()
	private var basemap = false

	constructor(file: KFile) {
		this.file = file
		this.handle = FileSystem.SYSTEM.openReadOnly(file.path().toPath())
		this.codedIS = CodedInputStream(handle)
		this.routeAdapter = BinaryMapRouteReaderAdapter(this)
		this.hhAdapter = BinaryHHRouteReaderAdapter(this)
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
					val hhreg = HHRouteRegion()
					hhreg.length = readInt()
					hhreg.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(hhreg.length)
					hhAdapter.readHHIndex(hhreg, false)
					codedIS.popLimit(oldLimit)
					hhIndexes.add(hhreg)
					codedIS.seek(hhreg.filePointer + hhreg.length)
				}
				OsmAndStructure.MAPINDEX_FIELD_NUMBER -> {
					val mapIndex = MapIndex()
					mapIndex.length = readInt()
					mapIndex.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(mapIndex.length)
					readMapIndex(mapIndex, false)
					basemap = basemap || mapIndex.isBaseMap()
					codedIS.popLimit(oldLimit)
					codedIS.seek(mapIndex.filePointer + mapIndex.length)
					mapIndexes.add(mapIndex)
				}
				OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER,
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

	fun getMapIndexes(): List<MapIndex> = mapIndexes

	fun isBasemap(): Boolean = basemap

	fun containsMapData(): Boolean = mapIndexes.size > 0

	fun getRoutingIndexes(): List<RouteRegion> = routingIndexes

	fun containsRouteData(): Boolean = routingIndexes.size > 0

	fun getHHRoutingIndexes(): List<HHRouteRegion> = hhIndexes

	fun hasHHRoutingIndexes(): Boolean = hhIndexes.isNotEmpty()

	/**
	 * Whether a routing section has roads in the zoom 14 tile of the point. A region named in
	 * [checkedRegions] is skipped, and every region asked is added to it, so a caller asking
	 * several readers over the same regions asks each region once.
	 */
	fun containsActualRouteData(x31: Int, y31: Int, checkedRegions: MutableSet<String>?): Boolean {
		val zoomToLoad = 14
		val x = x31 shr zoomToLoad
		val y = y31 shr zoomToLoad
		val request = SearchRequest.buildSearchRouteRequest(
			x shl zoomToLoad, (x + 1) shl zoomToLoad, y shl zoomToLoad, (y + 1) shl zoomToLoad
		)
		for (reg in getRoutingIndexes()) {
			if (checkedRegions != null) {
				val name = reg.getName() ?: ""
				if (checkedRegions.contains(name)) {
					continue
				}
				checkedRegions.add(name)
			}
			val res = searchRouteIndexTree(request, reg.getSubregions())
			if (res.isNotEmpty()) {
				return true
			}
		}
		return false
	}

	fun containsMapData(tile31x: Int, tile31y: Int, zoom: Int): Boolean {
		for (mapIndex in mapIndexes) {
			for (root in mapIndex.getRoots()) {
				if (root.minZoom <= zoom && root.maxZoom >= zoom) {
					if (tile31x >= root.left && tile31x <= root.right &&
						root.top <= tile31y && root.bottom >= tile31y) {
						return true
					}
				}
			}
		}
		return false
	}

	fun containsMapData(left31x: Int, top31y: Int, right31x: Int, bottom31y: Int, zoom: Int): Boolean {
		for (mapIndex in mapIndexes) {
			for (root in mapIndex.getRoots()) {
				if (root.minZoom <= zoom && root.maxZoom >= zoom) {
					if (right31x >= root.left && left31x <= root.right &&
						root.top <= bottom31y && root.bottom >= top31y) {
						return true
					}
				}
			}
		}
		return false
	}

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

	fun searchRouteIndexTree(req: SearchRequest<RouteDataObject>, list: List<RouteSubregion>): List<RouteSubregion> {
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

	/** Reads every vertex of the HH section [reg], keyed by index, with the section's edge block headers. */
	fun initHHPoints(reg: HHRouteRegion, mapId: Short): KTLongObjectMap<NetworkDBPoint> {
		return hhAdapter.initRegionAndLoadPoints(reg, mapId)
	}

	/** Reads the edges of [point] in one direction, if they are not loaded yet; returns how many. */
	fun loadNetworkSegmentPoint(ctx: HHRoutingContext, reg: HHRouteRegionPointsCtx, point: NetworkDBPoint, reverse: Boolean): Int {
		return hhAdapter.loadNetworkSegmentPoint(ctx, reg, point, reverse)
	}

	/**
	 * Map section
	 */

	/**
	 * Reads the section header. With [onlyInitEncodingRules] it reads the table that decodes the
	 * type numbers and leaves the r-tree roots alone; without it, the other way round. The roots
	 * are needed to answer whether a file covers a place, the table only once something is read
	 * out of it, so a file that is never searched never pays for its table.
	 */
	private fun readMapIndex(index: MapIndex, onlyInitEncodingRules: Boolean) {
		var defaultId = 1
		var encodingRulesSize = 0L
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					// encoding rules are required!
					if (onlyInitEncodingRules) {
						index.finishInitializingTags()
					}
					return
				}
				OsmAndMapIndex.NAME_FIELD_NUMBER -> index.setName(codedIS.readString())
				OsmAndMapIndex.RULES_FIELD_NUMBER -> {
					if (onlyInitEncodingRules) {
						if (encodingRulesSize == 0L) {
							encodingRulesSize = codedIS.getTotalBytesRead()
						}
						val len = codedIS.readInt32()
						val oldLimit = codedIS.pushLimitLong(len.toLong())
						readMapEncodingRule(index, defaultId++)
						codedIS.popLimit(oldLimit)
						index.encodingRulesSizeBytes =
							(codedIS.getTotalBytesRead() - encodingRulesSize).toInt()
					} else {
						skipUnknownField(t)
					}
				}
				OsmAndMapIndex.LEVELS_FIELD_NUMBER -> {
					val length = readInt()
					val filePointer = codedIS.getTotalBytesRead()
					if (!onlyInitEncodingRules) {
						val oldLimit = codedIS.pushLimitLong(length)
						val mapRoot = readMapLevel(MapRoot())
						mapRoot.length = length
						mapRoot.filePointer = filePointer
						index.getRoots().add(mapRoot)
						codedIS.popLimit(oldLimit)
					}
					codedIS.seek(filePointer + length)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * One row of the type table. Its id is its position, unless the file states another one.
	 * Java interns the tag and the value, which shares the few dozen distinct tags of a file
	 * between its sections; there is no interning on Kotlin/Native, so each row keeps its own.
	 */
	private fun readMapEncodingRule(index: MapIndex, defaultId: Int) {
		var type = 0
		var tags: String? = null
		var value: String? = null
		var id = defaultId
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					index.initMapEncodingRule(type, id, tags, value)
					return
				}
				MapEncodingRule.VALUE_FIELD_NUMBER -> value = codedIS.readString()
				MapEncodingRule.TAG_FIELD_NUMBER -> tags = codedIS.readString()
				MapEncodingRule.TYPE_FIELD_NUMBER -> type = codedIS.readUInt32()
				MapEncodingRule.ID_FIELD_NUMBER -> id = codedIS.readUInt32()
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * Reads one zoom level of a section: its box and zoom range always, and its top level r-tree
	 * boxes only once [MapRoot.trees] has been created, which is how a second pass over the same
	 * level fills in the tree the first pass skipped.
	 */
	private fun readMapLevel(root: MapRoot): MapRoot {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return root
				MapRootLevel.BOTTOM_FIELD_NUMBER -> root.bottom = codedIS.readInt32()
				MapRootLevel.LEFT_FIELD_NUMBER -> root.left = codedIS.readInt32()
				MapRootLevel.RIGHT_FIELD_NUMBER -> root.right = codedIS.readInt32()
				MapRootLevel.TOP_FIELD_NUMBER -> root.top = codedIS.readInt32()
				MapRootLevel.MAXZOOM_FIELD_NUMBER -> root.maxZoom = codedIS.readInt32()
				MapRootLevel.MINZOOM_FIELD_NUMBER -> root.minZoom = codedIS.readInt32()
				MapRootLevel.BOXES_FIELD_NUMBER -> {
					val length = readInt()
					val filePointer = codedIS.getTotalBytesRead()
					val trees = root.trees
					if (trees != null) {
						val r = MapTree()
						// left, ... already initialized
						r.length = length
						r.filePointer = filePointer
						val oldLimit = codedIS.pushLimitLong(r.length)
						readMapTreeBounds(r, root.left, root.right, root.top, root.bottom)
						trees.add(r)
						codedIS.popLimit(oldLimit)
					}
					codedIS.seek(filePointer + length)
				}
				MapRootLevel.BLOCKS_FIELD_NUMBER -> codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
				else -> skipUnknownField(t)
			}
		}
	}

	/** The box of one r-tree node, each side stored as a delta to the parent's. */
	private fun readMapTreeBounds(tree: MapTree, aleft: Int, aright: Int, atop: Int, abottom: Int) {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				MapDataBox.BOTTOM_FIELD_NUMBER -> tree.bottom = codedIS.readSInt32() + abottom
				MapDataBox.LEFT_FIELD_NUMBER -> tree.left = codedIS.readSInt32() + aleft
				MapDataBox.RIGHT_FIELD_NUMBER -> tree.right = codedIS.readSInt32() + aright
				MapDataBox.TOP_FIELD_NUMBER -> tree.top = codedIS.readSInt32() + atop
				MapDataBox.OCEAN_FIELD_NUMBER -> tree.ocean = codedIS.readBool()
				MapDataBox.SHIFTTOMAPDATA_FIELD_NUMBER ->
					tree.mapDataBlock = readInt() + tree.filePointer
				else -> skipUnknownField(t)
			}
		}
	}

	fun searchMapIndex(req: SearchRequest<BinaryMapDataObject>): MutableList<BinaryMapDataObject> =
		searchMapIndex(req, null)

	/**
	 * Every map object of every section that falls inside the request's box at its zoom, or of
	 * [filterMapIndex] alone. The blocks are read in file order rather than in the order the tree
	 * walk found them, which keeps the reads moving forward through the file.
	 */
	fun searchMapIndex(
		req: SearchRequest<BinaryMapDataObject>, filterMapIndex: MapIndex?
	): MutableList<BinaryMapDataObject> {
		req.numberOfVisitedObjects = 0
		req.numberOfAcceptedObjects = 0
		req.numberOfAcceptedSubtrees = 0
		req.numberOfReadSubtrees = 0
		val foundSubtrees = ArrayList<MapTree>()
		for (mapIndex in mapIndexes) {
			if (filterMapIndex != null && mapIndex !== filterMapIndex) {
				continue
			}
			// lazy initializing rules
			if (mapIndex.encodingRules.isEmpty()) {
				codedIS.seek(mapIndex.filePointer)
				val oldLimit = codedIS.pushLimitLong(mapIndex.length)
				readMapIndex(mapIndex, true)
				codedIS.popLimit(oldLimit)
			}
			for (index in mapIndex.getRoots()) {
				if (index.minZoom <= req.zoom && index.maxZoom >= req.zoom) {
					if (index.right < req.left || index.left > req.right ||
						index.top > req.bottom || index.bottom < req.top) {
						continue
					}
					if (req.hasSearchBoxes() &&
						!req.containsSearchBox(index.left, index.top, index.right, index.bottom)) {
						continue
					}

					// lazy initializing trees
					if (index.trees == null) {
						index.trees = ArrayList()
						codedIS.seek(index.filePointer)
						val oldLimit = codedIS.pushLimitLong(index.length)
						readMapLevel(index)
						codedIS.popLimit(oldLimit)
					}

					for (tree in index.trees!!) {
						if (tree.right < req.left || tree.left > req.right ||
							tree.top > req.bottom || tree.bottom < req.top) {
							continue
						}
						if (req.hasSearchBoxes() &&
							!req.containsSearchBox(tree.left, tree.top, tree.right, tree.bottom)) {
							continue
						}
						codedIS.seek(tree.filePointer)
						val oldLimit = codedIS.pushLimitLong(tree.length)
						searchMapTreeBounds(tree, index, req, foundSubtrees)
						codedIS.popLimit(oldLimit)
					}

					foundSubtrees.sortBy { it.mapDataBlock }
					for (tree in foundSubtrees) {
						if (!req.isCancelled()) {
							codedIS.seek(tree.mapDataBlock)
							val length = codedIS.readRawVarint32()
							val oldLimit = codedIS.pushLimitLong(length.toLong())
							readMapDataBlocks(req, tree, mapIndex)
							codedIS.popLimit(oldLimit)
						}
					}
					foundSubtrees.clear()
				}
			}
		}
		return req.getSearchResults()
	}

	/**
	 * One block of map objects. The names come after the objects, as positions in a string table
	 * at the end of the block, so the objects are held back until it has been read and their
	 * names put in place.
	 */
	internal fun readMapDataBlocks(
		req: SearchRequest<BinaryMapDataObject>, tree: MapTree, root: MapIndex
	) {
		var tempResults: MutableList<BinaryMapDataObject>? = null
		var baseId = 0L
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					tempResults?.forEach { req.publish(it) }
					return
				}
				MapDataBlock.BASEID_FIELD_NUMBER -> baseId = codedIS.readUInt64()
				MapDataBlock.DATAOBJECTS_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					val mapObject = readMapDataObject(tree, req, root)
					if (mapObject != null) {
						mapObject.setId(mapObject.getId() + baseId)
						if (tempResults == null) {
							tempResults = ArrayList()
						}
						tempResults.add(mapObject)
					}
					codedIS.popLimit(oldLimit)
				}
				MapDataBlock.STRINGTABLE_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					val results = tempResults
					if (results != null) {
						val stringTable = readStringTable()
						for (rs in results) {
							val names = rs.objectNames ?: continue
							for (key in names.keys()) {
								val position = names[key]!![0].code
								names.put(key, stringTable[position])
							}
						}
					} else {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					}
					codedIS.popLimit(oldLimit)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * Walks the r-tree under [current], whose box is stored as deltas to [parent]'s, and collects
	 * the nodes that hold objects. A node is dropped as soon as all four sides have been read and
	 * the box turns out to miss the request.
	 */
	internal fun searchMapTreeBounds(
		current: MapTree, parent: MapTree,
		req: SearchRequest<BinaryMapDataObject>, foundSubtrees: MutableList<MapTree>
	) {
		var init = 0
		req.numberOfReadSubtrees++
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			if (init == 0xf) {
				init = 0
				// coordinates are init
				if (current.right < req.left || current.left > req.right ||
					current.top > req.bottom || current.bottom < req.top) {
					return
				}
				if (req.hasSearchBoxes() &&
					!req.containsSearchBox(current.left, current.top, current.right, current.bottom)) {
					return
				}
				req.numberOfAcceptedSubtrees++
			}
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				MapDataBox.BOTTOM_FIELD_NUMBER -> {
					current.bottom = codedIS.readSInt32() + parent.bottom
					init = init or 1
				}
				MapDataBox.LEFT_FIELD_NUMBER -> {
					current.left = codedIS.readSInt32() + parent.left
					init = init or 2
				}
				MapDataBox.RIGHT_FIELD_NUMBER -> {
					current.right = codedIS.readSInt32() + parent.right
					init = init or 4
				}
				MapDataBox.TOP_FIELD_NUMBER -> {
					current.top = codedIS.readSInt32() + parent.top
					init = init or 8
				}
				MapDataBox.SHIFTTOMAPDATA_FIELD_NUMBER -> {
					req.numberOfAcceptedSubtrees++
					current.mapDataBlock = readInt() + current.filePointer
					foundSubtrees.add(current)
				}
				MapDataBox.OCEAN_FIELD_NUMBER -> {
					val ocean = codedIS.readBool()
					current.ocean = ocean
					req.publishOceanTile(ocean)
				}
				MapDataBox.BOXES_FIELD_NUMBER -> {
					// left, ... already initialized
					val child = MapTree()
					child.length = readInt()
					child.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(child.length)
					if (current.ocean != null) {
						child.ocean = current.ocean
					}
					searchMapTreeBounds(child, current, req, foundSubtrees)
					codedIS.popLimit(oldLimit)
					codedIS.seek(child.filePointer + child.length)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * One map object, or null when its points miss the request's box or its types are turned down
	 * by the filter. The points come first in the file, so the box is checked before the rest is
	 * parsed; they are deltas at five bits of precision less than 31, to the corner of [tree]'s
	 * box rounded down the same way.
	 */
	private fun readMapDataObject(
		tree: MapTree, req: SearchRequest<BinaryMapDataObject>, root: MapIndex
	): BinaryMapDataObject? {
		var tag = CodedInputStream.getTagFieldNumber(codedIS.readTag())
		val area = MapData.AREACOORDINATES_FIELD_NUMBER == tag
		if (!area && MapData.COORDINATES_FIELD_NUMBER != tag) {
			throw IllegalArgumentException()
		}
		req.cacheCoordinates.clear()
		var size = codedIS.readRawVarint32()
		var old = codedIS.pushLimitLong(size.toLong())
		var px = tree.left and MASK_TO_READ
		var py = tree.top and MASK_TO_READ
		var contains = false
		var minX = Int.MAX_VALUE
		var maxX = 0
		var minY = Int.MAX_VALUE
		var maxY = 0
		req.numberOfVisitedObjects++
		while (codedIS.getBytesUntilLimit() > 0) {
			val x = (codedIS.readSInt32() shl SHIFT_COORDINATES) + px
			val y = (codedIS.readSInt32() shl SHIFT_COORDINATES) + py
			req.cacheCoordinates.add(x)
			req.cacheCoordinates.add(y)
			px = x
			py = y
			if (!contains && req.left <= x && req.right >= x && req.top <= y && req.bottom >= y) {
				contains = true
			}
			if (!contains) {
				minX = min(minX, x)
				maxX = max(maxX, x)
				minY = min(minY, y)
				maxY = max(maxY, y)
			}
		}
		if (!contains) {
			if (maxX >= req.left && minX <= req.right && minY <= req.bottom && maxY >= req.top) {
				contains = true
			}
		}
		codedIS.popLimit(old)
		if (!contains) {
			codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
			return null
		}

		// read

		var innercoordinates: MutableList<KTIntArrayList>? = null
		var additionalTypes: KTIntArrayList? = null
		var stringNames: KTIntObjectMap<String>? = null
		var stringOrder: KTIntArrayList? = null
		var id = 0L
		var labelX = 0
		var labelY = 0

		var loop = true
		while (loop) {
			val t = codedIS.readTag()
			tag = CodedInputStream.getTagFieldNumber(t)
			when (tag) {
				0 -> loop = false
				MapData.POLYGONINNERCOORDINATES_FIELD_NUMBER -> {
					if (innercoordinates == null) {
						innercoordinates = ArrayList()
					}
					val polygon = KTIntArrayList()
					innercoordinates.add(polygon)
					px = tree.left and MASK_TO_READ
					py = tree.top and MASK_TO_READ
					size = codedIS.readRawVarint32()
					old = codedIS.pushLimitLong(size.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						val x = (codedIS.readSInt32() shl SHIFT_COORDINATES) + px
						val y = (codedIS.readSInt32() shl SHIFT_COORDINATES) + py
						polygon.add(x)
						polygon.add(y)
						px = x
						py = y
					}
					codedIS.popLimit(old)
				}
				MapData.ADDITIONALTYPES_FIELD_NUMBER -> {
					additionalTypes = KTIntArrayList()
					val sizeL = codedIS.readRawVarint32()
					old = codedIS.pushLimitLong(sizeL.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						additionalTypes.add(codedIS.readRawVarint32())
					}
					codedIS.popLimit(old)
				}
				MapData.TYPES_FIELD_NUMBER -> {
					req.cacheTypes.clear()
					val sizeL = codedIS.readRawVarint32()
					old = codedIS.pushLimitLong(sizeL.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						req.cacheTypes.add(codedIS.readRawVarint32())
					}
					codedIS.popLimit(old)
					val accept = req.searchFilter?.accept(req.cacheTypes, root) ?: true
					if (!accept) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						return null
					}
					req.numberOfAcceptedObjects++
				}
				MapData.ID_FIELD_NUMBER -> id = codedIS.readSInt64()
				MapData.STRINGNAMES_FIELD_NUMBER -> {
					stringNames = KTIntObjectMap()
					stringOrder = KTIntArrayList()
					val sizeL = codedIS.readRawVarint32()
					old = codedIS.pushLimitLong(sizeL.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						val stag = codedIS.readRawVarint32()
						val pId = codedIS.readRawVarint32()
						// the name itself is in the block's string table, which comes after the
						// objects; until then the position stands in for it as a single char
						stringNames.put(stag, pId.toChar().toString())
						stringOrder.add(stag)
					}
					codedIS.popLimit(old)
				}
				MapData.LABELCOORDINATES_FIELD_NUMBER -> {
					val sizeL = codedIS.readRawVarint32()
					old = codedIS.pushLimitLong(sizeL.toLong())
					var i = 0
					while (codedIS.getBytesUntilLimit() > 0) {
						if (i == 0) {
							labelX = codedIS.readSInt32()
						} else if (i == 1) {
							labelY = codedIS.readSInt32()
						} else {
							codedIS.readRawVarint32()
						}
						i++
					}
					codedIS.popLimit(old)
				}
				else -> skipUnknownField(t)
			}
		}
		val dataObject = BinaryMapDataObject()
		dataObject.area = area
		dataObject.coordinates = req.cacheCoordinates.toArray()
		dataObject.objectNames = stringNames
		dataObject.namesOrder = stringOrder
		dataObject.polygonInnerCoordinates = if (innercoordinates == null) {
			emptyArray()
		} else {
			Array(innercoordinates.size) { innercoordinates[it].toArray() }
		}
		dataObject.types = req.cacheTypes.toArray()
		dataObject.additionalTypes = additionalTypes?.toArray() ?: IntArray(0)
		dataObject.id = id
		dataObject.mapIndex = root
		dataObject.labelX = labelX
		dataObject.labelY = labelY

		return dataObject
	}

	fun close() {
		handle.close()
	}

	/** Field numbers of `OsmAndStructure` in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndStructure {
		const val VERSION_FIELD_NUMBER = 1
		const val TRANSPORTINDEX_FIELD_NUMBER = 4
		const val MAPINDEX_FIELD_NUMBER = BinaryMapIndexReader.MAPINDEX_FIELD_NUMBER
		const val ADDRESSINDEX_FIELD_NUMBER = 7
		const val POIINDEX_FIELD_NUMBER = 8
		const val ROUTINGINDEX_FIELD_NUMBER = 9
		const val HHROUTINGINDEX_FIELD_NUMBER = BinaryMapIndexReader.HHROUTINGINDEX_FIELD_NUMBER
		const val DATECREATED_FIELD_NUMBER = 18
		const val VERSIONCONFIRM_FIELD_NUMBER = 32
	}

	companion object {
		const val DETAILED_MAP_MIN_ZOOM = 9
		const val TRANSPORT_STOP_ZOOM = 24

		/** Map object points are stored at five bits of precision less than 31. */
		const val SHIFT_COORDINATES = 5
		const val LABEL_ZOOM_ENCODE = 31 - SHIFT_COORDINATES

		/** The field of `OsmAndStructure` a map section occupies; [MapIndex.getFieldNumber]. */
		const val MAPINDEX_FIELD_NUMBER = 6

		/** The field of `OsmAndStructure` an HH routing section occupies; [HHRouteRegion.getFieldNumber]. */
		const val HHROUTINGINDEX_FIELD_NUMBER = 10

		private val MASK_TO_READ = ((1 shl SHIFT_COORDINATES) - 1).inv()
	}

	/** Field numbers of the map section messages in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndMapIndex {
		const val NAME_FIELD_NUMBER = 2
		const val RULES_FIELD_NUMBER = 4
		const val LEVELS_FIELD_NUMBER = 5
	}

	private object MapEncodingRule {
		const val TAG_FIELD_NUMBER = 3
		const val VALUE_FIELD_NUMBER = 5
		const val ID_FIELD_NUMBER = 7
		const val TYPE_FIELD_NUMBER = 10
	}

	private object MapRootLevel {
		const val MAXZOOM_FIELD_NUMBER = 1
		const val MINZOOM_FIELD_NUMBER = 2
		const val LEFT_FIELD_NUMBER = 3
		const val RIGHT_FIELD_NUMBER = 4
		const val TOP_FIELD_NUMBER = 5
		const val BOTTOM_FIELD_NUMBER = 6
		const val BOXES_FIELD_NUMBER = 7
		const val BLOCKS_FIELD_NUMBER = 15
	}

	private object MapDataBox {
		const val LEFT_FIELD_NUMBER = 1
		const val RIGHT_FIELD_NUMBER = 2
		const val TOP_FIELD_NUMBER = 3
		const val BOTTOM_FIELD_NUMBER = 4
		const val SHIFTTOMAPDATA_FIELD_NUMBER = 5
		const val OCEAN_FIELD_NUMBER = 6
		const val BOXES_FIELD_NUMBER = 7
	}

	private object MapDataBlock {
		const val BASEID_FIELD_NUMBER = 10
		const val DATAOBJECTS_FIELD_NUMBER = 12
		const val STRINGTABLE_FIELD_NUMBER = 15
	}

	private object MapData {
		const val COORDINATES_FIELD_NUMBER = 1
		const val AREACOORDINATES_FIELD_NUMBER = 2
		const val POLYGONINNERCOORDINATES_FIELD_NUMBER = 4
		const val ADDITIONALTYPES_FIELD_NUMBER = 6
		const val TYPES_FIELD_NUMBER = 7
		const val LABELCOORDINATES_FIELD_NUMBER = 8
		const val STRINGNAMES_FIELD_NUMBER = 10
		const val ID_FIELD_NUMBER = 12
	}

	private object StringTable {
		const val S_FIELD_NUMBER = 1
	}
}
