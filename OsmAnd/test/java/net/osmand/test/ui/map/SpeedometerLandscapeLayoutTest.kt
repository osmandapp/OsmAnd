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
import net.osmand.PlatformUtil
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.quickaction.actions.DayNightModeAction
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.settings.enums.WidgetSize
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.controls.MapHudLayout
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.MapWidgetInfo
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState
import net.osmand.shared.grid.ButtonPositionSize
import net.osmand.test.common.AndroidTest
import net.osmand.test.common.OsmAndDialogInteractions.skipAppStartDialogs
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/**
 * #26217: in landscape, when the left button column has no room for the speedometer, it must neither
 * cover map buttons nor jump between two positions on every layout pass. The HUD content height is
 * swept with [MapHudLayout.setExternalVisibleArea], so the affected grid sizes are reached on any
 * phone, tablet or emulator, not only on screens of one particular height.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SpeedometerLandscapeLayoutTest : AndroidTest() {

	@get:Rule
	val activityRule = ActivityTestRule(MapActivity::class.java, true, false)

	private var showSpeedometerBefore = false
	private var speedometerSizeBefore = WidgetSize.MEDIUM
	private var screenOrientationBefore = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
	private var layoutMode: ScreenLayoutMode? = null
	private var leftPanelOrderBefore: String? = null
	private var leftPanelOrderChanged = false
	private var leftWidgetEnabledBefore: Boolean? = null
	private var buttonState: QuickActionButtonState? = null

	@Before
	override fun setup() {
		super.setup()
		val appMode = settings.applicationMode
		showSpeedometerBefore = settings.SHOW_SPEEDOMETER.getModeValue(appMode)
		speedometerSizeBefore = settings.SPEEDOMETER_SIZE.getModeValue(appMode)
		screenOrientationBefore = settings.MAP_SCREEN_ORIENTATION.get()
		settings.SHOW_SPEEDOMETER.setModeValue(appMode, true)
		settings.SPEEDOMETER_SIZE.setModeValue(appMode, WidgetSize.MEDIUM)
		settings.MAP_SCREEN_ORIENTATION.set(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
	}

	@After
	fun restoreSettings() {
		val appMode = settings.applicationMode
		val activity = activityRule.activity
		if (activity != null) {
			InstrumentationRegistry.getInstrumentation().runOnMainSync {
				activity.findViewById<MapHudLayout>(R.id.map_hud_layout)?.clearExternalVisibleArea()
				val enabledBefore = leftWidgetEnabledBefore
				if (enabledBefore != null) {
					val registry = app.osmandMap.mapLayers.mapWidgetRegistry
					registry.getWidgetInfoById(LEFT_PANEL_WIDGET.id)?.let { info ->
						registry.enableDisableWidgetForMode(appMode, info, if (enabledBefore) true else null, layoutMode, false)
					}
				}
			}
		}
		if (leftPanelOrderChanged) {
			WidgetsPanel.LEFT.getOrderPreference(settings, layoutMode).setModeValue(appMode, leftPanelOrderBefore)
		}
		buttonState?.let { app.mapButtonsHelper.removeQuickActionButtonState(it) }
		settings.SHOW_SPEEDOMETER.setModeValue(appMode, showSpeedometerBefore)
		settings.SPEEDOMETER_SIZE.setModeValue(appMode, speedometerSizeBefore)
		settings.MAP_SCREEN_ORIENTATION.set(screenOrientationBefore)
	}

	@Test
	fun speedometerDoesNotCoverButtonsOrJumpWhenLeftColumnIsFull() {
		// The user's setup from the issue: a custom button right above the menu button and a widget
		// in the left panel, which pushes the configure map, search and compass buttons down.
		addCustomButtonAboveMenu()
		putWidgetToLeftPanel()
		val activity = activityRule.launchActivity(null)
		skipAppStartDialogs(app)
		Espresso.onIdle()
		showLeftPanelWidget()

		val hud = activity.findViewById<MapHudLayout>(R.id.map_hud_layout)
		val speedometer = activity.findViewById<View>(R.id.speedometer_widget)
		val leftPanel = activity.findViewById<View>(R.id.map_left_widgets_panel)
		waitUntil({ "speedometer and left panel are shown (${describe(speedometer)}, ${describe(leftPanel)})" }) {
			activity.runOnUiThread { feedLocation() }
			isShown(speedometer) && isShown(leftPanel)
		}

		val problems = sweepContentHeights(activity, hud, speedometer)
		assertTrue("Speedometer layout problems:\n${problems.joinToString("\n")}", problems.isEmpty())
	}

	private fun sweepContentHeights(activity: MapActivity, hud: MapHudLayout, speedometer: View): List<String> {
		val speedometerBox = speedometer.findViewById<View>(R.id.speedometer_container)
		val density = activity.resources.displayMetrics.density
		val basePaddingTop = hud.paddingTop
		val naturalHeight = hud.height - hud.paddingTop - hud.paddingBottom
		val maxHeightDp = (naturalHeight / density).toInt()
		LOG.info("HUD ${hud.width}x${hud.height}, content height ${naturalHeight}px, density $density")

		val problems = ArrayList<String>()
		var heightDp = MIN_CONTENT_HEIGHT_DP
		while (heightDp <= maxHeightDp) {
			val contentHeight = (heightDp * density).roundToInt()
			InstrumentationRegistry.getInstrumentation().runOnMainSync {
				val location = IntArray(2)
				hud.getLocationOnScreen(location)
				hud.setExternalVisibleArea(Rect(location[0], location[1],
					location[0] + hud.width, location[1] + basePaddingTop + contentHeight))
				feedLocation()
			}
			for (i in 0 until WARM_UP_PASSES) {
				relayout(hud)
			}
			val samples = ArrayList<Rect>()
			val covered = LinkedHashSet<String>()
			for (i in 0 until SAMPLED_PASSES) {
				relayout(hud)
				samples.add(screenRect(speedometer))
				// The widget view has transparent padding around the speedometer box; only the box must
				// not cover buttons.
				covered.addAll(coveredButtons(hud, screenRect(speedometerBox)))
			}
			val positions = samples.distinct()
			LOG.info("Content height ${heightDp}dp: speedometer $positions, covers $covered")
			if (!isShown(speedometer)) {
				problems.add("${heightDp}dp: speedometer is not shown (${describe(speedometer)})")
			} else if (positions.size > 1) {
				problems.add("${heightDp}dp: speedometer jumps between $positions")
			} else if (covered.isNotEmpty()) {
				problems.add("${heightDp}dp: speedometer ${positions[0]} covers $covered")
			}
			heightDp += HEIGHT_STEP_DP
		}
		return problems
	}

	private fun relayout(hud: MapHudLayout) {
		val instrumentation = InstrumentationRegistry.getInstrumentation()
		instrumentation.runOnMainSync { hud.updateButtons() }
		instrumentation.waitForIdleSync()
		// MapHudLayout refreshes 100 ms after a widget or panel changes its size.
		Thread.sleep(REFRESH_WAIT_MILLIS)
		instrumentation.waitForIdleSync()
	}

	private fun addCustomButtonAboveMenu() {
		val buttonsHelper = app.mapButtonsHelper
		val state = buttonsHelper.createNewButtonState()
		state.setEnabled(true)
		buttonsHelper.addQuickAction(state, DayNightModeAction())
		buttonsHelper.addQuickActionButtonState(state)
		buttonState = state

		val position = ButtonPositionSize(state.id, BUTTON_SIZE, true, false)
			.setMargin(0, MENU_ROW_HEIGHT)
			.setMoveAny()
		@Suppress("UNCHECKED_CAST")
		val landscapePosition = settings.getPreference(state.id + "_position_landscape") as CommonPreference<Long>
		landscapePosition.setModeValue(settings.applicationMode, position.toLongValue())
	}

	// The panel of a widget is resolved from the panel orders when the widget is created, so the order
	// has to be in place before the map screen is opened.
	private fun putWidgetToLeftPanel() {
		val appMode = settings.applicationMode
		val mode = if (settings.USE_SEPARATE_LAYOUTS.get()) ScreenLayoutMode.LANDSCAPE else null
		layoutMode = mode
		val orderPreference = WidgetsPanel.LEFT.getOrderPreference(settings, mode)
		leftPanelOrderBefore = orderPreference.getModeValue(appMode)
		leftPanelOrderChanged = true
		WidgetsPanel.LEFT.setWidgetsOrder(appMode, listOf(listOf(LEFT_PANEL_WIDGET.id)), settings, mode)
	}

	private fun showLeftPanelWidget() {
		val appMode = settings.applicationMode
		val mode = layoutMode
		val registry = app.osmandMap.mapLayers.mapWidgetRegistry
		var widgetInfo: MapWidgetInfo? = null
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			widgetInfo = registry.getWidgetInfoById(LEFT_PANEL_WIDGET.id)
		}
		val info = widgetInfo
		assertTrue("${LEFT_PANEL_WIDGET.id} widget is not registered", info != null)
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			leftWidgetEnabledBefore = info!!.isEnabledForAppMode(appMode, mode)
			registry.enableDisableWidgetForMode(appMode, info, true, mode, false)
			app.osmandMap.mapLayers.mapInfoLayer?.recreateControls()
		}
		Espresso.onIdle()
	}

	private fun feedLocation() {
		val location = Location("test", LATITUDE, LONGITUDE)
		location.speed = SPEED_MPS
		location.time = System.currentTimeMillis()
		app.locationProvider.setLocationFromSimulation(location)
		app.osmandMap.mapView.refreshMap()
	}

	private fun coveredButtons(hud: MapHudLayout, speedometerRect: Rect): List<String> {
		val result = ArrayList<String>()
		for (button in visibleMapButtons(hud)) {
			val buttonRect = screenRect(button)
			if (Rect.intersects(speedometerRect, buttonRect)) {
				result.add("${AndroidUtils.getViewName(button)} $buttonRect")
			}
		}
		return result
	}

	private fun visibleMapButtons(root: ViewGroup): List<MapButton> {
		val result = ArrayList<MapButton>()
		for (i in 0 until root.childCount) {
			val child = root.getChildAt(i)
			if (child is MapButton && isShown(child)) {
				result.add(child)
			} else if (child is ViewGroup && child.visibility == View.VISIBLE) {
				result.addAll(visibleMapButtons(child))
			}
		}
		return result
	}

	private fun isShown(view: View): Boolean {
		return view.isShown && view.width > 0 && view.height > 0
	}

	private fun describe(view: View): String {
		return "visibility=${view.visibility} size=${view.width}x${view.height}"
	}

	private fun screenRect(view: View): Rect {
		val xy = IntArray(2)
		view.getLocationOnScreen(xy)
		return Rect(xy[0], xy[1], xy[0] + view.width, xy[1] + view.height)
	}

	private fun waitUntil(what: () -> String, condition: () -> Boolean) {
		val end = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
		while (System.currentTimeMillis() < end) {
			if (condition()) {
				return
			}
			Thread.sleep(250)
		}
		throw AssertionError("Timed out waiting until ${what()}")
	}

	companion object {
		private val LOG = PlatformUtil.getLog(SpeedometerLandscapeLayoutTest::class.java)

		private val LEFT_PANEL_WIDGET = WidgetType.BATTERY

		private const val BUTTON_SIZE = 7
		private const val MENU_ROW_HEIGHT = 8
		private const val LATITUDE = 50.4501
		private const val LONGITUDE = 30.5234
		private const val SPEED_MPS = 15f

		// Half a grid cell, so every grid height is reached with the HUD both rounded up and down.
		private const val HEIGHT_STEP_DP = 4
		private const val MIN_CONTENT_HEIGHT_DP = 240
		private const val WARM_UP_PASSES = 2
		private const val SAMPLED_PASSES = 4
		private const val REFRESH_WAIT_MILLIS = 250L
		private const val WAIT_TIMEOUT_MILLIS = 15_000L
	}
}
