package net.osmand.test.common

import android.graphics.Point
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.helpers.MapDisplayPositionManager.ICoveredScreenRectProvider
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.controls.maphudbuttons.MapButton
import net.osmand.plus.views.mapwidgets.widgets.RulerWidget
import org.junit.Assert.assertTrue

/**
 * Checks where the map controls (map buttons, rulers and map sliders) and the map center are
 * relative to the panels shown over the map, in the state the map screen is in right now:
 *
 * 1. A map control does not overlap a panel, unless it belongs to that panel or the panel is drawn
 *    above it and hides it completely (an opaque menu over the map buttons, which may show its own
 *    copies of them). A control that is clipped by a panel edge or drawn over a panel is broken. It
 *    also stays clear of the system bars. The panels are the top and bottom widget panels, the
 *    rects that shown fragments report as [ICoveredScreenRectProvider], and the panel shown in
 *    `bottomFragmentContainer`.
 * 2. A panel shown in `bottomFragmentContainer` starts at the top of the container, and the map
 *    HUD ends there too. Map controls are laid out at the bottom of the HUD, so a gap above the
 *    container or above the panel inside it is an empty strip of map between the controls and the
 *    panel.
 * 3. The map center, which zoom buttons and the location marker use, is on the visible map: not
 *    under a panel and not under a system bar.
 */
object MapHudInvariants {

	private const val CHECK_INTERVAL_MS = 100L
	private const val STABLE_CHECKS_COUNT = 5
	private const val BOTTOM_CONTAINER = "bottomFragmentContainer"

	private val SLIDER_IDS = intArrayOf(R.id.map_transparency_layout, R.id.layer_param_layout)
	private val WIDGET_PANEL_IDS = intArrayOf(R.id.top_widgets_panel, R.id.map_bottom_widgets_panel)

	data class Snapshot(
		val controls: Map<String, Rect>,
		val panels: Map<String, Rect>,
		/** Control and panel names of the overlaps that are allowed, see the class description. */
		val allowedOverlaps: Set<Pair<String, String>>,
		val safeArea: Rect,
		val bottomPanel: String?,
		val hudBottom: Int?,
		val bottomContainerTop: Int?,
		val bottomPanelContentTop: Int?,
		val mapCenter: Point?
	)

	/**
	 * Waits until the map controls, the panels and the map center stop moving and fails with every
	 * broken invariant.
	 */
	fun assertHolds(activity: MapActivity) {
		val idlingResource = LayoutSettledIdlingResource(activity)
		IdlingRegistry.getInstance().register(idlingResource)
		try {
			Espresso.onIdle()
		} finally {
			IdlingRegistry.getInstance().unregister(idlingResource)
		}
		var snapshot: Snapshot? = null
		InstrumentationRegistry.getInstrumentation().runOnMainSync { snapshot = capture(activity) }
		val violations = findViolations(requireNotNull(snapshot))
		assertTrue(violations.joinToString("\n", "Map HUD invariants are broken:\n", "\n$snapshot"),
				violations.isEmpty())
	}

