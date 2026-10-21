package net.osmand.test.junit

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.myplaces.MyPlacesActivity
import net.osmand.plus.settings.enums.TracksSortMode
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.test.common.AndroidTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test for the "Fix location icon in Tracks" item of issue #26134.
 *
 * My Places runs in its own activity. MapActivity.onPause pauses location and compass updates,
 * and nothing on the Tracks tab resumed them, so OsmAndLocationProvider.getHeading() stayed null
 * and every direction arrow in the track lists was drawn with angle 0 - pointing straight up.
 *
 * [headingIsAvailableOnTracksTab] fails on the code before the fix.
 * [leavingMyPlacesPausesCompass] guards the other half: the updates resumed for My Places must
 * be paused again when it goes to the background, as MapActivity does for itself.
 * Both tests sort the Tracks tab by "Nearest to current location": only that sort shows the arrows
 * and turns the updates on.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MyPlacesTracksCompassTest : AndroidTest() {

	companion object {
		private const val HEADING_TIMEOUT_MS = 5000L
		private const val ROOT_FOLDER_ID = ""
	}

	private var savedRootSortMode: TracksSortMode? = null

	@Before
	fun saveRootSortMode() {
		savedRootSortMode = app.trackSortModesHelper.getSortMode(ROOT_FOLDER_ID, TracksSortScope.TRACKS)
	}

	@After
	fun restoreRootSortMode() {
		app.trackSortModesHelper.setSortMode(ROOT_FOLDER_ID, TracksSortScope.TRACKS, savedRootSortMode)
	}

	@Test
	fun headingIsAvailableOnTracksTab() {
		assumeTrue("Device has no compass sensor", hasCompass())
		setRootSortMode(TracksSortMode.NEAREST)
		pauseUpdatesLikeMapActivity()

		ActivityScenario.launch<MyPlacesActivity>(tracksTabIntent()).use {
			assertNotNull("No compass heading while the My Places Tracks tab is shown,"
					+ " so every direction arrow points up", waitForHeading())
		}
	}

	@Test
	fun leavingMyPlacesPausesCompass() {
		assumeTrue("Device has no compass sensor", hasCompass())
		setRootSortMode(TracksSortMode.NEAREST)
		pauseUpdatesLikeMapActivity()

		ActivityScenario.launch<MyPlacesActivity>(tracksTabIntent()).use { scenario ->
			waitForHeading()
			scenario.moveToState(Lifecycle.State.CREATED)
			assertNull("Compass is still running after My Places went to the background",
				app.locationProvider.heading)
		}
	}

	private fun setRootSortMode(sortMode: TracksSortMode) {
		app.trackSortModesHelper.setSortMode(ROOT_FOLDER_ID, TracksSortScope.TRACKS, sortMode)
	}

	private fun hasCompass(): Boolean {
		val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
		return app.locationProvider.hasOrientationSensor(sensorManager)
				|| (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
				&& sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
	}

	private fun pauseUpdatesLikeMapActivity() {
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			app.locationProvider.pauseAllUpdates()
		}
		assertNull(app.locationProvider.heading)
	}

	private fun tracksTabIntent(): Intent {
		val params = Bundle()
		params.putInt(MyPlacesActivity.TAB_ID, MyPlacesActivity.GPX_TAB)
		return Intent(app, MyPlacesActivity::class.java)
			.putExtra(MapActivity.INTENT_PARAMS, params)
	}

	private fun waitForHeading(): Float? {
		val deadline = SystemClock.uptimeMillis() + HEADING_TIMEOUT_MS
		var heading = app.locationProvider.heading
		while (heading == null && SystemClock.uptimeMillis() < deadline) {
			SystemClock.sleep(100)
			heading = app.locationProvider.heading
		}
		return heading
	}
}
