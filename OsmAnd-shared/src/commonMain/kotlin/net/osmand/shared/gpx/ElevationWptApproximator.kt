package net.osmand.shared.gpx

import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.util.KMapUtils

class ElevationWptApproximator(var points: MutableList<WptPt>) : ElevationApproximator() {

    private var approximatedWpts = ArrayList<WptPt>()

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

    fun getApproximatedWpts(): ArrayList<WptPt> {
        return approximatedWpts
    }

    override fun filterSurvived(survivedCount: Int, pointsCount: Int, survived: BooleanArray) {
        val distances = DoubleArray(survivedCount + 2)
        val elevations = DoubleArray(survivedCount + 2)
        approximatedWpts = ArrayList()

        var k = 0
        var lastSurvived = 0
        for (i in 0 until pointsCount) {
            if (!survived[i]) {
                continue
            }
            distances[k] = if (lastSurvived == 0) 0.0 else KMapUtils.getDistance(
                getPointLatitude(i), getPointLongitude(i),
                getPointLatitude(lastSurvived), getPointLongitude(lastSurvived)
            )
            elevations[k] = getPointElevation(i)
            approximatedWpts.add(points[i])
            k++
            lastSurvived = i
        }

        setDistance(distances)
        setElevations(elevations)
    }
}