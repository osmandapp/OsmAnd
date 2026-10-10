package net.osmand.shared.data

import net.osmand.shared.osm.PoiCategory
import net.osmand.shared.osm.PoiType
import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmStatic

object AmenityTagEntriesBuilder {

	const val TRANSLATIONS_SEPARATOR = " • "

	@JvmStatic
	fun buildPoiTypesGroupEntry(
		key: String, name: String?, textPrefix: String?, types: List<PoiType>, order: Int, iconId: Int,
		iconNameCandidates: List<String>, fallbackIconId: Int, poiAdditional: Boolean,
		collapsableCategory: PoiCategory?
	): AmenityTagEntry {
		val text = types.joinToString(TRANSLATIONS_SEPARATOR) { it.getTranslation() }
		return AmenityTagEntry.Builder(key)
			.setName(name)
			.setTextPrefix(textPrefix)
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
