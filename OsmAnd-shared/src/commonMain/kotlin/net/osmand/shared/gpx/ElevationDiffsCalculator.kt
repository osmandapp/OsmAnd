package net.osmand.shared.gpx

import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KMapUtils
import kotlin.math.sqrt

abstract class ElevationDiffsCalculator {

	private val ELE_THRESHOLD = 7.0

	private var diffElevationUp = 0.0
	private var diffElevationDown = 0.0
	private var extremums = mutableListOf<Extremum>()

	data class Extremum(val dist: Double, val ele: Double, val index: Int)

	data class SlopeInfo(
		val startPointIndex: Int,
		val endPointIndex: Int,
		var elevDiff: Double,
		var distance: Double = 0.0,
		var maxSpeed: Double = 0.0,
        var movingTime: Long = 0L
	)

	private var lastUphill: SlopeInfo? = null
	private var lastDownhill: SlopeInfo? = null

	abstract fun getPointDistance(index: Int): Double

	abstract fun getPointElevation(index: Int): Double
	abstract fun getPointIndex(index: Int): Int

	abstract fun getPointsCount(): Int

	fun getDiffElevationUp(): Double {
		return diffElevationUp
	}

	fun getDiffElevationDown(): Double {
		return diffElevationDown
	}

	fun getExtremums(): List<Extremum> {
		return extremums.toList()
	}

	fun getLastUphill(): SlopeInfo? {
		return lastUphill
	}

	fun getLastDownhill(): SlopeInfo? {
		return lastDownhill
	}

	private fun getProjectionDist(x: Double, y: Double, fromx: Double, fromy: Double, tox: Double, toy: Double): Double {
		val mDist = (fromx - tox) * (fromx - tox) + (fromy - toy) * (fromy - toy)
		val projection = KMapUtils.scalarMultiplication(fromx, fromy, tox, toy, x, y)
		val (prx, pry) = when {
			projection < 0 -> Pair(fromx, fromy)
			projection >= mDist -> Pair(tox, toy)
			else -> Pair(fromx + (tox - fromx) * (projection / mDist), fromy + (toy - fromy) * (projection / mDist))
		}
		return sqrt((prx - x) * (prx - x) + (pry - y) * (pry - y))
	}

	private fun findMaximumExtremumBetween(start: Int, end: Int, points: BooleanArray) {
		val firstPointDist = getPointDistance(start)
		val firstPointEle = getPointElevation(start)
		val endPointEle = getPointElevation(end)
		val endPointDist = getPointDistance(end)
		var max = start
		var maxDiff = ELE_THRESHOLD
		for (i in start + 1 until end) {
			val md = getProjectionDist(getPointDistance(i), getPointElevation(i),
				firstPointDist, firstPointEle, endPointDist, endPointEle)
			if (md > maxDiff) {
				max = i
				maxDiff = md
			}
		}
		if (max != start) {
			points[max] = true
			findMaximumExtremumBetween(start, max, points)
			findMaximumExtremumBetween(max, end, points)
		}
	}

	fun calculateElevationDiffs() {
		val pointsCount = getPointsCount()
		if (pointsCount < 2) {
			return
		}
		lastUphill = null
		lastDownhill = null
		diffElevationUp = 0.0
		diffElevationDown = 0.0

		val points = BooleanArray(pointsCount)
		points[0] = true
		points[pointsCount - 1] = true
		findMaximumExtremumBetween(0, pointsCount - 1, points)

		extremums = mutableListOf()
		for (i in points.indices) {
			if (points[i]) {
				extremums.add(Extremum(getPointDistance(i), getPointElevation(i), getPointIndex(i)))
			}
		}

		var currentUphill: SlopeInfo? = null
		var currentDownhill: SlopeInfo? = null

		for (i in 1 until extremums.size) {
			val start = extremums[i - 1]
			val end = extremums[i]
			val eleDiffSumm = end.ele - start.ele

			if (eleDiffSumm > 0) {
				diffElevationUp += eleDiffSumm
				currentUphill = processLastSlope(currentUphill, start, end, eleDiffSumm) { lastUphill = it }
				currentDownhill = null
			} else if (eleDiffSumm < 0) {
				val elevAbs = -eleDiffSumm
				diffElevationDown += elevAbs
				currentDownhill = processLastSlope(currentDownhill, start, end, elevAbs) { lastDownhill = it }
				currentUphill = null
			}
		}
	}

	private fun processLastSlope(
		current: SlopeInfo?,
		start: Extremum,
		end: Extremum,
		eleDiffSumm: Double,
		setter: (SlopeInfo) -> Unit
	): SlopeInfo {
		val updated = if (current != null && current.endPointIndex == start.index) {
			current.copy(
				endPointIndex = end.index,
				elevDiff = current.elevDiff + eleDiffSumm
			)
		} else {
			SlopeInfo(
				startPointIndex = start.index,
				endPointIndex = end.index,
				elevDiff = eleDiffSumm
			)
		}
		setter(updated)
		return updated
	}

	companion object {
		fun calculateDiffs(points: List<WptPt>) {
			val approximator: ElevationApproximator =
				object : ElevationApproximator() {
					override fun getPointLatitude(index: Int): Double {
						return points[index].lat
					}

					override fun getPointLongitude(index: Int): Double {
						return points[index].lon
					}

					override fun getPointElevation(index: Int): Double {
						return points[index].ele
					}

					override fun getPointsCount(): Int {
						return points.size
					}
				}
			approximator.approximate()
			val distances: DoubleArray? = approximator.getDistances()
			val elevations: DoubleArray? = approximator.getElevations()
			val pointIndexes: IntArray? = approximator.getSurvivedIndexes()
			if (distances != null && elevations != null && pointIndexes != null) {
				var diffElevationUp = 0.0
				var diffElevationDown = 0.0
				val elevationDiffsCalc: ElevationDiffsCalculator =
					object : ElevationDiffsCalculator() {
						override fun getPointDistance(index: Int): Double {
							return distances[index]
						}

						override fun getPointElevation(index: Int): Double {
							return elevations[index]
						}

						override fun getPointIndex(index: Int): Int {
							return pointIndexes[index]
						}

						override fun getPointsCount(): Int {
							return distances.size
						}
					}
				elevationDiffsCalc.calculateElevationDiffs()
				diffElevationUp += elevationDiffsCalc.getDiffElevationUp()
				diffElevationDown += elevationDiffsCalc.getDiffElevationDown()
				println(
					"GPX points=" + points.size + " approx points=" + distances.size
							+ " diffUp=" + diffElevationUp + " diffDown=" + diffElevationDown)
			}

		}

	}
}

fun main() {
	val gpxFile: GpxFile =
		GpxUtilities.loadGpxFile(KFile("/Users/crimean/Downloads/2011-09-27_Mulhacen.gpx"))
	val points: List<WptPt> =
		gpxFile.tracks.get(0).segments.get(0).points
	ElevationDiffsCalculator.calculateDiffs(points)
	val start = 200
	for (i in start + 150 until start + 160) {
		ElevationDiffsCalculator.calculateDiffs(points.subList(start, i))
	}
}
