package net.osmand.shared.binary

import net.osmand.shared.util.collections.KTIntArrayList

/*
 * The messages of the name index of an obf file that a name index reader keeps whole: the common
 * words of an index, and the block of atoms of one prefix of the address or the poi name index.
 *
 * Copies of the classes protobuf generates from OBF.proto into `OsmandOdb` in OsmAnd-java, with the
 * same names and getters, so that the code reading them can be compared line by line. Shared has no
 * generated protobuf code, so they read their fields by hand, as the readers do, and as the
 * generated parser does: repeated numbers packed or not, a known field with another wire type and
 * unknown fields skipped, and an address atom without its required type fails the parse of its
 * block. [AddressNameIndexData.getSerializedSize] is the length the block was read from, which the
 * generated class computes back from its fields.
 */

private val EMPTY_INTS = IntArray(0)

private val EMPTY_BYTES = ByteArray(0)

/** The numbers of a repeated field: one varint, or a packed run of them. */
private fun readInts(codedIS: CodedInputStream, tag: Int, out: KTIntArrayList?): KTIntArrayList {
	val list = out ?: KTIntArrayList()
	if (CodedInputStream.getTagWireType(tag) == CodedInputStream.WIRETYPE_LENGTH_DELIMITED) {
		val length = codedIS.readRawVarint32()
		val oldLimit = codedIS.pushLimitLong(length.toLong())
		while (codedIS.getBytesUntilLimit() > 0) {
			list.add(codedIS.readRawVarint32())
		}
		codedIS.popLimit(oldLimit)
	} else {
		list.add(codedIS.readRawVarint32())
	}
	return list
}

/** A `fixed32` field: four bytes, low byte first. */
private fun readFixed32(codedIS: CodedInputStream): Int {
	val b1 = codedIS.readRawByte().toInt() and 0xff
	val b2 = codedIS.readRawByte().toInt() and 0xff
	val b3 = codedIS.readRawByte().toInt() and 0xff
	val b4 = codedIS.readRawByte().toInt() and 0xff
	return b1 or (b2 shl 8) or (b3 shl 16) or (b4 shl 24)
}

private fun KTIntArrayList?.toInts(): IntArray = if (this == null) EMPTY_INTS else toArray()

/** The message in [data], which holds it whole, as a raw block of a name index does. */
private inline fun <T> parseBytes(data: ByteArray, parse: (CodedInputStream) -> T): T {
	val handle = ByteArrayFileHandle(data)
	val codedIS = CodedInputStream(handle, maxOf(1, data.size))
	codedIS.pushLimitLong(data.size.toLong())
	return parse(codedIS)
}

/** `OsmandOdb.CommonIndexedStats`: the common and frequent words of a name index, delta encoded. */
class CommonIndexedStats private constructor() {

	private var value: List<String> = emptyList()
	private var matched: IntArray = EMPTY_INTS
	private var nonindexed: IntArray = EMPTY_INTS

	fun getValueList(): List<String> = value

	fun getValueCount(): Int = value.size

	fun getValue(index: Int): String = value[index]

	fun getMatchedList(): List<Int> = matched.asList()

	fun getMatchedCount(): Int = matched.size

	fun getMatched(index: Int): Int = matched[index]

	fun getNonindexedList(): List<Int> = nonindexed.asList()

	fun getNonindexedCount(): Int = nonindexed.size

	fun getNonindexed(index: Int): Int = nonindexed[index]

	companion object {
		internal const val VALUE_FIELD_NUMBER = 4
		internal const val MATCHED_FIELD_NUMBER = 5
		internal const val NONINDEXED_FIELD_NUMBER = 6

		/** Reads the fields up to the current limit of [codedIS]. */
		fun parseFrom(codedIS: CodedInputStream): CommonIndexedStats {
			val res = CommonIndexedStats()
			var value: MutableList<String>? = null
			var matched: KTIntArrayList? = null
			var nonindexed: KTIntArrayList? = null
			while (true) {
				val tag = codedIS.readTag()
				when (tag) {
					0 -> break
					34 -> (value ?: ArrayList<String>().also { value = it }).add(codedIS.readString())
					40, 42 -> matched = readInts(codedIS, tag, matched)
					48, 50 -> nonindexed = readInts(codedIS, tag, nonindexed)
					else -> codedIS.skipField(tag)
				}
			}
			res.value = value.orEmpty()
			res.matched = matched.toInts()
			res.nonindexed = nonindexed.toInts()
			return res
		}
	}
}

