package net.osmand.search;

import java.io.IOException;

import junit.framework.TestCase;
import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchCoreFactory.SearchLocationAndUrlAPI;
import net.osmand.search.core.SearchPhrase;

import org.junit.Assert;
import org.junit.Test;

public class TestLocationSearch {
	

	@Test
	public void testLocationSearch() throws IOException {
		search("5.0,3.0", new LatLon(5, 3));
	}

	private void search(String string, LatLon latLon) throws IOException {
		SearchResultMatcher srm = new SearchUICore.SearchResultMatcher(null, 0, null, 100);
		new SearchCoreFactory.SearchLocationAndUrlAPI().
			search(new SearchPhrase(null).generateNewPhrase(string, null), srm);
		Assert.assertEquals(srm.getRequestResults().size(), 1);
		Assert.assertEquals(srm.getRequestResults().get(0).location, latLon);
	}
}
