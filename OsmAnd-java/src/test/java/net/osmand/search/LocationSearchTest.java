package net.osmand.search;

import java.io.IOException;

import net.osmand.OsmAndCollator;
import net.osmand.data.LatLon;
import net.osmand.search.SearchUICore.SearchResultMatcher;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;


public class LocationSearchTest {

	private void search(String string, LatLon latLon) throws IOException {
		SearchResultMatcher srm = new SearchUICore.SearchResultMatcher(null, null, 0, null, 100);
		new SearchCoreFactory.SearchLocationAndUrlAPI(null).
			search(SearchPhrase.emptyPhrase().generateNewPhrase(string, null), srm);
		Assert.assertEquals(1, srm.getRequestResults().size());
		Assert.assertEquals(latLon, srm.getRequestResults().get(0).location);
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
	public void testUTMSearch() throws IOException {
		search("17N6734294749123", new LatLon(42.875017, -78.87659050764749));
		search("17 N 673429 4749123", new LatLon(42.875017, -78.87659050764749));
		search("36N 609752 5064037", new LatLon(45.721184, 34.410328));
		
	}
	
	@Test
	public void testBasicSpaceSearch() throws IOException {
		search("5.0 3.0", new LatLon(5, 3));
		search("-5.0 -3.0", new LatLon(-5, -3));
		search("-45.5 3.0S", new LatLon(-45.5, -3));
		search("45.5S 3.0 W", new LatLon(-45.5, -3));
		search("5.445 3.523", new LatLon(5.445, 3.523));
		search("5:1:1 3:1", new LatLon(5 + 1/60f + 1/3600f, 3 + 1/60f));
		search("5:1#1 3#1", new LatLon(5 + 1/60f + 1/3600f, 3 + 1/60f));
		search("5#1#1 3#1", new LatLon(5 + 1/60f + 1/3600f, 3 + 1/60f));
		search("5'1'1 3'1", new LatLon(5 + 1/60f + 1/3600f, 3 + 1/60f));
		search("Lat: 5.0 Lon: 3.0", new LatLon(5, 3));
		search("0 n, 78 w", new LatLon(0, -78));
		search("0 N, 78 W", new LatLon(0, -78));
		search("N 0 W 78", new LatLon(0, -78));
		search("n 0 w 78", new LatLon(0, -78));
	}
	
	@Test
	public void testSimpleURLSearch() throws IOException {
		search("ftp://simpleurl?lat=34.23&lon=-53.2&z=15", new LatLon(34.23, -53.2));
		search("ftp://simpleurl?z=15&lat=34.23&lon=-53.2", new LatLon(34.23, -53.2));
	}
	
	@Test
	public void testAdvancedSpaceSearch() throws IOException {
		search("5 30 30 N 4 30 W", new LatLon(5.5 + 30 / 3600f, -4.5));
		search("5 30  -4 30", new LatLon(5.5, -4.5));
		search("S 5 30  4 30 W", new LatLon(-5.5, -4.5));
		search("S5.4232  4.30W", new LatLon(-5.4232, -4.3));
		search("S5.4232  W4.30", new LatLon(-5.4232, -4.3));
		search("5.4232, W4.30", new LatLon(5.4232, -4.3));
		search("5.4232N, 45 30.5W", new LatLon(5.4232, -(45 + 30.5 / 60f)));
	}
	
	@Test
	public void testArcgisSpaceSearch() throws IOException {
		search("43°S 79°23′13.7″W", new LatLon(-43,-(79 + 23/60f + 13.7/3600f)));
		search("43°38′33.24″N 79°23′13.7″W", new LatLon(43 + 38/60f + 33.24/3600f,-(79 + 23/60f + 13.7/3600f)));
		search("45° 30'30\"W 3.0", new LatLon(45 + 0.5 + 1 / 120f, -3));
		search("43° 79°23′13.7″E", new LatLon(43,79 + 23/60f + 13.7/3600f));
		search("43°38′ 79°23′13.7″E", new LatLon(43 + 38/60f,79 + 23/60f + 13.7/3600f));
		search("43°38′23\" 79°23′13.7″E", new LatLon(43 + 38/60f + 23/3600f,79 + 23/60f + 13.7/3600f));
	}

	@Test
	public void testCommaLatLonSearch() throws IOException {
		
		search("33,95060 °S, 151,14453° E", new LatLon(-33.95060, 151.14453));
		search("33,95060, 151,14453", new LatLon(33.95060, 151.14453));
		search("33,95060 151,14453", new LatLon(33.95060,151.14453));

		search("15,1235 S, 23,1244 W", new LatLon(-15.1235, -23.1244));
		search("-15,1235, 23,1244", new LatLon(-15.1235, 23.1244));
//		search("15,1235 S, 23,1244", new LatLon(-15.1235, 23.1244));
	}
}
