package net.osmand.shared.routing

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * Live state of one route calculation: how far it has got, what it has loaded, and how long that
 * took.
 *
 * A copy of `net.osmand.router.RouteCalculationProgress`, which stays in OsmAnd-java: android, tools and the C++ core keep using
 * the original, and this copy is for iOS. Keep the two identical, field names included.
 */
class RouteCalculationProgress {

	@JvmField
	var segmentNotFound: Int = -1

	@JvmField
	var distanceFromBegin: Float = 0f

	@JvmField
	var directDistance: Float = 0f

	@JvmField
	var directSegmentQueueSize: Int = 0

	@JvmField
	var distanceFromEnd: Float = 0f

	@JvmField
	var reverseSegmentQueueSize: Int = 0

	@JvmField
	var reverseDistance: Float = 0f

	@JvmField
	var totalEstimatedDistance: Float = 0f

	@JvmField
	var totalApproximateDistance: Float = 0f

	@JvmField
	var approximatedDistance: Float = 0f

	@JvmField
	var routingCalculatedTime: Float = 0f

	@JvmField
	var visitedSegments: Int = 0

	@JvmField
	var visitedDirectSegments: Int = 0

	@JvmField
	var visitedOppositeSegments: Int = 0

	@JvmField
	var directQueueSize: Int = 0

	@JvmField
	var oppositeQueueSize: Int = 0

	@JvmField
	var finalSegmentsFound: Int = 0

	@JvmField
	var totalIterations: Int = 1

	@JvmField
	var iteration: Int = -1

	@JvmField
	var timeNanoToCalcDeviation: Long = 0

	@JvmField
	var timeToLoad: Long = 0

	@JvmField
	var timeToLoadHeaders: Long = 0

	@JvmField
	var timeToFindInitialSegments: Long = 0

	@JvmField
	var timeToCalculate: Long = 0

	@JvmField
	var distinctLoadedTiles: Int = 0

	@JvmField
	var maxLoadedTiles: Int = 0

	@JvmField
	var loadedPrevUnloadedTiles: Int = 0

	@JvmField
	var unloadedTiles: Int = 0

	@JvmField
	var loadedTiles: Int = 0

	@JvmField
	var isCancelled: Boolean = false

	@JvmField
	var requestPrivateAccessRouting: Boolean = false

	@JvmField
	var routeCalculationStartTime: Long = 0

	@JvmField
	var missingMapsCalculationResult: MissingMapsResult? = null

	// An int rather than the enum, as in java, where the C++ core reads and writes it
	private var fastRoutingStatusOrdinal: Int = FastRoutingState.Status.READY.ordinal

	private var hhIterationStep: Int = HHIteration.HH_NOT_STARTED.ordinal
	private var hhTargetsDone: Int = 0
	private var hhTargetsTotal: Int = 0
	private var hhCurrentStepProgress: Double = 0.0
	private var hhCalcCounter: Int = 0

