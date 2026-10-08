package net.osmand.shared.osm

/**
 * One leaf of poi_types.xml: either a poi type such as "bakery", or an additional attribute such
 * as "opening_hours". Which of the two it is depends on whether it has a parent type.
 *
 * A copy of `PoiType` in OsmAnd-java, which stays there for android and tools; this copy is for
 * iOS. A reference type carries no data of its own: every getter forwards to the type it points
 * at, so that one type can appear under several filters without being copied.
 */
class PoiType(
	poiTypes: MapPoiTypes,
	private val category: PoiCategory?,
	private val filter: PoiFilter?,
	keyName: String,
	iconName: String?
) : AbstractPoiType(keyName, poiTypes, iconName) {

	private var parentType: AbstractPoiType? = null
	private var referenceType: PoiType? = null
	private var osmTag: String? = null
	private var osmTag2: String? = null
	private var osmValue: String? = null
	private var osmValue2: String? = null

	private var editTag: String? = null
	private var editValue: String? = null
	private var editTag2: String? = null
	private var editValue2: String? = null
	private var filterOnly: Boolean = false

	private var nameTag: String? = null
	private var text: Boolean = false
	private var nameOnly: Boolean = false
	private var relation: Boolean = false
	private var order: Int = DEFAULT_ORDER
	private var topIndex: Boolean = false
	private var hidden: Boolean = false
	private var maxPerMap: Int = 0
	private var minCount: Int = 0
	private var defaultForCategory: Boolean = false

	fun getReferenceType(): PoiType? = referenceType

	fun setReferenceType(referenceType: PoiType?) {
		this.referenceType = referenceType
	}

	fun isReference(): Boolean = referenceType != null

	fun getOsmTag(): String? {
		if (isReference()) {
			return referenceType?.getOsmTag()
		}
		if (editTag != null) {
			return editTag
		}
		val osmTag = this.osmTag
		if (osmTag != null && osmTag.startsWith("osmand_amenity")) {
			return "amenity"
		}
		return osmTag
	}

	override fun isNonIndx(): Boolean {
		if (isReference()) {
			return referenceType?.isNonIndx() == true
		}
		return super.isNonIndx()
	}

	fun getRawOsmTag(): String? {
		if (isReference()) {
			return referenceType?.getOsmTag()
		}
		return osmTag
	}

	fun setOsmEditTagValue(osmTag: String?, editValue: String?) {
		this.editTag = osmTag
		this.editValue = editValue
	}

	fun setOsmEditTagValue2(osmTag: String?, editValue: String?) {
		this.editTag2 = osmTag
		this.editValue2 = editValue
	}

	fun getEditOsmTag(): String? {
		if (isReference()) {
			return referenceType?.getEditOsmTag()
		}
		return editTag ?: getOsmTag()
	}

	fun getEditOsmValue(): String? {
		if (isReference()) {
			return referenceType?.getEditOsmValue()
		}
		return editValue ?: getOsmValue()
	}

	fun getEditOsmTag2(): String? {
		if (isReference()) {
			return referenceType?.getEditOsmTag2()
		}
		return editTag2
	}

	fun getEditOsmValue2(): String? {
		if (isReference()) {
			return referenceType?.getEditOsmValue2()
		}
		return editValue2
	}

	fun setOsmTag(osmTag: String?) {
		this.osmTag = osmTag
	}

	fun getOsmTag2(): String? {
		if (isReference()) {
			return referenceType?.getOsmTag2()
		}
		return osmTag2
	}

	fun setOsmTag2(osmTag2: String?) {
		this.osmTag2 = osmTag2
	}

	fun getOsmValue(): String? {
		if (isReference()) {
			return referenceType?.getOsmValue()
		}
		return osmValue
	}

	fun setOsmValue(osmValue: String?) {
		this.osmValue = osmValue
	}

	fun getOsmValue2(): String? {
		if (isReference()) {
			return referenceType?.getOsmValue2()
		}
		return osmValue2
	}

	fun setOsmValue2(osmValue2: String?) {
		this.osmValue2 = osmValue2
	}

	fun isFilterOnly(): Boolean = filterOnly

	fun setFilterOnly(filterOnly: Boolean) {
		this.filterOnly = filterOnly
	}

	fun getCategory(): PoiCategory? = category

	fun getFilter(): PoiFilter? = filter

	override fun putTypes(
		acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>
	): MutableMap<PoiCategory, LinkedHashSet<String>?> {
		if (isAdditional()) {
			parentType?.putTypes(acceptedTypes)
			if (filterOnly && category != null) {
				val set = acceptedTypes[category]
				for (pt in category.getPoiTypes()) {
					for (poiType in pt.getPoiAdditionals()) {
						if (poiType.getKeyName() == keyName) {
							set?.add(pt.getKeyName())
						}
					}
				}
			}
			return acceptedTypes
		}
		val rt = getReferenceType()
		val poiType = rt ?: this
		val poiCategory = poiType.category
		if (poiCategory != null) {
			if (!acceptedTypes.containsKey(poiCategory)) {
				acceptedTypes[poiCategory] = LinkedHashSet()
			}
			val set = acceptedTypes[poiCategory]
			set?.add(poiType.getKeyName())
		}
		return acceptedTypes
	}

	override fun getParentTypeName(): String {
		val parentType = this.parentType
		return when {
			parentType != null -> parentType.getTranslation()
			category != null -> category.getTranslation()
			else -> ""
		}
	}

	fun setAdditional(parentType: AbstractPoiType?) {
		this.parentType = parentType
	}

	override fun isAdditional(): Boolean = parentType != null

	fun getParentType(): AbstractPoiType? = parentType

	fun isText(): Boolean = text

	fun setText(text: Boolean) {
		this.text = text
	}

	fun getNameTag(): String? = nameTag

	fun setNameTag(nameTag: String?) {
		this.nameTag = nameTag
	}

	fun isNameOnly(): Boolean = nameOnly

	fun setNameOnly(nameOnly: Boolean) {
		this.nameOnly = nameOnly
	}

	fun isRelation(): Boolean = relation

	fun setRelation(relation: Boolean) {
		this.relation = relation
	}

	fun getOrder(): Int = order

	fun setOrder(order: Int) {
		this.order = order
	}

	fun setHidden(hidden: Boolean) {
		this.hidden = hidden
	}

	fun isHidden(): Boolean = hidden

	override fun toString(): String {
		return "PoiType{" +
				"category=" + category +
				", parentType=" + parentType +
				", referenceType=" + referenceType +
				", osmTag='" + osmTag + '\'' +
				", osmTag2='" + osmTag2 + '\'' +
				", osmValue='" + osmValue + '\'' +
				", osmValue2='" + osmValue2 + '\'' +
				", text=" + text +
				", nameOnly=" + nameOnly +
				", relation=" + relation +
				", order=" + order +
				", hidden=" + hidden +
				'}'
	}

	fun isTopIndex(): Boolean = topIndex

	fun setTopIndex(topIndex: Boolean) {
		this.topIndex = topIndex
	}

	fun getMaxPerMap(): Int = maxPerMap

	fun setMaxPerMap(maxPerMap: Int) {
		this.maxPerMap = maxPerMap
	}

	fun getMinCount(): Int = minCount

	fun setMinCount(minCount: Int) {
		this.minCount = minCount
	}

	fun getOsmTagsValues(): Map<String, String> {
		val tags = LinkedHashMap<String, String>()
		val tag1 = getRawOsmTag()
		val val1 = getOsmValue()
		if (tag1 != null && val1 != null) {
			tags[tag1] = val1
		}
		val tag2 = getOsmTag2()
		val val2 = getOsmValue2()
		if (tag2 != null && val2 != null) {
			tags[tag2] = val2
		}
		return tags
	}

	fun isDefaultForCategory(): Boolean = defaultForCategory

	fun setDefaultForCategory(defaultForCategory: Boolean) {
		this.defaultForCategory = defaultForCategory
	}

	companion object {
		const val DEFAULT_ORDER: Int = 90
		const val DEFAULT_GROUP_ORDER: Int = 40
		const val DEFAULT_MIN_COUNT: Int = 3
		const val DEFAULT_MAX_PER_MAP: Int = 100
	}
}
