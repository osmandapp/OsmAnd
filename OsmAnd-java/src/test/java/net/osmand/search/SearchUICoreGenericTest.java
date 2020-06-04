package net.osmand.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultCollection;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchSettings;
import net.osmand.util.MapUtils;

public class SearchUICoreGenericTest {


	@BeforeClass
	public static void setUp() {
		SearchUICoreTest.defaultSetup();
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

	private SearchResult searchResult(List<SearchResult> rs, SearchPhrase phrase, String text, int dist) {
		SearchResult res = new SearchResult(phrase);
		res.localeName = text;
		double d1 = MapUtils.getDistance(0, 0, 0, 1);
		res.location = new LatLon(0, dist / d1);
		rs.add(res);
		return res;
	}

	
}
