package net.osmand.test.ui.track

import android.graphics.PointF
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.github.mikephil.charting.charts.Chart
import net.osmand.data.LatLon
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
import net.osmand.test.common.OsmAndDialogInteractions.clickViewWithId
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
import kotlin.math.sin

/**
 * A point picked on the chart of the track menu's Track tab has to be shown in the part of the map the menu
 * does not cover (TP-11 in https://github.com/osmandapp/OsmAnd/issues/25590).
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class TrackMenuChartPointTest : AndroidTest() {

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
	 * The track is tapped while it is fully on the screen, so the map is not fitted, and the Track tab expands
	 * the menu over the lower part of the track. Opening the tab must not move the map, and a point picked on
	 * the chart has to end up in the visible part of the map.
	 */
	@Test
	fun testChartPointShownAboveMenu() {
		skipAppStartDialogs(app)
		assumeTrue("The menu covers the lower part of the map only in portrait", isPortrait())
		val rect = gpxFile.getRect()
		scenarioRule.scenario.onActivity { activity ->
			activity.mapView.fitRectToMap(rect.left, rect.right, rect.top, rect.bottom, 0, 0, 0)
		}
		awaitSettled()

		val segment = gpxFile.tracks[0].segments[0]
		val (northEnd, southEnd) = findNorthAndSouthEnds(segment)
		// as GPXLayer opens the track menu when the track is tapped, here at its southern end
		val tappedPoint = SelectedGpxPoint(selectedGpxFile, southEnd)
		scenarioRule.scenario.onActivity { activity ->
			TrackMenuFragment.showInstance(activity, selectedGpxFile, tappedPoint)
		}
		awaitSettled()
		val overviewState = getMapState()

		clickViewWithId(R.id.action_track)
		awaitSettled()
		val trackTabState = getMapState()
		assertEquals("The Track tab opens the menu at half screen", MenuState.HALF_SCREEN, trackTabState.menuState)
		assertEquals("Opening the Track tab must not move the map",
			overviewState.target.latitude, trackTabState.target.latitude, 1e-6)
		assertEquals("Opening the Track tab must not move the map",
			overviewState.target.longitude, trackTabState.target.longitude, 1e-6)

		var chartShown = false
		onTrackMenu { fragment -> chartShown = findChart(fragment.requireView()) != null }
		assumeTrue("The Track tab list creates the chart only when it fits into the half screen menu", chartShown)
		// the track goes from south to north, so the end of the chart is its northern end
		assertChartPointShown(true, northEnd, trackTabState.zoom)
		assertChartPointShown(false, southEnd, trackTabState.zoom)
	}

	private fun assertChartPointShown(chartEnd: Boolean, point: WptPt, zoom: Float) {
		onTrackMenu { fragment -> tapChart(fragment, chartEnd) }
		awaitSettled()

		val mapState = getMapState()
		val pixel = getPixel(point)
		val message = "Chart ${if (chartEnd) "end" else "start"}, track point ${point.lat}, ${point.lon} at $pixel: $mapState"
		val tolerance = mapState.height * BOUNDS_TOLERANCE
		assertTrue(message, pixel.y >= mapState.visibleMap.top - tolerance)
		assertTrue(message, pixel.y <= mapState.visibleMap.bottom + tolerance)
		assertTrue(message, pixel.x >= mapState.visibleMap.left - tolerance)
		assertTrue(message, pixel.x <= mapState.visibleMap.right + tolerance)
		assertEquals("Picking a chart point must not change the zoom", zoom, mapState.zoom, 0.01f)
	}

	private fun isPortrait(): Boolean {
		var portrait = false
		scenarioRule.scenario.onActivity { activity -> portrait = AndroidUiHelper.isOrientationPortrait(activity) }
		return portrait
	}

	private fun getMapState(): MapState {
		var state: MapState? = null
		scenarioRule.scenario.onActivity { activity ->
			val mapView = activity.mapView
			val tb = mapView.rotatedTileBox
			val fragment = findTrackMenu(activity)
			val menuView = fragment?.view?.findViewById<View>(R.id.main_view)
			assertNotNull("Track menu is not shown", menuView)
			val mapTop = AndroidUtils.getLocationOnScreen(mapView.view!!)[1]
			// in portrait the menu covers the bottom of the map
			val visibleMap = Rect(0, 0, tb.pixWidth, AndroidUtils.getViewBoundOnScreen(menuView!!).top - mapTop)
			state = MapState(tb.pixHeight, visibleMap, fragment!!.currentMenuState,
				(tb.zoom + tb.zoomFloatPart).toFloat(), LatLon(tb.latitude, tb.longitude))
		}
		return state!!
	}

	private fun getPixel(point: WptPt): PointF {
		val pixel = PointF()
		scenarioRule.scenario.onActivity { activity ->
			val tb = activity.mapView.rotatedTileBox
			pixel.set(tb.getPixXFromLatLon(point.lat, point.lon), tb.getPixYFromLatLon(point.lat, point.lon))
		}
		return pixel
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
		val height: Int,
		val visibleMap: Rect,
		val menuState: Int,
		val zoom: Float,
		val target: LatLon
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
		private const val GPX_NAME = "track_menu_chart_point_test.gpx"

		// north-south zigzag of about 1.0 x 0.4 km with altitude: taller than the map area left by the menu
		private const val TRACK_CENTER_LAT = 50.19
		private const val TRACK_CENTER_LON = 18.44
		private const val TRACK_HALF_HEIGHT_DEG = 0.0045
		private const val TRACK_HALF_WIDTH_DEG = 0.003
		private const val TRACK_POINTS = 21
		private const val BOUNDS_TOLERANCE = 0.01f
		private const val CHECK_INTERVAL_MS = 100L
		private const val STABLE_CHECKS_COUNT = 5

		private fun createTallTrack(): GpxFile {
			val segment = TrkSegment()
			for (i in 0 until TRACK_POINTS) {
				val lat = TRACK_CENTER_LAT - TRACK_HALF_HEIGHT_DEG + 2 * TRACK_HALF_HEIGHT_DEG * i / (TRACK_POINTS - 1)
				val lon = TRACK_CENTER_LON + if (i % 2 == 0) -TRACK_HALF_WIDTH_DEG else TRACK_HALF_WIDTH_DEG
				// the Track tab shows its chart only for a track with altitude or speed
				segment.points.add(WptPt(lat, lon).apply { ele = 200 + 20 * sin(i.toDouble()) })
			}
			val track = Track()
			track.segments.add(segment)
			val gpxFile = GpxFile(null as String?)
			gpxFile.tracks.add(track)
			return gpxFile
		}

		private fun findNorthAndSouthEnds(segment: TrkSegment): Pair<WptPt, WptPt> {
			return segment.points.maxBy { it.lat } to segment.points.minBy { it.lat }
		}

		/**
		 * Sends a tap at the first or the last value of the Track tab chart to the chart view, as a finger would.
		 * The chart may be below the part of the menu that is on the screen, so the events go to the view directly.
		 */
		private fun tapChart(fragment: TrackMenuFragment, chartEnd: Boolean) {
			val chart = findChart(fragment.requireView())
			assertNotNull("The Track tab shows no chart", chart)
			val handler = chart!!.viewPortHandler
			val x = if (chartEnd) handler.contentRight() - 1 else handler.contentLeft() + 1
			val y = handler.contentCenter.y
			val downTime = SystemClock.uptimeMillis()
			val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
			val up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0)
			chart.dispatchTouchEvent(down)
			chart.dispatchTouchEvent(up)
			down.recycle()
			up.recycle()
		}

		private fun findChart(view: View): Chart<*>? {
			if (view is Chart<*> && view.id == R.id.chart && !view.isEmpty && view.width > 0) {
				return view
			}
			if (view is ViewGroup) {
				for (i in 0 until view.childCount) {
					findChart(view.getChildAt(i))?.let { return it }
				}
			}
			return null
		}
	}
}
