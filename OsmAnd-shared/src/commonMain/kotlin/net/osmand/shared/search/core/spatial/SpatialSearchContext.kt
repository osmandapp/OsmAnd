package net.osmand.shared.search.core.spatial

import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.util.JavaFormat

/**
 * The state of one spatial text search: the maps, the words of the query and their atoms.
 *
 * A copy of `SpatialSearchContext` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. So far only its statistics, which the words of a query count into; the search
 * itself comes in a later change.
 */
class SpatialSearchContext private constructor() {

	class SpatialSearchStats {
		var requestTime = Timer()
		var step1Atoms = Timer()
		var sub1FileAtomsTime = Timer()
		var sub1MatchTime = Timer()
		var sub1PartMatchTime = sub1MatchTime // new Timer();
		var sub1PoiNameBoundaryTime = Timer()
		var tokenObjs = 0

		var step2Compute = Timer()
		var sub2LoadObjectsBldTime = Timer()
		var sub2ReadObjTime = Timer()
		var maxCombinations = 0

		var step3Sort = Timer()

		var readTableBytes = 0L
		var readAtomsBytes = 0L
		var readObjsBytes = 0L
		var skipTableBytes = 0L
		var skipAtomsBytes = 0L

		// separate api
		var poiByTypeTime = Timer()
		var poiByTypeBytes = 0L
		var poiByTypeBboxes = 0L

		var doTiming = true
		var printLogs = true

		inner class Timer {
			var time = 0L
			var intervals = 0
			var endTime = nanoTime()

			fun start() {
				intervals++
				if (doTiming) {
					time -= nanoTime()
				}
			}

			fun finish() {
				if (doTiming) {
					time += nanoTime()
				}
			}

			fun ms(): Double = time / 1e6
		}

		override fun toString(): String =
			"Search Stats ${f(requestTime.ms())} ms (read ${g((readTableBytes + readAtomsBytes + readObjsBytes) / 1024)} KB): " +
					"1. Atoms ${f(step1Atoms.ms())} ms ${g(tokenObjs.toLong())} (read ${f(sub1FileAtomsTime.ms())} " +
					"${sub1FileAtomsTime.intervals}, match ${f(sub1MatchTime.ms())} ${g(sub1MatchTime.intervals.toLong())}, " +
					"poi ${f(sub1PoiNameBoundaryTime.ms())}), " +
					"2. Compute ${f(step2Compute.ms())} ms ${g(maxCombinations.toLong())} " +
					"(validate ${f(sub2LoadObjectsBldTime.ms())} > read ${f(sub2ReadObjTime.ms())})"

		private fun f(ms: Double): String = JavaFormat.fixed(ms, 1)

		private fun g(value: Long): String = JavaFormat.grouped(value)
	}
}
