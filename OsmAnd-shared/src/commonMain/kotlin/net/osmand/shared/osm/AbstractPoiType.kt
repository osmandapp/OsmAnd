package net.osmand.shared.osm

import kotlin.jvm.JvmField

/**
 * Anything poi_types.xml can name: a category, a filter inside it, a type, or an additional
 * attribute of one. What they share is a key name, the registry they came from, and the list of
 * additional attributes that apply to them.
 *
 * A copy of `AbstractPoiType` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS.
 *
 * [equals] is by key name and there is no `hashCode` to match it, exactly as in java: a type put in
 * a hash set is therefore found by identity and not by name, and code that relies on that - the
 * basemap set of a category, for one - would behave differently if the two were brought in line.
 */
abstract class AbstractPoiType(
	@JvmField protected val keyName: String,
	protected val registry: MapPoiTypes,
	private val iconName: String? = null
) {

	private var poiAdditionals: MutableList<PoiType>? = null
	private var poiAdditionalsCategorized: MutableList<PoiType>? = null
	private var poiAdditionalsByKeyName: MutableMap<String, PoiType>? = null
	private var topVisible: Boolean = false
	private var lang: String? = null
	private var baseLangType: AbstractPoiType? = null
	private var notEditableOsm: Boolean = false
	private var poiAdditionalCategory: String? = null
	private var excludedPoiAdditionalCategories: MutableList<String>? = null
	private var synonyms: String? = null
	private var enTranslation: String? = null
	private var translation: String? = null
	private var nonIndx: Boolean = false

	fun setBaseLangType(baseLangType: AbstractPoiType?) {
		this.baseLangType = baseLangType
	}

	fun getBaseLangType(): AbstractPoiType? = baseLangType

	fun setLang(lang: String?) {
		this.lang = lang
	}

	fun getLang(): String? = lang

	fun getKeyName(): String = keyName

	internal fun getIconNameInternal(): String? = iconName

	fun getIconKeyName(): String = formatKeyName(iconName ?: getKeyName())

	fun getFormattedKeyName(): String = formatKeyName(getKeyName())

	protected fun formatKeyName(keyName: String): String {
		var kn = keyName
		if (kn.startsWith("osmand_")) {
			kn = kn.substring("osmand_".length)
		}
		return kn.replace(':', '_')
	}

	fun setTopVisible(topVisible: Boolean) {
		this.topVisible = topVisible
	}

	fun isTopVisible(): Boolean = topVisible

	/** Overridden by [PoiType]; anything else is never an additional attribute. */
	open fun isAdditional(): Boolean = false

	fun getTranslation(): String {
		var translation = this.translation
		if (translation == null) {
			translation = registry.getTranslation(this)
			this.translation = translation
		}
		return translation
	}

	fun getSynonyms(): String {
		var synonyms = this.synonyms
		if (synonyms == null) {
			synonyms = registry.getSynonyms(this)
			this.synonyms = synonyms
		}
		return synonyms
	}

	fun getEnTranslation(): String {
		var enTranslation = this.enTranslation
		if (enTranslation == null) {
			enTranslation = registry.getEnTranslation(this)
			this.enTranslation = enTranslation
		}
		return enTranslation
	}

	fun getPoiAdditionalCategoryTranslation(): String? {
		val category = poiAdditionalCategory
		return if (category != null) registry.getPoiTranslation(category) else null
	}

	fun hasValidTranslation(): Boolean = registry.hasValidTranslation(this)

	open fun setNonIndx(nonIndx: Boolean) {
		this.nonIndx = nonIndx
	}

	open fun isNonIndx(): Boolean = nonIndx

	fun addPoiAdditional(tp: PoiType) {
		var additionals = poiAdditionals
		if (additionals == null) {
			additionals = ArrayList()
			poiAdditionals = additionals
		}
		additionals.add(tp)
		var byKeyName = poiAdditionalsByKeyName
		if (byKeyName == null) {
			byKeyName = HashMap()
			poiAdditionalsByKeyName = byKeyName
		}
		byKeyName[tp.getKeyName()] = tp
		if (tp.getPoiAdditionalCategory() != null) {
			var categorized = poiAdditionalsCategorized
			if (categorized == null) {
				categorized = ArrayList()
				poiAdditionalsCategorized = categorized
			}
			categorized.add(tp)
		}
	}

	fun getPoiAdditionalByKeyName(name: String): PoiType? = poiAdditionalsByKeyName?.get(name)

	fun getPoiAdditionals(): List<PoiType> = poiAdditionals ?: emptyList()

	fun getPoiAdditionalsCategorized(): List<PoiType> = poiAdditionalsCategorized ?: emptyList()

	fun isNotEditableOsm(): Boolean = notEditableOsm

	fun setNotEditableOsm(notEditableOsm: Boolean) {
		this.notEditableOsm = notEditableOsm
	}

	fun getPoiAdditionalCategory(): String? = poiAdditionalCategory

	fun setPoiAdditionalCategory(poiAdditionalCategory: String?) {
		this.poiAdditionalCategory = poiAdditionalCategory
	}

	fun getExcludedPoiAdditionalCategories(): List<String>? = excludedPoiAdditionalCategories

	fun addExcludedPoiAdditionalCategories(excludedPoiAdditionalCategories: Array<String>) {
		var excluded = this.excludedPoiAdditionalCategories
		if (excluded == null) {
			excluded = ArrayList()
			this.excludedPoiAdditionalCategories = excluded
		}
		excluded.addAll(excludedPoiAdditionalCategories)
	}

	/** The categories and type names a filter on this type accepts, added to [acceptedTypes]. */
	abstract fun putTypes(
		acceptedTypes: MutableMap<PoiCategory, LinkedHashSet<String>?>
	): MutableMap<PoiCategory, LinkedHashSet<String>?>

	open fun getParentTypeName(): String = ""

	override fun toString(): String = keyName

	override fun equals(other: Any?): Boolean {
		if (other !is AbstractPoiType) {
			return false
		}
		return keyName == other.keyName
	}

	fun getOriginalIconName(): String? = iconName
}
