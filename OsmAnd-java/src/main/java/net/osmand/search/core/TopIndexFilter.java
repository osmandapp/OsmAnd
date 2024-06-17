package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.osm.MapPoiTypes;

public class TopIndexFilter implements BinaryMapIndexReader.SearchPoiAdditionalFilter {

	private PoiSubType poiSubType;
	private SearchPhrase.NameStringMatcher nameStringMatcher;
	private MapPoiTypes types;
	private String valueKey;
	private String tag; // brand, operator, ...
	private String translatedName;
	
	public TopIndexFilter(PoiSubType poiSubType, SearchPhrase.NameStringMatcher nameStringMatcher, MapPoiTypes types, String value) {
		this.valueKey = value.toLowerCase().replace(':', '_').replaceAll("\'", "").replace(' ', '_').replaceAll("\"", "");
		this.poiSubType = poiSubType;
		this.nameStringMatcher = nameStringMatcher;
		this.types = types;
		tag = poiSubType.name.replace(MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX, "");
		translatedName = types.getPoiTranslation(valueKey);
	}

	@Override
	public boolean accept(PoiSubType poiSubType, String value) {
		String translate = types.getPoiTranslation(value);
		if (this.poiSubType.name.equals(poiSubType.name)) {
			return nameStringMatcher.matches(value) || nameStringMatcher.matches(translate);
		}
		return false;
	}
	
	public String getTag() {
		return tag;
	}

	@Override
	public String getName() {
		return translatedName;
	}

	@Override
	public String getIconResource() {
		//Example: mcdonalds, bank_of_america
		return valueKey;
	}
}