/**
 * `OsmandOdb.OsmAndAddressNameIndexData.AddressNameIndexData`: the block of one prefix of the
 * address name index, the dictionary of the suffixes its names continue with and its atoms.
 */
class AddressNameIndexData private constructor() {

	private var atomsLength = 0
	private var hasAtomsLength = false
	private var suffixesDictionary: List<String> = emptyList()
	private var suffixesCommonDictionary: IntArray = EMPTY_INTS
	private var atom: List<AddressNameIndexDataAtom> = emptyList()
	private var serializedSize = 0

	fun hasAtomsLength(): Boolean = hasAtomsLength

	fun getAtomsLength(): Int = atomsLength

	fun getSuffixesDictionaryList(): List<String> = suffixesDictionary

	fun getSuffixesDictionaryCount(): Int = suffixesDictionary.size

	fun getSuffixesDictionary(index: Int): String = suffixesDictionary[index]

	fun getSuffixesCommonDictionaryList(): List<Int> = suffixesCommonDictionary.asList()

	fun getSuffixesCommonDictionaryCount(): Int = suffixesCommonDictionary.size

	fun getSuffixesCommonDictionary(index: Int): Int = suffixesCommonDictionary[index]

	fun getAtomList(): List<AddressNameIndexDataAtom> = atom

	fun getAtomCount(): Int = atom.size

	fun getAtom(index: Int): AddressNameIndexDataAtom = atom[index]

	fun getSerializedSize(): Int = serializedSize

	companion object {
		internal const val SUFFIXESDICTIONARY_FIELD_NUMBER = 2
		internal const val SUFFIXESCOMMONDICTIONARY_FIELD_NUMBER = 3
		internal const val ATOM_FIELD_NUMBER = 4
		internal const val ATOMSLENGTH_FIELD_NUMBER = 7

		/** Reads the fields up to the current limit of [codedIS]. */
		fun parseFrom(codedIS: CodedInputStream): AddressNameIndexData {
			val res = AddressNameIndexData()
			val start = codedIS.getTotalBytesRead()
			var suffixes: MutableList<String>? = null
			var common: KTIntArrayList? = null
			var atoms: MutableList<AddressNameIndexDataAtom>? = null
			while (true) {
				val tag = codedIS.readTag()
				when (tag) {
					0 -> break
					18 -> (suffixes ?: ArrayList<String>().also { suffixes = it }).add(codedIS.readString())
					24, 26 -> common = readInts(codedIS, tag, common)
					34 -> {
						val length = codedIS.readRawVarint32()
						val oldLimit = codedIS.pushLimitLong(length.toLong())
						(atoms ?: ArrayList<AddressNameIndexDataAtom>().also { atoms = it })
							.add(AddressNameIndexDataAtom.parseFrom(codedIS))
						codedIS.popLimit(oldLimit)
					}
					56 -> {
						res.atomsLength = codedIS.readRawVarint32()
						res.hasAtomsLength = true
					}
					else -> codedIS.skipField(tag)
				}
			}
			res.suffixesDictionary = suffixes.orEmpty()
			res.suffixesCommonDictionary = common.toInts()
			res.atom = atoms.orEmpty()
			res.serializedSize = (codedIS.getTotalBytesRead() - start).toInt()
			for (i in res.atom.indices) {
				if (!res.atom[i].hasType()) {
					throw IllegalStateException("Message missing required fields: atom[$i].type")
				}
			}
			return res
		}

		fun parseFrom(data: ByteArray): AddressNameIndexData = parseBytes(data) { parseFrom(it) }
	}
}

