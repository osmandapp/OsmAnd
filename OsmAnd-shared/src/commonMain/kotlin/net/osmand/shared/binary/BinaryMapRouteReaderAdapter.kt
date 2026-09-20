package net.osmand.shared.binary

import net.osmand.shared.routing.RouteDataObject
import net.osmand.shared.routing.RouteDataObject.RestrictionInfo
import net.osmand.shared.routing.RouteRegion
import net.osmand.shared.routing.RouteSubregion
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.collections.KTLongArrayList
import net.osmand.shared.util.collections.KTLongObjectMap
import net.osmand.shared.util.internString

/**
 * Reads the routing section of an obf file: the encoding rules of a [RouteRegion], the r-tree of
 * boxes over its roads, and the roads of a box.
 *
 * A copy of `BinaryMapRouteReaderAdapter` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS, so a route can be calculated there without the C++ core. The
 * three data classes the java one nests - `RouteTypeRule`, `RouteRegion`, `RouteSubregion` - were
 * copied earlier and live in `net.osmand.shared.routing`. The method names are java's, so the two
 * can be compared side by side.
 */
class BinaryMapRouteReaderAdapter internal constructor(private val map: BinaryMapIndexReader) {

	private val codedIS: CodedInputStream = map.codedIS

	// Scratch space for reading a block: java allocates these per road and per block, which the
	// jvm's young generation makes free and Kotlin/Native's collector does not. The reader is used
	// from one thread at a time, as the java one is.
	private val pointsXScratch = KTIntArrayList()
	private val pointsYScratch = KTIntArrayList()
	private val typesScratch = KTIntArrayList()
	private val pointTypesScratch = KTIntArrayList()
	private val idTables = KTLongArrayList()
	private val restrictionMap = KTLongObjectMap<RestrictionInfo>()

	private fun skipUnknownField(t: Int) {
		map.skipUnknownField(t)
	}

	private fun readInt(): Long = map.readInt()

