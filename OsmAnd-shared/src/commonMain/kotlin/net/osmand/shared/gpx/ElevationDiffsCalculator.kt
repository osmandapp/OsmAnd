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

	data class Extremum(val dist: Double, val ele: Double)

	abstract fun getPointDistance(index: Int): Double

	abstract fun getPointElevation(index: Int): Double

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
		val points = BooleanArray(pointsCount)
		points[0] = true
		points[pointsCount - 1] = true
		findMaximumExtremumBetween(0, pointsCount - 1, points)

		extremums = mutableListOf()
		for (i in points.indices) {
			if (points[i]) {
				extremums.add(Extremum(getPointDistance(i), getPointElevation(i)))
			}
		}

		for (i in 1 until extremums.size) {
			val prevElevation = extremums[i - 1].ele
			val elevation = extremums[i].ele
			val eleDiffSumm = elevation - prevElevation
			if (eleDiffSumm > 0) {
				diffElevationUp += eleDiffSumm
			} else {
				diffElevationDown -= eleDiffSumm
			}
		}
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
			if (distances != null && elevations != null) {
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
