package net.osmand.shared.data

import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmStatic

object AmenityTagEntriesBuilder {

	const val TRANSLATIONS_SEPARATOR = " • "
	private const val YES = "yes"

	enum class ValueLabel {
		NAME_OVER_VALUE, NAME, WHOLE
	}

	data class ValueLabelParts(val tagName: String?, val tagValue: String)

	@JvmStatic
	fun buildPoiTypesGroupEntry(
		key: String, name: String?, textPrefix: String?, types: List<PoiType>, order: Int, iconId: Int,
		iconNameCandidates: List<String>, fallbackIconId: Int, poiAdditional: Boolean,
		collapsableCategory: PoiCategory?
	): AmenityTagEntry {
		val parts = if (poiAdditional) types.map { getValueLabelParts(it) } else null
		val prefix = parts?.firstNotNullOfOrNull { it.tagName } ?: textPrefix
		val text = parts?.joinToString(TRANSLATIONS_SEPARATOR) { it.tagValue }
			?: types.joinToString(TRANSLATIONS_SEPARATOR) { it.getTranslation() }
		return AmenityTagEntry.Builder(key)
			.setName(name)
			.setTextPrefix(prefix)
			.setText(text)
			.setOrder(order)
			.setIconId(iconId)
			.setIconNameCandidates(iconNameCandidates)
			.setFallbackIconId(fallbackIconId)
			.setTextLinesLimit(1)
			.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP)
			.setCollapsablePoiTypes(types)
			.setPoiAdditional(poiAdditional)
			.setCollapsableCategory(collapsableCategory)
			.build()
	}

	/**
	 * Values of additional properties are written "Name: value" in phrases.xml ("Fridge: yes", "Kayaks: limited").
	 * @return name and value, or null for a translation without the separator ("Cash")
	 */
	@JvmStatic
	fun splitNameValue(translation: String?): Pair<String, String>? {
		if (translation.isNullOrEmpty()) {
			return null
		}
		val index = translation.indexOf(':')
		if (index < 0) {
			return null
		}
		val name = translation.substring(0, index).trim()
		val value = translation.substring(index + 1).trim()
		// a name has letters: "1:25 fuel" is a value, not "1" + "25 fuel"
		return if (name.none { it.isLetter() } || value.isEmpty()) null else Pair(name, value)
	}

	@JvmStatic
	fun getValueLabel(type: PoiType): ValueLabel = when {
		isOneTagGroup(type) -> ValueLabel.NAME_OVER_VALUE
		isYesValue(type) -> ValueLabel.NAME
		else -> ValueLabel.WHOLE
	}

	@JvmStatic
	fun getValueLabelParts(translation: String, label: ValueLabel): ValueLabelParts {
		val nameValue = if (label == ValueLabel.WHOLE) null else splitNameValue(translation)
		return when {
			nameValue == null -> ValueLabelParts(null, translation)
			label == ValueLabel.NAME -> ValueLabelParts(null, nameValue.first)
			else -> ValueLabelParts(
				nameValue.first, KAlgorithms.capitalizeFirstLetter(nameValue.second) ?: nameValue.second
			)
		}
	}

	@JvmStatic
	fun getValueLabelParts(type: PoiType): ValueLabelParts =
		getValueLabelParts(type.getTranslation(), getValueLabel(type))

	// all values of the group are one tag (smoking=yes/no/outside), not a list of options (boat:kayak:rental, ...)
	private fun isOneTagGroup(type: PoiType): Boolean {
		val category = type.getPoiAdditionalCategory() ?: return true
		val parent = type.getParentType() ?: return true
		val tags = (parent.getPoiAdditionals() + parent.getPoiAdditionalsCategorized())
			.filter { it.getPoiAdditionalCategory() == category }
			.mapNotNull { it.getOsmTag() }
			.toSet()
		return tags.size <= 1
	}

	private fun isYesValue(type: PoiType): Boolean {
		if (YES == type.getOsmValue()) {
			return true
		}
		val enTranslation = type.getEnTranslation()
		val value = splitNameValue(enTranslation)?.second ?: enTranslation
		return YES.equals(value, ignoreCase = true)
	}

	@JvmStatic
	fun sortInfoEntries(entries: MutableList<AmenityTagEntry>) {
		entries.sortWith { entry1, entry2 ->
			if (entry1.order != entry2.order) {
				entry1.order.compareTo(entry2.order)
			} else {
				compareNullable(entry1.name, entry2.name)
			}
		}
	}

	@JvmStatic
	fun sortDescriptionEntries(descriptions: MutableList<AmenityTagEntry>, preferredLang: String?) {
		if (KAlgorithms.isEmpty(preferredLang)) {
			return
		}
		val langSuffix = ":$preferredLang"
		val index = descriptions.indexOfFirst { it.key.length > langSuffix.length && it.key.endsWith(langSuffix) }
		if (index >= 0) {
			descriptions.add(0, descriptions.removeAt(index))
		}
	}

	// as Algorithms.compare of OsmAnd-java: a missing name sorts last
	private fun compareNullable(s1: String?, s2: String?): Int = when {
		s1 == s2 -> 0
		s1 == null -> 1
		s2 == null -> -1
		else -> s1.compareTo(s2)
	}
}
