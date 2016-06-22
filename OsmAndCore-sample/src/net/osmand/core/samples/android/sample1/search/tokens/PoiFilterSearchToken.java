package net.osmand.core.samples.android.sample1.search.tokens;

import android.support.annotation.NonNull;

import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;

public class PoiFilterSearchToken extends SearchToken {
	private PoiFilter poiFilter;

	public PoiFilterSearchToken(@NonNull PoiFilter poiFilter, int startIndex, String queryText) {
		super(TokenType.POI_FILTER, startIndex, queryText, poiFilter.getKeyName());
		this.poiFilter = poiFilter;
	}

	public PoiFilter getPoiFilter() {
		return poiFilter;
	}
}
