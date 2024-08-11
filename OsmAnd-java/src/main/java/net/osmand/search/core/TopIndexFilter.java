package net.osmand.search.core;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiSubType;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;

public class TopIndexFilter implements BinaryMapIndexReader.SearchPoiAdditionalFilter {

	private PoiSubType poiSubType;
	private MapPoiTypes types;
	private String valueKey;
	private String tag; // brand, operator, ...
	private String value;

	public TopIndexFilter(PoiSubType poiSubType, MapPoiTypes types, String value) {
		this.valueKey = getValueKey(value);
		this.poiSubType = poiSubType;
		this.types = types;
		this.value = value;
		tag = poiSubType.name.replace(MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX, "");
	}

	@Override
	public boolean accept(PoiSubType poiSubType, String value) {
		return this.poiSubType.name.equals(poiSubType.name) && this.value.equals(value);
	}
	
	public String getTag() {
		return tag;
	}

	public String getFilterId() {
		return MapPoiTypes.TOP_INDEX_ADDITIONAL_PREFIX + tag + "_" + getValueKey(value);
	}

	@Override
	public String getName() {
		// type of object: brand, operator
		AbstractPoiType pt = types.getAnyPoiAdditionalTypeByKey(tag);
		if (pt != null) {
			return pt.getTranslation();
		}
		return types.getPoiTranslation(tag);
	}

	@Override
	public String getIconResource() {
		//Example: mcdonalds, bank_of_america
		return valueKey;
	}

	public static String getValueKey(String value) {
		return value.toLowerCase().replace(':', '_').replaceAll("\'", "").replace(' ', '_').replaceAll("\"", "");
	}

	public String getValue() {
		return value;
	}
}
