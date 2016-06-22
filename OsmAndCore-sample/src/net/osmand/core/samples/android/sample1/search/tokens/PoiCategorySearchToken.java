package net.osmand.core.samples.android.sample1.search.tokens;

import android.support.annotation.NonNull;

import net.osmand.osm.PoiCategory;

public class PoiCategorySearchToken extends SearchToken {
	private PoiCategory poiCategory;

	public PoiCategorySearchToken(@NonNull PoiCategory poiCategory, int startIndex, String queryText) {
		super(TokenType.POI_CATEGORY, startIndex, queryText, poiCategory.getKeyName());
		this.poiCategory = poiCategory;
	}

	public PoiCategory getPoiCategory() {
		return poiCategory;
	}
}