/** `OsmandOdb.AddressNameIndexDataAtom`: one object of the address name index under a prefix. */
class AddressNameIndexDataAtom private constructor() {

	private var type = 0
	private var hasType = false
	private var suffixesBitsetIndex: IntArray = EMPTY_INTS
	private var extraSuffix: List<String> = emptyList()
	private var otherWordsCount: IntArray = EMPTY_INTS
	private var bbox: ByteArray = EMPTY_BYTES
	private var hasBbox = false
	private var enclosingObjects = 0
	private var hasEnclosingObjects = false
	private var shiftToIndex: IntArray = EMPTY_INTS
	private var shiftToCityIndex: IntArray = EMPTY_INTS
	private var xy16: IntArray = EMPTY_INTS

	fun hasType(): Boolean = hasType

	/** 0 boundary, 1 city, 2 postcode, 3 village, 4 street: the index of `CityBlocks`. */
	fun getType(): Int = type

	fun getSuffixesBitsetIndexList(): List<Int> = suffixesBitsetIndex.asList()

	fun getSuffixesBitsetIndexCount(): Int = suffixesBitsetIndex.size

	fun getSuffixesBitsetIndex(index: Int): Int = suffixesBitsetIndex[index]

	fun getExtraSuffixList(): List<String> = extraSuffix

	fun getExtraSuffixCount(): Int = extraSuffix.size

	fun getExtraSuffix(index: Int): String = extraSuffix[index]

	fun getOtherWordsCountList(): List<Int> = otherWordsCount.asList()

	fun getOtherWordsCountCount(): Int = otherWordsCount.size

	fun getOtherWordsCount(index: Int): Int = otherWordsCount[index]

	fun hasBbox(): Boolean = hasBbox

	/** The bytes of the field, empty when it is not set, as the generated getter returns. */
	fun getBbox(): ByteArray = bbox

	fun hasEnclosingObjects(): Boolean = hasEnclosingObjects

	fun getEnclosingObjects(): Int = enclosingObjects

	fun getShiftToIndexList(): List<Int> = shiftToIndex.asList()

	fun getShiftToIndexCount(): Int = shiftToIndex.size

	fun getShiftToIndex(index: Int): Int = shiftToIndex[index]

	fun getShiftToCityIndexList(): List<Int> = shiftToCityIndex.asList()

	fun getShiftToCityIndexCount(): Int = shiftToCityIndex.size

	fun getShiftToCityIndex(index: Int): Int = shiftToCityIndex[index]

	fun getXy16List(): List<Int> = xy16.asList()

	fun getXy16Count(): Int = xy16.size

	fun getXy16(index: Int): Int = xy16[index]

	companion object {
		internal const val TYPE_FIELD_NUMBER = 3
		internal const val SHIFTTOINDEX_FIELD_NUMBER = 5
		internal const val SHIFTTOCITYINDEX_FIELD_NUMBER = 6
		internal const val XY16_FIELD_NUMBER = 7
		internal const val SUFFIXESBITSETINDEX_FIELD_NUMBER = 8
		internal const val EXTRASUFFIX_FIELD_NUMBER = 9
		internal const val OTHERWORDSCOUNT_FIELD_NUMBER = 10
		internal const val BBOX_FIELD_NUMBER = 12
		internal const val ENCLOSINGOBJECTS_FIELD_NUMBER = 16

		/**
		 * Reads the fields up to the current limit of [codedIS]. The type may be missing here: the
		 * block it is part of checks its atoms, as the generated parser of the block does.
		 */
		internal fun parseFrom(codedIS: CodedInputStream): AddressNameIndexDataAtom {
			val res = AddressNameIndexDataAtom()
			var bits: KTIntArrayList? = null
			var extra: MutableList<String>? = null
			var other: KTIntArrayList? = null
			var shifts: KTIntArrayList? = null
			var cityShifts: KTIntArrayList? = null
			var xy: KTIntArrayList? = null
			while (true) {
				val tag = codedIS.readTag()
				when (tag) {
					0 -> break
					24 -> {
						res.type = codedIS.readRawVarint32()
						res.hasType = true
					}
					40, 42 -> shifts = readInts(codedIS, tag, shifts)
					48, 50 -> cityShifts = readInts(codedIS, tag, cityShifts)
					56, 58 -> xy = readInts(codedIS, tag, xy)
					64, 66 -> bits = readInts(codedIS, tag, bits)
					74 -> (extra ?: ArrayList<String>().also { extra = it }).add(codedIS.readString())
					80, 82 -> other = readInts(codedIS, tag, other)
					98 -> {
						res.bbox = codedIS.readBytes()
						res.hasBbox = true
					}
					128 -> {
						res.enclosingObjects = codedIS.readRawVarint32()
						res.hasEnclosingObjects = true
					}
					else -> codedIS.skipField(tag)
				}
			}
			res.suffixesBitsetIndex = bits.toInts()
			res.extraSuffix = extra.orEmpty()
			res.otherWordsCount = other.toInts()
			res.shiftToIndex = shifts.toInts()
			res.shiftToCityIndex = cityShifts.toInts()
			res.xy16 = xy.toInts()
			return res
		}
	}
}

