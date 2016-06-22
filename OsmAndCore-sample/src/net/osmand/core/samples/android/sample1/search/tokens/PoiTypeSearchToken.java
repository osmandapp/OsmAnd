package net.osmand.core.samples.android.sample1.search.tokens;

import android.support.annotation.NonNull;

import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;

public class PoiTypeSearchToken extends SearchToken {
	private PoiType poiType;

	public PoiTypeSearchToken(@NonNull PoiType poiType, int startIndex, String queryText) {
		super(TokenType.POI_TYPE, startIndex, queryText, poiType.getKeyName());
		this.poiType = poiType;
	}

	public PoiType getPoiType() {
		return poiType;
	}
}
