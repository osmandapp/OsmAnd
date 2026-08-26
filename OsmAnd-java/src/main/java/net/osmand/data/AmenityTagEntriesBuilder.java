package net.osmand.data;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.util.List;

public class AmenityTagEntriesBuilder {

	public static final String TRANSLATIONS_SEPARATOR = " • ";

	private AmenityTagEntriesBuilder() {
	}

	public static AmenityTagEntry buildPoiTypesGroupEntry(String key, String name, String textPrefix,
                  List<PoiType> types, int order, int iconId, List<String> iconNameCandidates,
                  int fallbackIconId, boolean poiAdditional, PoiCategory collapsableCategory) {
		StringBuilder text = new StringBuilder();
		for (PoiType pt : types) {
			String translation = pt.getTranslation();
			if (Algorithms.isNotEmpty(text)) {
				text.append(TRANSLATIONS_SEPARATOR);
			}
			text.append(translation);
		}
		return new AmenityTagEntry.Builder(key)
				.setName(name)
				.setTextPrefix(textPrefix)
				.setText(text.toString())
				.setOrder(order)
				.setIconId(iconId)
				.setIconNameCandidates(iconNameCandidates)
				.setFallbackIconId(fallbackIconId)
				.setTextLinesLimit(1)
				.setCollapsableEntryType(AmenityTagEntry.CollapsableEntryType.POI_TYPE_GROUP)
				.setCollapsablePoiTypes(types)
				.setPoiAdditional(poiAdditional)
				.setCollapsableCategory(collapsableCategory)
				.build();
	}

	public static void sortInfoEntries(List<AmenityTagEntry> entries) {
		entries.sort((entry1, entry2) -> {
			if (entry1.order != entry2.order) {
				return Integer.compare(entry1.order, entry2.order);
			}
			return Algorithms.compare(entry1.name, entry2.name);
		});
	}

	public static void sortDescriptionEntries(List<AmenityTagEntry> descriptions, String preferredLang) {
		if (Algorithms.isEmpty(preferredLang)) {
			return;
		}
		String langSuffix = ":" + preferredLang;
		for (var it = descriptions.iterator(); it.hasNext(); ) {
			AmenityTagEntry desc = it.next();
			if (desc.key.length() > langSuffix.length() && desc.key.endsWith(langSuffix)) {
				it.remove();
				descriptions.add(0, desc);
				break;
			}
		}
	}
}
