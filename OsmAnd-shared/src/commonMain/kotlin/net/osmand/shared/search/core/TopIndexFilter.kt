package net.osmand.shared.search.core

import net.osmand.shared.binary.PoiSubType
import net.osmand.shared.binary.SearchPoiAdditionalFilter
import net.osmand.shared.osm.MapPoiTypes
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KSearchAlgorithms
import kotlin.jvm.JvmStatic

/**
 * The pois of one value of a top index tag, such as one brand or one operator.
 *
 * A copy of `TopIndexFilter` in OsmAnd-java, which stays there for android and tools; this copy is
 * for iOS.
 */
class TopIndexFilter(
	private val poiSubType: PoiSubType,
	private val types: MapPoiTypes,
	private val value: String
) : SearchPoiAdditionalFilter {

	private val valueKey: String = getValueKey(value)
	private val tag: String = poiSubType.name!!.replace(MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX, "") // brand, operator, ...

	override fun accept(poiSubType: PoiSubType, value: String): Boolean {
		return this.poiSubType.name!! == poiSubType.name && this.value.equals(value, ignoreCase = true)
	}

	fun getTag(): String = tag

	fun getFilterId(): String = MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX + tag + "_" + getValueKey(value)

	override fun getName(): String? {
		// type of object: brand, operator
		val pt = types.getAnyPoiAdditionalTypeByKey(tag)
		if (pt != null) {
			return pt.getTranslation()
		}
		return types.getPoiTranslation(tag)
	}

	override fun getIconResource(): String {
		//Example: mcdonalds, bank_of_america
		return valueKey
	}

	override fun equals(other: Any?): Boolean {
		if (other !is TopIndexFilter) {
			return false
		}
		return this.tag == other.tag && this.value.equals(other.value, ignoreCase = true)
	}

	override fun hashCode(): Int = KAlgorithms.hash(tag, value)

	fun getValue(): String = value

	companion object {
		@JvmStatic
		fun getValueKey(value: String): String {
			val aligned = KSearchAlgorithms.alignChars(value)
			return aligned.lowercase().replace(':', '_').replace("'", "").replace(' ', '_').replace("\"", "")
		}
	}
}