/**
 * `OsmandOdb.OsmAndPoiNameIndex.OsmAndPoiNameIndexData`: the block of one prefix of the poi name
 * index, the dictionary of the suffixes its names continue with and its atoms.
 */
class OsmAndPoiNameIndexData private constructor() {

	private var atomsLength = 0
	private var hasAtomsLength = false
	private var suffixesDictionary: List<String> = emptyList()
	private var suffixesCommonDictionary: IntArray = EMPTY_INTS
	private var atoms: List<OsmAndPoiNameIndexDataAtom> = emptyList()
	private var serializedSize = 0

	fun hasAtomsLength(): Boolean = hasAtomsLength

	fun getAtomsLength(): Int = atomsLength

	fun getSuffixesDictionaryList(): List<String> = suffixesDictionary

	fun getSuffixesDictionaryCount(): Int = suffixesDictionary.size

	fun getSuffixesDictionary(index: Int): String = suffixesDictionary[index]

	fun getSuffixesCommonDictionaryList(): List<Int> = suffixesCommonDictionary.asList()

	fun getSuffixesCommonDictionaryCount(): Int = suffixesCommonDictionary.size

	fun getSuffixesCommonDictionary(index: Int): Int = suffixesCommonDictionary[index]

	fun getAtomsList(): List<OsmAndPoiNameIndexDataAtom> = atoms

	fun getAtomsCount(): Int = atoms.size

	fun getAtoms(index: Int): OsmAndPoiNameIndexDataAtom = atoms[index]

	fun getSerializedSize(): Int = serializedSize

	companion object {
		internal const val SUFFIXESCOMMONDICTIONARY_FIELD_NUMBER = 1
		internal const val SUFFIXESDICTIONARY_FIELD_NUMBER = 2
		internal const val ATOMS_FIELD_NUMBER = 3
		internal const val ATOMSLENGTH_FIELD_NUMBER = 7

		/** Reads the fields up to the current limit of [codedIS]. */
		fun parseFrom(codedIS: CodedInputStream): OsmAndPoiNameIndexData {
			val res = OsmAndPoiNameIndexData()
			val start = codedIS.getTotalBytesRead()
			var suffixes: MutableList<String>? = null
			var common: KTIntArrayList? = null
			var atoms: MutableList<OsmAndPoiNameIndexDataAtom>? = null
			while (true) {
				val tag = codedIS.readTag()
				when (tag) {
					0 -> break
					8, 10 -> common = readInts(codedIS, tag, common)
					18 -> (suffixes ?: ArrayList<String>().also { suffixes = it }).add(codedIS.readString())
					26 -> {
						val length = codedIS.readRawVarint32()
						val oldLimit = codedIS.pushLimitLong(length.toLong())
						(atoms ?: ArrayList<OsmAndPoiNameIndexDataAtom>().also { atoms = it })
							.add(OsmAndPoiNameIndexDataAtom.parseFrom(codedIS))
						codedIS.popLimit(oldLimit)
					}
					56 -> {
						res.atomsLength = codedIS.readRawVarint32()
						res.hasAtomsLength = true
					}
					else -> codedIS.skipField(tag)
				}
			}
			res.suffixesDictionary = suffixes.orEmpty()
			res.suffixesCommonDictionary = common.toInts()
			res.atoms = atoms.orEmpty()
			res.serializedSize = (codedIS.getTotalBytesRead() - start).toInt()
			return res
		}

		fun parseFrom(data: ByteArray): OsmAndPoiNameIndexData = parseBytes(data) { parseFrom(it) }
	}
}

