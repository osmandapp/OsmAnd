package net.osmand.shared.binary

import net.osmand.shared.data.Building
import net.osmand.shared.data.City
import net.osmand.shared.data.CityType
import net.osmand.shared.data.KLatLon
import net.osmand.shared.data.MapObject
import net.osmand.shared.data.Postcode
import net.osmand.shared.data.Street
import net.osmand.shared.util.KCollatorStringMatcher
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KSearchAlgorithms
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.KTransliterationHelper
import net.osmand.shared.util.collections.KTIntArrayList

/**
 * Reads the address section of an obf file: the settlements, postcodes and boundaries in their
 * blocks, their streets with houses and crossings, and the name index over all of them.
 *
 * A copy of `BinaryMapAddressReaderAdapter` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS. The read statistics java collects are left out, as they serve the
 * obf inspection tools, and so is `readNameIndex`, which reads the whole name index for the spatial
 * search, which is not copied yet.
 *
 * A settlement and its streets are stored apart: the settlement's header points at the block of
 * its streets, and each street carries its houses and the streets it crosses, with coordinates as
 * deltas to the settlement's at zoom 24.
 */
class BinaryMapAddressReaderAdapter(private val map: BinaryMapIndexReader) {

	private val codedIS: CodedInputStream = map.codedIS

	private fun skipUnknownField(t: Int) {
		map.skipUnknownField(t)
	}

	private fun readInt(): Long = map.readInt()

