package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrecalculatedRouteDirectionTest {

	/** Six points a hundredth of a degree apart, west to east along the same parallel. */
	private val line = Array(6) { KLatLon(50.0, 30.0 + it * 0.01) }

	private fun x(i: Int) = KMapUtils.get31TileNumberX(line[i].longitude)
	private fun y(i: Int) = KMapUtils.get31TileNumberY(line[i].latitude)

	private fun direction() = PrecalculatedRouteDirection.build(line, 20f)

	private fun router(): GeneralRouter = GeneralRouter(GeneralRouterProfile.CAR, LinkedHashMap())

	@Test
	fun testEveryPointOfTheRouteFindsItself() {
		val direction = direction()
		for (i in line.indices) {
			assertEquals(i, direction.getIndex(x(i), y(i)), "point $i")
		}
	}

	@Test
	fun testAPointBesideTheRouteFindsTheNearestOne() {
		val direction = direction()
		// a little north of the third point, well inside the search box
		val aside = KMapUtils.get31TileNumberY(50.0005)
		assertEquals(2, direction.getIndex(x(2), aside))
	}

	@Test
	fun testAPointFarFromTheRouteIsNotFound() {
		assertEquals(-1, direction().getIndex(KMapUtils.get31TileNumberX(10.0), KMapUtils.get31TileNumberY(10.0)))
	}

	@Test
	fun testDeviationDistanceGrowsWithTheDistanceFromTheRoute() {
		val direction = direction()
		val onRoute = direction.getDeviationDistance(x(2), y(2))
		val aside = direction.getDeviationDistance(x(2), KMapUtils.get31TileNumberY(50.0005))
		assertTrue(aside > onRoute, "expected $aside to be more than $onRoute")
		// the ends have no segment to project onto
		assertEquals(0f, direction.getDeviationDistance(x(0), y(0)))
		assertEquals(0f, direction.getDeviationDistance(x(5), y(5)))
	}

	@Test
	fun testAdoptCutsTheRouteBetweenTheTwoPoints() {
		val adopted = direction().adopt(x(1), y(1), x(4), y(4), router())
		assertNotNull(adopted)
		// the adopted copy is indexed from its own start, and holds only the four points it kept
		assertEquals(0, adopted.getIndex(x(1), y(1)))
		assertEquals(3, adopted.getIndex(x(4), y(4)))
		// a point outside the cut falls back to the nearest one that is still there
		assertEquals(0, adopted.getIndex(x(0), y(0)))
		assertEquals(3, adopted.getIndex(x(5), y(5)))
	}

	@Test
	fun testAdoptWorksBackwards() {
		val adopted = direction().adopt(x(4), y(4), x(1), y(1), router())
		assertNotNull(adopted)
		assertEquals(0, adopted.getIndex(x(1), y(1)))
		assertEquals(3, adopted.getIndex(x(4), y(4)))
	}

	@Test
	fun testAdoptGivesUpWhenAPointIsNotOnTheRoute() {
		val far = KMapUtils.get31TileNumberX(10.0)
		assertNull(direction().adopt(far, far, x(4), y(4), router()))
		assertNull(direction().adopt(x(1), y(1), far, far, router()))
	}

	@Test
	fun testAdoptCarriesFollowNext() {
		val direction = direction()
		assertFalse(direction.isFollowNext())
		direction.setFollowNext(true)
		assertTrue(direction.adopt(x(1), y(1), x(4), y(4), router())!!.isFollowNext())
	}

	@Test
	fun testTimeEstimateGrowsWithTheDistanceFromTheStart() {
		val adopted = direction().adopt(x(0), y(0), x(5), y(5), router())!!
		// measured from the start, so it is the time already spent getting there
		val early = adopted.timeEstimate(x(0), y(0), x(1), y(1))
		val late = adopted.timeEstimate(x(0), y(0), x(4), y(4))
		assertTrue(early > 0, "expected a positive estimate, got $early")
		assertTrue(late > early, "expected $late to be more than $early")
	}

	@Test
	fun testTimeEstimateFromTheEndCountsWhatIsLeft() {
		val adopted = direction().adopt(x(0), y(0), x(5), y(5), router())!!
		// measured against the target, so it is the time still to come
		val near = adopted.timeEstimate(x(5), y(5), x(4), y(4))
		val far = adopted.timeEstimate(x(5), y(5), x(1), y(1))
		assertTrue(near > 0, "expected a positive estimate, got $near")
		assertTrue(far > near, "expected $far to be more than $near")
	}

	@Test
	fun testTimeEstimateHasNothingToSayAtTheVeryEnds() {
		val adopted = direction().adopt(x(0), y(0), x(5), y(5), router())!!
		assertEquals(-1f, adopted.timeEstimate(x(0), y(0), x(0), y(0)))
		assertEquals(-1f, adopted.timeEstimate(x(5), y(5), x(5), y(5)))
	}

	@Test
	fun testTimeEstimateRefusesTwoPointsThatAreNeitherEnd() {
		val adopted = direction().adopt(x(0), y(0), x(5), y(5), router())!!
		try {
			adopted.timeEstimate(x(1), y(1), x(2), y(2))
			throw AssertionError("expected the estimate to refuse two interior points")
		} catch (e: UnsupportedOperationException) {
			assertTrue(e.message!!.contains("startPoint:"), e.message!!)
		}
	}

	@Test
	fun testPreciseStartAndEndCanBeMovedAfterwards() {
		val adopted = direction().adopt(x(0), y(0), x(5), y(5), router())!!
		assertTrue(adopted.timeEstimate(x(0), y(0), x(2), y(2)) > 0)

		// the planner snapped the real start and target onto roads a point along
		adopted.updatePreciseStartEnd(x(1), y(1), x(4), y(4))
		val after = adopted.timeEstimate(x(1), y(1), x(2), y(2))
		assertTrue(after > 0, "expected a positive estimate from the new start, got $after")

		// and the old start is no longer one of the two ends it will measure against
		try {
			adopted.timeEstimate(x(0), y(0), x(2), y(2))
			throw AssertionError("expected the old start to be refused")
		} catch (e: UnsupportedOperationException) {
			assertTrue(e.message!!.contains("startPoint:"), e.message!!)
		}
	}

	@Test
	fun testBuildFromARouteTrimsBothEnds() {
		val segments = segments(300f, 300f, 300f, 300f)
		// 350 m off each end leaves the two middle segments
		val direction = PrecalculatedRouteDirection.build(segments, 350f, 20f)
		assertNotNull(direction)

		// and nothing is left when the cutoff eats the whole route
		assertNull(PrecalculatedRouteDirection.build(segments, 5000f, 20f))
	}

	private fun segments(vararg distances: Float): List<RouteSegmentResult> {
		val region = RouteRegion()
		region.initRouteEncodingRule(0, "", "")
		region.initRouteEncodingRule(1, "highway", "primary")
		return distances.mapIndexed { i, distance ->
			val road = RouteDataObject(region)
			road.id = (i + 1).toLong() shl 6
			road.types = intArrayOf(1)
			road.pointsX = intArrayOf(x(i), x(i + 1))
			road.pointsY = intArrayOf(y(i), y(i + 1))
			val segment = RouteSegmentResult(road, 0, 1)
			segment.setDistance(distance)
			segment
		}
	}
}