	fun getInfo(firstPhase: RouteCalculationProgress?): MutableMap<String, Any> {
		val first = firstPhase ?: RouteCalculationProgress()
		val tiles = LinkedHashMap<String, Any>()
		tiles["loadedTiles"] = this.loadedTiles - first.loadedTiles
		tiles["loadedTilesDistinct"] = this.distinctLoadedTiles - first.distinctLoadedTiles
		tiles["loadedTilesPrevUnloaded"] = this.loadedPrevUnloadedTiles - first.loadedPrevUnloadedTiles
		tiles["loadedTilesMax"] = max(this.maxLoadedTiles, this.distinctLoadedTiles)
		tiles["unloadedTiles"] = this.unloadedTiles - first.unloadedTiles
		val segms = LinkedHashMap<String, Any>()
		segms["visited"] = this.visitedSegments - first.visitedSegments
		segms["queueDirectSize"] = this.directQueueSize - first.directQueueSize
		segms["queueOppositeSize"] = this.reverseSegmentQueueSize - first.reverseSegmentQueueSize
		segms["visitedDirectPoints"] = this.visitedDirectSegments - first.visitedDirectSegments
		segms["visitedOppositePoints"] = this.visitedOppositeSegments - first.visitedOppositeSegments
		segms["finalSegmentsFound"] = this.finalSegmentsFound - first.finalSegmentsFound
		val time = LinkedHashMap<String, Any>()
		val timeToCalc = ((this.timeToCalculate - first.timeToCalculate) / 1.0e9).toFloat()
		time["timeToCalculate"] = timeToCalc
		val timeToLoad = ((this.timeToLoad - first.timeToLoad) / 1.0e9).toFloat()
		time["timeToLoad"] = timeToLoad
		val timeToLoadHeaders = ((this.timeToLoadHeaders - first.timeToLoadHeaders) / 1.0e9).toFloat()
		time["timeToLoadHeaders"] = timeToLoadHeaders
		val timeToFindInitialSegments =
			((this.timeToFindInitialSegments - first.timeToFindInitialSegments) / 1.0e9).toFloat()
		time["timeToFindInitialSegments"] = timeToFindInitialSegments
		val timeExtra = ((this.timeNanoToCalcDeviation - first.timeNanoToCalcDeviation) / 1.0e9).toFloat()
		time["timeExtra"] = timeExtra
		val metrics = LinkedHashMap<String, Any>()
		if (timeToLoad + timeToLoadHeaders > 0) {
			metrics["tilesPerSec"] = (this.loadedTiles - first.loadedTiles) / (timeToLoad + timeToLoadHeaders)
		}
		val pureTime = timeToCalc - (timeToLoad + timeToLoadHeaders + timeToFindInitialSegments)
		if (pureTime > 0) {
			metrics["segmentsPerSec"] = (this.visitedSegments - first.visitedSegments) / pureTime
		} else {
			metrics["segmentsPerSec"] = 0f
		}
		val map = LinkedHashMap<String, Any>()
		map["tiles"] = sortedByKey(tiles)
		map["segments"] = segms
		map["time"] = time
		map["metrics"] = metrics
		return sortedByKey(map)
	}

	private fun getLinearProgressHH(): Float {
		var progress = 0f
		for (i in HHIteration.entries) {
			if (i.ordinal == hhIterationStep) {
				// the multiply runs in double, the way it did in java, and narrows on the way back
				progress = (progress + hhCurrentStepProgress * i.approxStepPercent.toFloat()).toFloat() // current step
				break
			} else {
				progress += i.approxStepPercent.toFloat() // passed step
			}
		}

		// 1. implement 2-3 reiterations progress

		if (hhTargetsTotal > 0) {
			progress = (100f * hhTargetsDone + progress) / hhTargetsTotal // intermediate points
		}

		return min(progress, 99f)
	}

	fun getLinearProgress(): Float {
		if (hhIterationStep != HHIteration.HH_NOT_STARTED.ordinal) {
			return getLinearProgressHH()
		}
		val p = max(distanceFromBegin, distanceFromEnd)
		val all = totalEstimatedDistance * 1.35f
		var pr = 0f
		if (all > 0) {
			pr = min(p * p / (all * all), 1f)
		}
		val progress: Float
		if (totalIterations <= 1) {
			progress = INITIAL_PROGRESS + pr * (1f - INITIAL_PROGRESS)
		} else if (totalIterations <= 2) {
			progress = if (iteration < 1) {
				pr * FIRST_ITERATION + INITIAL_PROGRESS
			} else {
				(INITIAL_PROGRESS + FIRST_ITERATION) + pr * (1f - FIRST_ITERATION - INITIAL_PROGRESS)
			}
		} else {
			progress = ((iteration + min(pr.toDouble(), 0.7)) / totalIterations).toFloat()
		}
		return min(progress * 100f, 99f)
	}

	fun getApproximationProgress(): Float {
		var progress = 0f
		if (totalApproximateDistance > 0) {
			progress = approximatedDistance / totalApproximateDistance
		}
		progress = INITIAL_PROGRESS + progress * (1f - INITIAL_PROGRESS)
		return min(progress * 100f, 99f)
	}

	fun nextIteration() {
		iteration++
		totalEstimatedDistance = 0f
	}

