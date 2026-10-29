package net.osmand.search.core.spatial;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapPoiTypes.PoiTranslator;
import net.osmand.search.core.spatial.SpatialPoiSearch.SpatialPoiType;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialSearchResults;
import net.osmand.search.core.spatial.SpatialTextSearch.SpatialTextSearchSettings;

public class SpatialPoiSearchTest {

	@Test
	public void testMatchedNameKeepsDiacritics() throws IOException {
		MapPoiTypes poiTypes = new MapPoiTypes(null);
		poiTypes.init();
		poiTypes.setPoiTranslator(new PhrasesTranslator(Map.of(
				"sports_centre", "Sports center;Спортивний центр;спорткомплекс",
				"shelter", "Shelter;Unterstand;Schutzhütte")));
		SpatialPoiSearch poiSearch = new SpatialPoiSearch(poiTypes);

		Assert.assertEquals("Спортивний центр", getMatchedName(poiSearch, "спорт", "sports_centre"));
		Assert.assertEquals("Schutzhütte", getMatchedName(poiSearch, "schutz", "shelter"));
	}

	private static String getMatchedName(SpatialPoiSearch poiSearch, String query, String categoryKey) throws IOException {
		SpatialSearchContext ctx = new SpatialSearchContext(SpatialTextSearchSettings.suggestionSettings(),
				Collections.emptyList(), poiSearch, new LatLon(51.3397, 12.3731));
		SpatialSearchResults res = new SpatialTextSearch().searchAPI(query, ctx);
		Assert.assertNotNull(res.mainResults);
		for (SpatialSearchResult r : res.mainResults) {
			SpatialPoiType category = r.getPoiCategory(poiSearch);
			if (category != null && category.getKey().equals(categoryKey)) {
				return r.getPoiCategoryMatchedName(poiSearch);
			}
		}
		Assert.fail("No category " + categoryKey + " for " + query);
		return null;
	}

	private record PhrasesTranslator(Map<String, String> phrases) implements PoiTranslator {

		@Override
		public String getTranslation(AbstractPoiType type) {
			return getTranslation(type.getKeyName());
		}

		@Override
		public String getTranslation(String keyName) {
			String phrase = phrases.get(keyName);
			return phrase == null ? null : phrase.split(";")[0];
		}

		@Override
		public String getEnTranslation(AbstractPoiType type) {
			return null;
		}

		@Override
		public String getEnTranslation(String keyName) {
			return null;
		}

		@Override
		public String getSynonyms(AbstractPoiType type) {
			return getSynonyms(type.getKeyName());
		}

		@Override
		public String getSynonyms(String keyName) {
			String phrase = phrases.get(keyName);
			return phrase == null ? null : phrase.substring(phrase.indexOf(';') + 1);
		}

		@Override
		public String getAllLanguagesTranslationSuffix() {
			return "";
		}
	}
}
