package net.osmand.shared.binary

import net.osmand.shared.data.Amenity
import net.osmand.shared.data.AmenityRoutePoint
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.KLocation
import net.osmand.shared.data.KQuadRect
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.search.core.KHashQuadTree
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTIntLongMap
import net.osmand.shared.util.collections.KTLongHashSet
import kotlin.math.abs

/**
 * Reads the poi section of an obf file: the tables that decode an amenity, the r-tree of boxes
 * over it, and the amenities themselves.
 *
 * A copy of `BinaryMapPoiReaderAdapter` in OsmAnd-java, which stays there for android and tools;
 * this copy is for iOS. The read statistics java collects behind its stats hooks are left out, as
 * they serve the obf inspection tools, and so is `readNameIndex`, which walks the whole name index
 * for those same tools.
 *
 * Amenities are stored twice over: once in the boxes of the tree, which carry only the types they
 * hold, and once in the data blocks the boxes point at. A search walks the tree with the filter,
 * collects the offsets of the blocks worth reading, and only then reads them, in file order.
 */
class BinaryMapPoiReaderAdapter(private val map: BinaryMapIndexReader) {

	private val codedIS: CodedInputStream = map.codedIS

	private val poiTypes: MapPoiTypes
		get() = MapPoiTypes.getDefault()

	private fun skipUnknownField(t: Int) {
		map.skipUnknownField(t)
	}

	private fun readInt(): Long = map.readInt()

	private fun readPoiBoundariesIndex(region: PoiRegion) {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndTileBox.LEFT_FIELD_NUMBER -> region.left31 = codedIS.readUInt32()
				OsmAndTileBox.RIGHT_FIELD_NUMBER -> region.right31 = codedIS.readUInt32()
				OsmAndTileBox.TOP_FIELD_NUMBER -> region.top31 = codedIS.readUInt32()
				OsmAndTileBox.BOTTOM_FIELD_NUMBER -> region.bottom31 = codedIS.readUInt32()
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * The section header. Without [readCategories] it stops at the first table, which is all the
	 * constructor needs: the name and the box tell whether the file covers a place at all.
	 */
	fun readPoiIndex(region: PoiRegion, readCategories: Boolean) {
		var length: Int
		var oldLimit: Long
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiIndex.NAME_FIELD_NUMBER -> region.name = codedIS.readString()
				OsmAndPoiIndex.BOUNDARIES_FIELD_NUMBER -> {
					length = codedIS.readRawVarint32()
					oldLimit = codedIS.pushLimitLong(length.toLong())
					readPoiBoundariesIndex(region)
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.CATEGORIESTABLE_FIELD_NUMBER -> {
					if (!readCategories) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						return
					}
					length = codedIS.readRawVarint32()
					oldLimit = codedIS.pushLimitLong(length.toLong())
					readCategory(region)
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.SUBTYPESTABLE_FIELD_NUMBER -> {
					if (!readCategories) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						return
					}
					length = codedIS.readRawVarint32()
					oldLimit = codedIS.pushLimitLong(length.toLong())
					readSubtypes(region)
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.BOXES_FIELD_NUMBER -> {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					return
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/** The categories and their subcategories, in the order the numbers in the file refer to. */
	private fun readCategory(region: PoiRegion) {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndCategoryTable.CATEGORY_FIELD_NUMBER -> {
					val cat = codedIS.readString()
					region.categories.add(cat)
					region.categoriesType.add(poiTypes.getPoiCategoryByName(cat.lowercase(), true))
					region.subcategories.add(ArrayList())
					region.subcategoryFreqs.add(KTIntArrayList())
				}
				OsmAndCategoryTable.SUBCATEGORIES_FIELD_NUMBER ->
					region.subcategories[region.subcategories.size - 1].add(codedIS.readString())
				OsmAndCategoryTable.FREQUENCY_FIELD_NUMBER ->
					region.categoryFreqs.add(codedIS.readUInt32())
				OsmAndCategoryTable.SUBCATFREQ_FIELD_NUMBER ->
					region.subcategoryFreqs[region.subcategoryFreqs.size - 1].add(codedIS.readUInt32())
				else -> skipUnknownField(t)
			}
		}
	}

	/** The additional attributes and their value lists. */
	private fun readSubtypes(region: PoiRegion) {
		while (true) {
			val outT = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(outT)) {
				0 -> return
				OsmAndSubtypesTable.SUBTYPES_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					val st = PoiSubType()
					var cycle = true
					while (cycle) {
						val inT = codedIS.readTag()
						when (CodedInputStream.getTagFieldNumber(inT)) {
							0 -> cycle = false
							OsmAndPoiSubtype.NAME_FIELD_NUMBER -> st.name = codedIS.readString()
							OsmAndPoiSubtype.FREQUENCY_FIELD_NUMBER -> st.frequency = codedIS.readUInt32()
							OsmAndPoiSubtype.SUBCATWIKIDATAIDS_FIELD_NUMBER -> {
								var wikidataIds = st.wikidataIds
								if (wikidataIds == null) {
									wikidataIds = ArrayList()
									st.wikidataIds = wikidataIds
								}
								wikidataIds.add(codedIS.readString())
							}
							OsmAndPoiSubtype.SUBTYPEVALUE_FIELD_NUMBER -> {
								var possibleValues = st.possibleValues
								if (possibleValues == null) {
									possibleValues = ArrayList()
									st.possibleValues = possibleValues
								}
								possibleValues.add(codedIS.readString())
							}
							OsmAndPoiSubtype.SUBTYPEVALUESFREQ_FIELD_NUMBER -> {
								var freqs = st.possibleValuesFreqs
								if (freqs == null) {
									freqs = KTIntArrayList()
									st.possibleValuesFreqs = freqs
								}
								freqs.add(codedIS.readUInt32())
							}
							OsmAndPoiSubtype.ISTEXT_FIELD_NUMBER -> st.text = codedIS.readBool()
							else -> skipUnknownField(inT)
						}
					}
					region.subTypes.add(st)
					if (poiTypes.topIndexPoiAdditional.containsKey(st.name)) {
						region.topIndexSubTypes.add(st)
					}
					codedIS.popLimit(oldLimit)
				}
				else -> skipUnknownField(outT)
			}
		}
	}

