package net.osmand.test.ui.map

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import net.osmand.Location
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.controls.MapHudLayout
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression for #26217: in landscape the left button column is too short to lift the
 * speedometer above the compass, so it must move sideways instead of covering buttons.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapHudLayoutLandscapeTest : AndroidTest() {

	@get:Rule
	val activityRule = ActivityTestRule(MapActivity::class.java, true, false)

	private var showSpeedometerBefore = false
	private var screenOrientationBefore = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

	@Before
	override fun setup() {
		super.setup()
		grantLocationPermission()
		showSpeedometerBefore = settings.SHOW_SPEEDOMETER.getModeValue(settings.applicationMode)
		screenOrientationBefore = settings.MAP_SCREEN_ORIENTATION.get()
		settings.SHOW_SPEEDOMETER.setModeValue(settings.applicationMode, true)
		settings.MAP_SCREEN_ORIENTATION.set(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
	}

	@After
	fun restoreSettings() {
		settings.SHOW_SPEEDOMETER.setModeValue(settings.applicationMode, showSpeedometerBefore)
		settings.MAP_SCREEN_ORIENTATION.set(screenOrientationBefore)
	}

	@Test
	fun speedometerDoesNotCoverButtonsInLandscape() {
		val activity = activityRule.launchActivity(null)
		skipAppStartDialogs(app)
		Espresso.onIdle()

		val hud = activity.findViewById<MapHudLayout>(R.id.map_hud_layout)
		val speedometer = activity.findViewById<View>(R.id.speedometer_widget)
		val compass = activity.findViewById<View>(R.id.map_compass_button)
		// The widget only shows up once the provider knows a location with speed;
		// setLocationFromService() bypasses that outside Android Auto, so use the simulation path.
		waitUntil({ "speedometer is shown (speedometer=${describe(speedometer)}, compass=${describe(compass)})" }) {
			activity.runOnUiThread {
				val location = Location("test", 37.4220, -122.0840)
				location.speed = 15f
				location.time = System.currentTimeMillis()
				app.locationProvider.setLocationFromSimulation(location)
				app.osmandMap.mapView.refreshMap()
			}
			isShown(speedometer) && isShown(compass)
		}

		activity.runOnUiThread { hud.updateButtons() }
		Espresso.onIdle()
		waitUntil({ "layout settled after updateButtons" }) { !hud.isLayoutRequested }

		val hudRect = screenRect(hud)
		val speedometerRect = screenRect(speedometer)
		val compassRect = screenRect(compass)

		for (button in visibleMapButtons(hud)) {
			val buttonRect = screenRect(button)
			assertFalse(
				"speedometer $speedometerRect covers ${AndroidUtils.getViewName(button)} $buttonRect",
				Rect.intersects(speedometerRect, buttonRect)
			)
		}
		assertTrue(
			"speedometer $speedometerRect should be right of the compass $compassRect",
			speedometerRect.left >= compassRect.right
		)
		assertTrue(
			"speedometer $speedometerRect should stay in the lower half of $hudRect",
			speedometerRect.bottom > hudRect.centerY()
		)
	}

	private fun grantLocationPermission() {
		val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
		for (permission in listOf("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION")) {
			automation.executeShellCommand("pm grant ${app.packageName} $permission").close()
		}
	}

	private fun isShown(view: View): Boolean {
		return view.visibility == View.VISIBLE && view.width > 0 && view.height > 0
	}

	private fun describe(view: View): String {
		return "visibility=${view.visibility} size=${view.width}x${view.height}"
	}

	private fun screenRect(view: View): Rect {
		val xy = IntArray(2)
		view.getLocationOnScreen(xy)
		return Rect(xy[0], xy[1], xy[0] + view.width, xy[1] + view.height)
	}

	private fun visibleMapButtons(root: ViewGroup): List<MapButton> {
		val result = ArrayList<MapButton>()
		for (i in 0 until root.childCount) {
			val child = root.getChildAt(i)
			if (child is MapButton && isShown(child)) {
				result.add(child)
			} else if (child is ViewGroup) {
				result.addAll(visibleMapButtons(child))
			}
		}
		return result
	}

	private fun waitUntil(what: () -> String, timeoutMillis: Long = 15_000, condition: () -> Boolean) {
		val end = System.currentTimeMillis() + timeoutMillis
		while (System.currentTimeMillis() < end) {
			if (condition()) {
				return
			}
			Thread.sleep(250)
		}
		throw AssertionError("Timed out waiting until ${what()}")
	}
}
