package net.osmand.test.common

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.SimulationMode
import net.osmand.plus.views.mapwidgets.widgets.SunriseSunsetWidget
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SunriseSunsetTest : AndroidTest() {
	@Test
	fun testTimeFormat() {
		val time1h1m = 61L * 60 * 1000
		val time1h = 60L * 60 * 1000
		val time59m = 59L * 60 * 1000
		val text1h1m = SunriseSunsetWidget.formatTimeLeft(app, time1h1m)
		val text1h = SunriseSunsetWidget.formatTimeLeft(app, time1h)
		val text59m = SunriseSunsetWidget.formatTimeLeft(app, time59m)
		assert("01:01 h" == text1h1m)
		assert("01:00 h" == text1h)
		assert("00:59 h" == text59m)
	}
}