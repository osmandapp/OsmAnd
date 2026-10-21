package net.osmand.shared.binary

import net.osmand.shared.IndexConstants
import net.osmand.shared.data.Amenity
import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Street
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.io.KFile
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.routing.HHRouteRegionPointsCtx
import net.osmand.shared.routing.HHRoutingContext
import net.osmand.shared.routing.NetworkDBPoint
import net.osmand.shared.routing.RouteDataObject
import net.osmand.shared.routing.RouteRegion
import net.osmand.shared.routing.RouteSubregion
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import okio.FileHandle
import okio.IOException
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.math.max
import kotlin.math.min

/**
 * An open obf file, with the index sections it carries located and the routing ones read.
 *
 * A copy of `BinaryMapIndexReader` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS, so a route can be calculated and a map read there without the C++ core. It
 * carries the structure walk in the constructor, the routing and HH sections, the map, poi and
 * address sections, `readInt` and the string table, and skips the transport section, which the
 * searches copied so far do not read. The method names are java's, so the two can be compared side
 * by side.
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
	private val poiAdapter: BinaryMapPoiReaderAdapter
	private val addressAdapter: BinaryMapAddressReaderAdapter

	private var version: Int = 0
	private var dateCreated: Long = 0
	private var initCorrectly = false

	private val mapIndexes = ArrayList<MapIndex>()
	private val poiIndexes = ArrayList<PoiRegion>()
	private val routingIndexes = ArrayList<RouteRegion>()
	private val hhIndexes = ArrayList<HHRouteRegion>()
	private val addressIndexes = ArrayList<AddressRegion>()
	private var basemap = false

	constructor(file: KFile) {
		this.file = file
		this.handle = FileSystem.SYSTEM.openReadOnly(file.path().toPath())
		this.codedIS = CodedInputStream(handle)
		this.routeAdapter = BinaryMapRouteReaderAdapter(this)
		this.hhAdapter = BinaryHHRouteReaderAdapter(this)
		this.poiAdapter = BinaryMapPoiReaderAdapter(this)
		this.addressAdapter = BinaryMapAddressReaderAdapter(this)
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
				OsmAndStructure.POIINDEX_FIELD_NUMBER -> {
					val poiInd = PoiRegion()
					poiInd.length = readInt()
					poiInd.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(poiInd.length)
					poiAdapter.readPoiIndex(poiInd, false)
					codedIS.popLimit(oldLimit)
					codedIS.seek(poiInd.filePointer + poiInd.length)
					poiIndexes.add(poiInd)
				}
				OsmAndStructure.ADDRESSINDEX_FIELD_NUMBER -> {
					val region = AddressRegion()
					region.length = readInt()
					region.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(region.length)
					addressAdapter.readAddressIndex(region)
					if (region.name != null) {
						addressIndexes.add(region)
					}
					codedIS.popLimit(oldLimit)
					codedIS.seek(region.filePointer + region.length)
				}
				OsmAndStructure.TRANSPORTINDEX_FIELD_NUMBER -> {
					val length = readInt()
					codedIS.seek(codedIS.getTotalBytesRead() + length)
				}
				OsmAndStructure.VERSIONCONFIRM_FIELD_NUMBER -> {
					val cversion = codedIS.readUInt32()
					calculateCenterPointForRegions()
					initCorrectly = cversion == version
					reachedEnd = true
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * The centre each address section reports as the centre of its region: the hub graph's box if
	 * the file has one, else the coarsest map level or the routing box of a section of the same name.
	 */
	private fun calculateCenterPointForRegions() {
		for (reg in addressIndexes) {
			for (h in hhIndexes) {
				val top = h.top
				if (top != null) { // name null Algorithms.objectEquals(reg.name, h.name)
					val qr = top.getLatLonBox()
					reg.calculatedCenter = KLatLon(qr.centerY(), qr.centerX())
					break
				}
			}
			if (reg.calculatedCenter == null) {
				for (map in mapIndexes) {
					if (reg.name == map.name) {
						if (map.getRoots().size > 0) {
							reg.calculatedCenter = map.getCenterLatLon()
							break
						}
					}
				}
			}
			if (reg.calculatedCenter == null) {
				for (map in routingIndexes) {
					if (reg.name == map.name) {
						reg.calculatedCenter = KLatLon(
							map.getTopLatitude() / 2 + map.getBottomLatitude() / 2,
							map.getLeftLongitude() / 2 + map.getRightLongitude() / 2
						)
						break
					}
				}
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

	fun getPoiIndexes(): List<PoiRegion> = poiIndexes

	fun containsPoiData(): Boolean = poiIndexes.size > 0

	fun containsPoiData(left31x: Int, top31y: Int, right31x: Int, bottom31y: Int): Boolean {
		for (index in poiIndexes) {
			if (right31x >= index.left31 && left31x <= index.right31 &&
				index.top31 <= bottom31y && index.bottom31 >= top31y) {
				return true
			}
		}
		return false
	}

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
		val coordinates = req.cacheCoordinates
		codedIS.readSInt32s(coordinates)
		if (coordinates.size % 2 == 1) {
			// the last y of an odd count is read past the end of the field, as java reads it
			coordinates.add(codedIS.readSInt32())
		}
		val data = coordinates.data
		val left = req.left
		val right = req.right
		val top = req.top
		val bottom = req.bottom
		var i = 0
		val n = coordinates.size
		while (i < n) {
			val x = (data[i] shl SHIFT_COORDINATES) + px
			val y = (data[i + 1] shl SHIFT_COORDINATES) + py
			data[i] = x
			data[i + 1] = y
			px = x
			py = y
			if (!contains && left <= x && right >= x && top <= y && bottom >= y) {
				contains = true
			}
			if (!contains) {
				minX = min(minX, x)
				maxX = max(maxX, x)
				minY = min(minY, y)
				maxY = max(maxY, y)
			}
			i += 2
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
					codedIS.readSInt32s(polygon)
					if (polygon.size % 2 == 1) {
						// the last y of an odd count is read past the end of the field, as java reads it
						polygon.add(codedIS.readSInt32())
					}
					val points = polygon.data
					var k = 0
					while (k < polygon.size) {
						val x = (points[k] shl SHIFT_COORDINATES) + px
						val y = (points[k + 1] shl SHIFT_COORDINATES) + py
						points[k] = x
						points[k + 1] = y
						px = x
						py = y
						k += 2
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

	/**
	 * Poi section
	 */

	/** Reads the decoding tables of [poiIndex], unless they are read already. */
	fun initCategories(poiIndex: PoiRegion) {
		poiAdapter.initCategories(poiIndex)
	}

	fun initCategories() {
		for (poiIndex in poiIndexes) {
			poiAdapter.initCategories(poiIndex)
		}
	}

	fun searchPoi(req: SearchRequest<Amenity>): MutableList<Amenity> = searchPoi(req, null)

	/** Every amenity of every poi section that the request's box and filters accept. */
	fun searchPoi(req: SearchRequest<Amenity>, onlyIndex: PoiRegion?): MutableList<Amenity> {
		req.numberOfVisitedObjects = 0
		req.numberOfAcceptedObjects = 0
		req.numberOfAcceptedSubtrees = 0
		req.numberOfReadSubtrees = 0
		val lst = if (onlyIndex == null) poiIndexes else listOf(onlyIndex)
		for (poiIndex in lst) {
			poiAdapter.initCategories(poiIndex)
			codedIS.seek(poiIndex.filePointer)
			val old = codedIS.pushLimitLong(poiIndex.length)
			poiAdapter.searchPoiIndex(req.left, req.right, req.top, req.bottom, req, poiIndex)
			codedIS.popLimit(old)
		}
		return req.getSearchResults()
	}

	/**
	 * Reads the tag groups of the tiles of [tileIds] that this section has not read yet, so that
	 * the amenities already in hand can be told which settlements they are in. False when there is
	 * nothing left to read.
	 */
	fun readAmenityBboxes(pr: PoiRegion, tileIds: KTLongHashSet): Boolean {
		poiAdapter.initCategories(pr)
		val missing = pr.checkMissingTagGroups(tileIds)
		if (missing.size() == 0) {
			return false
		}
		val sr = SearchRequest<Amenity>()
		codedIS.seek(pr.filePointer)
		val oldLim = codedIS.pushLimitLong(pr.length)
		pr.updReadTagGroups(missing) // update before as tileIds is modified
		poiAdapter.readPoiBboxes(pr, sr, missing)
		codedIS.popLimit(oldLim)

		return true
	}

	/** One block of amenities at a known offset, or one amenity of it when [index] is not -1. */
	fun readAmenityBlock(pr: PoiRegion, offset: Long, index: Int): MutableList<Amenity> {
		poiAdapter.initCategories(pr)
		codedIS.seek(pr.filePointer + offset)
		val len = readInt()
		val oldLim = codedIS.pushLimitLong(len)
		val sr = SearchRequest<Amenity>()
		poiAdapter.readPoiData(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, sr, pr, index, null, 0)
		codedIS.popLimit(oldLim)
		return sr.getSearchResults()
	}

	/** The amenities of every poi section whose name matches the request's query. */
	fun searchPoiByName(req: SearchRequest<Amenity>): MutableList<Amenity> {
		val nameQuery = req.nameQuery
		if (nameQuery == null || nameQuery.isEmpty()) {
			throw IllegalArgumentException()
		}
		for (poiIndex in poiIndexes) {
			poiAdapter.initCategories(poiIndex)
			codedIS.seek(poiIndex.filePointer)
			val old = codedIS.pushLimitLong(poiIndex.length)
			poiAdapter.searchPoiByName(poiIndex, req)
			codedIS.popLimit(old)
		}
		return req.getSearchResults()
	}

	/**
	 * The categories whose name starts with [query], and for the rest the subcategories that do.
	 * A category that matches itself is put in with a null list, meaning all of its subcategories.
	 */
	fun searchPoiCategoriesByName(
		query: String, map: MutableMap<PoiCategory, MutableList<String>?>
	): MutableMap<PoiCategory, MutableList<String>?> {
		if (query.isEmpty()) {
			throw IllegalArgumentException()
		}
		for (poiIndex in poiIndexes) {
			poiAdapter.initCategories(poiIndex)
			for (i in poiIndex.categories.indices) {
				val cat = poiIndex.categories[i]
				val catType = poiIndex.categoriesType[i] ?: continue
				if (KCollatorStringMatcher.cmatches(cat, query, KStringMatcherMode.CHECK_STARTS_FROM_SPACE)) {
					map[catType] = null
				} else {
					val subcats = poiIndex.subcategories[i]
					for (subcat in subcats) {
						if (KCollatorStringMatcher.cmatches(
								subcat, query, KStringMatcherMode.CHECK_STARTS_FROM_SPACE
							)
						) {
							if (!map.containsKey(catType)) {
								map[catType] = ArrayList()
							}
							map[catType]?.add(subcat)
						}
					}
				}
			}
		}
		return map
	}

	/** The additional attributes whose name starts with [query], letter for letter. */
	fun searchPoiSubTypesByPrefix(query: String): MutableList<PoiSubType> {
		if (query.isEmpty()) {
			throw IllegalArgumentException()
		}
		val list = ArrayList<PoiSubType>()
		for (poiIndex in poiIndexes) {
			poiAdapter.initCategories(poiIndex)
			for (subType in poiIndex.subTypes) {
				if (subType.name?.startsWith(query) == true) {
					list.add(subType)
				}
			}
		}
		return list
	}

	/** The attributes the files were written with a top index for, which can be filtered on. */
	fun getTopIndexSubTypes(): MutableList<PoiSubType> {
		val list = ArrayList<PoiSubType>()
		for (poiIndex in poiIndexes) {
			poiAdapter.initCategories(poiIndex)
			list.addAll(poiIndex.topIndexSubTypes)
		}
		return list
	}

	/**
	 * Address section
	 */

	fun containsAddressData(): Boolean = addressIndexes.size > 0

	fun hasRegions(): Boolean = addressIndexes.isNotEmpty()

	fun getAddressIndexes(): List<AddressRegion> = addressIndexes

	fun getRegionNames(): MutableList<String> {
		val names = ArrayList<String>()
		for (r in addressIndexes) {
			names.add(r.name!!)
		}
		return names
	}

	/** The country part of the first region name, "Netherlands" of "Netherlands_noord-holland". */
	fun getCountryName(): String {
		val rg = getRegionNames()
		if (rg.size > 0) {
			return rg[0].split("_")[0]
		}
		return ""
	}

	/**
	 * The name of the region the file covers, readable: from the address section, or from the file
	 * name when there is none, with the version, the date of a live update and the country prefix
	 * taken off, "Noord-holland europe" of "Netherlands_noord-holland_europe_2.obf".
	 */
	fun getRegionName(): String {
		val rg = getRegionNames()
		if (rg.size == 0) {
			rg.add(file.name())
		}
		var ls = rg[0]
		if (ls.lastIndexOf('_') != -1) {
			if (OSM_DIFF_FILE_NAME.matches(ls)) {
				val m = OSM_DIFF_DATE_ENDING.find(ls)
				if (m != null) {
					ls = ls.substring(0, m.range.first)
					return if (ls.lastIndexOf('_') != -1) {
						ls.substring(0, ls.lastIndexOf('_')).replace('_', ' ')
					} else {
						ls
					}
				}
			} else {
				if (ls.contains(".")) {
					ls = ls.substring(0, ls.indexOf("."))
				}
				if (ls.endsWith("_" + IndexConstants.BINARY_MAP_VERSION)) {
					ls = ls.substring(0, ls.length - ("_" + IndexConstants.BINARY_MAP_VERSION).length)
				}
				if (ls.lastIndexOf('_') != -1) {
					ls = ls.substring(0, ls.lastIndexOf('_')).replace('_', ' ')
				}
				return ls
			}
		}
		return ls
	}

	fun getRegionCenter(): KLatLon? {
		for (r in addressIndexes) {
			val center = r.calculatedCenter
			if (center != null) {
				return center
			}
		}
		return null
	}

	fun getCities(resultMatcher: SearchRequest<City>?, type: CityBlocks?): MutableList<City> =
		getCities(resultMatcher, null, null, type, null)

	fun getCities(resultMatcher: SearchRequest<City>?, type: CityBlocks?, onlyRegion: AddressRegion?): MutableList<City> =
		getCities(resultMatcher, null, null, type, onlyRegion)

	/** The settlements of the blocks of [type], matched by [matcher] against every name they carry. */
	fun getCities(
		resultMatcher: SearchRequest<City>?, matcher: KStringMatcher?, lang: String?, type: CityBlocks?,
		onlyRegion: AddressRegion?
	): MutableList<City> {
		val cities = ArrayList<City>()
		val inds = if (onlyRegion == null) addressIndexes else listOf(onlyRegion)
		for (r in inds) {
			for (block in r.cities) {
				if (type != null && block.type == type.index) {
					codedIS.seek(block.filePointer)
					val old = codedIS.pushLimitLong(block.length)
					addressAdapter.readCities(cities, resultMatcher, matcher, r.attributeTagsTable)
					codedIS.popLimit(old)
				}
			}
		}
		return cities
	}

	fun preloadStreets(c: City, resultMatcher: SearchRequest<Street>?): Int = preloadStreets(c, resultMatcher, false)

	/** Reads the streets of [c] into it, with their houses and crossings when [loadBuildings]. */
	fun preloadStreets(c: City, resultMatcher: SearchRequest<Street>?, loadBuildings: Boolean): Int {
		val reg: AddressRegion
		try {
			reg = checkAddressIndex(c.getFileOffset())
		} catch (e: IllegalArgumentException) {
			throw IOException(e.message + " while reading " + c + " (id: " + c.getId() + ")")
		}
		codedIS.seek(c.getFileOffset())
		val size = codedIS.readRawVarint32()
		val old = codedIS.pushLimitLong(size.toLong())
		addressAdapter.readCityStreets(resultMatcher, c, loadBuildings, reg.attributeTagsTable)
		codedIS.popLimit(old)
		return size
	}

	private fun checkAddressIndex(offset: Long): AddressRegion {
		for (r in addressIndexes) {
			if (offset >= r.filePointer && offset <= (r.length + r.filePointer)) {
				return r
			}
		}
		throw IllegalArgumentException("Illegal offset $offset")
	}

	/** Reads the houses and crossings of [s] into it; a postcode's street keeps only its own houses. */
	fun preloadBuildings(s: Street, resultMatcher: SearchRequest<Building>?) {
		val reg = checkAddressIndex(s.getFileOffset())
		codedIS.seek(s.getFileOffset())
		val size = codedIS.readRawVarint32()
		val old = codedIS.pushLimitLong(size.toLong())
		val city = s.getCity()
		addressAdapter.readStreet(
			s, resultMatcher, true, 0, 0, if (city != null && city.isPostcode()) city.getName() else null,
			reg.attributeTagsTable
		)
		codedIS.popLimit(old)
	}

	fun searchAddressDataByName(req: SearchRequest<MapObject>): MutableList<MapObject> =
		searchAddressDataByName(req, null)

	/** The settlements and streets of every address section whose name matches the request's query. */
	fun searchAddressDataByName(req: SearchRequest<MapObject>, typeFilter: List<CityBlocks>?): MutableList<MapObject> {
		for (reg in addressIndexes) {
			if (reg.indexNameOffset != -1L) {
				codedIS.seek(reg.indexNameOffset)
				val len = readInt()
				val old = codedIS.pushLimitLong(len)
				addressAdapter.searchAddressDataByName(reg, req, typeFilter)
				codedIS.popLimit(old)
			}
			if (req.isCancelled()) {
				break
			}
		}
		return req.getSearchResults()
	}

	/**
	 * Where in the name index each of [queries] could sit. The index is a tree of string tables
	 * whose keys build up a name letter by letter, so a query word is looked for down every branch
	 * whose key it still shares a start with, in either direction: "bak" reaches "bakery", and
	 * "bakery" reaches the branch keyed "bak".
	 */
	internal fun readIndexedStringTablePrefixes(queries: List<String>): List<List<QueryToken.Prefix>> {
		val prefixesByQuery = ArrayList<MutableMap<String, Int>>(queries.size)
		for (i in queries.indices) {
			prefixesByQuery.add(LinkedHashMap())
		}
		readIndexedStringTablePrefixes(queries.map { KCollatorStringMatcher.PreparedName(it) }, "", prefixesByQuery)

		val result = ArrayList<List<QueryToken.Prefix>>(queries.size)
		for (prefixes in prefixesByQuery) {
			val tokenPrefixes = ArrayList<QueryToken.Prefix>(prefixes.size)
			for (entry in prefixes.entries) {
				tokenPrefixes.add(QueryToken.Prefix(entry.key, entry.value))
			}
			result.add(tokenPrefixes)
		}
		return result
	}

	/**
	 * The walk behind [readIndexedStringTablePrefixes]. The queries come prepared, and each key is
	 * prepared once for all of them: a table of a regional map holds tens of thousands of keys, and
	 * reducing both strings on every comparison was most of the time a name search took on
	 * Kotlin/Native.
	 */
	private fun readIndexedStringTablePrefixes(
		queries: List<KCollatorStringMatcher.PreparedName?>, prefix: String,
		prefixesByQuery: List<MutableMap<String, Int>>
	) {
		val matched = BooleanArray(queries.size)
		val matchedSubtables = BooleanArray(queries.size)
		var key: String? = null
		var shouldWeReadSubtable = false
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				IndexedStringTable.KEY_FIELD_NUMBER -> {
					var read = codedIS.readString()
					if (prefix.isNotEmpty()) {
						read = prefix + read
					}
					key = read
					shouldWeReadSubtable = matchIndexedStringTablePrefix(queries, read, matched, matchedSubtables)
				}
				IndexedStringTable.VAL_FIELD_NUMBER -> {
					val value = readInt().toInt() // FIXME for 64 bit support
					for (i in queries.indices) {
						if (matched[i] && key != null) {
							val tokenPrefixes = prefixesByQuery[i]
							val previousOffset = tokenPrefixes[key]
							if (previousOffset == null) {
								tokenPrefixes[key] = value
							} else if (previousOffset != value) {
								throw IllegalStateException(
									"Indexed string table contains multiple offsets for key: $key"
								)
							}
						}
					}
				}
				IndexedStringTable.SUBTABLES_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLim = codedIS.pushLimitLong(len.toLong())
					if (shouldWeReadSubtable && key != null) {
						val subqueries = ArrayList<KCollatorStringMatcher.PreparedName?>(queries)
						for (i in queries.indices) {
							if (!matchedSubtables[i]) {
								subqueries[i] = null
							}
						}
						readIndexedStringTablePrefixes(subqueries, key, prefixesByQuery)
					} else {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					}
					codedIS.popLimit(oldLim)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	private fun matchIndexedStringTablePrefix(
		queries: List<KCollatorStringMatcher.PreparedName?>, key: String, matched: BooleanArray,
		matchedSubtables: BooleanArray
	): Boolean {
		var shouldWeReadSubtable = false
		var preparedKey: KCollatorStringMatcher.PreparedName? = null
		for (i in queries.indices) {
			val query = queries[i]
			matched[i] = false
			matchedSubtables[i] = false
			if (query == null) {
				continue
			}
			val keyName = preparedKey ?: KCollatorStringMatcher.PreparedName(key).also { preparedKey = it }
			val keyStartsWithQuery = KCollatorStringMatcher.cmatchesPrepared(
				keyName, query.key, KStringMatcherMode.CHECK_ONLY_STARTS_WITH
			)
			val queryStartsWithKey = KCollatorStringMatcher.cmatchesPrepared(
				query, keyName.key, KStringMatcherMode.CHECK_ONLY_STARTS_WITH
			)
			val potentialBranchMatch = keyStartsWithQuery || queryStartsWithKey
			matched[i] = potentialBranchMatch
			matchedSubtables[i] = potentialBranchMatch
			shouldWeReadSubtable = shouldWeReadSubtable || potentialBranchMatch
		}
		return shouldWeReadSubtable
	}

	fun close() {
		handle.close()
	}

	/** Field numbers of `OsmAndStructure` in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndStructure {
		const val VERSION_FIELD_NUMBER = 1
		const val TRANSPORTINDEX_FIELD_NUMBER = 4
		const val MAPINDEX_FIELD_NUMBER = BinaryMapIndexReader.MAPINDEX_FIELD_NUMBER
		const val ADDRESSINDEX_FIELD_NUMBER = BinaryMapIndexReader.ADDRESSINDEX_FIELD_NUMBER
		const val POIINDEX_FIELD_NUMBER = BinaryMapIndexReader.POIINDEX_FIELD_NUMBER
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

		/** The field of `OsmAndStructure` an address section occupies; [AddressRegion.getFieldNumber]. */
		const val ADDRESSINDEX_FIELD_NUMBER = 7

		/** The field of `OsmAndStructure` a poi section occupies; [PoiRegion.getFieldNumber]. */
		const val POIINDEX_FIELD_NUMBER = 8

		/** The field of `OsmAndStructure` an HH routing section occupies; [HHRouteRegion.getFieldNumber]. */
		const val HHROUTINGINDEX_FIELD_NUMBER = 10

		private val MASK_TO_READ = ((1 shl SHIFT_COORDINATES) - 1).inv()

		private val OSM_DIFF_FILE_NAME = Regex("([a-zA-Z-]+_)+([0-9]+_){2}[0-9]+\\.obf")
		private val OSM_DIFF_DATE_ENDING = Regex("_([0-9]+_){2}[0-9]+\\.obf")
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

	private object IndexedStringTable {
		const val KEY_FIELD_NUMBER = 3
		const val VAL_FIELD_NUMBER = 4
		const val SUBTABLES_FIELD_NUMBER = 5
	}

	private object StringTable {
		const val S_FIELD_NUMBER = 1
	}
}
