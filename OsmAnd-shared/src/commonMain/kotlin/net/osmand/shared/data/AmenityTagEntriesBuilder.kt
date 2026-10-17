package net.osmand.shared.data

import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmStatic

object AmenityTagEntriesBuilder {

	const val TRANSLATIONS_SEPARATOR = " • "
	private const val YES = "yes"
	// ':' and the full-width forms used by the CJK translations, French puts spaces around it
	private val NAME_VALUE_SEPARATOR = Regex("\\s*[:\uFF1A\uFE30]\\s*")

	@JvmStatic
	fun buildPoiTypesGroupEntry(
		key: String, name: String?, textPrefix: String?, types: List<PoiType>, order: Int, iconId: Int,
		iconNameCandidates: List<String>, fallbackIconId: Int, poiAdditional: Boolean,
		collapsableCategory: PoiCategory?
	): AmenityTagEntry {
		var prefix = textPrefix
		val text: String
		val single = if (poiAdditional && types.size == 1) splitNameValue(types[0].getTranslation()) else null
		if (single != null) {
			// one value: "Smoking: no" -> "Smoking" over "No"
			prefix = single.first
			text = KAlgorithms.capitalizeFirstLetter(single.second) ?: single.second
		} else if (poiAdditional) {
			text = types.joinToString(TRANSLATIONS_SEPARATOR) { getGroupValueLabel(it) }
		} else {
			text = types.joinToString(TRANSLATIONS_SEPARATOR) { it.getTranslation() }
		}
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
		val match = NAME_VALUE_SEPARATOR.find(translation) ?: return null
		if (match.range.first == 0 || match.range.last + 1 >= translation.length) {
			return null
		}
		return Pair(translation.substring(0, match.range.first).trim(), translation.substring(match.range.last + 1).trim())
	}

	/**
	 * A value inside a group of several values: "Kayaks" for yes, "Kayaks: limited" otherwise, "Cash" as it is.
	 */
	@JvmStatic
	fun getGroupValueLabel(type: PoiType): String {
		val translation = type.getTranslation()
		val nameValue = splitNameValue(translation) ?: return translation
		return if (isYesValue(type)) nameValue.first else translation
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
