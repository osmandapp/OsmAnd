package net.osmand.plus.plugins.gtfs

import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.dashboard.DashboardType
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.router.gtfs.guidance.GuidanceController.Command

/**
 * The step panel over the bottom of the map while a [GtfsGuidanceSession] runs: the current instruction, an
 * extra line (distance left on a walk) and the buttons that make sense in the current state. Built in code
 * because it is one small view; it is added to the map HUD column, which moves the map controls up.
 */
class GtfsGuidancePanel(private val activity: MapActivity, private val session: GtfsGuidanceSession) {

	private val app = activity.app
	private val nightMode = app.daynightHelper.isNightMode(app.settings.applicationMode, ThemeUsageContext.MAP)
	private val title = TextView(activity)
	private val detail = TextView(activity)
	private val buttons = LinearLayout(activity)
	private val root = LinearLayout(activity)
	private var parent: ViewGroup? = null

	init {
		val pad = AndroidUtils.dpToPx(activity, 12f)
		root.orientation = LinearLayout.VERTICAL
		root.setPadding(pad, pad, pad, pad / 2)
		root.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode))
		root.elevation = AndroidUtils.dpToPx(activity, 4f).toFloat()
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
		title.setTypeface(title.typeface, Typeface.BOLD)
		title.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode))
		detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
		detail.setTextColor(ColorUtilities.getSecondaryTextColor(app, nightMode))
		buttons.orientation = LinearLayout.HORIZONTAL
		buttons.gravity = Gravity.END
		val header = LinearLayout(activity)
		header.orientation = LinearLayout.HORIZONTAL
		header.gravity = Gravity.CENTER_VERTICAL
		header.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
		header.addView(menuButton(), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
		root.addView(header)
		root.addView(detail)
		root.addView(buttons)
	}

	fun attach() {
		val container = activity.findViewById<ViewGroup>(R.id.map_hud_container) ?: return
		if (root.parent == null) {
			container.addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
			parent = container
		}
		update()
	}

	fun detach() {
		parent?.removeView(root)
		parent = null
	}

	fun update() {
		if (parent == null) {
			return
		}
		val status = session.status()
		title.text = session.title()
		val extra = session.detail()
		detail.text = extra
		detail.visibility = if (extra == null) View.GONE else View.VISIBLE
		buttons.removeAllViews()
		if (status.canBoard) {
			addButton(R.string.gtfs_guide_on_board, Command.BOARDED)
		}
		if (status.canAlight) {
			addButton(R.string.gtfs_guide_got_off, Command.ALIGHTED)
		}
		if (status.canSkip) {
			addButton(R.string.gtfs_guide_skip, Command.SKIP)
		}
		if (status.canReplan) {
			addButton(R.string.gtfs_guide_replan, Command.REPLAN)
		}
		addButton(R.string.gtfs_guide_cancel, Command.CANCEL)
	}

	/**
	 * While a trip is guided the stock navigation UI hides the map's Menu button, so the panel carries one that
	 * opens the same menu (and from there Plugins and the settings). Cancel ends guidance and navigation.
	 */
	private fun menuButton(): Button {
		val button = styledButton(R.string.gtfs_guide_menu)
		button.setOnClickListener {
			MapActivity.clearPrevActivityIntent()
			if (app.settings.SHOW_DASHBOARD_ON_MAP_SCREEN.get()) {
				activity.dashboard.setDashboardVisibility(true, DashboardType.DASHBOARD, AndroidUtils.getCenterViewCoordinates(it))
			} else {
				activity.openDrawer()
			}
		}
		return button
	}

	private fun styledButton(textId: Int): Button {
		val button = Button(activity, null, android.R.attr.borderlessButtonStyle)
		button.setText(textId)
		button.isAllCaps = false
		button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
		button.setTextColor(ColorUtilities.getActiveColor(app, nightMode))
		button.minWidth = 0
		button.minimumWidth = 0
		return button
	}

	private fun addButton(textId: Int, command: Command) {
		val button = styledButton(textId)
		button.setOnClickListener { session.command(command) }
		buttons.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
	}
}
