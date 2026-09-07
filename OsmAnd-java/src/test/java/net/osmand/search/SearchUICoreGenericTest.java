package net.osmand.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.Street;
import net.osmand.osm.MapPoiTypes;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchCoreFactory.SearchStreetByCityAPI;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.search.core.spatial.SpatialTextSearchAPI;
import net.osmand.util.MapUtils;

public class SearchUICoreGenericTest {


	@BeforeClass
	public static void setUp() {
		SearchUICoreTest.defaultSetup();
	}

	@Test
	public void testSpatialPoiSearchAfterSelectingCity() {
		SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
		core.init(true);
		SpatialTextSearchAPI api = core.getApiByClass(SpatialTextSearchAPI.class);
		Assert.assertNotNull(api);

		SearchPhrase unselectedPhrase = core.resetPhrase("Kyiv pizzeria");
		Assert.assertTrue(api.isSearchAvailable(unselectedPhrase));
		Assert.assertTrue(api.getSearchPriority(unselectedPhrase) != -1);

		SearchResult cityResult = selectCity(core, createCity());
		SearchStreetByCityAPI cityApi = core.getApiByClass(SearchStreetByCityAPI.class);
		Assert.assertNotNull(cityApi);
		Assert.assertTrue("Selecting a city must still suggest streets",
				cityApi.getSearchPriority(core.getPhrase()) != -1);

		SearchPhrase selectedPhrase = core.resetPhrase("Kyiv pizzeria");
		Assert.assertSame(cityResult, selectedPhrase.getLastSelectedWord().getResult());
		Assert.assertEquals("pizzeria", selectedPhrase.getUnknownSearchPhrase());
		Assert.assertTrue(api.isSearchAvailable(selectedPhrase));
		Assert.assertTrue("Selecting a city must not disable spatial POI text search",
				api.getSearchPriority(selectedPhrase) != -1);
		Assert.assertTrue("Typed text must be handled by spatial search instead of the street list API",
				cityApi.getSearchPriority(selectedPhrase) == -1);
		Assert.assertTrue(core.immediateSearch("Kyiv pizzeria", cityResult.location).isSkipSorting());
	}

	@Test
	public void testSpatialSearchAfterSelectingCityWithoutFile() {
		SearchUICore core = new SearchUICore(MapPoiTypes.getDefault(), "en", false);
		core.init(true);
		City city = createCity();
		SearchResult cityResult = selectCity(core, city);
		Assert.assertNull(cityResult.file);
		Assert.assertNull(city.getReferenceFile());

		Street street = new Street(city);
		street.setName("Khreshchatyk");
		street.setLocation(cityResult.location);
		SpatialTextSearchAPI api = core.getApiByClass(SpatialTextSearchAPI.class);
		Assert.assertNotNull(api);
		// Keep the real API selection rules, replacing only the OBF lookup.
		core.apis.set(core.apis.indexOf(api), new SpatialTextSearchAPI(MapPoiTypes.getDefault()) {
			@Override
			public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
				SearchResult result = new SearchResult(phrase);
				result.object = street;
				result.objectType = ObjectType.STREET;
				result.localeName = street.getName();
				result.location = street.getLocation();
				return resultMatcher.publish(result);
			}
		});

