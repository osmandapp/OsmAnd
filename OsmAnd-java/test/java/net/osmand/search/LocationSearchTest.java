package net.osmand.search;

import java.io.IOException;

import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;

import org.junit.Assert;
import org.junit.Test;


public class LocationSearchTest {

	private void search(String string, LatLon latLon) throws IOException {
		SearchResultMatcher srm = new SearchUICore.SearchResultMatcher(null, 0, null, 100);
		new SearchCoreFactory.SearchLocationAndUrlAPI().
			search(new SearchPhrase(null).generateNewPhrase(string, null), srm);
		Assert.assertEquals(srm.getRequestResults().size(), 1);
		Assert.assertEquals(srm.getRequestResults().get(0).location, latLon);
	}
	
	@Test
	public void testGeo() throws IOException {
		search("geo:34.99393,-106.61568 (Treasure Island, other irrelevant info) ", new LatLon(34.99393, -106.61568));
		search("http://download.osmand.net/go?lat=34.99393&lon=-106.61568&z=11", new LatLon(34.99393, -106.61568));
	}
	
	@Test
	public void testBasicCommaSearch() throws IOException {
		search("5.0,3.0", new LatLon(5, 3));
		search("5.445,3.523", new LatLon(5.445, 3.523));
		search("5:1:1,3:1", new LatLon(5 + 1/60f + 1/3600f, 3 + 1/60f));
	}

	@Test
	public void testBasicSpaceSearch() throws IOException {
		search("5.0 3.0", new LatLon(5, 3));
		search("5.445 3.523", new LatLon(5.445, 3.523));
		search("5:1:1 3:1", new LatLon(5 + 1/60f + 1/3600f, 3 + 1/60f));
	}
	// TODO 17R 419230 2714967
//	17N6734294749123
//	45° 30'30"W
//	-45
//	45 W
//	45 S
//	45.50W
//	45.50S
//	W45
//	S45
//	45 30.5W
//	44 30.5S
//	45 30 30 W
//	45 30 30 N
//	-45 30 30
//	45 30 30
//	45 30.50W
//	45 30.50
	

	

	
}