/** `OsmandOdb.OsmAndPoiNameIndexDataAtom`: the amenities of the poi name index under a prefix in one tile. */
class OsmAndPoiNameIndexDataAtom private constructor() {

	private var zoom = 0
	private var hasZoom = false
	private var x = 0
	private var hasX = false
	private var y = 0
	private var hasY = false
	private var suffixesBitsetIndex: IntArray = EMPTY_INTS
	private var extraSuffix: List<String> = emptyList()
	private var otherWordsCount: IntArray = EMPTY_INTS
	private var poiIndInBlock: IntArray = EMPTY_INTS
	private var bbox: ByteArray = EMPTY_BYTES
	private var hasBbox = false
	private var poiCategories: IntArray = EMPTY_INTS
	private var eloRating: IntArray = EMPTY_INTS
	private var shiftTo = 0
	private var hasShiftTo = false
	private var shiftToNextZ8Tile = 0
	private var hasShiftToNextZ8Tile = false
	private var shiftToNextZ10Tile = 0
	private var hasShiftToNextZ10Tile = false
	private var shiftToNextZ12Tile = 0
	private var hasShiftToNextZ12Tile = false

	fun hasZoom(): Boolean = hasZoom

	fun getZoom(): Int = zoom

	fun hasX(): Boolean = hasX

	fun getX(): Int = x

	fun hasY(): Boolean = hasY

	fun getY(): Int = y

	fun getSuffixesBitsetIndexList(): List<Int> = suffixesBitsetIndex.asList()

	fun getSuffixesBitsetIndexCount(): Int = suffixesBitsetIndex.size

	fun getSuffixesBitsetIndex(index: Int): Int = suffixesBitsetIndex[index]

	fun getExtraSuffixList(): List<String> = extraSuffix

	fun getExtraSuffixCount(): Int = extraSuffix.size

	fun getExtraSuffix(index: Int): String = extraSuffix[index]

	fun getOtherWordsCountList(): List<Int> = otherWordsCount.asList()

	fun getOtherWordsCountCount(): Int = otherWordsCount.size

	fun getOtherWordsCount(index: Int): Int = otherWordsCount[index]

	fun getPoiIndInBlockList(): List<Int> = poiIndInBlock.asList()

	fun getPoiIndInBlockCount(): Int = poiIndInBlock.size

	fun getPoiIndInBlock(index: Int): Int = poiIndInBlock[index]

	fun hasBbox(): Boolean = hasBbox

	/** The bytes of the field, empty when it is not set, as the generated getter returns. */
	fun getBbox(): ByteArray = bbox

	fun getPoiCategoriesList(): List<Int> = poiCategories.asList()

	fun getPoiCategoriesCount(): Int = poiCategories.size

	fun getPoiCategories(index: Int): Int = poiCategories[index]

	fun getEloRatingList(): List<Int> = eloRating.asList()

	fun getEloRatingCount(): Int = eloRating.size

	fun getEloRating(index: Int): Int = eloRating[index]

	fun hasShiftTo(): Boolean = hasShiftTo

	/** The field as protobuf reads a `fixed32`, low byte first; java's `BinaryMapIndexReader.convertFixed32ToRef` turns it into the offset. */
	fun getShiftTo(): Int = shiftTo

