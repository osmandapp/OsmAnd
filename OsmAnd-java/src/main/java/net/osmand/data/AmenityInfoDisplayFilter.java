package net.osmand.data;

import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;

public final class AmenityInfoDisplayFilter {

	private AmenityInfoDisplayFilter() {
	}

	public static boolean shouldDisplayKey(String key, String subtype, MapPoiTypes poiTypes) {
		AbstractPoiType t = poiTypes.getAnyPoiAdditionalTypeByKey(key);
		if (t instanceof PoiType poiType && poiType.isHidden()) {
			return false;
		}
		if (key.contains(Amenity.WIKIPEDIA)
				|| key.contains(Amenity.CONTENT)
				|| key.contains(Amenity.SHORT_DESCRIPTION)
				|| key.contains(MapPoiTypes.WIKI_LANG)) {
			return false;
		}
		if (MapPoiTypes.ROUTE_ARTICLE.equals(subtype) && key.contains(Amenity.DESCRIPTION)) {
			return false;
		}
		if (Amenity.LANG_YES.equals(key) || Amenity.SUBWAY_REGION.equals(key) || key.contains(Amenity.ROUTE)) {
			return false;
		}
		return !Amenity.NAME.equals(key);
	}
}
