package net.osmand.plus.routing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.Location
import net.osmand.NativeLibrary
import net.osmand.PlatformUtil
import net.osmand.binary.BinaryMapIndexReader
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.render.NativeOsmandLibrary
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.router.CountryTollAvoidanceRouter
import net.osmand.router.GeneralRouter
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig
import net.osmand.router.RouteCalculationProgress
import net.osmand.router.RoutePlannerFrontEnd
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode
import net.osmand.router.RouteSegmentResult
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits
import net.osmand.router.RoutingContext
import net.osmand.util.MapUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Uses already-installed maps and the real native library without editing saved settings. */
@LargeTest
@RunWith(AndroidJUnit4::class)
class CountryTollFastRoutingTest {

	private lateinit var app: OsmandApplication
	private lateinit var nativeLibrary: NativeOsmandLibrary
	private lateinit var maps: Array<BinaryMapIndexReader>
	private val log = PlatformUtil.getLog(CountryTollFastRoutingTest::class.java)

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		val deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2)
		while (app.isApplicationInitializing && System.currentTimeMillis() < deadline) {
			Thread.sleep(200)
		}
		assertFalse("Application initialization timed out", app.isApplicationInitializing)
		maps = app.resourceManager.routingMapFiles
		val countries = app.regions.getCountriesForMapFiles(maps.map { it.file.name })
		assumeTrue("Install Austrian and Slovak routing maps before running this integration test",
			countries.any { it.regionId == "europe_austria" } && countries.any { it.regionId == "europe_slovakia" })
		val loadedLibrary = NativeOsmandLibrary.getLoadedLibrary()
		assumeTrue("A working native routing library is required", loadedLibrary != null)
		nativeLibrary = loadedLibrary!!
	}

	@Test
	fun switzerlandSelectionKeepsNativeRoutingBetweenViennaAndBratislava() {
		compareNormalAndSwitzerlandRouting(LatLon(48.208355, 16.3725), LatLon(48.128616, 17.096422))
	}

	@Test
	fun switzerlandSelectionKeepsNativeRoutingBetweenBratislavaAndVienna() {
		compareNormalAndSwitzerlandRouting(LatLon(48.128616, 17.096422), LatLon(48.208355, 16.3725))
	}

	@Test
	fun switzerlandSelectionKeepsNativeRoutingBetweenViennaAndPrague() {
		val countries = app.regions.getCountriesForMapFiles(maps.map { it.file.name })
		assumeTrue("Install Czech routing maps for the longer-route regression",
			countries.any { it.regionId == "europe_czech-republic" })
		compareNormalAndSwitzerlandRouting(LatLon(48.208355, 16.3725), LatLon(50.087465, 14.421257))
	}

	@Test
	fun savedSwitzerlandSelectionKeepsNativeRoutingThroughRouteProvider() {
		calculateSavedProfileRoute(LatLon(48.208355, 16.3725), LatLon(48.128616, 17.096422), 60, false,
			listOf("europe_switzerland"))
	}

	@Test
	fun savedSwitzerlandSelectionUsesNativePenaltiesFromBratislavaToZurich() {
		val countries = app.regions.getCountriesForMapFiles(maps.map { it.file.name })
		assumeTrue("Install Swiss maps for the affected long-route regression",
			countries.any { it.regionId == "europe_switzerland" })
		assertTrue("Install the newly built native library for this regression", nativeLibrary.supportsCountryTollAvoidance())
		calculateSavedProfileRoute(LatLon(48.128616, 17.096422), LatLon(47.3769, 8.5417), 240, true,
			listOf("europe_switzerland"))
	}

	@Test
	fun savedCountrySelectionUsesNativePenaltiesFromBratislavaToZurich() {
		assertTrue("Install the newly built native library for this regression", nativeLibrary.supportsCountryTollAvoidance())
		val selectedCountries = app.settings.AVOID_TOLL_ROADS_COUNTRIES.getStringsListForProfile(ApplicationMode.CAR)
		assumeTrue("Enable at least one country in the saved car profile for this integration check",
			!selectedCountries.isNullOrEmpty())
		calculateSavedProfileRoute(LatLon(48.128616, 17.096422), LatLon(47.3769, 8.5417), 240, true,
			selectedCountries)
	}

	@Test
	fun multipleCountrySelectionsApplyFromBratislavaToPrague() {
		requireCzechMaps()
		calculateSavedProfileRoute(LatLon(48.128616, 17.096422), LatLon(50.087465, 14.421257), 240, true,
			listOf("europe_slovakia", "europe_czech-republic"))
	}

	@Test
	fun selectedTransitCountryAppliesWithBothEndpointsOutsideIt() {
		requireCzechMaps()
		// The direct Bratislava–Prague route can stay outside Austria. An explicit Vienna
		// waypoint makes Austria a transit country rather than assuming a particular itinerary.
		calculateSavedProfileRoute(LatLon(48.128616, 17.096422), LatLon(50.087465, 14.421257), 240, true,
			listOf("europe_austria"), listOf(LatLon(48.208355, 16.3725)))
	}

	@Test
	fun globalTollAvoidanceDoesNotAddCountryRecalculation() {
		requireCzechMaps()
		val start = LatLon(48.128616, 17.096422)
		val end = LatLon(50.087465, 14.421257)
		val parameters = mapOf(GeneralRouter.AVOID_TOLL to "true")
		val normal = calculateRoute(start, end, parameters = parameters)
		val selected = calculateRoute(start, end, listOf("europe_slovakia", "europe_czech-republic"), parameters)
		assertEquals("Global toll avoidance must take precedence", normal.roads, selected.roads)
		assertEquals("Global avoidance must not add a second country search", normal.nativeCalls, selected.nativeCalls)
	}

	@Test
	fun cancellingAffectedNativeSearchAllowsANewCalculation() {
		assertTrue(nativeLibrary.supportsCountryTollHHRouting())
		val start = LatLon(48.128616, 17.096422)
		val end = LatLon(47.3769, 8.5417)
		val config = app.getRoutingConfigForMode(ApplicationMode.CAR).build("car", RoutingMemoryLimits(256, 256), emptyMap())
		val router = CountryTollAvoidanceRouter(config.router, app.regions, listOf("europe_slovakia"))
		config.router = router
		val progress = RouteCalculationProgress()
		val cancellation = Executors.newSingleThreadScheduledExecutor()
		var countryStarted = 0L
		val countedLibrary = countingNativeLibrary({}, {
			countryStarted = System.nanoTime()
			cancellation.schedule({ progress.isCancelled = true }, 500, TimeUnit.MILLISECONDS)
		})
		val frontend = RoutePlannerFrontEnd().apply {
			setDefaultHHRoutingConfig()
			setHHRouteCpp(true)
		}
		val context = frontend.buildRoutingContext(config, countedLibrary, maps, RouteCalculationMode.COMPLEX)
		context.calculationProgress = progress
		val deadline = cancellation.schedule({ progress.isCancelled = true }, 60, TimeUnit.SECONDS)
		try {
			initializeNativeMaps(start, end)
			// Cancel after entering the affected native phase, not while loading the app/maps.
			try {
				frontend.searchRoute(context, start, end, emptyList())
			} catch (cancelled: InterruptedException) {
				assertTrue(progress.isCancelled)
			}
			assertTrue("The test must reach the affected-country native phase", countryStarted > 0)
			assertTrue("Native search must observe cancellation", progress.isCancelled)
			val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - countryStarted)
			assertTrue("Native cancellation must return within 10 seconds", elapsed < 10000)
			assertTrue("Cancellation must restore the configured engine", frontend.isHHRoutingConfigured)
			assertTrue(context.nativeLib === countedLibrary)
			assertTrue(context.router === router)
			log.info("Country-native cancellation returned in $elapsed ms")
		} finally {
			deadline.cancel(false)
			cancellation.shutdownNow()
			context.unloadAllData()
		}
		// A cancelled search must not poison the next calculation's native context or progress.
		calculateRoute(start, LatLon(48.208355, 16.3725), listOf("europe_switzerland"))
	}

	private fun requireCzechMaps() {
		assumeTrue("Install Czech maps for this regression", app.regions.getCountriesForMapFiles(maps.map { it.file.name })
			.any { it.regionId == "europe_czech-republic" })
	}

	private fun calculateSavedProfileRoute(start: LatLon, end: LatLon, timeoutSeconds: Long,
		requireCountryNativeSearch: Boolean, testCountries: List<String>? = null, intermediates: List<LatLon> = emptyList()) {
		val mode = ApplicationMode.CAR
		assumeTrue("Global toll avoidance and safe mode must be off for the country-routing regression",
			!app.settings.getCustomRoutingBooleanProperty("avoid_toll", false).getModeValue(mode)
				&& !app.settings.SAFE_MODE.get())
		var environment: RoutingEnvironment? = null
		var nativeCalls = 0
		var nativeCountryCalls = 0
		var nativeCountryHHCalls = 0
		var countryRouter: CountryTollAvoidanceRouter? = null
		var candidateExposure: TollExposure? = null
		val countedLibrary = countingNativeLibrary({ nativeCalls++ }, { nativeCountryCalls++ }, { nativeCountryHHCalls++ }) {
			countrySearch, route ->
			if (!countrySearch && requireCountryNativeSearch) {
				candidateExposure = measureTollExposure(countryRouter!!, route.toList())
			}
		}
		val provider = object : RouteProvider() {
			override fun calculateRoutingEnvironment(params: RouteCalculationParams, calcGPXRoute: Boolean,
				skipComplex: Boolean): RoutingEnvironment? {
				val result = super.calculateRoutingEnvironment(params, calcGPXRoute, skipComplex)
				if (result != null) {
					if (testCountries != null) {
						// Override only this calculation's router, never the user's saved preference.
						val original = result.ctx.router as GeneralRouter
						val base = if (original is CountryTollAvoidanceRouter) original.withoutCountryAvoidance() else original
						result.ctx.setRouter(CountryTollAvoidanceRouter(base, app.regions, testCountries))
					}
					assertTrue("The saved selection must reach the actual application routing path",
						result.ctx.router is CountryTollAvoidanceRouter)
					countryRouter = result.ctx.router as CountryTollAvoidanceRouter
					assertTrue("Country selection must not disable the configured native engine", result.ctx.nativeLib != null)
					result.ctx.nativeLib = countedLibrary
					result.complexCtx?.nativeLib = countedLibrary
				}
				environment = result
				return result
			}
		}
		val params = RouteCalculationParams().apply {
			ctx = app
			this.mode = mode
			this.start = Location("country-toll-test", start.latitude, start.longitude)
			this.end = end
			this.intermediates = intermediates
			initialCalculation = true
			fast = app.settings.FAST_ROUTE_MODE.getModeValue(mode)
			leftSide = app.settings.DRIVING_REGION.get().leftHandDriving
			calculationProgress = RouteCalculationProgress()
		}
		val cancellation = Executors.newSingleThreadScheduledExecutor()
		val peakHeap = AtomicLong()
		val heapSampling = cancellation.scheduleAtFixedRate({
			val runtime = Runtime.getRuntime()
			peakHeap.set(maxOf(peakHeap.get(), runtime.totalMemory() - runtime.freeMemory()))
		}, 0, 100, TimeUnit.MILLISECONDS)
		val deadline = cancellation.schedule({ params.calculationProgress.isCancelled = true }, timeoutSeconds, TimeUnit.SECONDS)
		val started = System.nanoTime()
		try {
			val result = provider.calculateRouteImpl(params)
			assertFalse("Saved-profile route exceeded $timeoutSeconds seconds", params.calculationProgress.isCancelled)
			assertTrue("RouteProvider failed: ${result.errorMessage}", result.isCalculated)
			assertFalse("Prepared navigation directions must contain map locations", result.immutableAllLocations.isEmpty())
			assertTrue("RouteProvider must use the real native routing engine", nativeCalls > 0)
			val context = environment!!.complexCtx ?: environment!!.ctx
			assertTrue("The application route must not fall back to a Java graph search", context.finalRouteSegment == null)
			if (requireCountryNativeSearch) {
				assertTrue("Affected route must use the new native country penalty API", nativeCountryCalls > 0)
				assertTrue("Affected long route must retain native HH routing", nativeCountryHHCalls > 0)
				assertTrue("Calculation must leave at least 64 MiB of Java heap headroom",
					peakHeap.get() < Runtime.getRuntime().maxMemory() - (64L shl 20))
				val before = candidateExposure!!
				val after = measureTollExposure(countryRouter!!, result.originalRoute!!)
				assertTrue("The normal candidate must contain affected tolls", before.penaltySeconds > 0)
				assertTrue("Country-aware native routing must reduce selected-country toll penalties: $before -> $after",
					after.penaltySeconds < before.penaltySeconds)
				log.info("Native selected-country toll exposure: countries=$testCountries, before=$before, after=$after")
			} else {
				assertFalse((context.router as CountryTollAvoidanceRouter).affectsRoute(result.originalRoute!!))
			}
			log.info("Saved-profile country-toll integration: $start -> $end, " +
				"elapsed=${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)} ms, native calls=$nativeCalls, " +
				"country native calls=$nativeCountryCalls, country HH calls=$nativeCountryHHCalls, peak Java heap=${peakHeap.get() shr 20} MiB, " +
				"distance=${result.wholeDistance} m, locations=${result.immutableAllLocations.size}, countries=$testCountries, via=$intermediates")
		} finally {
			heapSampling.cancel(false)
			deadline.cancel(false)
			cancellation.shutdownNow()
			environment?.complexCtx?.unloadAllData()
			environment?.ctx?.unloadAllData()
		}
	}

	/** Measures only the additional road/booth costs used by this profile in the selected countries. */
	private fun measureTollExposure(router: CountryTollAvoidanceRouter, route: List<RouteSegmentResult>): TollExposure {
		val base = router.withoutCountryAvoidance()
		var selectedMeters = 0.0
		var otherMeters = 0.0
		var penaltySeconds = 0.0
		var booths = 0
		for (segment in route) {
			val road = segment.`object`
			val forward = segment.isForwardDirection
			val first = minOf(segment.startPointIndex, segment.endPointIndex)
			val last = maxOf(segment.startPointIndex, segment.endPointIndex)
			if (road.getValue("toll") == "yes") {
				var distance = 0.0
				for (point in first until last) {
					distance += MapUtils.getDistance(
						MapUtils.get31LatitudeY(road.getPoint31YTile(point)), MapUtils.get31LongitudeX(road.getPoint31XTile(point)),
						MapUtils.get31LatitudeY(road.getPoint31YTile(point + 1)), MapUtils.get31LongitudeX(road.getPoint31XTile(point + 1)))
				}
				val priority = router.defineSpeedPriority(road, forward).toDouble()
				val basePriority = base.defineSpeedPriority(road, forward).toDouble()
				if (priority < basePriority && priority > 0) {
					selectedMeters += distance
					penaltySeconds += distance / base.defineRoutingSpeed(road, forward) * (1 / priority - 1 / basePriority)
				} else {
					otherMeters += distance
				}
			}
			for (point in first..last) {
				val extra = router.defineRoutingObstacle(road, point, !forward) - base.defineRoutingObstacle(road, point, !forward)
				if (extra > 0) {
					booths++
					penaltySeconds += extra
				}
			}
		}
		return TollExposure(selectedMeters, otherMeters, booths, penaltySeconds)
	}

	private fun compareNormalAndSwitzerlandRouting(start: LatLon, end: LatLon) {
		val normal = calculateRoute(start, end)
		val selected = calculateRoute(start, end, listOf("europe_switzerland"))
		assertEquals("An irrelevant country's selection must not change the route", normal.roads, selected.roads)
		assertEquals("No additional native search or Java fallback is expected", normal.nativeCalls, selected.nativeCalls)
		assertTrue("Country selection must not turn a fast route into a multi-minute search",
			selected.elapsedMs < normal.elapsedMs * 3 + 5000)
		log.info("Native country-toll integration: $start -> $end, normal=${normal.elapsedMs} ms, " +
			"Switzerland=${selected.elapsedMs} ms, native calls=${selected.nativeCalls}, roads=${selected.roads.size}")
	}

	private fun calculateRoute(start: LatLon, end: LatLon, countries: List<String> = emptyList(),
		parameters: Map<String, String> = emptyMap()): MeasuredRoute {
		val config = app.getRoutingConfigForMode(ApplicationMode.CAR).build("car",
			RoutingMemoryLimits(256, 256), parameters)
		val initialized = System.nanoTime()
		val countryRouter = if (countries.isNotEmpty()) {
			CountryTollAvoidanceRouter(config.router, app.regions, countries)
		} else {
			null
		}
		if (countryRouter != null) config.router = countryRouter
		var nativeCalls = 0
		val countedLibrary = countingNativeLibrary({ nativeCalls++ })
		val frontend = RoutePlannerFrontEnd()
		frontend.setDefaultHHRoutingConfig()
		frontend.setHHRouteCpp(true)
		val context = frontend.buildRoutingContext(config, countedLibrary, maps, RouteCalculationMode.COMPLEX)
		context.calculationProgress = RouteCalculationProgress()
		val cancellation = Executors.newSingleThreadScheduledExecutor()
		val deadline = cancellation.schedule({ context.calculationProgress.isCancelled = true }, 60, TimeUnit.SECONDS)
		try {
			initializeNativeMaps(start, end)
			val result = frontend.searchRoute(context, start, end, emptyList())
			assertFalse("Route calculation exceeded 60 seconds", context.calculationProgress.isCancelled)
			assertTrue("Route calculation failed: ${result.error}", result.isCorrect)
			assertFalse("Route must contain actual map roads", result.list.isEmpty())
			assertTrue("The real native routing engine must be used", nativeCalls > 0)
			assertTrue("Native path must not be replaced by a Java graph search", context.finalRouteSegment == null)
			if (countryRouter != null) assertFalse(countryRouter.affectsRoute(result.list))
			val roads = result.list.map { "${it.`object`.id}:${it.startPointIndex}:${it.endPointIndex}" }
			return MeasuredRoute(roads, nativeCalls, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - initialized))
		} finally {
			deadline.cancel(false)
			cancellation.shutdownNow()
			context.unloadAllData()
		}
	}

	private fun initializeNativeMaps(start: LatLon, end: LatLon) {
		val left = minOf(MapUtils.get31TileNumberX(start.longitude), MapUtils.get31TileNumberX(end.longitude))
		val right = maxOf(MapUtils.get31TileNumberX(start.longitude), MapUtils.get31TileNumberX(end.longitude))
		val top = minOf(MapUtils.get31TileNumberY(start.latitude), MapUtils.get31TileNumberY(end.latitude))
		val bottom = maxOf(MapUtils.get31TileNumberY(start.latitude), MapUtils.get31TileNumberY(end.latitude))
		app.resourceManager.renderer.checkInitialized(15, nativeLibrary, left, right, bottom, top)
	}

	private fun countingNativeLibrary(onSearch: () -> Unit, onCountrySearch: () -> Unit = {},
		onCountryHHSearch: () -> Unit = {}, onResult: (Boolean, Array<RouteSegmentResult>) -> Unit = { _, _ -> }): NativeLibrary {
		return object : NativeLibrary() {
			override fun supportsCountryTollAvoidance(): Boolean = nativeLibrary.supportsCountryTollAvoidance()
			override fun supportsCountryTollHHRouting(): Boolean = nativeLibrary.supportsCountryTollHHRouting()

			override fun needRequestPrivateAccessRouting(context: RoutingContext, x31: IntArray, y31: IntArray): Boolean {
				return nativeLibrary.needRequestPrivateAccessRouting(context, x31, y31)
			}

			override fun runNativeRouting(context: RoutingContext, hhConfig: HHRoutingConfig?,
				regions: Array<RouteRegion>, basemap: Boolean): Array<RouteSegmentResult> {
				if (context.router is CountryTollAvoidanceRouter) {
					assertTrue("Country penalties require a capable native library", supportsCountryTollAvoidance())
					if (hhConfig != null) {
						assertTrue("HH must validate country-aware detailed costs", supportsCountryTollHHRouting())
						onCountryHHSearch()
					}
					onCountrySearch()
				}
				onSearch()
				val result = nativeLibrary.runNativeRouting(context, hhConfig, regions, basemap)
				// This observer runs before RouteResultPreparation, which normally initializes
				// the Java decoding rules for roads returned by native routing.
				for (segment in result) {
					val region = segment.`object`.region
					context.reverseMap[region]?.initRouteRegion(region)
				}
				onResult(context.router is CountryTollAvoidanceRouter, result)
				return result
			}
		}
	}

	private data class MeasuredRoute(val roads: List<String>, val nativeCalls: Int, val elapsedMs: Long)
	private data class TollExposure(val selectedMeters: Double, val otherMeters: Double, val booths: Int, val penaltySeconds: Double)
}