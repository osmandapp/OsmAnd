package net.osmand.data;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

public class AmenityRowsBuilder {

	public static final String TRANSLATIONS_SEPARATOR = " • ";

	private AmenityRowsBuilder() {
	}

	public static AmenityRowData buildPoiTypesGroupRow(String key, String name, String textPrefix,
			List<PoiType> types, int order, int iconId, String iconName, AmenityRowData extraCollapsableRow,
			boolean poiAdditional, PoiCategory collapsableCategory) {
		StringBuilder text = new StringBuilder();
		for (PoiType pt : types) {
			String translation = pt.getTranslation();
			if (text.length() > 0) {
				text.append(TRANSLATIONS_SEPARATOR);
			}
			text.append(translation);
		}
		return new AmenityRowData.Builder(key)
				.setName(name)
				.setTextPrefix(textPrefix)
				.setText(text.toString())
				.setOrder(order)
				.setIconId(iconId)
				.setIconName(iconName)
				.setTextLinesLimit(1)
				.setCollapsableRowType(AmenityRowData.CollapsableRowType.POI_TYPE_GROUP)
				.setCollapsablePoiTypes(types)
				.setPoiAdditional(poiAdditional)
				.setCollapsableCategory(collapsableCategory)
				.setCollapsableExtraRow(extraCollapsableRow)
				.build();
	}

	public static void sortInfoRows(List<AmenityRowData> rows) {
		Collections.sort(rows, (row1, row2) -> {
			if (row1.order != row2.order) {
				return Integer.compare(row1.order, row2.order);
			}
			return Algorithms.compare(row1.name, row2.name);
		});
	}

	public static void sortDescriptionRows(List<AmenityRowData> descriptions, String preferredLang) {
		if (Algorithms.isEmpty(preferredLang)) {
			return;
		}
		String langSuffix = ":" + preferredLang;
		AmenityRowData descInPrefLang = null;
		for (AmenityRowData desc : descriptions) {
			if (desc.key.length() > langSuffix.length() && desc.key.endsWith(langSuffix)) {
				descInPrefLang = desc;
				break;
			}
		}
		if (descInPrefLang != null) {
			descriptions.remove(descInPrefLang);
			descriptions.add(0, descInPrefLang);
		}
	}
}
