package net.osmand.plus.plugins.driverbreak;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.data.LatLon;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class PoiDiscoveryTest {

	@Test
	public void dntPrioritySortsNetworkCabinsFirst() {
		List<RestStop.NearbyPoi> pois = new ArrayList<>();
		pois.add(new RestStop.NearbyPoi(new LatLon(60.0, 10.0), "Far hut", "alpine_hut", 5000.0, false));
		pois.add(new RestStop.NearbyPoi(new LatLon(60.01, 10.01), "DNT hut", "wilderness_hut", 8000.0, true));
		List<RestStop.NearbyPoi> sorted = PoiDiscovery.sortPois(pois, 60.0, 10.0, true);
		Assert.assertTrue(sorted.get(0).isNetworkPriority());
	}

	@Test
	public void isNetworkTagDetectsDntOperator() {
		Assert.assertTrue(PoiDiscovery.isNetworkTag("DNT Oslo", null));
		Assert.assertTrue(PoiDiscovery.isNetworkTag(null, "SAC"));
		Assert.assertFalse(PoiDiscovery.isNetworkTag("Private owner", null));
	}

	@Test
	public void hikingOverpassQueryIncludesWaterAndCabins() {
		String query = PoiDiscovery.buildOverpassQuery(61.0, 9.0, TravelMode.HIKING, 2000);
		Assert.assertTrue(query.contains("drinking_water"));
		Assert.assertTrue(query.contains("wilderness_hut"));
	}

	@Test
	public void matchesAmenitySubTypeIncludesDrinkingWaterForHiking() {
		Assert.assertTrue(PoiDiscovery.matchesAmenitySubType("drinking_water", TravelMode.HIKING));
		Assert.assertTrue(PoiDiscovery.matchesAmenitySubType("spring", TravelMode.HIKING));
		Assert.assertFalse(PoiDiscovery.matchesAmenitySubType("fuel", TravelMode.HIKING));
	}

	@Test
	public void sampleRouteLocationsKeepsEndpointsAndReducesPoints() {
		java.util.List<net.osmand.Location> route = new java.util.ArrayList<>();
		for (int i = 0; i < 200; i++) {
			net.osmand.Location location = new net.osmand.Location("");
			location.setLatitude(61.0 + i * 0.001);
			location.setLongitude(10.0);
			route.add(location);
		}
		java.util.List<net.osmand.Location> sampled = PoiDiscovery.sampleRouteLocations(route, 5000);
		Assert.assertTrue(sampled.size() < route.size());
		Assert.assertEquals(route.get(0).getLatitude(), sampled.get(0).getLatitude(), 0.0001);
		Assert.assertEquals(route.get(route.size() - 1).getLatitude(),
				sampled.get(sampled.size() - 1).getLatitude(), 0.0001);
	}

	@Test
	public void routePoiIndexWithinRadiusFiltersByDistance() {
		java.util.List<RestStop.NearbyPoi> pois = new java.util.ArrayList<>();
		pois.add(new RestStop.NearbyPoi(new LatLon(61.0, 10.0), "Near", "drinking_water", 0.0));
		pois.add(new RestStop.NearbyPoi(new LatLon(61.05, 10.05), "Far", "drinking_water", 0.0));
		PoiDiscovery.RoutePoiIndex index = new PoiDiscovery.RoutePoiIndex(pois);
		java.util.List<RestStop.NearbyPoi> near = index.withinRadius(61.0, 10.0, 3000, false);
		Assert.assertEquals(1, near.size());
		Assert.assertEquals("Near", near.get(0).getName());
	}

	@Test
	public void overpassSampleIndicesCoversRouteEnds() {
		int[] indices = PoiDiscovery.overpassSampleIndices(100, 3);
		Assert.assertEquals(3, indices.length);
		Assert.assertEquals(0, indices[0]);
		Assert.assertEquals(99, indices[2]);
	}
}
