package net.osmand.shared.gpx

import net.osmand.shared.util.KMapUtils

abstract class ElevationApproximator {

	private val SLOPE_THRESHOLD = 70.0

	private var distances: DoubleArray? = null
	private var elevations: DoubleArray? = null

	abstract fun getPointLatitude(index: Int): Double

	abstract fun getPointLongitude(index: Int): Double

	abstract fun getPointElevation(index: Int): Double

	abstract fun getPointsCount(): Int

	fun getDistances(): DoubleArray? {
		return distances
	}

	fun getElevations(): DoubleArray? {
		return elevations
	}

	fun approximate(): Boolean {
		val pointsCount = getPointsCount()
		if (pointsCount < 4) {
			return false
		}

		val survived = BooleanArray(pointsCount)
		var lastSurvived = 0
		var survivedCount = 0
		for (i in 1 until pointsCount - 1) {
			val prevEle = getPointElevation(lastSurvived)
			val ele = getPointElevation(i)
			val eleNext = getPointElevation(i + 1)
			if ((ele - prevEle) * (eleNext - ele) > 0) {
				survived[i] = true
				lastSurvived = i
				survivedCount++
			}
		}
		survived[pointsCount - 1] = true
		survivedCount++
		if (survivedCount < 2) {
			return false
		}

		lastSurvived = 0
		survivedCount = 0
		for (i in 1 until pointsCount - 1) {
			if (!survived[i]) {
				continue
			}
			val ele = getPointElevation(i)
			val prevEle = getPointElevation(lastSurvived)
			val dist = KMapUtils.getDistance(
				getPointLatitude(i), getPointLongitude(i),
				getPointLatitude(lastSurvived), getPointLongitude(lastSurvived)
			)
			val slope = (ele - prevEle) * 100 / dist
			if (kotlin.math.abs(slope) > SLOPE_THRESHOLD) {
				survived[i] = false
				continue
			}
			lastSurvived = i
			survivedCount++
		}
		if (survivedCount < 2) {
			return false
		}

		survived[0] = true
		survived[pointsCount - 1] = true
		val distances = DoubleArray(survivedCount + 2)
		val elevations = DoubleArray(survivedCount + 2)
		var k = 0
		lastSurvived = 0
		for (i in 0 until pointsCount) {
			if (!survived[i]) {
				continue
			}
			distances[k] = if (lastSurvived == 0) 0.0 else KMapUtils.getDistance(
				getPointLatitude(i), getPointLongitude(i),
				getPointLatitude(lastSurvived), getPointLongitude(lastSurvived)
			)
			elevations[k] = getPointElevation(i)
			k++
			lastSurvived = i
		}
		this.distances = distances
		this.elevations = elevations
		return true
	}
}