	/** Reads the decoding tables of [region], unless they are read already. */
	fun initCategories(region: PoiRegion) {
		if (region.categories.isEmpty()) {
			codedIS.seek(region.filePointer)
			val oldLimit = codedIS.pushLimitLong(region.length)
			readPoiIndex(region, true)
			codedIS.popLimit(oldLimit)
		}
	}

	/**
	 * The amenities of [region] inside the box. The tree walk collects the offsets of the data
	 * blocks worth reading, and they are then read in file order rather than in the order the walk
	 * found them.
	 */
	fun searchPoiIndex(
		left31: Int, right31: Int, top31: Int, bottom31: Int,
		req: SearchRequest<Amenity>, region: PoiRegion
	) {
		val indexOffset = codedIS.getTotalBytesRead()
		var skipTiles: KTLongHashSet? = null
		if (req.zoom >= 0 && req.zoom < 16) {
			skipTiles = KTLongHashSet()
		}
		val offsetsMap = KTIntLongMap()
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiIndex.BOXES_FIELD_NUMBER -> {
					val length = readInt()
					val oldLimit = codedIS.pushLimitLong(length)
					readBoxField(
						left31, right31, top31, bottom31, 0, 0, 0, offsetsMap, skipTiles, req, region, null
					)
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.POIDATA_FIELD_NUMBER -> {
					val offsets = offsetsMap.keys()
					offsets.sort()
					skipTiles?.clear()
					for (offset in offsets) {
						var skipVal = offsetsMap[offset]
						if (skipTiles != null && skipVal != -1L) {
							val dzoom = ZOOM_TO_SKIP_FILTER_READ - ZOOM_TO_SKIP_FILTER
							val dx = skipVal shr ZOOM_TO_SKIP_FILTER_READ
							val dy = skipVal - (dx shl ZOOM_TO_SKIP_FILTER_READ)
							skipVal = ((dx shr dzoom) shl ZOOM_TO_SKIP_FILTER) or (dy shr dzoom)
							if (skipVal != -1L && skipTiles.contains(skipVal)) {
								continue
							}
						}
						codedIS.seek(offset + indexOffset)
						val len = readInt()
						val oldLim = codedIS.pushLimitLong(len)
						val read = readPoiData(
							left31, right31, top31, bottom31, req, region, -1, skipTiles,
							if (req.zoom == -1) 31 else req.zoom + ZOOM_TO_SKIP_FILTER
						)
						if (read && skipVal != -1L && skipTiles != null) {
							skipTiles.add(skipVal)
						}
						codedIS.popLimit(oldLim)
						if (req.isCancelled()) {
							return
						}
					}
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					return
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * One data block: the amenities of one tile, their coordinates delta encoded to it. With
	 * [toSkip] the block only gives up one amenity per tile of [zSkip], which is how a search at a
	 * low zoom thins out a dense area.
	 */
	fun readPoiData(
		left31: Int, right31: Int, top31: Int, bottom31: Int,
		req: SearchRequest<Amenity>, region: PoiRegion, index: Int,
		toSkip: KTLongHashSet?, zSkip: Int
	): Boolean {
		var x = 0
		var y = 0
		var zoom = 0
		var read = false
		var left = index
		while (true) {
			if (req.isCancelled()) {
				return read
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return read
				OsmAndPoiBoxData.X_FIELD_NUMBER -> x = codedIS.readUInt32()
				OsmAndPoiBoxData.ZOOM_FIELD_NUMBER -> zoom = codedIS.readUInt32()
				OsmAndPoiBoxData.Y_FIELD_NUMBER -> y = codedIS.readUInt32()
				OsmAndPoiBoxData.POIDATA_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLim = codedIS.pushLimitLong(len.toLong())
					val am = readPoiPoint(left31, right31, top31, bottom31, x, y, zoom, req, region, true)
					codedIS.popLimit(oldLim)
					if (am != null) {
						if (toSkip != null) {
							val location = am.getLocation()!!
							val xp = KMapUtils.getTileNumberX(zSkip.toDouble(), location.longitude).toInt()
							val yp = KMapUtils.getTileNumberY(zSkip.toDouble(), location.latitude).toInt()
							val valSkip = (xp.toLong() shl zSkip) or yp.toLong()
							if (!toSkip.contains(valSkip)) {
								req.collectRawData(am)
								if (req.publish(am)) {
									read = true
									toSkip.add(valSkip)
								}
							} else if (zSkip <= zoom) {
								codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
								return read
							}
						} else {
							req.collectRawData(am)
							if (req.publish(am)) {
								read = true
							}
						}
					}
					if (--left == -1) {
						// index initially could be -1
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						return read
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/** The nearest stretch of the path to [l], if it runs closer than [radius]. */
	private fun dist(l: KLatLon, locations: List<KLocation>, radius: Double): AmenityRoutePoint? {
		var dist = (radius + 0.1).toFloat()
		var arp: AmenityRoutePoint? = null
		// Special iterations because points stored by pairs!
		var i = 1
		while (i < locations.size) {
			val d = KMapUtils.getOrthogonalDistance(
				l.latitude, l.longitude,
				locations[i - 1].latitude, locations[i - 1].longitude,
				locations[i].latitude, locations[i].longitude
			).toFloat()
			if (d < dist) {
				arp = AmenityRoutePoint()
				dist = d
				arp.deviateDistance = dist.toDouble()
				arp.pointA = locations[i - 1]
				arp.pointB = locations[i]
			}
			i += 2
		}
		val pointA = arp?.pointA
		val pointB = arp?.pointB
		if (arp != null && arp.deviateDistance != 0.0 && pointA != null && pointB != null) {
			arp.deviationDirectionRight = KMapUtils.rightSide(
				l.latitude, l.longitude,
				pointA.latitude, pointA.longitude,
				pointB.latitude, pointB.longitude
			)
		}
		return arp
	}

	/**
	 * One amenity, or null when it falls outside the box, its types are turned down by the filter,
	 * or the path search leaves it too far from the path. The fields come in a fixed order, which
	 * lets the reader give up as soon as a type the filter wants has not appeared.
	 */
	private fun readPoiPoint(
		left31: Int, right31: Int, top31: Int, bottom31: Int,
		px: Int, py: Int, zoom: Int, req: SearchRequest<Amenity>, region: PoiRegion,
		checkBounds: Boolean
	): Amenity? {
		var am: Amenity? = null
		var x = 0
		var y = 0
		var precisionXY = 0
		var hasLocation = false
		val retValue = StringBuilder()
		var amenityType: PoiCategory? = null
		var textTags: ArrayDeque<String>? = null
		var hasSubcategoriesField = false
		var topIndexAdditonalFound = false
		var otherSubTypes: MutableMap<String, PoiCategory?>? = null
		while (true) {
			val t = codedIS.readTag()
			val tag = CodedInputStream.getTagFieldNumber(t)
			if (amenityType == null && (tag > OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER || tag == 0)) {
				codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
				return null
			}
			if (req.poiAdditionalFilter != null &&
				(tag > OsmAndPoiBoxDataAtom.SUBCATEGORIES_FIELD_NUMBER || tag == 0)
			) {
				if (!hasSubcategoriesField || !topIndexAdditonalFound) {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					return null
				}
			}
			when (tag) {
				0 -> {
					req.numberOfAcceptedObjects++
					if (hasLocation) {
						if (precisionXY != 0) {
							val xy = KMapUtils.calculateFinalXYFromBaseAndPrecisionXY(
								BASE_POI_ZOOM, FINAL_POI_ZOOM, precisionXY,
								x shr BASE_POI_SHIFT, y shr BASE_POI_SHIFT, true
							)
							val x31 = xy[0] shl FINAL_POI_SHIFT
							val y31 = xy[1] shl FINAL_POI_SHIFT
							am!!.setLocation(KMapUtils.get31LatitudeY(y31), KMapUtils.get31LongitudeX(x31))
						} else {
							am!!.setLocation(KMapUtils.get31LatitudeY(y), KMapUtils.get31LongitudeX(x))
						}
					} else {
						return null
					}

					if (req.radius > 0) {
						val loc = am.getLocation()!!
						val locs = req.tiles?.get(req.getTileHashOnPath(loc.latitude, loc.longitude))
							?: return null
						val arp = dist(loc, locs, req.radius) ?: return null
						am.setRoutePoint(arp)
					}
					if (req.poiTypeFilter != null && otherSubTypes != null) {
						// multivalue amenity, add other subtypes
						for (entry in otherSubTypes.entries) {
							val cat = entry.value
							if (am.getType() === cat) {
								am.setSubType(am.getSubType() + ";" + entry.key)
							}
						}
					}
					am.setRegionName(region.getName())
					return am
				}
				OsmAndPoiBoxDataAtom.DX_FIELD_NUMBER ->
					x = (codedIS.readSInt32() + (px shl (BASE_POI_ZOOM - zoom))) shl BASE_POI_SHIFT
				OsmAndPoiBoxDataAtom.DY_FIELD_NUMBER -> {
					y = (codedIS.readSInt32() + (py shl (BASE_POI_ZOOM - zoom))) shl BASE_POI_SHIFT
					req.numberOfVisitedObjects++
					if (checkBounds) {
						if (left31 > x || right31 < x || top31 > y || bottom31 < y) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
							return null
						}
					}
					am = Amenity()
					hasLocation = true
				}
				OsmAndPoiBoxDataAtom.SUBCATEGORIES_FIELD_NUMBER -> {
					val subtypev = codedIS.readUInt32()
					retValue.setLength(0)
					hasSubcategoriesField = true
					val st = region.getSubtypeFromId(subtypev, retValue)
					val poiAdditionalFilter = req.poiAdditionalFilter
					if (poiAdditionalFilter != null) {
						if (st != null && poiAdditionalFilter.accept(st, retValue.toString())) {
							topIndexAdditonalFound = true
						}
					}
					if (st != null) {
						am!!.setAdditionalInfo(st.name ?: "", retValue.toString())
					}
				}
				OsmAndPoiBoxDataAtom.TEXTCATEGORIES_FIELD_NUMBER -> {
					val texttypev = codedIS.readUInt32()
					retValue.setLength(0)
					val textt = region.getSubtypeFromId(texttypev, retValue)
					if (textt != null && textt.text) {
						var tags = textTags
						if (tags == null) {
							tags = ArrayDeque()
							textTags = tags
						}
						tags.addLast(textt.name ?: "")
					}
				}
				OsmAndPoiBoxDataAtom.TEXTVALUES_FIELD_NUMBER -> {
					val str = codedIS.readString()
					val tags = textTags
					if (tags != null && tags.isNotEmpty()) {
						am!!.setAdditionalInfo(tags.removeFirst(), str)
					}
				}
				OsmAndPoiBoxDataAtom.CATEGORIES_FIELD_NUMBER -> {
					val cat = codedIS.readUInt32()
					val subcatId = cat shr SHIFT_BITS_CATEGORY
					val catId = cat and CATEGORY_MASK
					var type: PoiCategory? = poiTypes.getOtherPoiCategory()
					var subtype = ""
					if (catId < region.categoriesType.size) {
						type = region.categoriesType[catId]
						val subcats = region.subcategories[catId]
						if (subcatId < subcats.size) {
							subtype = subcats[subcatId]
						}
					}
					subtype = poiTypes.replaceDeprecatedSubtype(type, subtype)
					val isForbidden = poiTypes.isTypeForbidden(subtype)
					val poiTypeFilter = req.poiTypeFilter
					if (!isForbidden && (poiTypeFilter == null || poiTypeFilter.accept(type, subtype))) {
						if (amenityType == null) {
							amenityType = type
							am!!.setSubType(subtype)
							am.setType(amenityType)
						} else {
							am!!.setSubType(am.getSubType() + ";" + subtype)
						}
					} else {
						var other = otherSubTypes
						if (other == null) {
							other = HashMap()
							otherSubTypes = other
						}
						other[subtype] = type
					}
				}
				OsmAndPoiBoxDataAtom.ID_FIELD_NUMBER -> am!!.setId(codedIS.readUInt64())
				OsmAndPoiBoxDataAtom.NAME_FIELD_NUMBER -> am!!.setName(codedIS.readString())
				OsmAndPoiBoxDataAtom.NAMEEN_FIELD_NUMBER -> am!!.setEnName(codedIS.readString())
				OsmAndPoiBoxDataAtom.PRECISIONXY_FIELD_NUMBER -> {
					if (hasLocation) {
						precisionXY = codedIS.readInt32()
					}
				}
				OsmAndPoiBoxDataAtom.TAGGROUPS_FIELD_NUMBER -> {
					val sz = codedIS.readRawVarint32()
					val old = codedIS.pushLimitLong(sz.toLong())
					while (codedIS.getBytesUntilLimit() > 0) {
						val tagGroupId = codedIS.readUInt32()
						val list = region.getTagValues(tagGroupId)
						if (list != null && list.isNotEmpty()) {
							am!!.addTagGroup(tagGroupId, list)
						}
					}
					codedIS.popLimit(old)
				}
				else -> skipUnknownField(t)
			}
		}
	}


	/**
	 * Name index
	 */

	private fun normalizeSearchPoiByNameQuery(query: String): String =
		query.replace("\"", "").lowercase()

	/**
	 * The amenities of [region] whose name matches the request's query. The name index gives the
	 * offsets of the data blocks that hold a candidate; the blocks are then read and each amenity
	 * matched again, because a block holds more than the one name that pointed at it.
	 */
	fun searchPoiByName(region: PoiRegion, req: SearchRequest<Amenity>) {
		var offsets = KTIntLongMap()
		val query = normalizeSearchPoiByNameQuery(req.nameQuery ?: "")
		val matcher = KCollatorStringMatcher(query, req.matcherMode)
		val indexOffset = codedIS.getTotalBytesRead()
		var coordsTagGroups = KTLongHashSet()
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiIndex.NAMEINDEX_FIELD_NUMBER -> {
					val length = readInt()
					val oldLimit = codedIS.pushLimitLong(length)
					// here offsets are sorted by distance
					offsets = readPoiNameIndex(query, req, region, coordsTagGroups)
					coordsTagGroups = region.checkMissingTagGroups(coordsTagGroups)
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.BOXES_FIELD_NUMBER -> {
					val length = readInt()
					val oldLimit = codedIS.pushLimitLong(length)
					if (coordsTagGroups.size() > 0) {
						readBoxField(
							0, 0, 0, 0, 0, 0, 0, KTIntLongMap(), null, req, region,
							prepareTileIdsToCheckTagGroups(coordsTagGroups)
						)
					} else {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					}
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.POIDATA_FIELD_NUMBER -> {
					// also offsets can be randomly skipped by limit
					val offKeys = sortOffsetsForReading(offsets)
					for (offKey in offKeys) {
						codedIS.seek(offKey + indexOffset)
						val len = readInt()
						val oldLim = codedIS.pushLimitLong(len)
						readPoiData(matcher, req, region)
						codedIS.popLimit(oldLim)

						if (req.isCancelled() || req.limitExceeded()) {
							return
						}
					}
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					if (coordsTagGroups.size() > 0) {
						region.updReadTagGroups(coordsTagGroups)
					}
					return
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * The nearest blocks first, and the rest in buckets by file offset, so that a search that stops
	 * early has the closest answers and one that runs on still reads forward through the file.
	 *
	 * Java leaves blocks at the same distance in the order its hash map happened to hand them out,
	 * which no other map reproduces; this breaks such ties by offset instead.
	 */
	private fun sortOffsetsForReading(offsets: KTIntLongMap): IntArray {
		val offKeys = offsets.keys()
		if (offKeys.isEmpty()) {
			return offKeys
		}
		val byDistance = offKeys.sortedWith(compareBy({ offsets[it] }, { it })).toIntArray()
		var p = BUCKET_SEARCH_BY_NAME * 3
		if (p < byDistance.size) {
			var i = p + BUCKET_SEARCH_BY_NAME
			while (true) {
				if (i > byDistance.size) {
					byDistance.sort(p, byDistance.size)
					break
				} else {
					byDistance.sort(p, i)
				}
				p = i
				i += BUCKET_SEARCH_BY_NAME
			}
		}
		return byDistance
	}

	/**
	 * Walks the name index for [query]: its string table gives the prefixes a query word could sit
	 * under, and the data under each prefix gives the blocks. A query of several words keeps only
	 * the blocks every word points at, unless the mode is MULTISEARCH, which keeps them all.
	 */
	private fun readPoiNameIndex(
		query: String, req: SearchRequest<Amenity>, region: PoiRegion, tagGroupCoords: KTLongHashSet
	): KTIntLongMap {
		val offsets = KTIntLongMap()
		var offset = 0L
		val listOfSepOffsets = ArrayList<KTIntLongMap>()
		val queries = KSearchAlgorithms.splitAndNormalize(query, true)
		var queryTokens: MutableList<QueryToken>? = null
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return offsets
				OsmAndPoiNameIndex.TABLE_FIELD_NUMBER -> {
					val length = readInt()
					val oldLimit = codedIS.pushLimitLong(length)
					offset = codedIS.getTotalBytesRead()

					val prefixCandidates = map.readIndexedStringTablePrefixes(queries)
					val tokens = ArrayList<QueryToken>(queries.size)
					for (i in queries.indices) {
						tokens.add(QueryToken(queries[i], req.matcherMode, prefixCandidates[i]))
					}
					queryTokens = tokens
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiNameIndex.DATA_FIELD_NUMBER -> {
					val tokens = queryTokens
					if (tokens != null) {
						for (tokenMatch in tokens) {
							val offsetMap = KTIntLongMap()
							listOfSepOffsets.add(offsetMap)
							for (prefix in tokenMatch.prefixes) {
								codedIS.seek(prefix.offset + offset)
								val len = codedIS.readRawVarint32()
								val oldLim = codedIS.pushLimitLong(len.toLong())
								readPoiNameIndexData(offsetMap, req, region, tagGroupCoords, tokenMatch, prefix)
								codedIS.popLimit(oldLim)
								if (req.isCancelled()) {
									codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
									return offsets
								}
							}
						}
					}
					if (listOfSepOffsets.size > 0) {
						if (req.matcherMode == KStringMatcherMode.MULTISEARCH) {
							for (m in listOfSepOffsets) {
								offsets.putAll(m)
							}
						} else {
							offsets.putAll(listOfSepOffsets[0])
							for (j in 1 until listOfSepOffsets.size) {
								val mp = listOfSepOffsets[j]
								// calculate intersection of mp & offsets
								for (chKey in offsets.keys()) {
									if (!mp.containsKey(chKey)) {
										offsets.remove(chKey)
									}
								}
							}
						}
					}
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					return offsets
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * The entries under one prefix of the name index. Each name is the prefix plus a suffix from
	 * the dictionary at the head of the block, so the dictionary decides which entries can match
	 * before any of them is read.
	 */
	private fun readPoiNameIndexData(
		offsets: KTIntLongMap, req: SearchRequest<Amenity>, region: PoiRegion,
		tagGroupCoords: KTLongHashSet, token: QueryToken?, prefix: QueryToken.Prefix?
	) {
		var suffixDictionary: MutableList<String>? = null
		val mask = if (token == null || prefix == null) null else token.SuffixMask(prefix)
		var suffixDictionaryInitialized = false
		var emptySuffixes = false
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiNameIndexData.SUFFIXESDICTIONARY_FIELD_NUMBER -> {
					val encodedSuffix = codedIS.readString()
					var dictionary = suffixDictionary
					if (dictionary == null) {
						dictionary = ArrayList()
						suffixDictionary = dictionary
					}
					if (KSearchAlgorithms.OLD_EMPTY_SUFFIX_DICTIONARY_SENTINEL == encodedSuffix) {
						emptySuffixes = true
					} else {
						val prevSuffix = if (dictionary.isEmpty()) null else dictionary[dictionary.size - 1]
						dictionary.add(
							KSearchAlgorithms.nameIndexDecodeDictionarySuffix(prevSuffix, encodedSuffix)
						)
					}
				}
				OsmAndPoiNameIndexData.ATOMS_FIELD_NUMBER -> {
					val dictionary = suffixDictionary
					if (emptySuffixes || (dictionary != null && dictionary.size == 1 &&
								dictionary[0] == KSearchAlgorithms.EMPTY_SUFFIX_DICTIONARY_SENTINEL)
					) {
						if (prefix != null && token != null && !token.matchFullPrefix(prefix.key)) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
							return
						}
					}
					if (!suffixDictionaryInitialized && mask != null) {
						mask.setDictionary(suffixDictionary)
						suffixDictionaryInitialized = true
					}
					val len = codedIS.readRawVarint32()
					val oldLim = codedIS.pushLimitLong(len.toLong())
					readPoiNameIndexDataAtom(offsets, req, region, tagGroupCoords, mask)
					codedIS.popLimit(oldLim)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/** One entry of the name index: the tile it sits in and the block that holds it. */
	private fun readPoiNameIndexDataAtom(
		offsets: KTIntLongMap, req: SearchRequest<Amenity>, region: PoiRegion,
		tagGroupCoords: KTLongHashSet, suffixMask: QueryToken.SuffixMask?
	) {
		var x = 0
		var y = 0
		var zoom = 15
		var shift = Int.MIN_VALUE
		var matched = false
		var noBisetIndex = true
		var maskIndex = 0
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					if ((suffixMask != null && suffixMask.shouldPassThrough()) || noBisetIndex) {
						// intermediate version ignore
						matched = true
					}
					if (!matched) {
						return
					}
					if (shift != Int.MIN_VALUE) {
						val x31 = x shl (31 - zoom)
						val y31 = y shl (31 - zoom)
						val x31r = (x + 1) shl (31 - zoom)
						val y31b = (y + 1) shl (31 - zoom)
						val r = KQuadRect(x31.toDouble(), y31.toDouble(), x31r.toDouble(), y31b.toDouble())
						if (req.contains(x31, y31, x31, y31) ||
							r.contains(req.x.toDouble(), req.y.toDouble(), req.x.toDouble(), req.y.toDouble())
						) {
							val d = abs(req.x.toLong() - x31) + abs(req.y.toLong() - y31)
							offsets.put(shift, d)
							tagGroupCoords.add(KHashQuadTree.encodeTileId(EVAL_TAG_GROUP_ZOOM, x31, y31))
						}
					}
					return
				}
				OsmAndPoiNameIndexDataAtom.X_FIELD_NUMBER -> x = codedIS.readUInt32()
				OsmAndPoiNameIndexDataAtom.Y_FIELD_NUMBER -> y = codedIS.readUInt32()
				OsmAndPoiNameIndexDataAtom.ZOOM_FIELD_NUMBER -> zoom = codedIS.readUInt32()
				OsmAndPoiNameIndexDataAtom.SUFFIXESBITSETINDEX_FIELD_NUMBER -> {
					noBisetIndex = false
					val index = codedIS.readUInt32()
					if (!matched && suffixMask != null && suffixMask.isMatched(maskIndex, index)) {
						matched = true
					}
					maskIndex++
				}
				OsmAndPoiNameIndexDataAtom.SHIFTTO_FIELD_NUMBER -> {
					val l = readInt()
					if (l > Int.MAX_VALUE) {
						throw IllegalStateException()
					}
					shift = l.toInt()
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * One data block, with every amenity in it matched against the query by name. A block is read
	 * because one of its amenities matched in the index, and the rest of it has to be filtered out
	 * here.
	 */
	private fun readPoiData(matcher: KCollatorStringMatcher, req: SearchRequest<Amenity>, region: PoiRegion) {
		var x = 0
		var y = 0
		var zoom = 0
		while (true) {
			if (req.isCancelled() || req.limitExceeded()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiBoxData.X_FIELD_NUMBER -> x = codedIS.readUInt32()
				OsmAndPoiBoxData.ZOOM_FIELD_NUMBER -> zoom = codedIS.readUInt32()
				OsmAndPoiBoxData.Y_FIELD_NUMBER -> y = codedIS.readUInt32()
				OsmAndPoiBoxData.POIDATA_FIELD_NUMBER -> {
					val len = codedIS.readRawVarint32()
					val oldLim = codedIS.pushLimitLong(len.toLong())
					val am = readPoiPoint(0, Int.MAX_VALUE, 0, Int.MAX_VALUE, x, y, zoom, req, region, false)
					codedIS.popLimit(oldLim)
					if (am != null) {
						var matches = matcher.matches(am.getName().lowercase()) ||
								matcher.matches(am.getEnName(true).lowercase())
						if (!matches) {
							for (s in am.getOtherNames()) {
								matches = matcher.matches(s.lowercase())
								if (matches) {
									break
								}
							}
							if (!matches) {
								for (key in am.getAdditionalInfoKeys()) {
									if (ObfConstants.isTagIndexedForSearchAsName(key) ||
										ObfConstants.isTagNonIndexedForSearchAsName(key) ||
										ObfConstants.isTagIndexedForSearchAsId(key) ||
										ObfConstants.isTagIndexedAsSearchRelated(key)
									) {
										// isTagIndexedAsSearchRelated could be toggled off to avoid unnecessary matches
										matches = matcher.matches(am.getAdditionalInfo(key) ?: "")
										if (matches) {
											break
										}
									}
								}
							}
						}
						if (matches) {
							req.collectRawData(am)
							req.publish(am)
						}
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/** Whether a box holds anything the filter wants, from the list of types the box carries. */
	private fun checkCategories(req: SearchRequest<Amenity>, region: PoiRegion): Boolean {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return false
				OsmAndPoiCategories.SUBCATEGORIES_FIELD_NUMBER -> {
					val subcat = codedIS.readUInt32()
					val subType = StringBuilder()
					val poiSubType = region.getSubtypeFromId(subcat, subType)
					val value = subType.toString()
					val poiAdditionalFilter = req.poiAdditionalFilter
					if (poiSubType != null && value.isNotEmpty() && poiAdditionalFilter != null &&
						poiAdditionalFilter.accept(poiSubType, value)
					) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						return true
					}
				}
				OsmAndPoiCategories.CATEGORIES_FIELD_NUMBER -> {
					var type: PoiCategory? = poiTypes.getOtherPoiCategory()
					var subtype = ""
					val cat = codedIS.readUInt32()
					val subcatId = cat shr SHIFT_BITS_CATEGORY
					val catId = cat and CATEGORY_MASK
					if (catId < region.categoriesType.size) {
						type = region.categoriesType[catId]
						val subcats = region.subcategories[catId]
						if (subcatId < subcats.size) {
							subtype = subcats[subcatId]
						}
					}
					subtype = poiTypes.replaceDeprecatedSubtype(type, subtype)
					val poiTypeFilter = req.poiTypeFilter
					if (poiTypeFilter != null && poiTypeFilter.accept(type, subtype)) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
						return true
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * One node of the r-tree over the poi section, whose box is delta encoded to its parent's.
	 * Collects into [offsetsMap] the offsets of the data blocks under it, with the tile a hit
	 * there should mark as covered, or -1 when nothing is being thinned out.
	 */
	private fun readBoxField(
		left31: Int, right31: Int, top31: Int, bottom31: Int,
		px: Int, py: Int, pzoom: Int, offsetsMap: KTIntLongMap, skipTiles: KTLongHashSet?,
		req: SearchRequest<Amenity>, region: PoiRegion, tagGroupsToRead: KTLongHashSet?
	): Boolean {
		req.numberOfReadSubtrees++
		val zoomToSkip = if (req.zoom == -1) 31 else req.zoom + ZOOM_TO_SKIP_FILTER_READ

		var existsCategories = false
		var intersectBbox = true
		var readSubBoxesTagGroup = false

		var zoom = pzoom
		var dy = py
		var dx = px
		var initCoords = false
		var xData = -1
		var yData = -1

		while (true) {
			if (req.isCancelled()) {
				return false
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return existsCategories
				OsmAndPoiBox.ZOOM_FIELD_NUMBER -> zoom = codedIS.readUInt32() + pzoom
				OsmAndPoiBox.LEFT_FIELD_NUMBER -> dx = codedIS.readSInt32()
				OsmAndPoiBox.TOP_FIELD_NUMBER -> {
					dy = codedIS.readSInt32()
					xData = dx + (px shl (zoom - pzoom))
					yData = dy + (py shl (zoom - pzoom))
					if (!initCoords) {
						val xL = xData shl (31 - zoom)
						val xR = ((xData + 1) shl (31 - zoom)) - 1
						val yT = yData shl (31 - zoom)
						val yB = ((yData + 1) shl (31 - zoom)) - 1
						intersectBbox = !(left31 > xR || xL > right31 || bottom31 < yT || yB < top31)
						if (tagGroupsToRead != null) {
							var xyLT = KHashQuadTree.encodeTileId31(EVAL_TAG_GROUP_ZOOM, xL, yT)
							var xyRB = KHashQuadTree.encodeTileId31(EVAL_TAG_GROUP_ZOOM, xR, yB)
							if (xyLT == xyRB) {
								readSubBoxesTagGroup = tagGroupsToRead.contains(xyLT)
							} else {
								// we also added all parent tiles so we can check intersection quickly
								while (xyLT != xyRB) {
									xyLT = xyLT shr 2
									xyRB = xyRB shr 2
								}
								readSubBoxesTagGroup = tagGroupsToRead.contains(xyLT)
							}
						}
						// check intersection
						if (!intersectBbox && !readSubBoxesTagGroup) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
							return false
						}
						req.numberOfAcceptedSubtrees++
						initCoords = true // already init
					}
				}
				OsmAndPoiBox.CATEGORIES_FIELD_NUMBER -> {
					if ((req.poiTypeFilter == null && req.poiAdditionalFilter == null) || readSubBoxesTagGroup) {
						skipUnknownField(t)
					} else {
						val length = codedIS.readRawVarint32()
						val oldLimit = codedIS.pushLimitLong(length.toLong())
						val check = checkCategories(req, region)
						codedIS.popLimit(oldLimit)
						if (!check) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
							return false
						}
						existsCategories = true
					}
				}
				OsmAndPoiBox.TAGGROUPS_FIELD_NUMBER -> {
					val tagGroupLength = codedIS.readRawVarint32()
					val old = codedIS.pushLimitLong(tagGroupLength.toLong())
					readTagGroups(region, req)
					codedIS.popLimit(old)
				}
				OsmAndPoiBox.SUBBOXES_FIELD_NUMBER -> {
					val length = readInt()
					val oldLimit = codedIS.pushLimitLong(length)
					val exists = readBoxField(
						left31, right31, top31, bottom31, xData, yData, zoom, offsetsMap, skipTiles,
						req, region, tagGroupsToRead
					)
					codedIS.popLimit(oldLimit)
					if (skipTiles != null && zoom >= zoomToSkip && exists && !readSubBoxesTagGroup) {
						val value = ((xData.toLong() shr (zoom - zoomToSkip)) shl zoomToSkip) or
								(yData.toLong() shr (zoom - zoomToSkip))
						if (skipTiles.contains(value)) {
							codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
							return true
						}
					}
				}
				OsmAndPoiBox.SHIFTTODATA_FIELD_NUMBER -> {
					var read = true
					val tiles = req.tiles
					if (tiles != null) {
						val zx = xData.toLong() shl (SearchRequest.ZOOM_TO_SEARCH_POI - zoom)
						val zy = yData.toLong() shl (SearchRequest.ZOOM_TO_SEARCH_POI - zoom)
						read = tiles.containsKey((zx shl SearchRequest.ZOOM_TO_SEARCH_POI) + zy)
					}
					val l = readInt()
					if (l > Int.MAX_VALUE) {
						throw IllegalStateException()
					}
					val offset = l.toInt()
					if (read && intersectBbox) {
						if (skipTiles != null && zoom >= zoomToSkip) {
							val valSkip = ((xData.toLong() shr (zoom - zoomToSkip)) shl zoomToSkip) or
									(yData.toLong() shr (zoom - zoomToSkip))
							offsetsMap.put(offset, valSkip)
							skipTiles.add(valSkip)
						} else {
							offsetsMap.put(offset, -1)
						}
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	private fun readTagGroups(region: PoiRegion, req: SearchRequest<Amenity>) {
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiTagGroups.GROUPS_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					readTagGroup(region, req)
					codedIS.popLimit(oldLimit)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/** One administrative area's tags, stored as a flat list of tag, value, tag, value. */
	private fun readTagGroup(region: PoiRegion, req: SearchRequest<Amenity>) {
		val tagValues = ArrayList<String>()
		var id = -1
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					if (id > 0 && tagValues.size > 1 && tagValues.size % 2 == 0) {
						val tagValuePairs = ArrayList<TagValuePair>()
						var i = 0
						while (i < tagValues.size) {
							tagValuePairs.add(TagValuePair(tagValues[i], tagValues[i + 1], -1))
							i += 2
						}
						region.setTagGroups(id, tagValuePairs)
					}
					return
				}
				OsmAndPoiTagGroup.ID_FIELD_NUMBER -> id = codedIS.readUInt32()
				OsmAndPoiTagGroup.TAGVALUES_FIELD_NUMBER -> tagValues.add(codedIS.readString())
				else -> skipUnknownField(t)
			}
		}
	}

	/** Reads the tag groups of the boxes covering [tileIds], without reading any amenity. */
	fun readPoiBboxes(region: PoiRegion, sr: SearchRequest<Amenity>, tileIds: KTLongHashSet?) {
		val ids = prepareTileIdsToCheckTagGroups(tileIds)
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndPoiIndex.BOXES_FIELD_NUMBER -> {
					val length = readInt()
					val oldLimit = codedIS.pushLimitLong(length)
					readBoxField(0, 0, 0, 0, 0, 0, 0, KTIntLongMap(), null, sr, region, ids)
					codedIS.popLimit(oldLimit)
				}
				OsmAndPoiIndex.POIDATA_FIELD_NUMBER -> {
					codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					return
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/** Adds every parent tile, so that a box can be tested against the whole set at once. */
	private fun prepareTileIdsToCheckTagGroups(tileIds: KTLongHashSet?): KTLongHashSet? {
		if (tileIds != null && tileIds.size() > 0) {
			val copy = KTLongHashSet()
			copy.addAll(tileIds)
			for (id in tileIds.toArray()) {
				var i = id
				while (i > 0) {
					i = i shr 2
					copy.add(i)
				}
			}
			return copy
		}
		return tileIds
	}

	companion object {
		const val SHIFT_BITS_CATEGORY = 7
		internal const val CATEGORY_MASK = (1 shl SHIFT_BITS_CATEGORY) - 1
		private const val ZOOM_TO_SKIP_FILTER_READ = 6
		private const val ZOOM_TO_SKIP_FILTER = 3
		private const val BUCKET_SEARCH_BY_NAME = 15 // should be bigger 100?
		private const val BASE_POI_SHIFT = SHIFT_BITS_CATEGORY // 7
		private const val FINAL_POI_SHIFT = BinaryMapIndexReader.SHIFT_COORDINATES // 5
		private const val BASE_POI_ZOOM = 31 - BASE_POI_SHIFT // 24 zoom
		private const val FINAL_POI_ZOOM = 31 - FINAL_POI_SHIFT // 26 zoom

		const val EVAL_TAG_GROUP_ZOOM = 14
	}

	/** Field numbers of the poi section messages in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndPoiIndex {
		const val NAME_FIELD_NUMBER = 1
		const val BOUNDARIES_FIELD_NUMBER = 2
		const val CATEGORIESTABLE_FIELD_NUMBER = 3
		const val NAMEINDEX_FIELD_NUMBER = 4
		const val SUBTYPESTABLE_FIELD_NUMBER = 5
		const val BOXES_FIELD_NUMBER = 6
		const val POIDATA_FIELD_NUMBER = 9
	}

	private object OsmAndTileBox {
		const val LEFT_FIELD_NUMBER = 1
		const val RIGHT_FIELD_NUMBER = 2
		const val TOP_FIELD_NUMBER = 3
		const val BOTTOM_FIELD_NUMBER = 4
	}

	private object OsmAndCategoryTable {
		const val CATEGORY_FIELD_NUMBER = 1
		const val FREQUENCY_FIELD_NUMBER = 2
		const val SUBCATEGORIES_FIELD_NUMBER = 3
		const val SUBCATFREQ_FIELD_NUMBER = 4
	}

	private object OsmAndSubtypesTable {
		const val SUBTYPES_FIELD_NUMBER = 4
	}

	private object OsmAndPoiSubtype {
		const val NAME_FIELD_NUMBER = 1
		const val ISTEXT_FIELD_NUMBER = 3
		const val FREQUENCY_FIELD_NUMBER = 5
		const val SUBTYPEVALUESFREQ_FIELD_NUMBER = 7
		const val SUBTYPEVALUE_FIELD_NUMBER = 8
		const val SUBCATWIKIDATAIDS_FIELD_NUMBER = 12
	}

	private object OsmAndPoiBox {
		const val ZOOM_FIELD_NUMBER = 1
		const val LEFT_FIELD_NUMBER = 2
		const val TOP_FIELD_NUMBER = 3
		const val CATEGORIES_FIELD_NUMBER = 4
		const val TAGGROUPS_FIELD_NUMBER = 8
		const val SUBBOXES_FIELD_NUMBER = 10
		const val SHIFTTODATA_FIELD_NUMBER = 14
	}

	private object OsmAndPoiCategories {
		const val CATEGORIES_FIELD_NUMBER = 3
		const val SUBCATEGORIES_FIELD_NUMBER = 5
	}

	private object OsmAndPoiBoxData {
		const val ZOOM_FIELD_NUMBER = 1
		const val X_FIELD_NUMBER = 2
		const val Y_FIELD_NUMBER = 3
		const val POIDATA_FIELD_NUMBER = 5
	}

	private object OsmAndPoiBoxDataAtom {
		const val DX_FIELD_NUMBER = 2
		const val DY_FIELD_NUMBER = 3
		const val CATEGORIES_FIELD_NUMBER = 4
		const val SUBCATEGORIES_FIELD_NUMBER = 5
		const val NAME_FIELD_NUMBER = 6
		const val NAMEEN_FIELD_NUMBER = 7
		const val ID_FIELD_NUMBER = 8
		const val TEXTCATEGORIES_FIELD_NUMBER = 14
		const val TEXTVALUES_FIELD_NUMBER = 15
		const val PRECISIONXY_FIELD_NUMBER = 16
		const val TAGGROUPS_FIELD_NUMBER = 17
	}

	private object OsmAndPoiNameIndex {
		const val TABLE_FIELD_NUMBER = 3
		const val DATA_FIELD_NUMBER = 5
	}

	private object OsmAndPoiNameIndexData {
		const val SUFFIXESDICTIONARY_FIELD_NUMBER = 2
		const val ATOMS_FIELD_NUMBER = 3
	}

	private object OsmAndPoiNameIndexDataAtom {
		const val ZOOM_FIELD_NUMBER = 2
		const val X_FIELD_NUMBER = 3
		const val Y_FIELD_NUMBER = 4
		const val SUFFIXESBITSETINDEX_FIELD_NUMBER = 5
		const val SHIFTTO_FIELD_NUMBER = 14
	}

	private object OsmAndPoiTagGroups {
		const val GROUPS_FIELD_NUMBER = 5
	}

	private object OsmAndPoiTagGroup {
		const val ID_FIELD_NUMBER = 1
		const val TAGVALUES_FIELD_NUMBER = 5
	}
}