	/** Has to be called on the main thread. */
	fun capture(activity: MapActivity): Snapshot {
		val fragments = activity.supportFragmentManager.fragments.filter { it.view?.isShown == true }

		val controlViews = LinkedHashMap<String, View>()
		collectControls(activity.window.decorView, fragments, controlViews)

		val panelViews = LinkedHashMap<String, View>()
		val panels = LinkedHashMap<String, Rect>()
		for (id in WIDGET_PANEL_IDS) {
			activity.findViewById<View>(id)?.takeIf { it.isShown }?.let { view ->
				AndroidUtils.getViewBoundOnScreen(view).takeUnless { it.isEmpty }?.let {
					panels[getName(view)] = it
					panelViews[getName(view)] = view
				}
			}
		}
		for (fragment in fragments) {
			val provider = fragment as? ICoveredScreenRectProvider ?: continue
			provider.coveredScreenRects.filterNot { it.isEmpty }.forEachIndexed { index, rect ->
				val name = "${fragment.javaClass.simpleName}#$index"
				panels[name] = rect
				panelViews[name] = fragment.requireView()
			}
		}

		var bottomPanelName: String? = null
		var hudBottom: Int? = null
		var bottomContainerTop: Int? = null
		var bottomPanelContentTop: Int? = null
		val bottomContainer = activity.findViewById<ViewGroup>(R.id.bottomFragmentContainer)
		val bottomPanel = bottomContainer?.takeIf { it.isShown }?.let { firstShownChild(it) }
		if (bottomPanel != null) {
			val containerRect = AndroidUtils.getViewBoundOnScreen(bottomContainer)
			panels[BOTTOM_CONTAINER] = containerRect
			panelViews[BOTTOM_CONTAINER] = bottomContainer
			bottomPanelName = fragments.firstOrNull { it.view === bottomPanel }?.javaClass?.simpleName
					?: getName(bottomPanel)
			hudBottom = activity.findViewById<View>(R.id.map_hud_layout)?.let { AndroidUtils.getViewBoundOnScreen(it).bottom }
			bottomContainerTop = containerRect.top
			bottomPanelContentTop = getContentTop(bottomPanel)
		}

		val controls = controlViews.mapValues { AndroidUtils.getViewBoundOnScreen(it.value) }
		val allowedOverlaps = HashSet<Pair<String, String>>()
		for ((control, controlView) in controlViews) {
			for ((panel, panelView) in panelViews) {
				val ownControl = isDescendant(controlView, panelView)
				val hidden = isDrawnAbove(panelView, controlView) && panels.getValue(panel).contains(controls.getValue(control))
				if (ownControl || hidden) {
					allowedOverlaps.add(control to panel)
				}
			}
		}

		val decorView = activity.window.decorView
		val safeArea = AndroidUtils.getViewBoundOnScreen(decorView)
		ViewCompat.getRootWindowInsets(decorView)?.let {
			val bars = it.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
			safeArea.set(safeArea.left + bars.left, safeArea.top + bars.top,
					safeArea.right - bars.right, safeArea.bottom - bars.bottom)
		}

		val app = activity.app
		val mapView = app.osmandMap.mapView
		val mapViewContainer = mapView.view
		val mapCenter = if (mapViewContainer != null && mapViewContainer.isShown) {
			val tileBox = mapView.rotatedTileBox
			val ratio = app.mapViewTrackingUtilities.mapDisplayPositionManager.mapRatio
			val location = AndroidUtils.getLocationOnScreen(mapViewContainer)
			Point(location[0] + (ratio.x * tileBox.pixWidth).toInt(), location[1] + (ratio.y * tileBox.pixHeight).toInt())
		} else {
			null
		}
		return Snapshot(controls, panels, allowedOverlaps, safeArea, bottomPanelName, hudBottom, bottomContainerTop,
				bottomPanelContentTop, mapCenter)
	}

	fun findViolations(snapshot: Snapshot): List<String> {
		val violations = ArrayList<String>()
		for ((control, controlRect) in snapshot.controls) {
			for ((panel, panelRect) in snapshot.panels) {
				if (Rect.intersects(controlRect, panelRect) && (control to panel) !in snapshot.allowedOverlaps) {
					violations.add("$control $controlRect overlaps panel $panel $panelRect")
				}
			}
			val hidden = snapshot.allowedOverlaps.any { it.first == control && Rect.intersects(controlRect, snapshot.panels.getValue(it.second)) }
			if (!hidden && !snapshot.safeArea.contains(controlRect)) {
				violations.add("$control $controlRect is under a system bar, safe area ${snapshot.safeArea}")
			}
		}
		val containerTop = snapshot.bottomContainerTop
		val hudBottom = snapshot.hudBottom
		if (containerTop != null && hudBottom != null && hudBottom != containerTop) {
			violations.add("Map HUD ends ${containerTop - hudBottom}px above $BOTTOM_CONTAINER with ${snapshot.bottomPanel}, "
					+ "leaving a strip of map between the map controls and the panel")
		}
		val contentTop = snapshot.bottomPanelContentTop
		if (containerTop != null && contentTop != null && contentTop != containerTop) {
			violations.add("${snapshot.bottomPanel} in $BOTTOM_CONTAINER starts ${contentTop - containerTop}px "
					+ "below the container top, leaving a strip of map between the map controls and the panel")
		}
		snapshot.mapCenter?.let { center ->
			for ((panel, panelRect) in snapshot.panels) {
				if (panelRect.contains(center.x, center.y)) {
					violations.add("Map center $center is under panel $panel $panelRect")
				}
			}
			if (!snapshot.safeArea.contains(center.x, center.y)) {
				violations.add("Map center $center is under a system bar, safe area ${snapshot.safeArea}")
			}
		}
		return violations
	}

