package net.osmand.test.ui.map

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin
import net.osmand.plus.quickaction.actions.DayNightModeAction
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.settings.enums.WidgetSize
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.controls.MapHudLayout
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState
import net.osmand.shared.grid.ButtonPositionSize
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
	private var speedometerSizeBefore = WidgetSize.MEDIUM
	private var screenOrientationBefore = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
	private var gridLogsBefore = false
	private var monitoringEnabledBefore = false
	private var developmentEnabledBefore = false
	private var leftPanelOrderBefore: String? = null
	private var rightPanelOrderBefore: String? = null
	private var panelsChanged = false
	private val customButtons = ArrayList<QuickActionButtonState>()
	private val enabledWidgets = ArrayList<String>()

	@Before
	override fun setup() {
		super.setup()
		grantLocationPermission()
		showSpeedometerBefore = settings.SHOW_SPEEDOMETER.getModeValue(settings.applicationMode)
		speedometerSizeBefore = settings.SPEEDOMETER_SIZE.getModeValue(settings.applicationMode)
		screenOrientationBefore = settings.MAP_SCREEN_ORIENTATION.get()
		gridLogsBefore = OsmandSettings.DEV_GRID_LAYOUT_SHOW_LOGS
		monitoringEnabledBefore = PluginsHelper.getPlugin(OsmandMonitoringPlugin::class.java)?.isEnabled == true
		developmentEnabledBefore = PluginsHelper.getPlugin(OsmandDevelopmentPlugin::class.java)?.isEnabled == true
		settings.SHOW_SPEEDOMETER.setModeValue(settings.applicationMode, true)
		settings.MAP_SCREEN_ORIENTATION.set(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
		OsmandSettings.DEV_GRID_LAYOUT_SHOW_LOGS = true
		ButtonPositionSize.DEBUG_PRINT_MOVE = true
	}

	@After
	fun restoreSettings() {
		val activity = activityRule.activity
		val plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin::class.java)
		if (plugin != null && plugin.isRecordingTrack) {
			plugin.stopRecording(true)
		}
		if (activity != null && enabledWidgets.isNotEmpty()) {
			val registry = app.osmandMap.mapLayers.mapWidgetRegistry
			val layoutMode = ScreenLayoutMode.getDefault(activity)
			InstrumentationRegistry.getInstrumentation().runOnMainSync {
				for (widgetId in enabledWidgets) {
					val info = registry.getWidgetInfoById(widgetId)
					if (info != null) {
						registry.enableDisableWidgetForMode(settings.applicationMode, info, null, layoutMode, false)
					}
				}
			}
		}
		if (plugin != null && !monitoringEnabledBefore) {
			PluginsHelper.enablePluginIfNeeded(null, app, plugin, false)
		}
		val developmentPlugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin::class.java)
		if (developmentPlugin != null && !developmentEnabledBefore) {
			PluginsHelper.enablePluginIfNeeded(null, app, developmentPlugin, false)
		}
		if (panelsChanged) {
			val layoutMode = landscapeLayoutMode()
			WidgetsPanel.LEFT.getOrderPreference(settings, layoutMode).setModeValue(settings.applicationMode, leftPanelOrderBefore)
			WidgetsPanel.RIGHT.getOrderPreference(settings, layoutMode).setModeValue(settings.applicationMode, rightPanelOrderBefore)
		}
		val buttonsHelper = app.mapButtonsHelper
		for (state in customButtons) {
			buttonsHelper.removeQuickActionButtonState(state)
		}
		OsmandSettings.DEV_GRID_LAYOUT_SHOW_LOGS = gridLogsBefore
		ButtonPositionSize.DEBUG_PRINT_MOVE = false
		settings.SHOW_SPEEDOMETER.setModeValue(settings.applicationMode, showSpeedometerBefore)
		settings.SPEEDOMETER_SIZE.setModeValue(settings.applicationMode, speedometerSizeBefore)
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

		for (button in visibleMapButtons(hud)) {
			val buttonRect = screenRect(button)
			assertFalse(
				"speedometer $speedometerRect covers ${AndroidUtils.getViewName(button)} $buttonRect",
				Rect.intersects(speedometerRect, buttonRect)
			)
		}
		assertTrue(
			"speedometer $speedometerRect should stay in the lower half of $hudRect",
			speedometerRect.bottom > hudRect.centerY()
		)
	}

	@Test
	fun speedometerDoesNotCoverTopButtonsWithCustomButtonAboveMenu() {
		settings.SPEEDOMETER_SIZE.setModeValue(settings.applicationMode, WidgetSize.MEDIUM)
		addCustomButton(0, MENU_ROW_HEIGHT)
		preparePanels(LEFT_PANEL_WIDGETS, null)

		val activity = launchWithSpeedometer()
		showWidgets(activity, LEFT_PANEL_WIDGETS)
		val hud = activity.findViewById<MapHudLayout>(R.id.map_hud_layout)
		val speedometer = activity.findViewById<View>(R.id.speedometer_widget)

		val samples = recordSpeedometer(activity, hud, speedometer, SCREENSHOT_SAMPLING_MILLIS)

		assertNoOverlaps(samples)
	}

	@Test
	fun speedometerKeepsOnePositionWhileTripRecordingWidgetsUpdate() {
		settings.SPEEDOMETER_SIZE.setModeValue(settings.applicationMode, WidgetSize.MEDIUM)
		addCustomButton(0, MENU_ROW_HEIGHT)
		addCustomButton(2 * BOTTOM_ROW_STEP, 0)
		addCustomButton(3 * BOTTOM_ROW_STEP, 0)
		preparePanels(LEFT_PANEL_WIDGETS, TRIP_RECORDING_WIDGETS)

		val activity = launchWithSpeedometer()
		showWidgets(activity, LEFT_PANEL_WIDGETS + TRIP_RECORDING_WIDGETS)
		startTripRecording(activity)
		val hud = activity.findViewById<MapHudLayout>(R.id.map_hud_layout)
		val speedometer = activity.findViewById<View>(R.id.speedometer_widget)

		val samples = recordSpeedometer(activity, hud, speedometer, VIDEO_SAMPLING_MILLIS)

		val jumps = samples.filter { it.timeMillis > SETTLE_MILLIS }
		assertTrue(
			"speedometer moved ${jumps.size} times after the layout settled:\n${samples.joinToString("\n")}",
			jumps.isEmpty()
		)
		assertNoOverlaps(samples)
	}

	private fun launchWithSpeedometer(): MapActivity {
		val activity = activityRule.launchActivity(null)
		skipAppStartDialogs(app)
		Espresso.onIdle()

		val speedometer = activity.findViewById<View>(R.id.speedometer_widget)
		waitUntil({ "speedometer is shown (speedometer=${describe(speedometer)})" }) {
			activity.runOnUiThread { feedLocation(0) }
			isShown(speedometer)
		}
		return activity
	}

	private fun addCustomButton(marginX: Int, marginY: Int) {
		val buttonsHelper = app.mapButtonsHelper
		val state = buttonsHelper.createNewButtonState()
		state.setEnabled(true)
		buttonsHelper.addQuickAction(state, DayNightModeAction())
		buttonsHelper.addQuickActionButtonState(state)
		customButtons.add(state)

		val position = ButtonPositionSize(state.id, BUTTON_SIZE, true, false)
			.setMargin(marginX, marginY)
			.setMoveAny()
		@Suppress("UNCHECKED_CAST")
		val landscapePosition = settings.getPreference(state.id + "_position_landscape") as CommonPreference<Long>
		landscapePosition.set(position.toLongValue())
		LOG.info("Custom button ${state.id} landscape position $position")
	}

	private fun preparePanels(leftWidgets: List<WidgetType>, rightWidgets: List<WidgetType>?) {
		val monitoringPlugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin::class.java)
		val developmentPlugin = PluginsHelper.getPlugin(OsmandDevelopmentPlugin::class.java)
		assertTrue("trip recording plugin is missing", monitoringPlugin != null)
		assertTrue("development plugin is missing", developmentPlugin != null)
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			PluginsHelper.enablePluginIfNeeded(null, app, monitoringPlugin, true)
			PluginsHelper.enablePluginIfNeeded(null, app, developmentPlugin, true)
		}

		val appMode = settings.applicationMode
		val layoutMode = landscapeLayoutMode()
		leftPanelOrderBefore = WidgetsPanel.LEFT.getOrderPreference(settings, layoutMode).getModeValue(appMode)
		rightPanelOrderBefore = WidgetsPanel.RIGHT.getOrderPreference(settings, layoutMode).getModeValue(appMode)
		panelsChanged = true
		WidgetsPanel.LEFT.setWidgetsOrder(appMode, listOf(leftWidgets.map { it.id }), settings, layoutMode)
		if (rightWidgets != null) {
			WidgetsPanel.RIGHT.setWidgetsOrder(appMode, listOf(rightWidgets.map { it.id }), settings, layoutMode)
		}
		LOG.info("Panels for $layoutMode: left=${leftWidgets.map { it.id }} right=${rightWidgets?.map { it.id }}")
	}

	private fun showWidgets(activity: MapActivity, widgets: List<WidgetType>) {
		val registry = app.osmandMap.mapLayers.mapWidgetRegistry
		val layoutMode = ScreenLayoutMode.getDefault(activity)
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			for (type in widgets) {
				val info = registry.getWidgetInfoById(type.id)
				if (info != null) {
					registry.enableDisableWidgetForMode(settings.applicationMode, info, true, layoutMode, false)
					enabledWidgets.add(type.id)
				} else {
					LOG.warn("Widget ${type.id} is not registered")
				}
			}
			app.osmandMap.mapLayers.mapInfoLayer?.recreateControls()
		}
		Espresso.onIdle()
		LOG.info("Shown widgets $enabledWidgets")
	}

	private fun landscapeLayoutMode(): ScreenLayoutMode? {
		return if (settings.USE_SEPARATE_LAYOUTS.get()) ScreenLayoutMode.LANDSCAPE else null
	}

	private fun startTripRecording(activity: MapActivity) {
		val plugin = PluginsHelper.getPlugin(OsmandMonitoringPlugin::class.java)
		assertTrue("trip recording plugin is missing", plugin != null)
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			plugin!!.startRecording(activity)
		}
		Espresso.onIdle()
		LOG.info("Trip recording started: ${plugin!!.isRecordingTrack}")
	}

	private fun recordSpeedometer(activity: MapActivity, hud: MapHudLayout, speedometer: View,
	                              durationMillis: Long): List<Sample> {
		val samples = ArrayList<Sample>()
		val start = System.currentTimeMillis()
		var lastRect: Rect? = null
		val listener = ViewTreeObserver.OnGlobalLayoutListener {
			if (isShown(speedometer)) {
				val rect = screenRect(speedometer)
				if (rect != lastRect) {
					lastRect = rect
					val sample = Sample(System.currentTimeMillis() - start, rect, overlappedButtons(hud, rect),
						panelSize(activity, R.id.map_left_widgets_panel), panelSize(activity, R.id.map_right_widgets_panel))
					samples.add(sample)
					LOG.info("Speedometer moved: $sample")
				}
			}
		}
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			hud.viewTreeObserver.addOnGlobalLayoutListener(listener)
			listener.onGlobalLayout()
		}
		var step = 1
		while (System.currentTimeMillis() - start < durationMillis) {
			val index = step++
			activity.runOnUiThread { feedLocation(index) }
			Thread.sleep(LOCATION_INTERVAL_MILLIS)
		}
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			hud.viewTreeObserver.removeOnGlobalLayoutListener(listener)
		}
		LOG.info("Speedometer samples (${samples.size}):\n${samples.joinToString("\n")}")
		return samples
	}

	private fun feedLocation(step: Int) {
		val location = Location("test", START_LAT + step * LAT_STEP, START_LON)
		location.speed = SPEED_MPS
		location.time = System.currentTimeMillis()
		app.locationProvider.setLocationFromSimulation(location)
		app.osmandMap.mapView.refreshMap()
	}

	private fun assertNoOverlaps(samples: List<Sample>) {
		assertTrue("speedometer was never shown", samples.isNotEmpty())
		val overlapping = samples.filter { it.overlapped.isNotEmpty() }
		assertTrue(
			"speedometer covered buttons in ${overlapping.size} of ${samples.size} positions:\n${overlapping.joinToString("\n")}",
			overlapping.isEmpty()
		)
	}

	private fun overlappedButtons(hud: MapHudLayout, speedometerRect: Rect): List<String> {
		val result = ArrayList<String>()
		for (button in visibleMapButtons(hud)) {
			val buttonRect = screenRect(button)
			if (Rect.intersects(speedometerRect, buttonRect)) {
				result.add("${AndroidUtils.getViewName(button)} $buttonRect")
			}
		}
		return result
	}

	private fun panelSize(activity: MapActivity, id: Int): String {
		val panel = activity.findViewById<View>(id)
		return if (panel == null) "none" else describe(panel)
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

	private data class Sample(
		val timeMillis: Long,
		val rect: Rect,
		val overlapped: List<String>,
		val leftPanel: String,
		val rightPanel: String
	) {
		override fun toString(): String {
			return "t=${timeMillis}ms speedometer=$rect overlaps=$overlapped leftPanel=$leftPanel rightPanel=$rightPanel"
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(MapHudLayoutLandscapeTest::class.java)

		private const val BUTTON_SIZE = 7
		private const val MENU_ROW_HEIGHT = 8
		private const val BOTTOM_ROW_STEP = 8
		private const val START_LAT = 50.4501
		private const val START_LON = 30.5234
		private const val LAT_STEP = 0.00001
		private const val SPEED_MPS = 0.1f
		private const val LOCATION_INTERVAL_MILLIS = 1000L
		private const val SCREENSHOT_SAMPLING_MILLIS = 5_000L
		private const val VIDEO_SAMPLING_MILLIS = 20_000L
		private const val SETTLE_MILLIS = 3_000L

		private val LEFT_PANEL_WIDGETS = listOf(WidgetType.DEV_ZOOM_LEVEL)

		private val TRIP_RECORDING_WIDGETS = listOf(
			WidgetType.TRIP_RECORDING_TIME,
			WidgetType.TRIP_RECORDING_DISTANCE,
			WidgetType.TRIP_RECORDING_UPHILL,
			WidgetType.TRIP_RECORDING_DOWNHILL,
			WidgetType.TRIP_RECORDING_AVG_SPEED
		)
	}
}
