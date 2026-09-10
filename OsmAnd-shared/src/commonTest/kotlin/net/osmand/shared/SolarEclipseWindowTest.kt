package net.osmand.shared

import io.github.cosinekitty.astronomy.GlobalSolarEclipseInfo
import io.github.cosinekitty.astronomy.MINUTES_PER_DAY
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.globalSolarEclipseWindow
import io.github.cosinekitty.astronomy.localSolarEclipseWindow
import io.github.cosinekitty.astronomy.searchGlobalSolarEclipse
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the difference between the worldwide bounds of a solar eclipse and the
 * circumstances seen by one observer, reported in issue 25830: the eclipse card
 * used to present the worldwide times as the times for the map center.
 *
 * Reference values are the published circumstances of the total solar eclipse of
 * 2026-08-12 and the local times quoted in that issue, all expressed in UT.
 */
class SolarEclipseWindowTest {

    private val toleranceMinutes = 6.0

    /** Soria, Spain - the observer location reported in the issue. */
    private val soria = Observer(41.7636, -2.4650, 0.0)

    /** Sydney, Australia - the same eclipse is not above the horizon anywhere near there. */
    private val sydney = Observer(-33.8688, 151.2093, 0.0)

    private fun eclipseOf20260812(): GlobalSolarEclipseInfo =
        searchGlobalSolarEclipse(Time(2026, 8, 1, 0, 0, 0.0))

    private fun assertTimeNear(expected: Time, actual: Time, message: String) {
        val offsetMinutes = abs(actual.ut - expected.ut) * MINUTES_PER_DAY
        assertTrue(offsetMinutes <= toleranceMinutes, "$message: off by $offsetMinutes minutes")
    }

    @Test
    fun testWorldwideWindowCoversTheWholeEarth() {
        val event = eclipseOf20260812()
        val window = globalSolarEclipseWindow(event)

        assertTimeNear(Time(2026, 8, 12, 15, 34, 0.0), window.start, "worldwide start")
        assertTimeNear(Time(2026, 8, 12, 17, 46, 0.0), event.peak, "greatest eclipse")
        assertTimeNear(Time(2026, 8, 12, 19, 57, 0.0), window.end, "worldwide end")
    }

    @Test
    fun testLocalWindowUsesObserverContactTimes() {
        val event = eclipseOf20260812()
        val local = assertNotNull(localSolarEclipseWindow(event, soria), "eclipse visible from Soria")

        assertTimeNear(Time(2026, 8, 12, 17, 34, 0.0), local.partialBegin.time, "local start")
        assertTimeNear(Time(2026, 8, 12, 18, 29, 0.0), local.peak.time, "local maximum")
        assertTimeNear(Time(2026, 8, 12, 19, 22, 0.0), local.partialEnd.time, "local end")
    }

    @Test
    fun testLocalWindowDiffersFromWorldwideWindow() {
        val event = eclipseOf20260812()
        val window = globalSolarEclipseWindow(event)
        val local = assertNotNull(localSolarEclipseWindow(event, soria), "eclipse visible from Soria")

        // The observer enters the penumbra long after it first touches the Earth,
        // and leaves it before the penumbra leaves the Earth.
        assertTrue(
            (local.partialBegin.time.ut - window.start.ut) * MINUTES_PER_DAY > 60.0,
            "local start must be well after the worldwide start"
        )
        assertTrue(
            (window.end.ut - local.partialEnd.time.ut) * MINUTES_PER_DAY > 15.0,
            "local end must be before the worldwide end"
        )
        assertTrue(
            local.partialBegin.time.ut < local.peak.time.ut &&
                local.peak.time.ut < local.partialEnd.time.ut,
            "local contact times must be ordered"
        )
    }

    @Test
    fun testLocalWindowReportsSunBelowHorizon() {
        val event = eclipseOf20260812()
        val local = assertNotNull(localSolarEclipseWindow(event, soria), "eclipse visible from Soria")

        assertTrue(local.partialBegin.altitude > 0.0, "eclipse starts while the Sun is up")
        assertTrue(local.partialEnd.altitude < 0.0, "the Sun sets before the eclipse ends")
    }

    @Test
    fun testLocalWindowIsNullWhereTheEclipseNeverReaches() {
        val event = eclipseOf20260812()
        assertNull(localSolarEclipseWindow(event, sydney), "eclipse must not be visible from Sydney")
    }
}