	private fun readBoundariesIndex() {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				// should be written to osmand.indexes cache
				OsmAndTileBox.LEFT_FIELD_NUMBER -> codedIS.readUInt32()
				OsmAndTileBox.RIGHT_FIELD_NUMBER -> codedIS.readUInt32()
				OsmAndTileBox.TOP_FIELD_NUMBER -> codedIS.readUInt32()
				OsmAndTileBox.BOTTOM_FIELD_NUMBER -> codedIS.readUInt32()
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readAddressIndex(region: AddressRegion) {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					val enName = region.enName
					if (enName == null || enName.isEmpty()) {
						val name = region.name
						region.enName = if (name == null) "" else KTransliterationHelper.transliterate(name)
					}
					return
				}
				OsmAndAddressIndex.NAME_FIELD_NUMBER -> region.name = codedIS.readString()
				OsmAndAddressIndex.NAME_EN_FIELD_NUMBER -> region.enName = codedIS.readString()
				OsmAndAddressIndex.BOUNDARIES_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					readBoundariesIndex()
					codedIS.popLimit(oldLimit)
					// java reads the English name right after the box
					region.enName = codedIS.readString()
				}
				OsmAndAddressIndex.ATTRIBUTETAGSTABLE_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					region.attributeTagsTable = map.readStringTable()
					codedIS.popLimit(oldLimit)
				}
				OsmAndAddressIndex.CITIES_FIELD_NUMBER -> {
					val block = CitiesBlock()
					region.cities.add(block)
					block.type = 1
					block.length = readInt()
					block.filePointer = codedIS.getTotalBytesRead()
					while (true) {
						val tt = codedIS.readTag()
						val ttag = CodedInputStream.getTagFieldNumber(tt)
						if (ttag == 0) {
							break
						} else if (ttag == CitiesIndex.TYPE_FIELD_NUMBER) {
							block.type = codedIS.readUInt32()
							break
						} else {
							skipUnknownField(tt)
						}
					}
					codedIS.seek(block.filePointer + block.length)
				}
				OsmAndAddressIndex.NAMEINDEX_FIELD_NUMBER -> {
					region.indexNameOffset = codedIS.getTotalBytesRead()
					val length = readInt()
					codedIS.seek(region.indexNameOffset + length + 4)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readCities(
		cities: MutableList<City>, resultMatcher: SearchRequest<City>?, matcher: KStringMatcher?,
		additionalTagsTable: List<String>?
	) {
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				CitiesIndex.CITIES_FIELD_NUMBER -> {
					val fp = codedIS.getTotalBytesRead()
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					val c = readCityHeader(resultMatcher, DefaultCityMatcher(matcher), fp, additionalTagsTable)
					if (c != null) {
						if (resultMatcher == null || resultMatcher.publish(c)) {
							cities.add(c)
						}
					}
					codedIS.popLimit(oldLimit)
					if (resultMatcher != null && resultMatcher.isCancelled()) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readCityStreets(
		resultMatcher: SearchRequest<Street>?, city: City, loadBuildings: Boolean, attributeTagsTable: List<String>?
	) {
		val location = city.getLocation()!!
		val x = KMapUtils.get31TileNumberX(location.longitude)
		val y = KMapUtils.get31TileNumberY(location.latitude)
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				CityBlockIndex.STREETS_FIELD_NUMBER -> {
					val s = Street(city)
					s.setFileOffset(codedIS.getTotalBytesRead())
					val length = codedIS.readRawVarint32()
					val oldLimit = codedIS.pushLimitLong(length.toLong())
					readStreet(
						s, null, loadBuildings, x shr 7, y shr 7, if (city.isPostcode()) city.getName() else null,
						attributeTagsTable
					)
					publishRawData(resultMatcher, s)
					if (resultMatcher == null || resultMatcher.publish(s)) {
						city.registerStreet(s)
					}
					if (resultMatcher != null && resultMatcher.isCancelled()) {
						codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
					}
					codedIS.popLimit(oldLimit)
				}
				CityBlockIndex.BUILDINGS_FIELD_NUMBER -> {
					// buildings for the town are not used now
					// java falls through to its default branch here, so the field is skipped twice
					skipUnknownField(t)
					skipUnknownField(t)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun interface CityMatcher {
		fun matches(city: City): Boolean
	}

	private class DefaultCityMatcher(private val stringMatcher: KStringMatcher?) : CityMatcher {

		override fun matches(city: City): Boolean {
			if (stringMatcher == null) {
				return true
			}
			var matches = stringMatcher.matches(city.getName())
			if (!matches) {
				for (n in city.getOtherNames()) {
					matches = stringMatcher.matches(n)
					if (matches) {
						break
					}
				}
			}
			return matches
		}
	}

	internal fun readCityHeader(
		resultMatcher: SearchRequest<in City>?, matcher: CityMatcher?, filePointer: Long,
		additionalTagsTable: List<String>?
	): City? {
		var x = 0
		var y = 0
		var c: City? = null
		var additionalTags: ArrayDeque<String>? = null
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					if (c != null) {
						c.setLocation(KMapUtils.get31LatitudeY(y), KMapUtils.get31LongitudeX(x))
						publishRawData(resultMatcher, c)
					}
					return if (c != null && (matcher == null || matcher.matches(c))) c else null
				}
				CityIndex.CITY_TYPE_FIELD_NUMBER -> {
					val type = codedIS.readUInt32()
					val vls = CityType.entries
					if (type <= CityType.POSTCODE.ordinal) {
						c = City(vls[type])
					}
					// Since 5.2 we skip unsupported city types c == null
				}
				CityIndex.ID_FIELD_NUMBER -> {
					val id = codedIS.readUInt64()
					c?.setId(id)
				}
				CityIndex.ATTRIBUTETAGIDS_FIELD_NUMBER -> {
					val tgid = codedIS.readUInt32()
					if (additionalTags == null) {
						additionalTags = ArrayDeque()
					}
					if (additionalTagsTable != null && tgid < additionalTagsTable.size) {
						additionalTags.addLast(additionalTagsTable[tgid])
					}
				}
				CityIndex.ATTRIBUTEVALUES_FIELD_NUMBER -> {
					val nm = codedIS.readString()
					if (c != null && additionalTags != null && additionalTags.size > 0) {
						val tg = additionalTags.removeFirst()
						if (tg.startsWith("name:")) {
							c.setName(tg.substring("name:".length), nm)
						}
					}
				}
				CityIndex.NAME_EN_FIELD_NUMBER -> {
					val enName = codedIS.readString()
					c?.setEnName(enName)
				}
				CityIndex.BOUNDARY_FIELD_NUMBER -> {
					val size = codedIS.readRawVarint32()
					val old = codedIS.pushLimitLong(size.toLong())
					val lst = KTIntArrayList()
					while (codedIS.getBytesUntilLimit() > 0) {
						lst.add(codedIS.readRawVarint32())
					}
					codedIS.popLimit(old)
					c?.setBbox31(lst.toArray())
				}
				CityIndex.NAME_FIELD_NUMBER -> {
					val name = codedIS.readString()
					if (c == null) {
						// TODO should be deleted in 5.3 (as server side assigns 6)
						c = City.createPostcode(name)
					}
					c.setName(name)
				}
				CityIndex.X_FIELD_NUMBER -> x = codedIS.readUInt32()
				CityIndex.Y_FIELD_NUMBER -> y = codedIS.readUInt32()
				CityIndex.SHIFTTOCITYBLOCKINDEX_FIELD_NUMBER -> {
					var offset = readInt()
					offset += filePointer
					c?.setFileOffset(offset)
				}
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readStreet(
		s: Street, buildingsMatcher: SearchRequest<Building>?, loadBuildingsAndIntersected: Boolean,
		city24X: Int, city24Y: Int, postcodeFilter: String?, additionalTagsTable: List<String>?
	): Street {
		var x = 0
		var y = 0
		var additionalTags: ArrayDeque<String>? = null
		val loadLocation = city24X != 0 || city24Y != 0
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					if (loadLocation) {
						s.setLocation(
							KMapUtils.getLatitudeFromTile(24.0, y.toDouble()),
							KMapUtils.getLongitudeFromTile(24.0, x.toDouble())
						)
					}
					return s
				}
				StreetIndex.ID_FIELD_NUMBER -> s.setId(codedIS.readUInt64())
				StreetIndex.ATTRIBUTETAGIDS_FIELD_NUMBER -> {
					val tgid = codedIS.readUInt32()
					if (additionalTags == null) {
						additionalTags = ArrayDeque()
					}
					if (additionalTagsTable != null && tgid < additionalTagsTable.size) {
						additionalTags.addLast(additionalTagsTable[tgid])
					}
				}
				StreetIndex.ATTRIBUTEVALUES_FIELD_NUMBER -> {
					val nm = codedIS.readString()
					if (additionalTags != null && additionalTags.size > 0) {
						val tg = additionalTags.removeFirst()
						if (tg.startsWith("name:")) {
							s.setName(tg.substring("name:".length), nm)
						}
					}
				}
				StreetIndex.NAME_EN_FIELD_NUMBER -> s.setEnName(codedIS.readString())
				StreetIndex.NAME_FIELD_NUMBER -> s.setName(codedIS.readString())
				StreetIndex.X_FIELD_NUMBER -> {
					val sx = codedIS.readSInt32()
					x = if (loadLocation) {
						sx + city24X
					} else {
						KMapUtils.getTileNumberX(24.0, s.getLocation()!!.longitude).toInt()
					}
				}
				StreetIndex.Y_FIELD_NUMBER -> {
					val sy = codedIS.readSInt32()
					y = if (loadLocation) {
						sy + city24Y
					} else {
						KMapUtils.getTileNumberY(24.0, s.getLocation()!!.latitude).toInt()
					}
				}
				StreetIndex.INTERSECTIONS_FIELD_NUMBER -> {
					val length = codedIS.readRawVarint32().toLong()
					if (loadBuildingsAndIntersected) {
						val oldLimit = codedIS.pushLimitLong(length)
						val si = readIntersectedStreet(s.getCity(), x, y, additionalTagsTable)
						s.addIntersectedStreet(si)
						codedIS.popLimit(oldLimit)
					} else {
						codedIS.skipRawBytes(length)
					}
				}
				StreetIndex.BUILDINGS_FIELD_NUMBER -> {
					val offset = codedIS.getTotalBytesRead()
					val length = codedIS.readRawVarint32().toLong()
					if (loadBuildingsAndIntersected) {
						val oldLimit = codedIS.pushLimitLong(length)
						val b = readBuilding(offset, x, y, additionalTagsTable)
						publishRawData(buildingsMatcher, b)
						if (postcodeFilter == null || postcodeFilter.equals(b.getPostcode(), ignoreCase = true)) {
							if (buildingsMatcher == null || buildingsMatcher.publish(b)) {
								s.addBuilding(b)
							}
						}
						codedIS.popLimit(oldLimit)
					} else {
						codedIS.skipRawBytes(length)
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readIntersectedStreet(
		c: City?, street24X: Int, street24Y: Int, additionalTagsTable: List<String>?
	): Street {
		var x = 0
		var y = 0
		val s = Street(c)
		var additionalTags: ArrayDeque<String>? = null
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					s.setLocation(
						KMapUtils.getLatitudeFromTile(24.0, y.toDouble()),
						KMapUtils.getLongitudeFromTile(24.0, x.toDouble())
					)
					return s
				}
				// java reads the id under the field number of BuildingIndex.id
				BuildingIndex.ID_FIELD_NUMBER -> s.setId(codedIS.readUInt64())
				StreetIntersection.NAME_EN_FIELD_NUMBER -> s.setEnName(codedIS.readString())
				StreetIntersection.NAME_FIELD_NUMBER -> s.setName(codedIS.readString())
				StreetIntersection.ATTRIBUTETAGIDS_FIELD_NUMBER -> {
					val tgid = codedIS.readUInt32()
					if (additionalTags == null) {
						additionalTags = ArrayDeque()
					}
					if (additionalTagsTable != null && tgid < additionalTagsTable.size) {
						additionalTags.addLast(additionalTagsTable[tgid])
					}
				}
				StreetIntersection.ATTRIBUTEVALUES_FIELD_NUMBER -> {
					val nm = codedIS.readString()
					if (additionalTags != null && additionalTags.size > 0) {
						val tg = additionalTags.removeFirst()
						if (tg.startsWith("name:")) {
							s.setName(tg.substring("name:".length), nm)
						}
					}
				}
				StreetIntersection.INTERSECTEDX_FIELD_NUMBER -> x = codedIS.readSInt32() + street24X
				StreetIntersection.INTERSECTEDY_FIELD_NUMBER -> y = codedIS.readSInt32() + street24Y
				else -> skipUnknownField(t)
			}
		}
	}

	internal fun readBuilding(
		fileOffset: Long, street24X: Int, street24Y: Int, additionalTagsTable: List<String>?
	): Building {
		var x = 0
		var y = 0
		var x2 = 0
		var y2 = 0
		var additionalTags: ArrayDeque<String>? = null
		val b = Building()
		b.setFileOffset(fileOffset)
		while (true) {
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> {
					b.setLocation(
						KMapUtils.getLatitudeFromTile(24.0, y.toDouble()),
						KMapUtils.getLongitudeFromTile(24.0, x.toDouble())
					)
					if (x2 != 0 && y2 != 0) {
						b.setLatLon2(
							KLatLon(
								KMapUtils.getLatitudeFromTile(24.0, y2.toDouble()),
								KMapUtils.getLongitudeFromTile(24.0, x2.toDouble())
							)
						)
					}
					return b
				}
				BuildingIndex.ID_FIELD_NUMBER -> b.setId(codedIS.readUInt64())
				BuildingIndex.NAME_EN_FIELD_NUMBER -> b.setEnName(codedIS.readString())
				BuildingIndex.NAME_FIELD_NUMBER -> b.setName(codedIS.readString())
				BuildingIndex.ATTRIBUTETAGIDS_FIELD_NUMBER -> {
					val tgid = codedIS.readUInt32()
					if (additionalTags == null) {
						additionalTags = ArrayDeque()
					}
					if (additionalTagsTable != null && tgid < additionalTagsTable.size) {
						additionalTags.addLast(additionalTagsTable[tgid])
					}
				}
				BuildingIndex.ATTRIBUTEVALUES_FIELD_NUMBER -> {
					val nm = codedIS.readString()
					if (additionalTags != null && additionalTags.size > 0) {
						val tg = additionalTags.removeFirst()
						if (tg.startsWith("name:")) {
							b.setName(tg.substring("name:".length), nm)
						}
					}
				}
				// no where to set now
				BuildingIndex.NAME_EN2_FIELD_NUMBER -> codedIS.readString()
				BuildingIndex.NAME2_FIELD_NUMBER -> b.setName2(codedIS.readString())
				BuildingIndex.INTERPOLATION_FIELD_NUMBER -> {
					val sint = codedIS.readSInt32()
					if (sint > 0) {
						b.setInterpolationInterval(sint)
					} else {
						b.setInterpolationType(Building.BuildingInterpolation.fromValue(sint))
					}
				}
				BuildingIndex.X_FIELD_NUMBER -> x = codedIS.readSInt32() + street24X
				BuildingIndex.X2_FIELD_NUMBER -> x2 = codedIS.readSInt32() + street24X
				BuildingIndex.Y_FIELD_NUMBER -> y = codedIS.readSInt32() + street24Y
				BuildingIndex.Y2_FIELD_NUMBER -> y2 = codedIS.readSInt32() + street24Y
				BuildingIndex.POSTCODE_FIELD_NUMBER -> b.setPostcode(codedIS.readString())
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * The settlements and streets of [reg] whose name matches the request's query. The string table
	 * of the name index gives the prefixes the query can sit under, the entries under each prefix
	 * give the offsets of the candidates by block type, and the candidates are then read in file
	 * order and matched again by every name they carry.
	 */
	internal fun searchAddressDataByName(
		reg: AddressRegion, req: SearchRequest<MapObject>, typeFilter: List<CityBlocks>?
	) {
		val types = typeFilter ?: CityBlocks.allTypes()
		val nameQuery = req.nameQuery ?: throw NullPointerException()
		val loffsets = KTIntArrayList()
		val stringMatcher = KCollatorStringMatcher(nameQuery, req.matcherMode)
		var queryToken: QueryToken? = null
		val postcode = Postcode.normalize(nameQuery, map.getCountryName())
		val postcodeMatcher = DefaultCityMatcher(KCollatorStringMatcher(postcode, req.matcherMode))
		val cityMatcher = DefaultCityMatcher(stringMatcher)
		val cityPostcodeMatcher = CityMatcher { city ->
			if (city.isPostcode()) postcodeMatcher.matches(city) else cityMatcher.matches(city)
		}
		var indexOffset = 0L
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			when (CodedInputStream.getTagFieldNumber(t)) {
				0 -> return
				OsmAndAddressNameIndexData.TABLE_FIELD_NUMBER -> {
					val length = readInt()
					indexOffset = codedIS.getTotalBytesRead()
					val oldLimit = codedIS.pushLimitLong(length)
					val prefixCandidates = map.readIndexedStringTablePrefixes(listOf(nameQuery))[0]
					val token = QueryToken(nameQuery, req.matcherMode, prefixCandidates)
					queryToken = token
					val uniqueOffsets = HashSet<Int>()
					for (prefix in token.prefixes) {
						if (uniqueOffsets.add(prefix.offset)) {
							loffsets.add(prefix.offset)
						}
					}
					codedIS.popLimit(oldLimit)
				}
				OsmAndAddressNameIndexData.ATOM_FIELD_NUMBER -> {
					val refs = Array(CityBlocks.STREET_TYPE.index + 1) { KTIntArrayList() }
					val refsToCities = Array(CityBlocks.STREET_TYPE.index + 1) { KTIntArrayList() }

					for (j in 0 until loffsets.size()) {
						val fp = indexOffset + loffsets[j]
						codedIS.seek(fp)
						val len = codedIS.readRawVarint32()
						val oldLim = codedIS.pushLimitLong(len.toLong())
						var matchedPrefix: QueryToken.Prefix? = null
						if (queryToken != null) {
							for (prefix in queryToken.prefixes) {
								if (prefix.offset == loffsets[j]) {
									matchedPrefix = prefix
									break
								}
							}
						}
						var suffixDictionaryInitialized = false
						var suffixDictionary: MutableList<String>? = null
						var suffixMask: QueryToken.SuffixMask? = null
						if (queryToken != null && matchedPrefix != null) {
							suffixMask = queryToken.SuffixMask(matchedPrefix)
						}
						var stag = 0
						var emptySuffixes = false
						loopAtoms@ do {
							val st = codedIS.readTag()
							stag = CodedInputStream.getTagFieldNumber(st)
							if (stag == AddressNameIndexData.SUFFIXESDICTIONARY_FIELD_NUMBER) {
								var dictionary = suffixDictionary
								if (dictionary == null) {
									dictionary = ArrayList()
									suffixDictionary = dictionary
								}
								val encodedSuffix = codedIS.readString()
								if (KSearchAlgorithms.OLD_EMPTY_SUFFIX_DICTIONARY_SENTINEL == encodedSuffix) {
									emptySuffixes = true
									continue@loopAtoms
								}
								val previousSuffix = if (dictionary.isEmpty()) null else dictionary[dictionary.size - 1]
								val decodedSuffix = KSearchAlgorithms.nameIndexDecodeDictionarySuffix(previousSuffix, encodedSuffix)
								dictionary.add(decodedSuffix)
							} else if (stag == AddressNameIndexData.ATOM_FIELD_NUMBER) {
								val dictionary = suffixDictionary
								if (emptySuffixes || (dictionary != null && dictionary.size == 1 &&
											dictionary[0] == KSearchAlgorithms.EMPTY_SUFFIX_DICTIONARY_SENTINEL)
								) {
									if (matchedPrefix != null && queryToken != null &&
										!queryToken.matchFullPrefix(matchedPrefix.key)
									) {
										codedIS.skipRawBytes(codedIS.getBytesUntilLimit())
										break@loopAtoms
									}
								}
								if (!suffixDictionaryInitialized && suffixMask != null) {
									suffixMask.setDictionary(suffixDictionary)
									suffixDictionaryInitialized = true
								}
								val slen = codedIS.readRawVarint32()
								val soldLim = codedIS.pushLimitLong(slen.toLong())
								readAddressNameData(req, refs, refsToCities, fp, suffixMask)
								codedIS.popLimit(soldLim)
							} else if (stag != 0) {
								skipUnknownField(st)
							}
						} while (stag != 0 && !req.isCancelled())

						codedIS.popLimit(oldLim)
						if (req.isCancelled()) {
							return
						}
					}

					for (block in types) {
						if (req.isCancelled()) {
							break
						}
						val list = refs[block.index]
						val listCities = refsToCities[block.index]
						if (block == CityBlocks.STREET_TYPE) {
							val streetGroups = HashMap<Int, City?>()
							val sortedCities = KTIntArrayList(listCities.toArray())
							sortedCities.sort()
							var j = 0
							while (j < sortedCities.size() && !req.isCancelled()) {
								val offset = sortedCities[j]
								if (j > 0 && offset == sortedCities[j - 1]) {
									j++
									continue
								}
								codedIS.seek(offset.toLong())
								val len = codedIS.readRawVarint32()
								val old = codedIS.pushLimitLong(len.toLong())
								val obj = readCityHeader(req, null, offset.toLong(), reg.attributeTagsTable)
								codedIS.popLimit(old)
								streetGroups[offset] = obj
								j++
							}
							for (k in 0 until list.size()) {
								streetGroups[list[k]] = streetGroups[listCities[k]]
							}
							list.sort()
							j = 0
							while (j < list.size() && !req.isCancelled()) {
								val offset = list[j]
								if (j > 0 && offset == list[j - 1]) {
									j++
									continue
								}
								val obj = streetGroups[offset]
								if (obj != null) {
									codedIS.seek(offset.toLong())
									val len = codedIS.readRawVarint32()
									val old = codedIS.pushLimitLong(len.toLong())
									val l = obj.getLocation()!!
									val s = Street(obj)
									s.setFileOffset(offset.toLong())
									readStreet(
										s, null, false, KMapUtils.get31TileNumberX(l.longitude) shr 7,
										KMapUtils.get31TileNumberY(l.latitude) shr 7,
										if (obj.isPostcode()) obj.getName() else null, reg.attributeTagsTable
									)
									publishRawData(req, s)
									var matches = stringMatcher.matches(s.getName())
									if (!matches) {
										for (n in s.getOtherNames()) {
											matches = stringMatcher.matches(n)
											if (matches) {
												break
											}
										}
									}
									if (matches) {
										req.publish(s)
									}
									codedIS.popLimit(old)
								}
								j++
							}
						} else {
							list.sort()
							val published = HashSet<Int>()
							var j = 0
							while (j < list.size() && !req.isCancelled()) {
								val offset = list[j]
								if (j > 0 && offset == list[j - 1]) {
									j++
									continue
								}
								codedIS.seek(offset.toLong())
								val len = codedIS.readRawVarint32()
								val old = codedIS.pushLimitLong(len.toLong())
								val obj = readCityHeader(req, cityPostcodeMatcher, list[j].toLong(), reg.attributeTagsTable)
								publishRawData(req, obj)
								if (obj != null && !published.contains(offset)) {
									req.publish(obj)
									published.add(offset)
								}
								codedIS.popLimit(old)
								j++
							}
						}
					}
					return
				}
				else -> skipUnknownField(t)
			}
		}
	}

	/**
	 * One entry of the name index, which can name several objects: each takes the block type, box
	 * and offsets seen so far, and is let through by the bitset of the suffixes it was written for.
	 */
	private fun readAddressNameData(
		req: SearchRequest<MapObject>, refs: Array<KTIntArrayList>, refsToCities: Array<KTIntArrayList>,
		fp: Long, suffixMask: QueryToken.SuffixMask?
	) {
		var toAdd: KTIntArrayList? = null
		var toAddCity: KTIntArrayList? = null
		var shiftindex = 0
		var shiftcityindex = 0
		var add = true
		var matched = false
		var noBisetIndex = true
		var maskIndex = 0
		while (true) {
			if (req.isCancelled()) {
				return
			}
			val t = codedIS.readTag()
			val tag = CodedInputStream.getTagFieldNumber(t)
			if (tag == 0 || tag == AddressNameIndexDataAtom.SHIFTTOINDEX_FIELD_NUMBER) {
				if (suffixMask != null && suffixMask.shouldPassThrough() || noBisetIndex) {
					// intermediate version ignore
					matched = true
				}
				if (toAdd != null && add && matched) {
					if (shiftindex != 0) {
						toAdd.add(shiftindex)
					}
					if (shiftcityindex != 0) {
						toAddCity?.add(shiftcityindex)
					}
				}
			}
			when (tag) {
				0 -> return
				AddressNameIndexDataAtom.SUFFIXESBITSETINDEX_FIELD_NUMBER -> {
					noBisetIndex = false
					val mask = codedIS.readUInt32()
					if (!matched && suffixMask != null && suffixMask.isMatched(maskIndex, mask)) {
						matched = true
					}
					maskIndex++
				}
				AddressNameIndexDataAtom.SHIFTTOCITYINDEX_FIELD_NUMBER -> {
					if (toAddCity != null) {
						shiftcityindex = (fp - codedIS.readInt32()).toInt()
					}
				}
				AddressNameIndexDataAtom.XY16_FIELD_NUMBER -> {
					val in32 = codedIS.readInt32()
					val x16 = (in32 ushr 16) shl 15
					val y16 = (in32 and ((1 shl 16) - 1)) shl 15
					add = !req.isBboxSpecified() || req.contains(x16, y16, x16, y16)
				}
				AddressNameIndexDataAtom.SHIFTTOINDEX_FIELD_NUMBER -> shiftindex = (fp - codedIS.readInt32()).toInt()
				AddressNameIndexDataAtom.TYPE_FIELD_NUMBER -> {
					val type = codedIS.readInt32()
					if (type >= 0 && type < refs.size) {
						toAdd = refs[type]
						toAddCity = refsToCities[type]
					}
				}
				else -> skipUnknownField(t)
			}
		}
	}

	private fun <T> publishRawData(resultMatcher: SearchRequest<in T>?, obj: T?) {
		if (resultMatcher != null && obj != null) {
			resultMatcher.collectRawData(obj)
		}
	}

	companion object {
		/** The field of `OsmAndAddressIndex` a block of settlements occupies; [CitiesBlock.getFieldNumber]. */
		const val CITIES_FIELD_NUMBER = 6
	}

	/** Field numbers of the address section messages in osmand_odb.proto, frozen by the obf format. */
	private object OsmAndAddressIndex {
		const val NAME_FIELD_NUMBER = 1
		const val NAME_EN_FIELD_NUMBER = 2
		const val BOUNDARIES_FIELD_NUMBER = 3
		const val ATTRIBUTETAGSTABLE_FIELD_NUMBER = 4
		const val CITIES_FIELD_NUMBER = BinaryMapAddressReaderAdapter.CITIES_FIELD_NUMBER
		const val NAMEINDEX_FIELD_NUMBER = 7
	}

	private object OsmAndTileBox {
		const val LEFT_FIELD_NUMBER = 1
		const val RIGHT_FIELD_NUMBER = 2
		const val TOP_FIELD_NUMBER = 3
		const val BOTTOM_FIELD_NUMBER = 4
	}

	/** `OsmAndAddressIndex.CitiesIndex`. */
	private object CitiesIndex {
		const val TYPE_FIELD_NUMBER = 2
		const val CITIES_FIELD_NUMBER = 5
	}

	private object CityIndex {
		const val CITY_TYPE_FIELD_NUMBER = 1
		const val NAME_FIELD_NUMBER = 2
		const val NAME_EN_FIELD_NUMBER = 3
		const val ID_FIELD_NUMBER = 4
		const val X_FIELD_NUMBER = 5
		const val Y_FIELD_NUMBER = 6
		const val ATTRIBUTETAGIDS_FIELD_NUMBER = 7
		const val ATTRIBUTEVALUES_FIELD_NUMBER = 8
		const val SHIFTTOCITYBLOCKINDEX_FIELD_NUMBER = 10
		const val BOUNDARY_FIELD_NUMBER = 12
	}

	private object CityBlockIndex {
		const val BUILDINGS_FIELD_NUMBER = 10
		const val STREETS_FIELD_NUMBER = 12
	}

	private object StreetIndex {
		const val NAME_FIELD_NUMBER = 1
		const val NAME_EN_FIELD_NUMBER = 2
		const val X_FIELD_NUMBER = 3
		const val Y_FIELD_NUMBER = 4
		const val INTERSECTIONS_FIELD_NUMBER = 5
		const val ID_FIELD_NUMBER = 6
		const val ATTRIBUTETAGIDS_FIELD_NUMBER = 7
		const val ATTRIBUTEVALUES_FIELD_NUMBER = 8
		const val BUILDINGS_FIELD_NUMBER = 12
	}

	private object StreetIntersection {
		const val NAME_FIELD_NUMBER = 2
		const val NAME_EN_FIELD_NUMBER = 3
		const val INTERSECTEDX_FIELD_NUMBER = 4
		const val INTERSECTEDY_FIELD_NUMBER = 5
		const val ATTRIBUTETAGIDS_FIELD_NUMBER = 7
		const val ATTRIBUTEVALUES_FIELD_NUMBER = 8
	}

	private object BuildingIndex {
		const val NAME_FIELD_NUMBER = 1
		const val NAME_EN_FIELD_NUMBER = 2
		const val NAME2_FIELD_NUMBER = 3
		const val NAME_EN2_FIELD_NUMBER = 4
		const val INTERPOLATION_FIELD_NUMBER = 5
		const val X_FIELD_NUMBER = 7
		const val Y_FIELD_NUMBER = 8
		const val X2_FIELD_NUMBER = 9
		const val Y2_FIELD_NUMBER = 10
		const val ID_FIELD_NUMBER = 13
		const val POSTCODE_FIELD_NUMBER = 14
		const val ATTRIBUTETAGIDS_FIELD_NUMBER = 15
		const val ATTRIBUTEVALUES_FIELD_NUMBER = 16
	}

	private object OsmAndAddressNameIndexData {
		const val TABLE_FIELD_NUMBER = 4
		const val ATOM_FIELD_NUMBER = 7
	}

	/** `OsmAndAddressNameIndexData.AddressNameIndexData`. */
	private object AddressNameIndexData {
		const val SUFFIXESDICTIONARY_FIELD_NUMBER = 2
		const val ATOM_FIELD_NUMBER = 4
	}

	private object AddressNameIndexDataAtom {
		const val TYPE_FIELD_NUMBER = 3
		const val SHIFTTOINDEX_FIELD_NUMBER = 5
		const val SHIFTTOCITYINDEX_FIELD_NUMBER = 6
		const val XY16_FIELD_NUMBER = 7
		const val SUFFIXESBITSETINDEX_FIELD_NUMBER = 8
	}
}
