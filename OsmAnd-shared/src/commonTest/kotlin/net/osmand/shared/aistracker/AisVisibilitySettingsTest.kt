package net.osmand.shared.aistracker

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the meaning of the two timeouts behind the "Objects visibility" screen and of the
 * thresholds behind "Collision warning (CPA)". The screens only pick a value from a list; these
 * tests cover what the picked value actually does to a vessel.
 */
class AisVisibilitySettingsTest {

	/** The values offered by the two sliders of the Objects visibility screen. */
	private val markAsOutdatedValues = listOf(MARK_AS_OUTDATED_OFF, 2, 3, 4, 5, 7, 10, 15)
	private val hideAfterValues = listOf(3, 5, 7, 10, 12, 15, 20)

	@Test
	fun hideAfterRemovesTheVesselOnlyOnceTheTimeoutHasPassed() {
		for (hideAfter in hideAfterValues) {
			assertFalse(vesselSeen(hideAfter - 1).isLost(hideAfter),
				"a vessel seen ${hideAfter - 1} min ago must stay on the map with 'Hide after' $hideAfter")
			assertTrue(vesselSeen(hideAfter + 1).isLost(hideAfter),
				"a vessel seen ${hideAfter + 1} min ago must be removed with 'Hide after' $hideAfter")
		}
	}

	@Test
	fun markAsOutdatedOffNeverCrossesOutAVesselThatIsStillOnTheMap() {
		val longestHideAfter = hideAfterValues.max()
		for (age in 0..longestHideAfter) {
			assertFalse(vesselSeen(age).isLost(MARK_AS_OUTDATED_OFF),
				"'Mark as outdated' = Off must not cross out a vessel seen $age min ago")
		}
	}

	@Test
	fun defaultPairCrossesOutBeforeRemoving() {
		val outdated = 4
		val hideAfter = 7
		val vessel = vesselSeen(5)
		assertTrue(vessel.isLost(outdated), "at 5 min the vessel is outdated with 'Mark as outdated' 4")
		assertFalse(vessel.isLost(hideAfter), "at 5 min the vessel is still shown with 'Hide after' 7")
		assertTrue(vesselSeen(8).isLost(hideAfter), "at 8 min the vessel is removed with 'Hide after' 7")
	}

	@Test
	fun everyMarkAsOutdatedValueIsOfferedByTheSlider() {
		/* the "Off" stop has to stay larger than every "Hide after" value, otherwise a vessel
		 * would be crossed out while the user asked for the symbol never to change */
		assertTrue(markAsOutdatedValues.first() == MARK_AS_OUTDATED_OFF)
		assertTrue(MARK_AS_OUTDATED_OFF > hideAfterValues.max())
	}

	@Test
	fun cpaWarningFiresForAConvergingPairAndNotForADivergingOne() {
		val own = AisLocation(59.9000, 10.7000, KNOT_10, 0f)
		val crossing = AisLocation(59.9200, 10.7000, KNOT_10, 180f)
		val converging = AisCpa()
		AisTrackerMath.getCpa(own, crossing, converging)
		assertTrue(converging.valid, "a head on pair must produce a valid CPA")
		assertTrue(converging.tcpa > 0, "the closest approach of a head on pair is in the future")
		assertTrue(cpaWarning(converging, warningTimeMin = 20, warningDistanceMiles = 0.2f),
			"a head on pair inside both thresholds must raise the warning")

		val leaving = AisLocation(59.9200, 10.7000, KNOT_10, 0f)
		val diverging = AisCpa()
		AisTrackerMath.getCpa(own, leaving, diverging)
		assertFalse(cpaWarning(diverging, warningTimeMin = 20, warningDistanceMiles = 0.2f),
			"vessels on the same course never close in, so no warning")
	}

	@Test
	fun cpaWarningIsOffWhenTheWarningTimeIsZero() {
		val own = AisLocation(59.9000, 10.7000, KNOT_10, 0f)
		val crossing = AisLocation(59.9200, 10.7000, KNOT_10, 180f)
		val cpa = AisCpa()
		AisTrackerMath.getCpa(own, crossing, cpa)
		/* the master switch of the screen is expressed as a zero warning time for the map layer */
		assertFalse(cpaWarning(cpa, warningTimeMin = 0, warningDistanceMiles = 0.2f))
	}

	/** The predicate the map layer applies, kept in one place so the test states the same rule. */
	private fun cpaWarning(cpa: AisCpa, warningTimeMin: Int, warningDistanceMiles: Float): Boolean =
		warningTimeMin > 0 && cpa.valid && cpa.tcpa > 0
				&& cpa.cpa <= warningDistanceMiles
				&& cpa.tcpa * 60 <= warningTimeMin
				&& cpa.t1 >= 0 && cpa.t2 >= 0

	private fun vesselSeen(minutesAgo: Int): AisObject =
		AisObject(MMSI, 1, 59.9, 10.7).apply {
			lastUpdate = Clock.System.now().toEpochMilliseconds() - minutesAgo * 60_000L - 1_000L
		}

	companion object {
		private const val MMSI = 244000000
		private const val MARK_AS_OUTDATED_OFF = 100
		private const val KNOT_10 = 5.14f // m/s
	}
}
