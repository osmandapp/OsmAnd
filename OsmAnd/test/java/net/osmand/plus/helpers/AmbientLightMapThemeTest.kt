package net.osmand.plus.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLightMapThemeTest {
	private val scheduled = mutableListOf<Runnable>()
	private val changes = mutableListOf<Boolean>()
	private val theme = AmbientLightMapTheme({ scheduled.add(it) }, { scheduled.remove(it) }, { changes.add(it) })

	@Test
	fun initialReadingIsAppliedImmediatelyAndTwilightKeepsItsTheme() {
		theme.onLight(Float.NaN)
		theme.onLight(98f)
		theme.onLight(101f)
		theme.onLight(99f)
		assertEquals(listOf(true), changes)
		assertTrue(scheduled.isEmpty())
	}

	@Test
	fun passingLampDoesNotLightUpTheMap() {
		theme.onLight(10f)
		theme.onLight(1000f)
		val flash = scheduled.single()
		theme.onLight(10f)
		assertTrue(scheduled.isEmpty())
		flash.run()
		assertEquals(listOf(true), changes)
	}

	@Test
	fun sustainedChangeNeedsNoMoreSensorEvents() {
		theme.onLight(800f)
		theme.onLight(15f)
		scheduled.removeAt(0).run()
		assertEquals(listOf(false, true), changes)

		theme.onLight(700f)
		val daylight = scheduled.single()
		theme.onLight(750f)
		assertSame(daylight, scheduled.single())
		scheduled.removeAt(0).run()
		assertEquals(listOf(false, true, false), changes)
	}

	@Test
	fun stoppedSessionCannotApplyItsOldCallbackAfterRestart() {
		theme.onLight(500f)
		theme.onLight(15f)
		val previous = scheduled.single()
		theme.reset()
		assertTrue(scheduled.isEmpty())

		theme.onLight(10f)
		theme.onLight(500f)
		previous.run()
		assertEquals(listOf(false, true), changes)
		scheduled.single().run()
		assertEquals(listOf(false, true, false), changes)
	}
}
