package net.osmand.shared

import net.osmand.shared.extensions.format
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.KMapUtils.get31TileNumberX
import net.osmand.shared.util.KMapUtils.get31TileNumberY
import kotlin.math.*
import kotlin.test.Test
import kotlin.test.assertTrue

class GetDistanceTest {
    val TEST_ELLIPSOID = true // KMapUtils.getEllipsoidDistance took 10.7s for 10m iterations
    val TEST_HAVERSINE = true // KMapUtils.getDistance took 6.7s for 10m iterations
    val TEST_SQR = true // KMapUtils.squareRootDist31 took 6.5s for 10m iterations

    val ELLIPSOID_MAX_DEVIATION = 0.001 // %
    val HAVERSINE_MAX_DEVIATION = 0.43 // %
    val SQR_MAX_DEVIATION = 10.1 // %

    val ITERATIONS = 1

    val tests = listOf(
        entry(559.36, 40.8212675, 14.4227469, 40.8212512, 14.4293773, "IT-Vesuvio (x)"),
        entry(542.81, 40.8237519, 14.4262445, 40.8188642, 14.4261801, "IT-Vesuvio (y)"),
        entry(4739.81, -39.29491586, 174.03285739, -39.29584579, 174.08778903, "NZ-Taranaki (x)"),
        entry(4524.64, -39.27883939, 174.06337019, -39.31948845, 174.06714675, "NZ-Taranaki (y)"),
        entry(19312.52, -39.29212599, 173.95213362, -39.29451731, 174.17598006, "NZ-Egmont (x)"),
        entry(19328.25, -39.20864435, 174.06371352, -39.38267339, 174.06989333, "NZ-Egmont (y)"),
        entry(54953.90, 48.22579901, 16.37482333, 48.16034314, 17.10747409, "Wien-Bratislava (x)"),
        entry(933673.41, 52.52435240, 13.40508151, 51.55122931, -0.13007474, "Berlin-London (x-axis)"),
        entry(790831.08, 60.01115926, 10.63476563, 60.18641482, 24.87304688, "Oslo-Helsinki (north-x-axis)"),
        entry(1171395.00, -6.12400803, 106.81804894, 3.13519342, 101.69842003, "Jakarta-Kuala-Lumpur (equator-y-axis)"),
        entry(1600970.59, 38.11914201, 13.36113620, 52.52435240, 13.40508151, "Palermo-Berlin (y-axis)"),
        entry(2194405.97, -70.66645729, -69.20288086, -51.66564060, -57.88348915, "Alexander-Falkland-Islands (south-y-axis)"),
        entry(7000804.41, 48.93849659, 2.24297214, 19.25116676, 72.73125339, "Paris-Mumbai (xy-axis)"),
        entry(7908575.30, -0.89518678, 32.83611534, 1.29094680, 103.85174034, "Lake-Viktoria-Singapore (equator-x-axis)"),
        entry(9190652.22, 0.0, -154.26, 40.76570188, -74.00214505, "Center-NewYork (test)"),

        // next entries require SQR deviation 10.1% instead of 3.23% (was enough for the previous entries)
        entry(6810098.37, -33.70408492, 151.04180026, 0.0, -154.26, "Sydney-Center (test-1)"),
        entry(6810795.91, -33.70408492, 151.04180026, 0.01, -154.26, "Sydney-Center (test-2)"),
        entry(6809400.88, -33.70408492, 151.04180026, -0.01, -154.26, "Sydney-Center (test-3)"),
        entry(15995251.85, -33.70408492, 151.04180026, 40.76570188, -74.00214505, "Sydney-NewYork (far)"),
    )

    data class entry(
        val ideal: Double,
        val lat1: Double,
        val lon1: Double,
        val lat2: Double,
        val lon2: Double,
        val note: String
    )

    @Test
    fun testGetDistance() {
        for (test in tests) {
            val lat1 = test.lat1
            val lon1 = test.lon1
            val lat2 = test.lat2
            val lon2 = test.lon2
            val note = test.note
            val ideal = test.ideal
            val x1 = get31TileNumberX(lon1)
            val y1 = get31TileNumberY(lat1)
            val x2 = get31TileNumberX(lon2)
            val y2 = get31TileNumberY(lat2)
            for (i in 0 until ITERATIONS) {
                if (TEST_ELLIPSOID) {
                    val distance1 = KMapUtils.getEllipsoidDistance(lat1, lon1, lat2, lon2)
                    val distance2 = KMapUtils.getEllipsoidDistance(lat2, lon2, lat1, lon1)
                    validate(distance1, ideal, ELLIPSOID_MAX_DEVIATION, "ellipsoid", note)
                    validate(distance2, ideal, ELLIPSOID_MAX_DEVIATION, "ellipsoid-rev", note)
                }
                if (TEST_HAVERSINE) {
                    val distance1 = KMapUtils.getDistance(lat1, lon1, lat2, lon2)
                    val distance2 = KMapUtils.getDistance(lat2, lon2, lat1, lon1)
                    validate(distance1, ideal, HAVERSINE_MAX_DEVIATION, "haversine", note)
                    validate(distance2, ideal, HAVERSINE_MAX_DEVIATION, "haversine-rev", note)
                }
                if (TEST_SQR) {
                    val distance1 = KMapUtils.squareRootDist31(x1, y1, x2, y2)
                    val distance2 = KMapUtils.squareRootDist31(x2, y2, x1, y1)
                    validate(distance1, ideal, SQR_MAX_DEVIATION, "sqare", note)
                    validate(distance2, ideal, SQR_MAX_DEVIATION, "sqare-rev", note)
                }
            }
        }
    }

    private fun validate(distance: Double, ideal: Double, maxDeviation: Double, type: String, note: String) {
        val deviation = abs(distance / ideal * 100 - 100)
        assertTrue(
            deviation < maxDeviation,
            "Failed: $type $note distance %.2f deviation %.5f > %.5f (%%)".format(distance, deviation, maxDeviation)
        )
    }
}
