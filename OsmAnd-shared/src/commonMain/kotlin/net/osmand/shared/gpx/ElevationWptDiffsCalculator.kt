package net.osmand.shared.gpx

import net.osmand.shared.gpx.primitives.WptPt

class ElevationWptDiffsCalculator(var distances: DoubleArray, var elevations: DoubleArray, var wptPts: MutableList<WptPt>): ElevationDiffsCalculator() {
    override fun getPointDistance(index: Int): Double {
        return distances[index]
    }

    override fun getPointElevation(index: Int): Double {
        return elevations[index]
    }

    override fun getPointsCount(): Int {
        return distances.size
    }

    override fun collectExtremums(points: BooleanArray){
        val extremums = mutableListOf<Extremum>()
        for (i in points.indices) {
            if (points[i]) {
                extremums.add(Extremum(getPointDistance(i), getPointElevation(i), wptPts[i]))
            }
        }
        setExtremums(extremums)
    }
}