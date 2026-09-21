package net.osmand.shared.binary

import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.util.collections.KTIntArrayList
import net.osmand.shared.util.collections.KTLongHashSet
import kotlin.jvm.JvmField

/**
 * A poi section of an obf file: the box it covers, the tables that decode the numbers an amenity
 * is stored as, and the tag groups of the administrative areas the amenities sit in.
 *
 * A copy of `BinaryMapPoiReaderAdapter.PoiRegion` in OsmAnd-java, which stays there for android
 * and tools; this copy is for iOS. The tables are read lazily, the first time something is read
 * out of the section, so a file that is never searched never pays for them.
 */
class PoiRegion : BinaryIndexPart() {

	@JvmField
	val categories: MutableList<String> = ArrayList()

	@JvmField
	val categoriesType: MutableList<PoiCategory?> = ArrayList()

	@JvmField
	val subcategories: MutableList<MutableList<String>> = ArrayList()

	@JvmField
	val categoryFreqs = KTIntArrayList()

	@JvmField
	val subcategoryFreqs: MutableList<KTIntArrayList> = ArrayList()

	@JvmField
	val subTypes: MutableList<PoiSubType> = ArrayList()

	@JvmField
	val topIndexSubTypes: MutableList<PoiSubType> = ArrayList()

	// tag groups
	private val tagGroups: MutableList<List<TagValuePair>?> = ArrayList()
	private val tagGroupsRead = KTLongHashSet()

	@JvmField
	var left31: Int = 0

	@JvmField
	var right31: Int = 0

	@JvmField
	var top31: Int = 0

	@JvmField
	var bottom31: Int = 0

	fun getLeft31(): Int = left31

	fun getRight31(): Int = right31

	fun getTop31(): Int = top31

	fun getBottom31(): Int = bottom31

	override fun getPartName(): String = "POI"

	fun getCategories(): List<String> = categories

	fun getSubcategories(): List<List<String>> = subcategories

	fun getCategoryFreqs(): KTIntArrayList = categoryFreqs

	fun getSubcategoryFreqs(): List<KTIntArrayList> = subcategoryFreqs

	fun getSubTypes(): List<PoiSubType> = subTypes

	fun getTopIndexSubTypes(): List<PoiSubType> = topIndexSubTypes

	override fun getFieldNumber(): Int = BinaryMapIndexReader.POIINDEX_FIELD_NUMBER

	/**
	 * The additional attribute an id stands for, with its value appended to [returnValue]. The low
	 * bit says how the id is split: a short form for the first 32 attributes, a long one beyond.
	 */
	fun getSubtypeFromId(id: Int, returnValue: StringBuilder): PoiSubType? {
		val tl: Int
		val sl: Int
		if (id % 2 == 0) {
			tl = (id shr 1) and ((1 shl 5) - 1)
			sl = id shr 6
		} else {
			tl = (id shr 1) and ((1 shl 15) - 1)
			sl = id shr 16
		}
		if (subTypes.size > tl) {
			val st = subTypes[tl]
			if (st.text) {
				return st
			} else {
				val possibleValues = st.possibleValues
				if (possibleValues != null && possibleValues.size > sl) {
					returnValue.append(possibleValues[sl])
					return st
				}
			}
		}
		return null
	}

	fun getTagValues(id: Int): List<TagValuePair>? {
		if (id >= tagGroups.size) {
			return null
		}
		return tagGroups[id]
	}

	fun setTagGroups(id: Int, tagValuePairs: List<TagValuePair>) {
		while (id >= tagGroups.size) {
			tagGroups.add(null)
		}
		tagGroups[id] = tagValuePairs
	}

	fun updReadTagGroups(coordsTagGroups: KTLongHashSet) {
		tagGroupsRead.addAll(coordsTagGroups)
	}

	/** Removes from [coordsTagGroups] the tiles whose tag groups this section has already read. */
	fun checkMissingTagGroups(coordsTagGroups: KTLongHashSet): KTLongHashSet {
		val remaining = KTLongHashSet()
		for (id in coordsTagGroups.toArray()) {
			if (!tagGroupsRead.contains(id)) {
				remaining.add(id)
			}
		}
		coordsTagGroups.clear()
		coordsTagGroups.addAll(remaining.toArray())
		return coordsTagGroups
	}

	/** The category an encoded type stands for, with its subtype appended to [subtype]. */
	fun decodePoiType(catFile: Int, subtype: StringBuilder): PoiCategory? {
		val subcatId = catFile shr BinaryMapPoiReaderAdapter.SHIFT_BITS_CATEGORY
		val catId = catFile and BinaryMapPoiReaderAdapter.CATEGORY_MASK
		var type: PoiCategory? = null
		if (catId < categoriesType.size) {
			type = categoriesType[catId]
			val subcats = subcategories[catId]
			if (subcatId < subcats.size) {
				subtype.append(subcats[subcatId])
			}
		}
		return type
	}
}
