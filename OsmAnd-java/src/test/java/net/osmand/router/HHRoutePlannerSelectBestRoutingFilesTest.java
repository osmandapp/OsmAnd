package net.osmand.router;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryHHRouteReaderAdapter.HHRouteRegion;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.router.HHRouteDataStructure.HHRouteRegionPointsCtx;
import net.osmand.router.HHRouteDataStructure.HHRoutingContext;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.util.MapUtils;

public class HHRoutePlannerSelectBestRoutingFilesTest {

	@Test
	public void testKyivToBukovelPrefersOlderUkraineMaps() throws IOException {
		LatLon start = new LatLon(50.451383, 30.509932);
		LatLon end = new LatLon(48.363032, 24.414250);
		// No ukraine_europe file exists. Keep the endpoint oblasts and intermediate maps in the same group.
		BinaryMapIndexReader kyiv = reader("Ukraine_kyiv_europe", 1, new QuadRect(29, 51.5, 32, 49));
		BinaryMapIndexReader bukovel = reader("Ukraine_ivano-frankivsk_europe", 1, new QuadRect(23.5, 49, 25.5, 47.5));
		BinaryMapIndexReader ternopil = reader("Ukraine_ternopil_europe", 1, new QuadRect(24.5, 50.3, 26.5, 48.5));
		// The newer neighbours intersect the route bbox, but cannot cover both endpoints.
		BinaryMapIndexReader moldova = reader("Moldova_europe", 2, new QuadRect(26.5, 48.5, 30.5, 45));
		BinaryMapIndexReader romania = reader("Romania_europe", 2, new QuadRect(20, 48.5, 30, 43.5));

		assertEquals(new HashSet<>(Arrays.asList(kyiv, bukovel, ternopil)),
				selectMaps(start, end, moldova, romania, kyiv, bukovel, ternopil));
	}

	@Test
	public void testChepstowToCardiffPrefersWalesPolygon() throws IOException {
		LatLon start = new LatLon(51.643, -2.676);
		LatLon end = new LatLon(51.4816, -3.1791);
		BinaryMapIndexReader wales = reader("Gb_wales_europe", 1, new QuadRect(-5.5, 53.5, -2, 51));
		// Both maps' routing bboxes cover both endpoints. The South West England polygon only covers the start.
		BinaryMapIndexReader england = reader("Gb_england_south-west-england_europe", 2, new QuadRect(-6, 52, -1, 49));

		assertEquals(Collections.singleton(wales), selectMaps(start, end, england, wales));
	}

	private Set<BinaryMapIndexReader> selectMaps(LatLon start, LatLon end, BinaryMapIndexReader... readers)
			throws IOException {
		try {
			RoutePlannerFrontEnd frontEnd = new RoutePlannerFrontEnd();
			RoutingConfiguration config = RoutingConfiguration.getDefault().build("car",
					new RoutingMemoryLimits(64, 64), Collections.emptyMap());
			RoutingContext ctx = frontEnd.buildRoutingContext(config, null, readers);
			frontEnd.calculateRegionsWithAllRoutePoints(ctx, PlatformUtil.getOsmandRegions(), start,
					Collections.singletonList(end));
			HHRoutingContext<NetworkDBPoint> hctx = new HHRoutingContext<>();
			hctx.rctx = ctx;
			HHRoutingContext<NetworkDBPoint> selected = HHRoutePlanner.create(ctx)
					.selectBestRoutingFiles(start, end, hctx, false);
			Set<BinaryMapIndexReader> result = new HashSet<>();
			for (HHRouteRegionPointsCtx<NetworkDBPoint> region : selected.regions) {
				result.add(region.file);
			}
			return result;
		} finally {
			for (BinaryMapIndexReader reader : readers) {
				reader.close();
			}
		}
	}

	private BinaryMapIndexReader reader(String name, long edition, QuadRect bounds) throws IOException {
		// Model routing block bounds without loading OBF data; use the real region polygons and group selection.
		BinaryMapIndexReader reader = new BinaryMapIndexReader(null, new File(name + "_2.obf"), false) {
			@Override
			public boolean containsActualRouteData(int x31, int y31, Set<String> checkedRegions) {
				double lon = MapUtils.get31LongitudeX(x31);
				double lat = MapUtils.get31LatitudeY(y31);
				return bounds.contains(lon, lat, lon, lat);
			}
		};
		RouteRegion routeRegion = new RouteRegion();
		routeRegion.setName(name);
		reader.getRoutingIndexes().add(routeRegion);
		HHRouteRegion hhRegion = new HHRouteRegion() {
			@Override
			public QuadRect getLatLonBbox() {
				return bounds;
			}
		};
		hhRegion.profile = "car";
		hhRegion.profileParams.add("");
		hhRegion.edition = edition;
		reader.getHHRoutingIndexes().add(hhRegion);
		return reader;
	}
}
