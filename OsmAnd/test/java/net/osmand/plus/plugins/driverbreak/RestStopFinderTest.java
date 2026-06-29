package net.osmand.plus.plugins.driverbreak;

import net.osmand.data.LatLon;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RestStopFinderTest {

	@Test
	public void limitPoisReturnsAllWhenUnderCap() {
		List<RestStop.NearbyPoi> pois = new ArrayList<>();
		pois.add(new RestStop.NearbyPoi(new LatLon(60.0, 10.0), "A", "fuel", 100.0));
		pois.add(new RestStop.NearbyPoi(new LatLon(60.01, 10.01), "B", "cafe", 200.0));
		List<RestStop.NearbyPoi> limited = RestStopFinder.limitPois(pois, 5);
		Assert.assertEquals(2, limited.size());
		Assert.assertSame(pois, limited);
	}

	@Test
	public void limitPoisTruncatesToMaxCount() {
		List<RestStop.NearbyPoi> pois = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			pois.add(new RestStop.NearbyPoi(new LatLon(60.0 + i * 0.001, 10.0), "P" + i, "fuel", i * 10.0));
		}
		List<RestStop.NearbyPoi> limited = RestStopFinder.limitPois(pois, 3);
		Assert.assertEquals(3, limited.size());
		Assert.assertEquals("P0", limited.get(0).getName());
		Assert.assertEquals("P2", limited.get(2).getName());
	}
}