		Assert.assertTrue(core.immediateSearch("Kyiv ", cityResult.location).getCurrentSearchResults().isEmpty());
		List<SearchResult> results = core.immediateSearch("Kyiv Khreshchatyk", cityResult.location)
				.getCurrentSearchResults();
		Assert.assertTrue("A city without a file must still allow spatial search results", results.size() == 1);
		SearchResult result = results.get(0);
		Assert.assertSame(street, result.object);
		Assert.assertSame(cityResult, result.requiredSearchPhrase.getLastSelectedWord().getResult());
		Assert.assertEquals("Kyiv Khreshchatyk", result.requiredSearchPhrase.getFullSearchPhrase());
	}
	

	@Test
	public void testDuplicates() throws IOException {
		SearchSettings ss = new SearchSettings((SearchSettings)null);
		ss = ss.setOriginalLocation(new LatLon(0, 0));
		SearchPhrase phrase = SearchPhrase.emptyPhrase(ss);
		SearchResultCollection cll = new SearchUICore.SearchResultCollection(phrase);
		List<SearchResult> rs = new ArrayList<>();
		SearchResult a1 = searchResult(rs, phrase, "a", 100);
		SearchResult b2 = searchResult(rs, phrase, "b", 200);
		SearchResult b1 = searchResult(rs, phrase, "b", 100);
		/*SearchResult a3 = */ searchResult(rs, phrase, "a", 100);
		cll.addSearchResults(rs, true, true);
		Assert.assertEquals(3, cll.getCurrentSearchResults().size());
		Assert.assertSame(a1, cll.getCurrentSearchResults().get(0));
		Assert.assertSame(b1, cll.getCurrentSearchResults().get(1));
		Assert.assertSame(b2, cll.getCurrentSearchResults().get(2));
	}
	
	@Test
	public void testNoResort() throws IOException {
		SearchSettings ss = new SearchSettings((SearchSettings)null);
		ss = ss.setOriginalLocation(new LatLon(0, 0));
		SearchPhrase phrase = SearchPhrase.emptyPhrase(ss);
		SearchResultCollection cll = new SearchUICore.SearchResultCollection(phrase);
		List<SearchResult> rs = new ArrayList<>();
		SearchResult a1 = searchResult(rs, phrase, "a", 100);
		cll.addSearchResults(rs, false, true);
		rs.clear();
		
		SearchResult b2 = searchResult(rs, phrase, "b", 200);
		cll.addSearchResults(rs, false, true);
		rs.clear();
		
		SearchResult b1 = searchResult(rs, phrase, "b", 100);
		cll.addSearchResults(rs, false, true);
		rs.clear();
		
		/*SearchResult a3 = */ searchResult(rs, phrase, "a", 100);
		cll.addSearchResults(rs, false, true);
		rs.clear();
		
		Assert.assertEquals(3, cll.getCurrentSearchResults().size());
		Assert.assertSame(a1, cll.getCurrentSearchResults().get(0));
		Assert.assertSame(b2, cll.getCurrentSearchResults().get(1));
		Assert.assertSame(b1, cll.getCurrentSearchResults().get(2));
		
		
		
	}
	
	
	@Test
	public void testNoResortDuplicate() throws IOException {
		SearchSettings ss = new SearchSettings((SearchSettings)null);
		ss = ss.setOriginalLocation(new LatLon(0, 0));
		SearchPhrase phrase = SearchPhrase.emptyPhrase(ss);
		SearchResultCollection cll = new SearchUICore.SearchResultCollection(phrase);
		List<SearchResult> rs = new ArrayList<>();
		SearchResult a1 = searchResult(rs, phrase, "a", 100);
		SearchResult b2 = searchResult(rs, phrase, "b", 200);
		SearchResult b1 = searchResult(rs, phrase, "b", 100);
		cll.addSearchResults(rs, false, true);
		rs.clear();
		/*SearchResult a3 = */ searchResult(rs, phrase, "a", 100);
		cll.addSearchResults(rs, false, true);
		rs.clear();
		
		Assert.assertEquals(3, cll.getCurrentSearchResults().size());
		Assert.assertSame(a1, cll.getCurrentSearchResults().get(0));
		Assert.assertSame(b1, cll.getCurrentSearchResults().get(1));
		Assert.assertSame(b2, cll.getCurrentSearchResults().get(2));
	}

	private City createCity() {
		City city = new City(City.CityType.CITY);
		city.setName("Kyiv");
		city.setLocation(50.4501, 30.5234);
		return city;
	}

	private SearchResult selectCity(SearchUICore core, City city) {
		SearchResult result = new SearchResult(core.resetPhrase(city.getName()));
		result.object = city;
		result.objectType = ObjectType.CITY;
		result.localeName = city.getName();
		result.location = city.getLocation();
		core.selectSearchResult(result);
		return result;
	}

	private SearchResult searchResult(List<SearchResult> rs, SearchPhrase phrase, String text, int dist) {
		SearchResult res = new SearchResult(phrase);
		res.localeName = text;
		double d1 = MapUtils.getDistance(0, 0, 0, 1);
		res.location = new LatLon(0, dist / d1);
		rs.add(res);
		return res;
	}

	
}