	private fun collectControls(view: View, fragments: List<Fragment>, controls: MutableMap<String, View>) {
		if (!view.isShown) {
			return
		}
		if (view is MapButton || view is RulerWidget || view.id in SLIDER_IDS) {
			if (view.width > 0 && view.height > 0) {
				val owner = fragments.lastOrNull { isDescendant(view, it.requireView()) }
				controls["${owner?.javaClass?.simpleName ?: "hud"}/${getName(view)}"] = view
			}
		} else if (view is ViewGroup) {
			for (i in 0 until view.childCount) {
				collectControls(view.getChildAt(i), fragments, controls)
			}
		}
	}

	private fun isDescendant(view: View, ancestor: View): Boolean {
		var current: View? = view
		while (current != null) {
			if (current === ancestor) {
				return true
			}
			current = current.parent as? View
		}
		return false
	}

	/** Whether [view] is drawn after [other], judged by the child order under their common ancestor. */
	private fun isDrawnAbove(view: View, other: View): Boolean {
		val path = getAncestors(view)
		val otherPath = getAncestors(other)
		val commonIndex = path.indexOfFirst { it in otherPath }
		if (commonIndex <= 0) {
			return false
		}
		val common = path[commonIndex] as ViewGroup
		val otherCommonIndex = otherPath.indexOf(common)
		if (otherCommonIndex <= 0) {
			return false
		}
		return common.indexOfChild(path[commonIndex - 1]) > common.indexOfChild(otherPath[otherCommonIndex - 1])
	}

	private fun getAncestors(view: View): List<View> {
		val ancestors = ArrayList<View>()
		var current: View? = view
		while (current != null) {
			ancestors.add(current)
			current = current.parent as? View
		}
		return ancestors
	}

	private fun firstShownChild(group: ViewGroup): View? {
		return (0 until group.childCount).map { group.getChildAt(it) }.firstOrNull { it.isShown && it.height > 0 }
	}

	private fun getContentTop(panel: View): Int {
		val children = if (panel is ViewGroup) {
			(0 until panel.childCount).map { panel.getChildAt(it) }.filter { it.isShown && it.height > 0 }
		} else {
			emptyList()
		}
		return if (children.isEmpty()) {
			AndroidUtils.getViewBoundOnScreen(panel).top + panel.paddingTop
		} else {
			children.minOf { AndroidUtils.getViewBoundOnScreen(it).top }
		}
	}

	private fun getName(view: View): String {
		val idName = if (view.id != View.NO_ID) {
			runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull()
		} else {
			null
		}
		return idName ?: "${view.javaClass.simpleName}@${Integer.toHexString(System.identityHashCode(view))}"
	}

	/**
	 * Idle once [capture] returns the same snapshot several times in a row, so that panels,
	 * window insets, the map controls grid and the map display position have settled.
	 */
	private class LayoutSettledIdlingResource(
		private val activity: MapActivity
	) : BaseIdlingResource(activity.app) {

		private val handler = Handler(Looper.getMainLooper())
		private var stableChecks = 0
		private var lastSnapshot: Snapshot? = null

		init {
			scheduleCheck()
		}

		override fun isIdleNow(): Boolean = stableChecks >= STABLE_CHECKS_COUNT

		private fun scheduleCheck() {
			handler.postDelayed({
				val snapshot = capture(activity)
				if (snapshot == lastSnapshot) {
					stableChecks++
				} else {
					stableChecks = 0
					lastSnapshot = snapshot
				}
				if (isIdleNow()) {
					notifyIdleTransition()
				} else {
					scheduleCheck()
				}
			}, CHECK_INTERVAL_MS)
		}
	}
}
