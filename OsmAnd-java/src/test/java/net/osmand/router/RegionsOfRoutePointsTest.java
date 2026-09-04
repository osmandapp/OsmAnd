package net.osmand.router;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;

/**
 * RoutingContext.regionsCoveringStartAndTargets feeds HHRoutePlanner.containsStartEndRegion(),
 * which compares them with the names of the routing sections of the downloaded maps.
 * The regions of every route point have to be listed, at every level of the hierarchy.
 */
public class RegionsOfRoutePointsTest {

	private static final LatLon KYIV = new LatLon(50.4501, 30.5234);
	private static final LatLon BUKOVEL = new LatLon(48.3585, 24.4045);
	private static final LatLon BUCHAREST = new LatLon(44.4268, 26.1025);

	@Test
	public void testRegionsOfRouteAcrossSubRegions() throws IOException {
		OsmandRegions or = PlatformUtil.getOsmandRegions();
		List<String> regions = RoutePlannerFrontEnd.collectRegionsOfRoutePoints(or, KYIV,
				Collections.singletonList(BUKOVEL));

		// Ukraine is published as oblasts only - the maps to route Kyiv -> Bukovel are named
		// after the sub-regions, so listing ukraine_europe alone would match no file at all
		assertTrue(regions.toString(), regions.contains("ukraine_kyiv_europe"));
		assertTrue(regions.toString(), regions.contains("ukraine_ivano-frankivsk_europe"));

		// the country is still listed, to match a country-wide map where one is published
		assertTrue(regions.toString(), regions.contains("ukraine_europe"));

		// neighbouring countries must not be listed: their maps must not win the group ranking
		assertFalse(regions.toString(), regions.contains("moldova_europe"));
		assertFalse(regions.toString(), regions.contains("romania_europe"));
	}

	@Test
	public void testRegionsOfCrossBorderRoute() throws IOException {
		OsmandRegions or = PlatformUtil.getOsmandRegions();
		List<String> regions = RoutePlannerFrontEnd.collectRegionsOfRoutePoints(or, KYIV,
				Arrays.asList(BUKOVEL, BUCHAREST));

		assertTrue(regions.toString(), regions.contains("ukraine_kyiv_europe"));
		assertTrue(regions.toString(), regions.contains("ukraine_ivano-frankivsk_europe"));
		assertTrue(regions.toString(), regions.contains("romania_europe"));
	}
}