	enum class HHIteration(@JvmField val approxStepPercent: Int) {
		HH_NOT_STARTED(0), // hhIteration is not filled
		SELECT_REGIONS(5),
		LOAD_POINTS(5),
		START_END_POINT(15),
		ROUTING(15),
		DETAILED(50),
		RECALCULATION(10),
		ALTERNATIVES(0), // disabled
		DONE(0); // success
	}

	fun hhIteration(step: HHIteration) {
		this.hhIterationStep = step.ordinal
		this.hhCurrentStepProgress = 0.0
	}

	fun hhIterationProgress(k: Double) {
		// validate 0-100% and disallow to progress back
		if (k >= 0 && k <= 1.0 && k > this.hhCurrentStepProgress) {
			this.hhCurrentStepProgress = k
		}
	}

	fun hhTargetsProgress(done: Int, total: Int) {
		this.hhTargetsDone = done
		this.hhTargetsTotal = total
	}

	fun hhUpdateCalcCounter(counter: Int) {
		this.hhCalcCounter = counter
	}

	fun hhGetCalcCounter(): Int {
		return this.hhCalcCounter
	}

	fun isSlowRoutingActive(): Boolean {
		return FastRoutingState.isSlowRoutingActive(fastRoutingStatusOrdinal)
	}

	fun hasMixedOrMissingMaps(): Boolean {
		return FastRoutingState.isMixedOrMissingMaps(fastRoutingStatusOrdinal)
	}

	fun hasAnyMissingMaps(): Boolean {
		return FastRoutingState.isMissingMaps(fastRoutingStatusOrdinal)
	}

	fun getFastRoutingStatus(): FastRoutingState.Status {
		return FastRoutingState.get(fastRoutingStatusOrdinal)
	}

	fun resetFastRoutingStatus() {
		fastRoutingStatusOrdinal = FastRoutingState.reset()
	}

	fun failFastRoutingStatus(hasUnsupportedParameters: Boolean) {
		fastRoutingStatusOrdinal = FastRoutingState.fail(fastRoutingStatusOrdinal, hasUnsupportedParameters)
	}

	fun raiseFastRoutingStatus(status: FastRoutingState.Status) {
		fastRoutingStatusOrdinal = FastRoutingState.raise(fastRoutingStatusOrdinal, status)
	}

	companion object {

		private const val INITIAL_PROGRESS = 0.01f
		private const val FIRST_ITERATION = 0.72f

		@JvmStatic
		fun capture(cp: RouteCalculationProgress): RouteCalculationProgress {
			val p = RouteCalculationProgress()
			p.timeNanoToCalcDeviation = cp.timeNanoToCalcDeviation
			p.timeToCalculate = cp.timeToCalculate
			p.timeToLoadHeaders = cp.timeToLoadHeaders
			p.timeToFindInitialSegments = cp.timeToFindInitialSegments
			p.timeToLoad = cp.timeToLoad

			p.visitedSegments = cp.visitedSegments
			p.directQueueSize = cp.directQueueSize
			p.reverseSegmentQueueSize = cp.reverseSegmentQueueSize
			p.visitedDirectSegments = cp.visitedDirectSegments
			p.visitedOppositeSegments = cp.visitedOppositeSegments
			p.finalSegmentsFound = cp.finalSegmentsFound

			p.loadedTiles = cp.loadedTiles
			p.distinctLoadedTiles = cp.distinctLoadedTiles
			p.maxLoadedTiles = cp.maxLoadedTiles
			p.loadedPrevUnloadedTiles = cp.loadedPrevUnloadedTiles

			cp.maxLoadedTiles = 0
			return p
		}

		/**
		 * Java built the maps [getInfo] returns with a `TreeMap`, and their order is user visible:
		 * the routing log prints the entries as they iterate. There is no sorted map in common
		 * code, so the keys are put in order once, here.
		 */
		private fun sortedByKey(values: Map<String, Any>): MutableMap<String, Any> {
			val sorted = LinkedHashMap<String, Any>()
			for (key in values.keys.sorted()) {
				sorted[key] = values.getValue(key)
			}
			return sorted
		}
	}
}
