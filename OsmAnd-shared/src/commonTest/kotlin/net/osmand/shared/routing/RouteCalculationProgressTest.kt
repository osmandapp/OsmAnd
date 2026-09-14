package net.osmand.shared.routing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteCalculationProgressTest {

	@Suppress("UNCHECKED_CAST")
	private fun section(info: Map<String, Any>, name: String): Map<String, Any> =
		info[name] as Map<String, Any>

	@Test
	fun testInfoKeysComeOutInTheOrderTheLogPrintsThem() {
		val info = RouteCalculationProgress().getInfo(null)
		// java built the outer map and the tiles map with a TreeMap, so both are sorted
		assertEquals(listOf("metrics", "segments", "tiles", "time"), info.keys.toList())
		assertEquals(
			listOf(
				"loadedTiles", "loadedTilesDistinct", "loadedTilesMax",
				"loadedTilesPrevUnloaded", "unloadedTiles"
			),
			section(info, "tiles").keys.toList()
		)
		// the other three keep insertion order
		assertEquals(
			listOf(
				"visited", "queueDirectSize", "queueOppositeSize",
				"visitedDirectPoints", "visitedOppositePoints", "finalSegmentsFound"
			),
			section(info, "segments").keys.toList()
		)
		assertEquals(
			listOf(
				"timeToCalculate", "timeToLoad", "timeToLoadHeaders",
				"timeToFindInitialSegments", "timeExtra"
			),
			section(info, "time").keys.toList()
		)
	}

	@Test
	fun testInfoKeepsCountsIntAndSecondsFloat() {
		// the routing log formats a Float with one decimal and everything else with toString,
		// so the boxed types are part of the output
		val info = RouteCalculationProgress().getInfo(null)
		assertIs<Int>(section(info, "tiles")["loadedTiles"])
		assertIs<Int>(section(info, "segments")["visited"])
		assertIs<Float>(section(info, "time")["timeToCalculate"])
		assertIs<Float>(section(info, "metrics")["segmentsPerSec"])
	}

	@Test
	fun testInfoConvertsNanosToSecondsAndDerivesRates() {
		val progress = RouteCalculationProgress()
		progress.timeToCalculate = 5_000_000_000L
		progress.timeToLoad = 1_000_000_000L
		progress.timeToLoadHeaders = 1_000_000_000L
		progress.timeToFindInitialSegments = 500_000_000L
		progress.timeNanoToCalcDeviation = 250_000_000L
		progress.loadedTiles = 300
		progress.visitedSegments = 1000

		val info = progress.getInfo(null)
		val time = section(info, "time")
		assertEquals(5f, time["timeToCalculate"])
		assertEquals(1f, time["timeToLoad"])
		assertEquals(1f, time["timeToLoadHeaders"])
		assertEquals(0.5f, time["timeToFindInitialSegments"])
		assertEquals(0.25f, time["timeExtra"])

		val metrics = section(info, "metrics")
		assertEquals(150f, metrics["tilesPerSec"])
		// 5s total less the 2.5s spent loading and finding the first segments
		assertEquals(400f, metrics["segmentsPerSec"])
	}

	@Test
	fun testInfoReportsTheSecondPhaseOnItsOwn() {
		val first = RouteCalculationProgress()
		first.loadedTiles = 100
		first.visitedSegments = 700
		first.timeToCalculate = 2_000_000_000L

		val progress = RouteCalculationProgress()
		progress.loadedTiles = 250
		progress.visitedSegments = 1000
		progress.timeToCalculate = 5_000_000_000L

		val info = progress.getInfo(first)
		assertEquals(150, section(info, "tiles")["loadedTiles"])
		assertEquals(300, section(info, "segments")["visited"])
		assertEquals(3f, section(info, "time")["timeToCalculate"])
	}

	@Test
	fun testInfoDropsTilesPerSecWhenNothingWasLoaded() {
		val info = RouteCalculationProgress().getInfo(null)
		val metrics = section(info, "metrics")
		assertNull(metrics["tilesPerSec"])
		assertEquals(0f, metrics["segmentsPerSec"])
	}

	@Test
	fun testInfoReportsTheLargerOfMaxAndDistinctTiles() {
		val progress = RouteCalculationProgress()
		progress.maxLoadedTiles = 40
		progress.distinctLoadedTiles = 90
		assertEquals(90, section(progress.getInfo(null), "tiles")["loadedTilesMax"])
		progress.maxLoadedTiles = 120
		assertEquals(120, section(progress.getInfo(null), "tiles")["loadedTilesMax"])
	}

	@Test
	fun testLinearProgressStartsAtOneAndStopsBelowHundred() {
		val progress = RouteCalculationProgress()
		assertEquals(1f, progress.getLinearProgress())

		progress.totalEstimatedDistance = 1000f
		progress.distanceFromBegin = 1350f
		assertEquals(99f, progress.getLinearProgress())
	}

	@Test
	fun testLinearProgressSplitsTwoIterations() {
		val progress = RouteCalculationProgress()
		progress.totalIterations = 2
		progress.totalEstimatedDistance = 1000f
		progress.distanceFromBegin = 1350f // pr saturates at 1

		progress.iteration = 0
		assertEquals(73f, progress.getLinearProgress())
		progress.iteration = 1
		assertEquals(99f, progress.getLinearProgress())
	}

	@Test
	fun testLinearProgressSpreadsThreeIterationsEvenly() {
		val progress = RouteCalculationProgress()
		progress.totalIterations = 3
		progress.iteration = 1
		progress.totalEstimatedDistance = 1000f
		progress.distanceFromBegin = 1350f // pr saturates at 1, and is then capped at 0.7
		assertEquals(((1 + 0.7) / 3 * 100).toFloat(), progress.getLinearProgress())
	}

	@Test
	fun testHhProgressAddsUpThePassedSteps() {
		val progress = RouteCalculationProgress()
		progress.hhIteration(RouteCalculationProgress.HHIteration.START_END_POINT)
		// SELECT_REGIONS and LOAD_POINTS are worth 5 each, the current step counts as nothing yet
		assertEquals(10f, progress.getLinearProgress())

		progress.hhIterationProgress(0.5)
		assertEquals(17.5f, progress.getLinearProgress())
	}

	@Test
	fun testHhProgressNeverGoesBack() {
		val progress = RouteCalculationProgress()
		progress.hhIteration(RouteCalculationProgress.HHIteration.START_END_POINT)
		progress.hhIterationProgress(0.5)
		progress.hhIterationProgress(0.2)
		progress.hhIterationProgress(-1.0)
		progress.hhIterationProgress(2.0)
		assertEquals(17.5f, progress.getLinearProgress())
	}

	@Test
	fun testHhProgressIsSharedBetweenTargets() {
		val progress = RouteCalculationProgress()
		progress.hhIteration(RouteCalculationProgress.HHIteration.START_END_POINT)
		progress.hhIterationProgress(0.5)
		progress.hhTargetsProgress(1, 2)
		// one target done out of two, plus what the second target has covered so far
		assertEquals(58.75f, progress.getLinearProgress())
	}

	@Test
	fun testHhNotStartedFallsBackToTheDistanceProgress() {
		val progress = RouteCalculationProgress()
		progress.hhIteration(RouteCalculationProgress.HHIteration.DETAILED)
		progress.hhIteration(RouteCalculationProgress.HHIteration.HH_NOT_STARTED)
		assertEquals(1f, progress.getLinearProgress())
	}

	@Test
	fun testApproximationProgress() {
		val progress = RouteCalculationProgress()
		assertEquals(1f, progress.getApproximationProgress())

		progress.totalApproximateDistance = 100f
		progress.approximatedDistance = 50f
		assertEquals(50.5f, progress.getApproximationProgress())

		progress.approximatedDistance = 100f
		assertEquals(99f, progress.getApproximationProgress())
	}

	@Test
	fun testNextIterationDropsTheEstimate() {
		val progress = RouteCalculationProgress()
		progress.totalEstimatedDistance = 5000f
		progress.nextIteration()
		assertEquals(0, progress.iteration)
		assertEquals(0f, progress.totalEstimatedDistance)
	}

	@Test
	fun testCaptureCopiesCountersAndResetsMaxLoadedTiles() {
		val progress = RouteCalculationProgress()
		progress.timeToCalculate = 7L
		progress.visitedSegments = 11
		progress.finalSegmentsFound = 2
		progress.loadedTiles = 13
		progress.maxLoadedTiles = 17

		val captured = RouteCalculationProgress.capture(progress)
		assertEquals(7L, captured.timeToCalculate)
		assertEquals(11, captured.visitedSegments)
		assertEquals(2, captured.finalSegmentsFound)
		assertEquals(13, captured.loadedTiles)
		assertEquals(17, captured.maxLoadedTiles)
		// the second phase counts its own peak from zero
		assertEquals(0, progress.maxLoadedTiles)
	}

	@Test
	fun testFastRoutingStatusOnlyMovesForward() {
		val progress = RouteCalculationProgress()
		assertEquals(FastRoutingState.Status.READY, progress.getFastRoutingStatus())

		progress.raiseFastRoutingStatus(FastRoutingState.Status.MISSING_MAPS_INTERMEDIATES)
		progress.raiseFastRoutingStatus(FastRoutingState.Status.MIXED_MAPS_INTERMEDIATES)
		assertEquals(FastRoutingState.Status.MISSING_MAPS_INTERMEDIATES, progress.getFastRoutingStatus())

		progress.resetFastRoutingStatus()
		assertEquals(FastRoutingState.Status.READY, progress.getFastRoutingStatus())
	}

	@Test
	fun testFailureKeepsTheReasonTheMapsGaveIt() {
		val missing = RouteCalculationProgress()
		missing.raiseFastRoutingStatus(FastRoutingState.Status.MISSING_MAPS_AT_START_OR_END)
		missing.failFastRoutingStatus(false)
		assertEquals(FastRoutingState.Status.FAILED_WITH_MISSING_MAPS, missing.getFastRoutingStatus())

		val mixed = RouteCalculationProgress()
		mixed.raiseFastRoutingStatus(FastRoutingState.Status.MIXED_MAPS_INTERMEDIATES)
		mixed.failFastRoutingStatus(false)
		assertEquals(FastRoutingState.Status.FAILED_WITH_MIXED_MAPS, mixed.getFastRoutingStatus())

		val plain = RouteCalculationProgress()
		plain.failFastRoutingStatus(false)
		assertEquals(FastRoutingState.Status.FAILED_NEED_MORE_LAND_MAPS, plain.getFastRoutingStatus())

		val unsupported = RouteCalculationProgress()
		unsupported.failFastRoutingStatus(true)
		assertEquals(FastRoutingState.Status.FAILED_UNSUPPORTED_PARAMETERS, unsupported.getFastRoutingStatus())
	}

	@Test
	fun testSlowRoutingIsActiveOnlyAfterAFailure() {
		val progress = RouteCalculationProgress()
		assertFalse(progress.isSlowRoutingActive())

		progress.raiseFastRoutingStatus(FastRoutingState.Status.MISSING_MAPS_INTERMEDIATES)
		assertFalse(progress.isSlowRoutingActive())
		assertTrue(progress.hasAnyMissingMaps())
		assertTrue(progress.hasMixedOrMissingMaps())

		progress.failFastRoutingStatus(false)
		assertTrue(progress.isSlowRoutingActive())
	}

	@Test
	fun testMixedMapsAreNotReportedAsMissing() {
		val progress = RouteCalculationProgress()
		progress.raiseFastRoutingStatus(FastRoutingState.Status.MIXED_MAPS_AT_START_OR_END)
		assertFalse(progress.hasAnyMissingMaps())
		assertTrue(progress.hasMixedOrMissingMaps())
	}

	@Test
	fun testSuccessAndCancellationAreNotFailures() {
		assertTrue(FastRoutingState.isSuccessStatus(FastRoutingState.Status.SUCCESS))
		assertFalse(FastRoutingState.isFailedStatus(FastRoutingState.Status.SUCCESS))
		assertTrue(FastRoutingState.isCancelledStatus(FastRoutingState.Status.CANCELLED))
		assertFalse(FastRoutingState.isFailedStatus(FastRoutingState.Status.CANCELLED))
		assertTrue(FastRoutingState.isFailedStatus(FastRoutingState.Status.FAILED_NO_HH_ROUTING_DATA))
	}

	@Test
	fun testCalcCounterRoundTrips() {
		val progress = RouteCalculationProgress()
		assertEquals(0, progress.hhGetCalcCounter())
		progress.hhUpdateCalcCounter(4)
		assertEquals(4, progress.hhGetCalcCounter())
	}
}
