package net.osmand.plus.views.layers

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

@RunWith(AndroidJUnit4::class)
class RulerGlobeDistanceTest {

	private val globeDistance = RulerGlobeDistance(MAX_DISTANCE, MAX_VISIBLE_DISTANCE)

	@Test
	fun closeGuessConvergesToProjectedRadius() {
		val distance = globeDistance.find(PIXEL_RADIUS, 520_000.0, ::orthographicRadius)

		assertEquals(PIXEL_RADIUS, orthographicRadius(distance), PIXEL_RADIUS * 0.02)
	}

	@Test
	fun guessFarBelowOnePixelIsEnlarged() {
		// Globe at zoom 1 with the ruler centre at the bottom of the screen: the Web Mercator
		// density gave 1866 m, which the globe draws with half a pixel
		val distance = globeDistance.find(PIXEL_RADIUS, 1866.0, ::orthographicRadius)

		assertTrue(distance.isFinite())
		assertEquals(PIXEL_RADIUS, orthographicRadius(distance), PIXEL_RADIUS * 0.02)
	}

	@Test
	fun distanceBeyondVisibleHalfIsRejected() {
		val distance = globeDistance.find(PIXEL_RADIUS, 1000.0) { Double.NaN }

		assertTrue(distance.isNaN())
	}

	@Test
	fun radiusLargerThanGlobeIsRejected() {
		val distance = globeDistance.find(GLOBE_RADIUS_PX * 2, 1_000_000.0, ::orthographicRadius)

		assertTrue(distance.isNaN())
	}

	// Screen radius of a circle on a globe seen from far away, rounded like integer screen points
	private fun orthographicRadius(distance: Double): Double {
		if (distance > MAX_VISIBLE_DISTANCE) {
			return Double.NaN
		}
		val radius = GLOBE_RADIUS_PX * sin(distance / EARTH_RADIUS)
		return floor(radius * 2) / 2
	}

	private companion object {
		const val EARTH_RADIUS = 6_371_000.0
		const val MAX_DISTANCE = PI * EARTH_RADIUS
		const val MAX_VISIBLE_DISTANCE = MAX_DISTANCE / 2
		const val GLOBE_RADIUS_PX = 1500.0
		const val PIXEL_RADIUS = 315.0
	}
}