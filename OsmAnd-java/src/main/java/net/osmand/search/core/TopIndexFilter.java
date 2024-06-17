package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.osm.MapPoiTypes;

public class TopIndexFilter implements BinaryMapIndexReader.SearchPoiAdditionalFilter {

	private PoiSubType poiSubType;
	private SearchPhrase.NameStringMatcher nameStringMatcher;
	private MapPoiTypes types;
	private String name;
	private String translatedName;
	private String value;

	public TopIndexFilter(PoiSubType poiSubType, SearchPhrase.NameStringMatcher nameStringMatcher, MapPoiTypes types, String value) {
		this.poiSubType = poiSubType;
		this.nameStringMatcher = nameStringMatcher;
		this.types = types;
		this.value = value;
		name = poiSubType.name.replace(MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX, "");
		translatedName = types.getPoiTranslation(name);
	}

	@Override
	public boolean accept(PoiSubType poiSubType, String value) {
		String translate = types.getPoiTranslation(value);
		if (this.poiSubType.name.equals(poiSubType.name)) {
			return nameStringMatcher.matches(value) || nameStringMatcher.matches(translate);
		}
		return false;
	}

	@Override
	public String getName() {
		return translatedName;
	}

	@Override
	public String getIconResource() {
		//Example: brand_mcdonalds, operator_bank_of_america
		String val = value.replaceAll(":", "");
		return name + "_" + val;
	}
}