	fun hasShiftToNextZ8Tile(): Boolean = hasShiftToNextZ8Tile

	fun getShiftToNextZ8Tile(): Int = shiftToNextZ8Tile

	fun hasShiftToNextZ10Tile(): Boolean = hasShiftToNextZ10Tile

	fun getShiftToNextZ10Tile(): Int = shiftToNextZ10Tile

	fun hasShiftToNextZ12Tile(): Boolean = hasShiftToNextZ12Tile

	fun getShiftToNextZ12Tile(): Int = shiftToNextZ12Tile

	companion object {
		internal const val ZOOM_FIELD_NUMBER = 2
		internal const val X_FIELD_NUMBER = 3
		internal const val Y_FIELD_NUMBER = 4
		internal const val SUFFIXESBITSETINDEX_FIELD_NUMBER = 5
		internal const val EXTRASUFFIX_FIELD_NUMBER = 7
		internal const val OTHERWORDSCOUNT_FIELD_NUMBER = 8
		internal const val POIINDINBLOCK_FIELD_NUMBER = 9
		internal const val BBOX_FIELD_NUMBER = 10
		internal const val POICATEGORIES_FIELD_NUMBER = 11
		internal const val ELORATING_FIELD_NUMBER = 12
		internal const val SHIFTTO_FIELD_NUMBER = 14
		internal const val SHIFTTONEXTZ8TILE_FIELD_NUMBER = 20
		internal const val SHIFTTONEXTZ10TILE_FIELD_NUMBER = 21
		internal const val SHIFTTONEXTZ12TILE_FIELD_NUMBER = 22

		/** Reads the fields up to the current limit of [codedIS]; the block reads its atoms. */
		internal fun parseFrom(codedIS: CodedInputStream): OsmAndPoiNameIndexDataAtom {
			val res = OsmAndPoiNameIndexDataAtom()
			var bits: KTIntArrayList? = null
			var extra: MutableList<String>? = null
			var other: KTIntArrayList? = null
			var indInBlock: KTIntArrayList? = null
			var categories: KTIntArrayList? = null
			var elo: KTIntArrayList? = null
			while (true) {
				val tag = codedIS.readTag()
				when (tag) {
					0 -> break
					16 -> {
						res.zoom = codedIS.readRawVarint32()
						res.hasZoom = true
					}
					24 -> {
						res.x = codedIS.readRawVarint32()
						res.hasX = true
					}
					32 -> {
						res.y = codedIS.readRawVarint32()
						res.hasY = true
					}
					40, 42 -> bits = readInts(codedIS, tag, bits)
					58 -> (extra ?: ArrayList<String>().also { extra = it }).add(codedIS.readString())
					64, 66 -> other = readInts(codedIS, tag, other)
					72, 74 -> indInBlock = readInts(codedIS, tag, indInBlock)
					82 -> {
						res.bbox = codedIS.readBytes()
						res.hasBbox = true
					}
					88, 90 -> categories = readInts(codedIS, tag, categories)
					96, 98 -> elo = readInts(codedIS, tag, elo)
					117 -> {
						res.shiftTo = readFixed32(codedIS)
						res.hasShiftTo = true
					}
					165 -> {
						res.shiftToNextZ8Tile = readFixed32(codedIS)
						res.hasShiftToNextZ8Tile = true
					}
					173 -> {
						res.shiftToNextZ10Tile = readFixed32(codedIS)
						res.hasShiftToNextZ10Tile = true
					}
					181 -> {
						res.shiftToNextZ12Tile = readFixed32(codedIS)
						res.hasShiftToNextZ12Tile = true
					}
					else -> codedIS.skipField(tag)
				}
			}
			res.suffixesBitsetIndex = bits.toInts()
			res.extraSuffix = extra.orEmpty()
			res.otherWordsCount = other.toInts()
			res.poiIndInBlock = indInBlock.toInts()
			res.poiCategories = categories.toInts()
			res.eloRating = elo.toInts()
			return res
		}
	}
}
