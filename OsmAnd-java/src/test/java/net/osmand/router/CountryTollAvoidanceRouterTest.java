package net.osmand.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.RouteDataObject;
import net.osmand.NativeLibrary;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.GeneralRouter.RouteAttributeEvalRule;
import net.osmand.router.GeneralRouter.RouteDataObjectAttribute;
import net.osmand.shared.routing.GeneralRouterProfile;
import net.osmand.util.MapUtils;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CountryTollAvoidanceRouterTest {

	@Test
	public void appliesExistingTollPenaltyOnlyInSelectedCountries() {
		GeneralRouter base = createRouter(Collections.emptyMap());
		GeneralRouter countryRouter = new CountryTollAvoidanceRouter(base, (x, y) -> x < 100);
		RouteRegion region = createRegion();
		RouteDataObject selected = createRoad(region, 50, true);
		RouteDataObject unselected = createRoad(region, 150, true);
		GeneralRouter tollRouter = createRouter(Collections.singletonMap(GeneralRouter.AVOID_TOLL, "true"));

		assertEquals(tollRouter.defineSpeedPriority(selected, true), countryRouter.defineSpeedPriority(selected, true), 0);
		// Same map region and tags, but a different country: do not share the geographic result.
		assertEquals(base.defineSpeedPriority(unselected, true), countryRouter.defineSpeedPriority(unselected, true), 0);
		assertEquals(0.1f, countryRouter.defineSpeedPriority(selected, false), 0);
		assertEquals(1f, countryRouter.defineSpeedPriority(unselected, false), 0);
	}

	@Test
	public void doesNotPenalizeFreeRoadsOrLookUpTheirCountry() {
		AtomicInteger queries = new AtomicInteger();
		GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> {
			queries.incrementAndGet();
			return true;
		});
		RouteDataObject road = createRoad(createRegion(), 50, false);
		assertEquals(1f, router.defineSpeedPriority(road, true), 0);
		assertEquals(0f, router.defineRoutingObstacle(road, 0, false), 0);
		assertEquals(0, queries.get());
	}

	@Test
	public void appliesTollBoothPenaltyAtTheBoothsLocation() {
		GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> x < 100);
		RouteDataObject road = createRoad(createRegion(), 50, false);
		road.pointsX = new int[] {50, 150};
		road.pointTypes = new int[][] {new int[] {3}, new int[] {3}};

		assertEquals(900f, router.defineRoutingObstacle(road, 0, false), 0);
		assertEquals(0f, router.defineRoutingObstacle(road, 1, false), 0);
		assertEquals(900f, router.defineRoutingObstacle(road, 0, true), 0);
		// This is a routing penalty, not extra time to add to the displayed ETA.
		assertEquals(180f, router.defineObstacle(road, 0, false), 0);
	}

	@Test
	public void globalAvoidTollsStillAppliesOutsideSelectedCountries() {
		GeneralRouter router = new CountryTollAvoidanceRouter(
				createRouter(Collections.singletonMap(GeneralRouter.AVOID_TOLL, "true")), (x, y) -> false);
		assertEquals(0.1f, router.defineSpeedPriority(createRoad(createRegion(), 150, true), true), 0);
	}

	@Test
	public void disablingCountryAvoidanceRestoresNormalRouting() {
		GeneralRouter base = createRouter(Collections.emptyMap());
		RouteDataObject road = createRoad(createRegion(), 50, true);
		assertEquals(0.1f, new CountryTollAvoidanceRouter(base, (x, y) -> true).defineSpeedPriority(road, true), 0);
		assertEquals(1f, new CountryTollAvoidanceRouter(base, (x, y) -> false).defineSpeedPriority(road, true), 0);
		assertEquals(1f, base.defineSpeedPriority(road, true), 0);
	}

	@Test
	public void preservesOtherProfileParameters() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("profile_truck", "true");
		params.put(GeneralRouter.VEHICLE_HEIGHT, "4.2");
		GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(params), (x, y) -> true);
		assertEquals(params, router.getParameterValues());
		assertFalse(params.containsKey(GeneralRouter.AVOID_TOLL));
	}

	@Test
	public void preservesExplicitlyAvoidedRoadsAndAccessRules() {
		GeneralRouter base = createRouter(Collections.emptyMap());
		RouteDataObject road = createRoad(createRegion(), 50, true);
		base.setImpassableRoads(Collections.singleton(road.id >> GeneralRouter.IMPASSABLE_ROAD_SHIFT));
		GeneralRouter router = new CountryTollAvoidanceRouter(base, (x, y) -> true);
		assertFalse(router.acceptLine(road));
		assertTrue(router.acceptLine(createRoad(createRegion(), 150, true)));
		RouteDataObject closed = createRoad(createRegion(), 75, true);
		closed.types = new int[] {1, 2, 4};
		assertFalse(router.acceptLine(closed));
	}

	@Test
	public void cachesCountryChecksAndClearsThemWithRoutingCaches() {
		AtomicInteger queries = new AtomicInteger();
		GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> {
			queries.incrementAndGet();
			return true;
		});
		RouteDataObject road = createRoad(createRegion(), 50, true);
		router.defineSpeedPriority(road, true);
		router.defineSpeedPriority(road, false);
		assertEquals(1, queries.get());
		router.clearCaches();
		router.defineSpeedPriority(road, true);
		assertEquals(2, queries.get());
	}

	@Test
	public void rebuildingRouterRetainsCountrySelection() {
		GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> x < 100);
		router.setImpassableRoads(Collections.singleton(50L >> GeneralRouter.IMPASSABLE_ROAD_SHIFT));
		GeneralRouter rebuilt = router.build(Collections.emptyMap());
		assertTrue(rebuilt instanceof CountryTollAvoidanceRouter);
		assertEquals(0.1f, rebuilt.defineSpeedPriority(createRoad(createRegion(), 50, true), true), 0);
		assertEquals(1f, rebuilt.defineSpeedPriority(createRoad(createRegion(), 150, true), true), 0);
		assertFalse(rebuilt.acceptLine(createRoad(createRegion(), 50, true)));
	}

	@Test
	public void rebuiltAndPlainRoutersKeepUpdatedRoadRestrictionsAndProfileParameters() {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> true);
		RouteDataObject road = createRoad(createRegion(), 150, true);
		router.setImpassableRoads(Collections.singleton(road.id >> GeneralRouter.IMPASSABLE_ROAD_SHIFT));
		Map<String, String> parameters = Collections.singletonMap(GeneralRouter.VEHICLE_HEIGHT, "4.2");
		CountryTollAvoidanceRouter rebuilt = (CountryTollAvoidanceRouter) router.build(parameters);
		GeneralRouter plain = rebuilt.withoutCountryAvoidance();
		assertEquals(parameters, plain.getParameterValues());
		assertFalse(rebuilt.acceptLine(road));
		assertFalse(plain.acceptLine(road));
		assertEquals(1f, plain.defineSpeedPriority(road, true), 0);
		assertEquals(0.1f, rebuilt.defineSpeedPriority(road, true), 0);
		plain.setImpassableRoads(Collections.emptySet());
		assertTrue(plain.acceptLine(road));
		assertFalse("Rebuilding must copy, not share mutable road restrictions", rebuilt.acceptLine(road));
	}

	@Test
	public void checkingOrdinaryRoadsSkipsAllCountryQueries() {
		AtomicInteger queries = new AtomicInteger();
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> {
			queries.incrementAndGet();
			return true;
		});
		RouteDataObject road = createRoad(createRegion(), 50, false);
		road.pointsX = new int[10000];
		road.pointsY = new int[10000];
		assertFalse(router.affectsRoute(Collections.singletonList(new RouteSegmentResult(road, 0, 9999))));
		assertEquals(0, queries.get());
	}

	@Test
	public void checkingRouteSkipsNonTollProfileRuleEvaluations() {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> true) {
			@Override
			public float defineSpeedPriority(RouteDataObject road, boolean dir) {
				org.junit.Assert.fail("Ordinary road priority must not be evaluated by affectsRoute");
				return 0;
			}

			@Override
			public float defineRoutingObstacle(RouteDataObject road, int point, boolean backward) {
				org.junit.Assert.fail("Ordinary points must not be evaluated by affectsRoute");
				return 0;
			}
		};
		assertFalse(router.affectsRoute(Collections.singletonList(
				new RouteSegmentResult(createRoad(createRegion(), 50, false), 0, 1))));
	}

	@Test
	public void checkingRebuiltGlobalRouterHasNoAdditionalCountryCosts() {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> true);
		router = (CountryTollAvoidanceRouter) router.build(Collections.singletonMap(GeneralRouter.AVOID_TOLL, "true"));
		RouteDataObject road = createRoad(createRegion(), 50, true);
		road.pointTypes = new int[][] {new int[] {3}, new int[] {3}};
		assertFalse(router.affectsRoute(Collections.singletonList(new RouteSegmentResult(road, 0, 1))));
		assertEquals(0.1f, router.defineSpeedPriority(road, true), 0);
		assertEquals(900f, router.defineRoutingObstacle(road, 0, false), 0);
	}

	@Test
	public void countryCacheStaysCorrectAfterEviction() {
		AtomicInteger queries = new AtomicInteger();
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> {
			queries.incrementAndGet();
			return x % 2 == 0;
		});
		RouteRegion region = createRegion();
		for (int i = 0; i < 10000; i++) {
			assertEquals(i % 2 == 0 ? 0.1f : 1f, router.defineSpeedPriority(createRoad(region, i, true), true), 0);
		}
		assertEquals(10000, queries.get());
		assertEquals(0.1f, router.defineSpeedPriority(createRoad(region, 0, true), true), 0);
		assertEquals("The oldest entry must have been evicted", 10001, queries.get());
	}

	@Test
	public void emptySelectionNeverPenalizesRoadsOrBooths() throws Exception {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()),
				new OsmandRegions(false), Collections.emptyList());
		RouteDataObject road = createRoad(createRegion(), 50, true);
		road.pointTypes = new int[][] {new int[] {3}, new int[] {3}};
		assertEquals(1f, router.defineSpeedPriority(road, true), 0);
		assertEquals(0f, router.defineRoutingObstacle(road, 0, false), 0);
		assertFalse(router.affectsRoute(Collections.singletonList(new RouteSegmentResult(road, 0, 1))));
		assertEquals(0, router.getNativeCountryPolygons().length);
	}

	@Test
	public void matchesCountryBoundariesForRegionalMapsAndCrossBorderRoutes() throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), regions,
					Arrays.asList("europe_austria", "europe_czech-republic", "russia", "northamerica_us"));
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(48.2082, 16.3738), true), 0); // Vienna
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(50.0875, 14.4213), true), 0); // Prague
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(55.7558, 37.6173), true), 0); // Moscow
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(40.7128, -74.0060), true), 0); // New York
			assertEquals(1f, router.defineSpeedPriority(createRoadAt(48.1486, 17.1077), true), 0); // Bratislava
			assertEquals(1f, router.defineSpeedPriority(createRoadAt(52.5200, 13.4050), true), 0); // Berlin
			assertEquals(1f, router.defineSpeedPriority(createRoadAt(0, -30), true), 0); // Ocean: no country

			regions.cacheAllCountries();
			router.clearCaches();
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(50.0875, 14.4213), true), 0);
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(55.7558, 37.6173), true), 0);
			assertEquals(1f, router.defineSpeedPriority(createRoadAt(48.1486, 17.1077), true), 0);
		} finally {
			regions.close();
		}
	}

	@Test(expected = UncheckedIOException.class)
	public void doesNotSilentlyIgnoreUnreadableCountryData() {
		OsmandRegions regions = new OsmandRegions(false) {
			@Override
			public List<BinaryMapDataObject> getRegionBoundaryObjects(Collection<String> regionIds) throws IOException {
				throw new IOException("Unreadable country boundaries");
			}
		};
		GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), regions,
				Collections.singleton("europe_austria"));
		router.defineSpeedPriority(createRoad(createRegion(), 50, true), true);
	}

	@Test(timeout = 30000)
	public void doesNotReadCountryFileForEveryTollRoad() throws Exception {
		AtomicInteger pointQueries = new AtomicInteger();
		AtomicInteger bulkReads = new AtomicInteger();
		OsmandRegions regions = new OsmandRegions(false) {
			@Override
			public List<BinaryMapDataObject> getRegionBoundaryObjects(Collection<String> regionIds) throws IOException {
				bulkReads.incrementAndGet();
				return super.getRegionBoundaryObjects(regionIds);
			}

			@Override
			public List<BinaryMapDataObject> query(int x31, int y31) throws IOException {
				pointQueries.incrementAndGet();
				return super.query(x31, y31);
			}
		};
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			long started = System.nanoTime();
			GeneralRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), regions,
					Collections.singleton("europe_czech-republic"));
			for (int i = 0; i < 100; i++) {
				assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(50.0875 + i * 0.00001, 14.4213), true), 0);
			}
			System.out.println("Country toll lookup: " + pointQueries.get() + " point queries, "
					+ (System.nanoTime() - started) / 1000000 + " ms for 100 distinct toll-road locations");
			assertEquals("Country lookup must not perform a file search for every new road", 0, pointQueries.get());
			assertEquals("Load the selected polygons once per calculation", 1, bulkReads.get());
			router = router.build(Collections.emptyMap());
			router.clearCaches();
			assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(50.0875, 14.4213), true), 0);
			assertEquals("Rebuilt profiles must share the boundary snapshot", 1, bulkReads.get());
			assertEquals(0, pointQueries.get());
		} finally {
			regions.close();
		}
	}

	@Test(timeout = 30000)
	public void boundarySnapshotDoesNotDependOnOpenSharedReader() throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		GeneralRouter router;
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), regions,
					Arrays.asList("europe_austria", "europe_gb", "europe_france"));
		} finally {
			regions.close();
		}
		assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(48.2082, 16.3738), true), 0); // Vienna
		assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(51.5074, -0.1278), true), 0); // London
		assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(54.5973, -5.9301), true), 0); // Belfast
		assertEquals(0.1f, router.defineSpeedPriority(createRoadAt(48.8566, 2.3522), true), 0); // Paris
		assertEquals(1f, router.defineSpeedPriority(createRoadAt(48.1486, 17.1077), true), 0); // Bratislava
	}

	@Test(timeout = 60000)
	public void realRouteCompletesWhenOneCountryIsSwitchedOff() throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			FixtureRoute normal = calculateFixtureRoute(regions, null, false);
			FixtureRoute global = calculateFixtureRoute(regions, null, true);
			FixtureRoute selected = calculateFixtureRoute(regions, Collections.singleton("europe_italy"), false);
			FixtureRoute selectedOther = calculateFixtureRoute(regions, Collections.singleton("europe_austria"), false);
			FixtureRoute bothSelected = calculateFixtureRoute(regions, Arrays.asList("europe_italy", "europe_austria"), false);
			assertEquals("A selected country must match global avoidance for a route within it", global.roads, selected.roads);
			assertEquals(global.routingTime, selected.routingTime, 0.01f);
			assertEquals("Switching off Italy while Austria stays selected must restore normal routing", normal.roads, selectedOther.roads);
			assertEquals(normal.routingTime, selectedOther.routingTime, 0.01f);
			assertEquals(global.roads, bothSelected.roads);
			assertEquals(global.routingTime, bothSelected.routingTime, 0.01f);
			// The same roads may be unavoidable in this small map, but their toll penalty must change.
			assertTrue("Fixture must exercise actual toll penalties", global.routingTime > normal.routingTime);
		} finally {
			regions.close();
		}
	}

	@Test(timeout = 30000)
	public void longRouteBasemapPhaseUsesSameCountryAwareRouter() throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			RoutingConfiguration config = new RoutingConfiguration();
			config.router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), regions,
					Collections.singleton("europe_austria"));
			AtomicInteger basePhases = new AtomicInteger();
			String stopAfterBasePhase = "Test stopped after checking the basemap phase";
			RoutePlannerFrontEnd frontend = new RoutePlannerFrontEnd() {
				@Override
				public RouteCalcResult searchRoute(RoutingContext context, LatLon start, LatLon end,
				                                  List<LatLon> intermediates, PrecalculatedRouteDirection direction)
						throws IOException, InterruptedException {
					if (context.calculationMode == RouteCalculationMode.BASE) {
						basePhases.incrementAndGet();
						assertSame("Both phases must retain the country-aware router", config.router, context.getRouter());
						assertEquals(0.1f, context.getRouter().defineSpeedPriority(createRoadAt(48.2082, 16.3738), true), 0);
						assertEquals(1f, context.getRouter().defineSpeedPriority(createRoadAt(48.1486, 17.1077), true), 0);
						// This tests phase dispatch, not a long-distance route on synthetic/missing map data.
						return new RouteCalcResult(stopAfterBasePhase);
					}
					return super.searchRoute(context, start, end, intermediates, direction);
				}
			};
			frontend.CALCULATE_MISSING_MAPS = false;
			RoutingContext context = frontend.buildRoutingContext(config, null, new BinaryMapIndexReader[0],
					RouteCalculationMode.COMPLEX);
			RouteCalcResult result = frontend.searchRoute(context, new LatLon(48.2082, 16.3738),
					new LatLon(48.1486, 17.1077), Collections.emptyList());
			assertEquals(stopAfterBasePhase, result.getError());
			assertEquals(1, basePhases.get());
			assertEquals(2, context.calculationProgress.totalIterations);
		} finally {
			regions.close();
		}
	}

	@Test(timeout = 30000)
	public void viennaBratislavaKeepsNativeFastRoutingWhenOnlySwitzerlandIsSelected() throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			CountryTollAvoidanceRouter countryRouter = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()),
					regions, Collections.singleton("europe_switzerland"));
			AtomicInteger nativeCalls = new AtomicInteger();
			RouteSegmentResult[] candidate = {
					new RouteSegmentResult(createRoadAt(48.2082, 16.3738), 0, 1),
					new RouteSegmentResult(createRoadAt(48.1486, 17.1077), 0, 1)
			};
			NativeLibrary nativeLibrary = new NativeLibrary() {
				@Override
				public boolean needRequestPrivateAccessRouting(RoutingContext context, int[] x31, int[] y31) {
					return false;
				}

				@Override
				public RouteSegmentResult[] runNativeRouting(RoutingContext context,
				                                            HHRouteDataStructure.HHRoutingConfig hhConfig,
				                                            RouteRegion[] mapRegions, boolean basemap) {
					assertFalse(context.getRouter() instanceof CountryTollAvoidanceRouter);
					assertTrue("Retain the configured fast-routing engine", hhConfig != null);
					assertFalse("No slow basemap calculation for an irrelevant country", basemap);
					nativeCalls.incrementAndGet();
					return candidate;
				}
			};
			RoutingConfiguration config = new RoutingConfiguration();
			config.router = countryRouter;
			RoutePlannerFrontEnd frontend = new RoutePlannerFrontEnd();
			frontend.CALCULATE_MISSING_MAPS = false;
			frontend.setDefaultHHRoutingConfig();
			frontend.setUseOnlyHHRouting(true);
			frontend.setHHRouteCpp(true);
			RoutingContext context = frontend.buildRoutingContext(config, nativeLibrary, new BinaryMapIndexReader[0],
					RouteCalculationMode.COMPLEX);
			// The native engine is stubbed; this is an engine-dispatch regression, not a full map calculation.
			context.requestNativePrepareResult = true;
			RouteCalcResult result = frontend.searchRoute(context, new LatLon(48.2082, 16.3738),
					new LatLon(48.1486, 17.1077), Collections.emptyList());
			assertTrue(result.isCorrect());
			assertEquals(1, nativeCalls.get());
			assertEquals(2, result.getList().size());
			assertSame(countryRouter, context.getRouter());
			assertSame(nativeLibrary, context.nativeLib);
			assertTrue(frontend.isHHRoutingConfigured());
			assertFalse(countryRouter.affectsRoute(result.getList()));
		} finally {
			regions.close();
		}
	}

	@Test(timeout = 60000)
	public void affectedNativeCandidateFallsBackToRealCountryAwareRouting() throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			RoutingConfiguration config = RoutingConfiguration.getDefault().build("car",
					new RoutingMemoryLimits(128, 256), Collections.emptyMap());
			CountryTollAvoidanceRouter countryRouter = new CountryTollAvoidanceRouter(config.router, regions,
					Collections.singleton("europe_italy"));
			config.router = countryRouter;
			AtomicInteger nativeCalls = new AtomicInteger();
			NativeLibrary nativeLibrary = new NativeLibrary() {
				@Override
				public boolean needRequestPrivateAccessRouting(RoutingContext context, int[] x31, int[] y31) {
					return false;
				}

				@Override
				public RouteSegmentResult[] runNativeRouting(RoutingContext context,
				                                            HHRouteDataStructure.HHRoutingConfig hhConfig,
				                                            RouteRegion[] mapRegions, boolean basemap) {
					assertFalse(context.getRouter() instanceof CountryTollAvoidanceRouter);
					nativeCalls.incrementAndGet();
					context.requestNativePrepareResult = true;
					return new RouteSegmentResult[] {new RouteSegmentResult(createRoadAt(45.546784, 9.060545), 0, 1)};
				}
			};
			File map = new File("src/test/resources/routing/Routing_test_84.obf");
			try (RandomAccessFile file = new RandomAccessFile(map, "r")) {
				BinaryMapIndexReader reader = new BinaryMapIndexReader(file, map);
				RoutePlannerFrontEnd frontend = new RoutePlannerFrontEnd();
				frontend.CALCULATE_MISSING_MAPS = false;
				RoutingContext context = frontend.buildRoutingContext(config, nativeLibrary,
						new BinaryMapIndexReader[] {reader}, RouteCalculationMode.NORMAL);
				RouteCalcResult result = frontend.searchRoute(context, new LatLon(45.546784, 9.060545),
						new LatLon(45.524871, 9.072073), Collections.emptyList());
				assertTrue(result.isCorrect());
				assertEquals("Only the first search can use native routing", 1, nativeCalls.get());
				assertTrue("Java fallback must search the real map, not return the single-road stub", result.getList().size() > 1);
				assertSame(countryRouter, context.getRouter());
				assertSame(nativeLibrary, context.nativeLib);
				context.unloadAllData();
			}
		} finally {
			regions.close();
		}
	}

	@Test(timeout = 30000)
	public void affectedCandidateUsesNativeCountryApiWhenSupported() throws Exception {
		checkAffectedNativeCandidate(false);
	}

	@Test(timeout = 30000)
	public void affectedCandidateRetainsNativeHHOnlyWithDetailedCountryCostValidation() throws Exception {
		checkAffectedNativeCandidate(true);
	}

	private void checkAffectedNativeCandidate(boolean supportsHH) throws Exception {
		OsmandRegions regions = new OsmandRegions(false);
		try {
			regions.prepareFile(new File(OsmandRegions.class.getResource("regions.ocbf").toURI()).getAbsolutePath());
			CountryTollAvoidanceRouter countryRouter = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()),
					regions, Collections.singleton("europe_switzerland"));
			AtomicInteger plainSearches = new AtomicInteger();
			AtomicInteger countrySearches = new AtomicInteger();
			NativeLibrary nativeLibrary = new NativeLibrary() {
				@Override
				public boolean supportsCountryTollAvoidance() {
					return true;
				}

				@Override
				public boolean supportsCountryTollHHRouting() {
					return supportsHH;
				}

				@Override
				public boolean needRequestPrivateAccessRouting(RoutingContext context, int[] x31, int[] y31) {
					return false;
				}

				@Override
				public RouteSegmentResult[] runNativeRouting(RoutingContext context,
				                                            HHRouteDataStructure.HHRoutingConfig hhConfig,
				                                            RouteRegion[] mapRegions, boolean basemap) {
					context.requestNativePrepareResult = true;
					if (context.getRouter() instanceof CountryTollAvoidanceRouter) {
						assertTrue("Only capable native libraries may receive country penalties", supportsCountryTollAvoidance());
						assertEquals("Retain HH only when the native library validates country costs", supportsHH, hhConfig != null);
						assertEquals("Native routing must retain the configured Java memory limit", 128L << 20,
								context.config.memoryLimitation);
						countrySearches.incrementAndGet();
						return new RouteSegmentResult[] {new RouteSegmentResult(createRoadAt(48.1286, 17.0964), 0, 1)};
					}
					plainSearches.incrementAndGet();
					return new RouteSegmentResult[] {new RouteSegmentResult(createRoadAt(47.3769, 8.5417), 0, 1)};
				}
			};
			RoutingConfiguration config = new RoutingConfiguration();
			config.router = countryRouter;
			config.memoryLimitation = 128L << 20;
			RoutePlannerFrontEnd frontend = new RoutePlannerFrontEnd();
			frontend.CALCULATE_MISSING_MAPS = false;
			frontend.setDefaultHHRoutingConfig();
			frontend.setHHRouteCpp(true);
			RoutingContext context = frontend.buildRoutingContext(config, nativeLibrary, new BinaryMapIndexReader[0],
					RouteCalculationMode.NORMAL);
			context.requestNativePrepareResult = true;
			RouteCalcResult result = frontend.searchRoute(context, new LatLon(48.1286, 17.0964),
					new LatLon(47.3769, 8.5417), Collections.emptyList());
			assertTrue(result.isCorrect());
			assertEquals(1, plainSearches.get());
			assertEquals(1, countrySearches.get());
			assertSame(countryRouter, context.getRouter());
			assertSame(nativeLibrary, context.nativeLib);
			assertTrue(frontend.isHHRoutingConfigured());
			assertEquals(128L << 20, config.memoryLimitation);
			assertTrue("The selected native path must not expand a Java graph", context.finalRouteSegment == null);
		} finally {
			regions.close();
		}
	}

	@Test
	public void javaFallbackReservesHeapAndHonorsLowerConfiguredLimits() {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> true);
		long limit = router.getJavaFallbackMemoryLimit(512L << 20);
		assertTrue(limit >= (8L << 20));
		assertTrue(limit <= (64L << 20));
		assertEquals(4L << 20, router.getJavaFallbackMemoryLimit(4L << 20));
		assertEquals((512L << 20) / 5, CountryTollAvoidanceRouter.javaHeapReserve(512L << 20));
		assertTrue(CountryTollAvoidanceRouter.hasJavaHeapHeadroom(512L << 20, 300L << 20));
		assertFalse(CountryTollAvoidanceRouter.hasJavaHeapHeadroom(512L << 20, 450L << 20));
	}

	@Test
	public void customSelectorCannotSilentlyUseNativeCountryGeometry() {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()), (x, y) -> true);
		assertTrue(router.getNativeCountryPolygons() == null);
		assertFalse("Older unloaded native libraries must fail capability detection safely",
				new NativeLibrary().supportsCountryTollAvoidance());
		assertFalse("Older native libraries must not claim detailed HH cost validation",
				new NativeLibrary().supportsCountryTollHHRouting());
	}

	@Test(timeout = 30000)
	public void restoresRouterAndEngineSettingsWhenNativeCandidateFails() throws Exception {
		CountryTollAvoidanceRouter countryRouter = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()),
				(x, y) -> true);
		NativeLibrary nativeLibrary = new NativeLibrary() {
			@Override
			public boolean needRequestPrivateAccessRouting(RoutingContext context, int[] x31, int[] y31) {
				return false;
			}

			@Override
			public RouteSegmentResult[] runNativeRouting(RoutingContext context,
			                                            HHRouteDataStructure.HHRoutingConfig hhConfig,
			                                            RouteRegion[] mapRegions, boolean basemap) throws IllegalStateException {
				throw new IllegalStateException("Native candidate failed");
			}
		};
		RoutingConfiguration config = new RoutingConfiguration();
		config.router = countryRouter;
		RoutePlannerFrontEnd frontend = new RoutePlannerFrontEnd();
		frontend.CALCULATE_MISSING_MAPS = false;
		frontend.setDefaultHHRoutingConfig();
		frontend.setHHRouteCpp(true);
		RoutingContext context = frontend.buildRoutingContext(config, nativeLibrary, new BinaryMapIndexReader[0],
				RouteCalculationMode.NORMAL);
		try {
			frontend.searchRoute(context, new LatLon(48.2082, 16.3738), new LatLon(48.1486, 17.1077),
					Collections.emptyList());
			org.junit.Assert.fail("Expected native candidate failure");
		} catch (IllegalStateException expected) {
			assertEquals("Native candidate failed", expected.getMessage());
		}
		assertSame(countryRouter, context.getRouter());
		assertSame(nativeLibrary, context.nativeLib);
		assertTrue(frontend.isHHRoutingConfigured());
	}

	@Test
	public void detectsSelectedTransitCountryAndTraversedBoothsNotJustEndpoints() {
		CountryTollAvoidanceRouter router = new CountryTollAvoidanceRouter(createRouter(Collections.emptyMap()),
				(x, y) -> x < 100);
		RouteDataObject outside = createRoad(createRegion(), 150, true);
		RouteDataObject inside = createRoad(createRegion(), 50, true);
		assertFalse(router.affectsRoute(Collections.singletonList(new RouteSegmentResult(outside, 0, 1))));
		assertTrue(router.affectsRoute(Arrays.asList(new RouteSegmentResult(outside, 0, 1),
				new RouteSegmentResult(inside, 0, 1), new RouteSegmentResult(outside, 0, 1))));

		RouteDataObject booths = createRoad(createRegion(), 150, false);
		booths.pointsX = new int[] {50, 150, 150};
		booths.pointsY = new int[] {50, 50, 50};
		booths.pointTypes = new int[][] {new int[] {3}, null, null};
		assertFalse("A booth outside the traversed part must not force a recalculation",
				router.affectsRoute(Collections.singletonList(new RouteSegmentResult(booths, 1, 2))));
		assertTrue(router.affectsRoute(Collections.singletonList(new RouteSegmentResult(booths, 0, 2))));
		assertTrue(router.affectsRoute(Collections.singletonList(new RouteSegmentResult(booths, 2, 0))));
	}

	@Test
	public void plainCandidateRouterPreservesOtherPreferencesAndAvoidedRoads() {
		Map<String, String> params = Collections.singletonMap(GeneralRouter.DEFAULT_SPEED, "17");
		CountryTollAvoidanceRouter countryRouter = new CountryTollAvoidanceRouter(createRouter(params), (x, y) -> true);
		RouteDataObject avoided = createRoad(createRegion(), 50, true);
		countryRouter.setImpassableRoads(Collections.singleton(avoided.id >> GeneralRouter.IMPASSABLE_ROAD_SHIFT));
		GeneralRouter plain = countryRouter.withoutCountryAvoidance();
		assertFalse(plain instanceof CountryTollAvoidanceRouter);
		assertEquals(17f, plain.getDefaultSpeed(), 0);
		assertEquals(1f, plain.defineSpeedPriority(avoided, true), 0);
		assertFalse(plain.acceptLine(avoided));
		assertTrue(plain.acceptLine(createRoad(createRegion(), 150, true)));
	}

	private static FixtureRoute calculateFixtureRoute(OsmandRegions regions, Collection<String> countryIds,
	                                              boolean globalAvoidance) throws Exception {
		Map<String, String> params = new LinkedHashMap<>();
		if (globalAvoidance) {
			params.put(GeneralRouter.AVOID_TOLL, "true");
		}
		RoutingConfiguration config = RoutingConfiguration.getDefault().build("car",
				new RoutingMemoryLimits(RoutingConfiguration.DEFAULT_MEMORY_LIMIT * 3,
						RoutingConfiguration.DEFAULT_NATIVE_MEMORY_LIMIT), params);
		if (countryIds != null) {
			config.router = new CountryTollAvoidanceRouter(config.router, regions, countryIds);
		}
		File map = new File("src/test/resources/routing/Routing_test_84.obf");
		try (RandomAccessFile file = new RandomAccessFile(map, "r")) {
			BinaryMapIndexReader reader = new BinaryMapIndexReader(file, map);
			RoutePlannerFrontEnd frontend = new RoutePlannerFrontEnd();
			frontend.CALCULATE_MISSING_MAPS = false;
			RoutingContext context = frontend.buildRoutingContext(config, null,
					new BinaryMapIndexReader[] {reader}, RouteCalculationMode.COMPLEX);
			long started = System.nanoTime();
			RouteCalcResult result = frontend.searchRoute(context, new LatLon(45.546784, 9.060545),
					new LatLon(45.524871, 9.072073), Collections.emptyList());
			assertTrue("Route must finish successfully: " + result.getError(), result.isCorrect());
			assertFalse(result.detailed.isEmpty());
			FixtureRoute route = new FixtureRoute();
			route.routingTime = context.routingTime;
			for (RouteSegmentResult segment : result.detailed) {
				route.roads.add(segment.getObject().getId());
			}
			System.out.println("Country toll real route: countries=" + countryIds + ", global=" + globalAvoidance
					+ ", elapsed=" + (System.nanoTime() - started) / 1000000 + " ms, segments=" + route.roads.size());
			context.unloadAllData();
			return route;
		}
	}

	private static class FixtureRoute {
		private final List<Long> roads = new ArrayList<>();
		private float routingTime;
	}

	private static RouteDataObject createRoadAt(double latitude, double longitude) {
		RouteDataObject road = createRoad(createRegion(), MapUtils.get31TileNumberX(longitude), true);
		int y = MapUtils.get31TileNumberY(latitude);
		road.pointsY = new int[] {y, y};
		return road;
	}

	private static GeneralRouter createRouter(Map<String, String> params) {
		GeneralRouter router = new GeneralRouter(GeneralRouterProfile.CAR, Collections.emptyMap());
		RouteAttributeEvalRule access = router.getObjContext(RouteDataObjectAttribute.ACCESS).registerNewRule("-1", null);
		access.registerAndTagValueCondition("access", "no", false);
		router.getObjContext(RouteDataObjectAttribute.ACCESS).registerNewRule("1", null);

		RouteAttributeEvalRule toll = router.getObjContext(RouteDataObjectAttribute.ROAD_PRIORITIES).registerNewRule("0.1", null);
		toll.registerAndParamCondition(GeneralRouter.AVOID_TOLL, false);
		toll.registerAndTagValueCondition("toll", "yes", false);
		router.getObjContext(RouteDataObjectAttribute.ROAD_PRIORITIES).registerNewRule("1", null);

		RouteAttributeEvalRule booth = router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES).registerNewRule("900", null);
		booth.registerAndParamCondition(GeneralRouter.AVOID_TOLL, false);
		booth.registerAndTagValueCondition("barrier", "toll_booth", false);
		router.getObjContext(RouteDataObjectAttribute.ROUTING_OBSTACLES).registerNewRule("0", null);
		RouteAttributeEvalRule time = router.getObjContext(RouteDataObjectAttribute.OBSTACLES).registerNewRule("180", null);
		time.registerAndTagValueCondition("barrier", "toll_booth", false);
		return router.build(params);
	}

	private static RouteRegion createRegion() {
		RouteRegion region = new RouteRegion();
		region.initRouteEncodingRule(1, "highway", "primary");
		region.initRouteEncodingRule(2, "toll", "yes");
		region.initRouteEncodingRule(3, "barrier", "toll_booth");
		region.initRouteEncodingRule(4, "access", "no");
		return region;
	}

	private static RouteDataObject createRoad(RouteRegion region, int x, boolean toll) {
		RouteDataObject road = new RouteDataObject(region);
		road.id = x;
		road.pointsX = new int[] {x, x};
		road.pointsY = new int[] {50, 50};
		road.types = toll ? new int[] {1, 2} : new int[] {1};
		return road;
	}
}