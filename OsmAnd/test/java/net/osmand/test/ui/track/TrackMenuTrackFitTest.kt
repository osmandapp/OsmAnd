package net.osmand.test.ui.track

import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.ContextMenuFragment.MenuState
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.importfiles.ImportHelper
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController.SelectedGpxPoint
import net.osmand.plus.shared.SharedUtil
import net.osmand.plus.track.GpxSelectionParams
import net.osmand.plus.track.fragments.TrackMenuFragment
import net.osmand.plus.track.helpers.SelectedGpxFile
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Track
import net.osmand.shared.gpx.primitives.TrkSegment
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.BaseIdlingResource
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The track menu has to fit the track into the part of the map it does not cover
 * (https://github.com/osmandapp/OsmAnd/issues/25590).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TrackMenuTrackFitTest : AndroidTest() {

	@get:Rule
	val scenarioRule = ActivityScenarioRule(MapActivity::class.java)

	private lateinit var gpxFile: GpxFile
	private lateinit var selectedGpxFile: SelectedGpxFile

	@Before
	override fun setup() {
		super.setup()
		gpxFile = createTallTrack()
		val file = File(ImportHelper.getGpxDestinationDir(app, true), GPX_NAME)
		assertNull(SharedUtil.writeGpxFile(file, gpxFile))
		gpxFile.path = file.absolutePath
		selectedGpxFile = app.selectedGpxHelper.selectGpxFile(gpxFile, GpxSelectionParams.getDefaultSelectionParams())
	}

	@After
	override fun cleanUp() {
		app.selectedGpxHelper.selectGpxFile(gpxFile, GpxSelectionParams.newInstance().hideFromMap())
		File(gpxFile.path).delete()
		super.cleanUp()
	}

	/**
	 * When the menu changes its state, the track is fitted into the map part above the menu. That part is
	 * known only after the menu has moved, so the fitted track must not be shifted by the map center
	 * moving there afterwards.
	 */
	@Test
	fun testTrackFittedAboveMenuAfterMenuStateChange() {
		skipAppStartDialogs(app)
		assumeTrue("The menu has its half screen and header states only in portrait", isPortrait())
		scenarioRule.scenario.onActivity { activity ->
			TrackMenuFragment.showInstance(activity, selectedGpxFile, null as SelectedGpxPoint?)
		}
		awaitSettled()

		for (state in intArrayOf(MenuState.HALF_SCREEN, MenuState.HEADER_ONLY, MenuState.HALF_SCREEN)) {
			// a track that is not fully on the screen is fitted again when the menu changes its state
			zoomIntoTrack()
			onTrackMenu { fragment -> fragment.openMenuScreen(state, true) }
			awaitSettled()

			val mapState = getMapState()
			assertEquals("Menu state", state, mapState.menuState)
			val message = "After moving the menu to state $state: $mapState"
			val trackCenterY = (mapState.trackBounds.top + mapState.trackBounds.bottom) / 2f
			assertEquals(message, mapState.mapCenter.y / mapState.height, trackCenterY / mapState.height, CENTER_TOLERANCE)
			assertBoundsInsideVisibleMap(message, mapState.trackBounds, mapState)
		}
	}

	private fun assertBoundsInsideVisibleMap(message: String, bounds: Rect, mapState: MapState) {
		val visible = mapState.visibleMap
		val tolerance = mapState.height * BOUNDS_TOLERANCE
		assertTrue(message, bounds.top >= visible.top - tolerance)
		assertTrue(message, bounds.bottom <= visible.bottom + tolerance)
		assertTrue(message, bounds.left >= visible.left - tolerance)
		assertTrue(message, bounds.right <= visible.right + tolerance)
	}

	private fun isPortrait(): Boolean {
		var portrait = false
		scenarioRule.scenario.onActivity { activity -> portrait = AndroidUiHelper.isOrientationPortrait(activity) }
		return portrait
	}

	private fun zoomIntoTrack() {
		scenarioRule.scenario.onActivity { activity ->
			val mapView = activity.mapView
			mapView.setIntZoom(ZOOM_INTO_TRACK)
			mapView.setLatLon(TRACK_CENTER_LAT, TRACK_CENTER_LON)
			mapView.refreshMap()
		}
		awaitSettled()
	}

	private fun getMapState(): MapState {
		var state: MapState? = null
		scenarioRule.scenario.onActivity { activity ->
			val mapView = activity.mapView
			val tb = mapView.rotatedTileBox
			val width = tb.pixWidth
			val height = tb.pixHeight
			val ratio = app.mapViewTrackingUtilities.mapDisplayPositionManager.mapRatio

			val fragment = findTrackMenu(activity)
			val menuView = fragment?.view?.findViewById<View>(R.id.main_view)
			assertNotNull("Track menu is not shown", menuView)
			val mapTop = AndroidUtils.getLocationOnScreen(mapView.view!!)[1]
			// in portrait the menu covers the bottom of the map
			val visibleMap = Rect(0, 0, width, AndroidUtils.getViewBoundOnScreen(menuView!!).top - mapTop)

			val rect = gpxFile.getRect()
			val trackBounds = Rect()
			var first = true
			for ((lat, lon) in listOf(rect.top to rect.left, rect.top to rect.right,
					rect.bottom to rect.left, rect.bottom to rect.right)) {
				val x = tb.getPixXFromLatLon(lat, lon).toInt()
				val y = tb.getPixYFromLatLon(lat, lon).toInt()
				if (first) {
					trackBounds.set(x, y, x, y)
					first = false
				} else {
					trackBounds.union(x, y)
				}
			}
			state = MapState(width, height, visibleMap, fragment!!.currentMenuState,
				PointF(ratio.x * width, ratio.y * height), trackBounds)
		}
		return state!!
	}

	private fun onTrackMenu(action: (TrackMenuFragment) -> Unit) {
		scenarioRule.scenario.onActivity { activity ->
			val fragment = findTrackMenu(activity)
			assertNotNull("Track menu is not shown", fragment)
			action(fragment!!)
		}
	}

	private fun findTrackMenu(activity: MapActivity): TrackMenuFragment? {
		return activity.supportFragmentManager.findFragmentByTag(TrackMenuFragment.TAG) as? TrackMenuFragment
	}

	private fun awaitSettled() {
		val idlingResource = MapSettledIdlingResource(app)
		registerIdlingResources(idlingResource)
		Espresso.onIdle()
		unregisterIdlingResources(idlingResource)
	}

	private data class MapState(
		val width: Int,
		val height: Int,
		val visibleMap: Rect,
		val menuState: Int,
		val mapCenter: PointF,
		val trackBounds: Rect
	)

	/**
	 * Idle once the menu, the map center ratio and the map position have stopped changing.
	 */
	private inner class MapSettledIdlingResource(app: OsmandApplication) : BaseIdlingResource(app) {

		private var stableChecks = 0
		private var lastState = ""

		init {
			scheduleCheck()
		}

		override fun isIdleNow(): Boolean = stableChecks >= STABLE_CHECKS_COUNT

		private fun scheduleCheck() {
			Handler(Looper.getMainLooper()).postDelayed({
				val mapView = app.osmandMap.mapView
				val activity = mapView.mapActivity
				val fragment = activity?.let { findTrackMenu(it) }
				val menuView = fragment?.view?.findViewById<View>(R.id.main_view)
				val menuTop = menuView?.let { AndroidUtils.getViewBoundOnScreen(it).top } ?: -1
				val tb = mapView.rotatedTileBox
				val animating = mapView.animatedDraggingThread.isAnimating
				val state = "$menuTop|${app.mapViewTrackingUtilities.mapDisplayPositionManager.mapRatio}" +
						"|${tb.latitude}|${tb.longitude}|${tb.zoom}|${tb.zoomFloatPart}"
				if (!animating && state == lastState) {
					stableChecks++
				} else {
					stableChecks = 0
					lastState = state
				}
				if (isIdleNow) {
					notifyIdleTransition()
				} else {
					scheduleCheck()
				}
			}, CHECK_INTERVAL_MS)
		}
	}

	companion object {
		private const val GPX_NAME = "track_menu_fit_test.gpx"

		// north-south zigzag of about 1.0 x 0.4 km: taller than the map area left by the menu
		private const val TRACK_CENTER_LAT = 50.19
		private const val TRACK_CENTER_LON = 18.44
		private const val TRACK_HALF_HEIGHT_DEG = 0.0045
		private const val TRACK_HALF_WIDTH_DEG = 0.003
		private const val TRACK_POINTS = 21
		private const val ZOOM_INTO_TRACK = 18
		private const val CENTER_TOLERANCE = 0.03f
		private const val BOUNDS_TOLERANCE = 0.01f
		private const val CHECK_INTERVAL_MS = 100L
		private const val STABLE_CHECKS_COUNT = 5

		private fun createTallTrack(): GpxFile {
			val segment = TrkSegment()
			for (i in 0 until TRACK_POINTS) {
				val lat = TRACK_CENTER_LAT - TRACK_HALF_HEIGHT_DEG + 2 * TRACK_HALF_HEIGHT_DEG * i / (TRACK_POINTS - 1)
				val lon = TRACK_CENTER_LON + if (i % 2 == 0) -TRACK_HALF_WIDTH_DEG else TRACK_HALF_WIDTH_DEG
				segment.points.add(WptPt(lat, lon))
			}
			val track = Track()
			track.segments.add(segment)
			val gpxFile = GpxFile(null as String?)
			gpxFile.tracks.add(track)
			return gpxFile
		}
	}
}
