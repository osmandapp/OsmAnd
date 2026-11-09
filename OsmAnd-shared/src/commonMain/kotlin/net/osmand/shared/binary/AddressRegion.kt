package net.osmand.shared.binary

import net.osmand.shared.data.KLatLon

/**
 * The address section of an obf file, as far as the file's header walk reads it: its names, the
 * table of extra name tags, where each block of settlements starts and where the name index is.
 *
 * A copy of `BinaryMapAddressReaderAdapter.AddressRegion` in OsmAnd-java, lifted out of the
 * adapter as [PoiRegion] was.
 */
class AddressRegion : BinaryIndexPart() {

	internal var enName: String? = null
	internal var indexNameOffset: Long = -1
	internal var attributeTagsTable: List<String> = ArrayList()
	internal val cities: MutableList<CitiesBlock> = ArrayList()

	internal var calculatedCenter: KLatLon? = null

	fun getEnName(): String? = enName

	fun getCities(): List<CitiesBlock> = cities

	fun getAttributeTagsTable(): List<String> = attributeTagsTable

	fun getIndexNameOffset(): Long = indexNameOffset

	override fun getPartName(): String = "Address"

	override fun getFieldNumber(): Int = BinaryMapIndexReader.ADDRESSINDEX_FIELD_NUMBER
}

/** One block of settlements of one [CityBlocks] type in an [AddressRegion]. */
class CitiesBlock : BinaryIndexPart() {

	internal var type: Int = 0

	fun getType(): Int = type

	override fun getPartName(): String = "City"

	override fun getFieldNumber(): Int = BinaryMapAddressReaderAdapter.CITIES_FIELD_NUMBER
}

/**
 * The kinds of block the address section keeps settlements in, by the number the file gives them.
 * A copy of `BinaryMapAddressReaderAdapter.CityBlocks` in OsmAnd-java.
 */
enum class CityBlocks(val index: Int, val cityGroupType: Boolean) {
	UNKNOWN_TYPE(-1, false), // unsupported block types will be parsed as unknown
	BOUNDARY_TYPE(0, false), // to avoid crash < 5.2 assign to 0
	CITY_TOWN_TYPE(1, true),
	// the correct type is -1, this is order in sections for postcode
	POSTCODES_TYPE(2, true),
	VILLAGES_TYPE(3, true),
	STREET_TYPE(4, false);

	companion object {

		fun getByType(index: Int): CityBlocks {
			for (c in entries) {
				if (c.index == index) {
					return c
				}
			}
			return UNKNOWN_TYPE
		}

		fun allTypes(): List<CityBlocks> {
			val lst = ArrayList<CityBlocks>()
			for (c in entries) {
				if (c != UNKNOWN_TYPE) {
					lst.add(c)
				}
			}
			return lst
		}
	}
}