	internal fun readRouteIndex(region: RouteRegion) {
		var routeEncodingRule = 1
		var routeEncodingRulesSize = 0L
		while (true) {
			val t = codedIS.readTag()
			when (val tag = CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					region.completeRouteEncodingRules()
					return
				}
				OsmAndRoutingIndex.NAME_FIELD_NUMBER -> region.name = codedIS.readString()
				OsmAndRoutingIndex.RULES_FIELD_NUMBER -> {
					val len = codedIS.readInt32()
					if (routeEncodingRulesSize == 0L) {
						routeEncodingRulesSize = codedIS.getTotalBytesRead()
					}
					val oldLimit = codedIS.pushLimitLong(len.toLong())
					readRouteEncodingRule(region, routeEncodingRule++)
					codedIS.popLimit(oldLimit)
					region.routeEncodingRulesBytes = (codedIS.getTotalBytesRead() - routeEncodingRulesSize).toInt()
				}
				OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER, OsmAndRoutingIndex.BASEMAPBOXES_FIELD_NUMBER -> {
					val subregion = RouteSubregion(region)
					subregion.length = readInt()
					subregion.filePointer = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(subregion.length)
					readRouteTree(subregion, null, 0, true)
					if (tag == OsmAndRoutingIndex.ROOTBOXES_FIELD_NUMBER) {
						var exist = false
						for (s in region.subregions) {
							if (s.filePointer == subregion.filePointer) {
								exist = true
								break
							}
						}
						if (!exist) {
							region.subregions.add(subregion)
						}
					} else {
						var exist = false
						for (s in region.basesubregions) {
							if (s.filePointer == subregion.filePointer) {
								exist = true
								break
							}
						}
						if (!exist) {
							region.basesubregions.add(subregion)
						}
					}
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					codedIS.popLimit(oldLimit)
				}
				OsmAndRoutingIndex.BLOCKS_FIELD_NUMBER -> {
					// Finish reading file!
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
				}
				else -> skipUnknownField(t)
			}
		}
	}

	private fun readRouteDataObject(reg: RouteRegion, pleftx: Int, ptopy: Int): RouteDataObject {
		val o = RouteDataObject(reg)
		val pointsX = pointsXScratch
		val pointsY = pointsYScratch
		val types = typesScratch
		pointsX.clear()
		pointsY.clear()
		types.clear()
		var globalpointTypes: ArrayList<IntArray?>? = null
		var globalpointNames: ArrayList<KTIntArrayList?>? = null
		while (true) {
			val ts = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(ts)) {
				0 -> {
					o.pointsX = pointsX.toArray()
					o.pointsY = pointsY.toArray()
					o.types = types.toArray()
					val pointTypesRead = globalpointTypes
					if (pointTypesRead != null && pointTypesRead.size > 0) {
						o.pointTypes = Array(pointTypesRead.size) { k -> pointTypesRead[k] }
					}
					val pointNamesRead = globalpointNames
					if (pointNamesRead != null && pointNamesRead.size > 0) {
						val pointNames = arrayOfNulls<Array<String>>(pointNamesRead.size)
						val pointNameTypes = arrayOfNulls<IntArray>(pointNamesRead.size)
						for (k in pointNames.indices) {
							val l = pointNamesRead[k]
							if (l != null) {
								val nameTypes = IntArray(l.size / 2)
								val names = Array(l.size / 2) { "" }
								var ik = 0
								while (ik < l.size) {
									nameTypes[ik / 2] = l[ik]
									// the string table index, resolved to the string in readRouteTreeData
									names[ik / 2] = l[ik + 1].toChar().toString()
									ik += 2
								}
								pointNameTypes[k] = nameTypes
								pointNames[k] = names
							}
						}
						o.pointNames = pointNames
						o.pointNameTypes = pointNameTypes
					}
					return o
				}
				RouteData.TYPES_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(len.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						types.add(codedIS.readRawVarint32())
					}
					codedIS.popLimit(oldLimit)
				}
				RouteData.STRINGNAMES_FIELD_NUMBER -> {
					val names = KTIntObjectMap<String>()
					o.names = names
					val sizeL = codedIS.readRawVarint32()
					val old = codedIS.pushLimitLong(sizeL.toLong())
					val list = KTIntArrayList()
					while (codedIS.getBytesUntilLimit() > 0) {
						val stag = codedIS.readRawVarint32()
						val pId = codedIS.readRawVarint32()
						names.put(stag, pId.toChar().toString())
						list.add(stag)
					}
					o.nameIds = list.toArray()
					codedIS.popLimit(old)
				}
				RouteData.POINTS_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(len.toLong())
					var px = pleftx shr SHIFT_COORDINATES
					var py = ptopy shr SHIFT_COORDINATES
					while (codedIS.getBytesUntilLimit() > 0) {
						val x = codedIS.readSInt32() + px
						val y = codedIS.readSInt32() + py
						pointsX.add(x shl SHIFT_COORDINATES)
						pointsY.add(y shl SHIFT_COORDINATES)
						px = x
						py = y
					}
					codedIS.popLimit(oldLimit)
				}
				RouteData.POINTNAMES_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(len.toLong())
					val names = globalpointNames ?: ArrayList<KTIntArrayList?>().also { globalpointNames = it }
					while (codedIS.getBytesUntilLimit() > 0) {
						val pointInd = codedIS.readRawVarint32()
						val pointNameType = codedIS.readRawVarint32()
						val nameInd = codedIS.readRawVarint32()
						while (pointInd >= names.size) {
							names.add(null)
						}
						if (names[pointInd] == null) {
							names[pointInd] = KTIntArrayList()
						}
						names[pointInd]!!.add(pointNameType)
						names[pointInd]!!.add(nameInd)
					}
					codedIS.popLimit(oldLimit)
				}
				RouteData.POINTTYPES_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(len.toLong())
					val pointTypesRead = globalpointTypes ?: ArrayList<IntArray?>().also { globalpointTypes = it }
					while (codedIS.getBytesUntilLimit() > 0) {
						val pointInd = codedIS.readRawVarint32()
						val pointTypes = pointTypesScratch
						pointTypes.clear()
						val lens = codedIS.readRawVarint32()
						val oldLimits = codedIS.pushLimitLong(lens.toLong())
						while (codedIS.getBytesUntilLimit() > 0) {
							pointTypes.add(codedIS.readRawVarint32())
						}
						codedIS.popLimit(oldLimits)
						while (pointInd >= pointTypesRead.size) {
							pointTypesRead.add(null)
						}
						pointTypesRead[pointInd] = pointTypes.toArray()
					}
					codedIS.popLimit(oldLimit)
				}
				RouteData.ROUTEID_FIELD_NUMBER -> o.id = codedIS.readInt32().toLong()
				else -> skipUnknownField(ts)
			}
		}
	}

	private fun readRouteTreeData(
		routeTree: RouteSubregion, idTables: KTLongArrayList,
		restrictions: KTLongObjectMap<RestrictionInfo>
	) {
		val dataObjects = ArrayList<RouteDataObject?>()
		routeTree.dataObjects = dataObjects
		idTables.clear()
		restrictions.clear()
		var stringTable: List<String>? = null
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					val it = restrictions.iterator()
					while (it.hasNext()) {
						it.advance()
						val from = it.key().toInt()
						val fromr = dataObjects[from]!!
						val count = it.value().length()
						fromr.restrictions = LongArray(count)
						var value: RestrictionInfo? = it.value()
						for (k in 0 until count) {
							if (value != null) {
								var via = 0L
								if (value.viaWay != 0L) {
									via = idTables[value.viaWay.toInt()]
								}
								fromr.setRestriction(k, idTables[value.toWay.toInt()], value.type, via)
							}
							value = value?.next
						}
					}
					for (o in dataObjects) {
						if (o != null) {
							if (o.id < idTables.size) {
								o.id = idTables[o.id.toInt()]
							}
							val names = o.names
							if (names != null && stringTable != null) {
								val keys = names.keys()
								for (j in keys.indices) {
									names.put(keys[j], stringTable[names[keys[j]]!![0].code])
								}
							}
							val pointNames = o.pointNames
							if (pointNames != null && stringTable != null) {
								for (ar in pointNames) {
									if (ar != null) {
										for (j in ar.indices) {
											ar[j] = stringTable[ar[j][0].code]
										}
									}
								}
							}
						}
					}
					return
				}
				RouteDataBlock.DATAOBJECTS_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					val obj = readRouteDataObject(routeTree.routeReg, routeTree.left, routeTree.top)
					while (obj.id >= dataObjects.size) {
						dataObjects.add(null)
					}
					dataObjects[obj.id.toInt()] = obj
					codedIS.popLimit(oldLimit)
				}
				RouteDataBlock.IDTABLE_FIELD_NUMBER -> {
					var routeId = 0L
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					idLoop@ while (true) {
						val ts = codedIS.readTag()
						when (CodedInputStream.getTagFieldNumber(ts)) {
							0 -> break@idLoop
							IdTable.ROUTEID_FIELD_NUMBER -> {
								routeId += codedIS.readSInt64()
								idTables.add(routeId)
							}
							else -> skipUnknownField(ts)
						}
					}
					codedIS.popLimit(oldLimit)
				}
				RouteDataBlock.RESTRICTIONS_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					val ri = RestrictionInfo()
					var from = 0L
					idLoop@ while (true) {
						val ts = codedIS.readTag()
						when (CodedInputStream.getTagFieldNumber(ts)) {
							0 -> break@idLoop
							RestrictionData.FROM_FIELD_NUMBER -> from = codedIS.readInt32().toLong()
							RestrictionData.TO_FIELD_NUMBER -> ri.toWay = codedIS.readInt32().toLong()
							RestrictionData.TYPE_FIELD_NUMBER -> ri.type = codedIS.readInt32()
							RestrictionData.VIA_FIELD_NUMBER -> ri.viaWay = codedIS.readInt32().toLong()
							else -> skipUnknownField(ts)
						}
					}
					var prev = restrictions[from]
					if (prev != null) {
						while (prev!!.next != null) {
							prev = prev.next
						}
						prev.next = ri
					} else {
						restrictions.put(from, ri)
					}
					codedIS.popLimit(oldLimit)
				}
				RouteDataBlock.STRINGTABLE_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					stringTable = map.readStringTable()
					codedIS.popLimit(oldLimit)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	private fun readRouteEncodingRule(index: RouteRegion, ruleId: Int) {
		var id = ruleId
		var tags: String? = null
		var value: String? = null
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					index.initRouteEncodingRule(id, tags!!, value)
					return
				}
				RouteEncodingRule.VALUE_FIELD_NUMBER -> value = internString(codedIS.readString())
				RouteEncodingRule.TAG_FIELD_NUMBER -> tags = internString(codedIS.readString())
				RouteEncodingRule.ID_FIELD_NUMBER -> id = codedIS.readUInt32()
				else -> skipUnknownField(t)
			}
		}
	}

	private fun readRouteTree(thisTree: RouteSubregion, parentTree: RouteSubregion?, depth: Int, readCoordinates: Boolean): RouteSubregion {
		var readChildren = depth != 0
		if (readChildren) {
			thisTree.subregions = ArrayList()
		}
		thisTree.routeReg.regionsRead++
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return thisTree
				RouteDataBox.LEFT_FIELD_NUMBER -> {
					val i = codedIS.readSInt32()
					if (readCoordinates) {
						thisTree.left = i + (parentTree?.left ?: 0)
					}
				}
				RouteDataBox.RIGHT_FIELD_NUMBER -> {
					val i = codedIS.readSInt32()
					if (readCoordinates) {
						thisTree.right = i + (parentTree?.right ?: 0)
					}
				}
				RouteDataBox.TOP_FIELD_NUMBER -> {
					val i = codedIS.readSInt32()
					if (readCoordinates) {
						thisTree.top = i + (parentTree?.top ?: 0)
					}
				}
				RouteDataBox.BOTTOM_FIELD_NUMBER -> {
					val i = codedIS.readSInt32()
					if (readCoordinates) {
						thisTree.bottom = i + (parentTree?.bottom ?: 0)
					}
				}
				RouteDataBox.SHIFTTODATA_FIELD_NUMBER -> {
					thisTree.shiftToData = readInt()
					if (!readChildren) {
						// usually 0
						thisTree.subregions = ArrayList()
						readChildren = true
					}
				}
				RouteDataBox.BOXES_FIELD_NUMBER -> {
					if (readChildren) {
						val subregion = RouteSubregion(thisTree.routeReg)
						subregion.length = readInt()
						subregion.filePointer = codedIS.getTotalBytesRead()
						val oldLimit = codedIS.pushLimitLong(subregion.length)
						readRouteTree(subregion, thisTree, depth - 1, true)
						thisTree.subregions!!.add(subregion)
						codedIS.popLimit(oldLimit)
						codedIS.seek(subregion.filePointer + subregion.length)
					} else {
						codedIS.seek(thisTree.filePointer + thisTree.length)
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	fun initRouteTypesIfNeeded(req: SearchRequest, list: List<RouteSubregion>) {
		for (rs in list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				initRouteRegion(rs.routeReg)
			}
		}
	}

	fun initRouteRegion(routeReg: RouteRegion) {
		if (routeReg.routeEncodingRules.isEmpty()) {
			codedIS.seek(routeReg.filePointer)
			val oldLimit = codedIS.pushLimitLong(routeReg.length)
			readRouteIndex(routeReg)
			codedIS.popLimit(oldLimit)
		}
	}

	fun loadRouteRegionData(rs: RouteSubregion): List<RouteDataObject?> {
		if (rs.dataObjects == null) {
			codedIS.seek(rs.filePointer + rs.shiftToData)
			val limit = codedIS.readRawVarint32()
			val oldLimit = codedIS.pushLimitLong(limit.toLong())
			readRouteTreeData(rs, idTables, restrictionMap)
			codedIS.popLimit(oldLimit)
		}
		val res = rs.dataObjects!!
		rs.dataObjects = null
		return res
	}

	fun loadRouteRegionData(toLoad: MutableList<RouteSubregion>, matcher: ResultMatcher<RouteDataObject>) {
		toLoad.sortWith { o1, o2 ->
			val p1 = o1.filePointer + o1.shiftToData
			val p2 = o2.filePointer + o2.shiftToData
			if (p1 == p2) 0 else if (p1 < p2) -1 else 1
		}
		for (rs in toLoad) {
			if (rs.dataObjects == null) {
				codedIS.seek(rs.filePointer + rs.shiftToData)
				val limit = codedIS.readRawVarint32()
				val oldLimit = codedIS.pushLimitLong(limit.toLong())
				readRouteTreeData(rs, idTables, restrictionMap)
				codedIS.popLimit(oldLimit)
			}
			for (ro in rs.dataObjects!!) {
				if (ro != null) {
					matcher.publish(ro)
				}
				if (matcher.isCancelled()) {
					break
				}
			}
			// free objects
			rs.dataObjects = null
			if (matcher.isCancelled()) {
				break
			}
		}
	}

	fun searchRouteRegionTree(req: SearchRequest, list: List<RouteSubregion>, toLoad: MutableList<RouteSubregion>): List<RouteSubregion> {
		for (rs in list) {
			if (req.intersects(rs.left, rs.top, rs.right, rs.bottom)) {
				if (rs.subregions == null) {
					codedIS.seek(rs.filePointer)
					val old = codedIS.pushLimitLong(rs.length)
					readRouteTree(rs, null, if (req.contains(rs.left, rs.top, rs.right, rs.bottom)) -1 else 1, false)
					codedIS.popLimit(old)
				}
				searchRouteRegionTree(req, rs.subregions!!, toLoad)

				if (rs.shiftToData != 0L) {
					toLoad.add(rs)
				}
			}
		}
		return toLoad
	}

	/** Field numbers of `OsmAndRoutingIndex` and its nested messages in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndRoutingIndex {
		const val NAME_FIELD_NUMBER = 1
		const val RULES_FIELD_NUMBER = 2
		const val ROOTBOXES_FIELD_NUMBER = 3
		const val BASEMAPBOXES_FIELD_NUMBER = 4
		const val BLOCKS_FIELD_NUMBER = 5
	}

	private object RouteEncodingRule {
		const val TAG_FIELD_NUMBER = 3
		const val VALUE_FIELD_NUMBER = 5
		const val ID_FIELD_NUMBER = 7
	}

	private object RouteDataBox {
		const val LEFT_FIELD_NUMBER = 1
		const val RIGHT_FIELD_NUMBER = 2
		const val TOP_FIELD_NUMBER = 3
		const val BOTTOM_FIELD_NUMBER = 4
		const val SHIFTTODATA_FIELD_NUMBER = 5
		const val BOXES_FIELD_NUMBER = 7
	}

	private object RouteDataBlock {
		const val IDTABLE_FIELD_NUMBER = 5
		const val DATAOBJECTS_FIELD_NUMBER = 6
		const val RESTRICTIONS_FIELD_NUMBER = 7
		const val STRINGTABLE_FIELD_NUMBER = 8
	}

	private object RouteData {
		const val POINTS_FIELD_NUMBER = 1
		const val POINTTYPES_FIELD_NUMBER = 4
		const val POINTNAMES_FIELD_NUMBER = 5
		const val TYPES_FIELD_NUMBER = 7
		const val ROUTEID_FIELD_NUMBER = 12
		const val STRINGNAMES_FIELD_NUMBER = 14
	}

	private object IdTable {
		const val ROUTEID_FIELD_NUMBER = 1
	}

	private object RestrictionData {
		const val TYPE_FIELD_NUMBER = 1
		const val FROM_FIELD_NUMBER = 2
		const val TO_FIELD_NUMBER = 3
		const val VIA_FIELD_NUMBER = 4
	}

	companion object {
		private const val SHIFT_COORDINATES = 4
	}
}
