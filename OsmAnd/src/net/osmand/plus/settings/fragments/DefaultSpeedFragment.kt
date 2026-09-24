package net.osmand.plus.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider
import net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID
import net.osmand.plus.R
import net.osmand.plus.base.AppModeDependentComponent
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.widgets.ui.GroupFooterView
import net.osmand.plus.widgets.ui.GroupHeaderView
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow

/**
 * Default speed of a profile, and whether the arrival time follows the measured average speed.
 * The min. and max. speed only change the routing, so their group starts collapsed.
 */
class DefaultSpeedFragment : BaseMaterialFragment() {

	private lateinit var speedHelper: VehicleSpeedHelper
	private lateinit var config: VehicleSpeedHelper.SpeedConfig
	private var limitsExpanded = false
	private var changed = false

	override fun getStatusBarColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		limitsExpanded = savedInstanceState?.getBoolean(STATE_LIMITS_EXPANDED) == true
		speedHelper = VehicleSpeedHelper(osmandApp, appMode)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		config = speedHelper.createSpeedConfig()
		val view = inflater.inflate(R.layout.fragment_default_speed, container, false)
		setupToolbar(view)
		setupDefaultSpeed(view)
		setupRoutingLimits(view)
		return view
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(STATE_LIMITS_EXPANDED, limitsExpanded)
	}

	/* rows and slider cards share child ids, so restored view state lands in the wrong row -
	 * the screen state is taken from the preferences again after the restore */
	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)
		val view = view
		if (view != null) {
			updateAverageSpeed(view)
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		if (changed) {
			osmandApp.routingHelper.onSettingsChanged(appMode)
			changed = false
		}
	}

	private fun setupToolbar(view: View) {
		val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
		toolbar.setTitle(R.string.default_speed_setting_title)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
		toolbar.menu.clear()
		toolbar.menu.add(R.string.reset_to_default).apply {
			val icon = AppCompatResources.getDrawable(view.context, R.drawable.ic_action_reset)?.mutate()
			icon?.setTint(AndroidUtils.getColorFromAttr(view.context, R.attr.colorOnSurfaceVariant))
			setIcon(icon)
			setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
			setOnMenuItemClickListener {
				resetToDefault()
				true
			}
		}
	}

	private fun setupDefaultSpeed(view: View) {
		bindSpeedCard(view.findViewById(R.id.default_speed_card), R.string.default_speed_setting_title,
			config.defaultSpeed) { value ->
			val speed = value.coerceIn(config.minSpeed, config.maxSpeed)
			config.defaultSpeed = speed
			appMode.setDefaultSpeed(speed / config.ratio)
			speed
		}

		val averageRow = SettingRow(view.findViewById(R.id.average_speed_row))
		averageRow.setIcon(null)
		averageRow.setTitle(R.string.eta_use_average_speed)
		averageRow.setOnClickListener {
			val pref = settings().ETA_USE_AVERAGE_SPEED
			pref.setModeValue(appMode, !pref.getModeValue(appMode))
			changed = true
			updateAverageSpeed(view)
		}

		val intervalCard: View = view.findViewById(R.id.interval_card)
		intervalCard.findViewById<TextView>(R.id.title).setText(R.string.shared_string_interval)
		val slider: Slider = intervalCard.findViewById(R.id.slider)
		slider.isSaveEnabled = false
		slider.valueFrom = 0f
		slider.valueTo = (INTERVALS_MIN.size - 1).toFloat()
		slider.stepSize = 1f
		slider.addOnChangeListener { _, value, fromUser ->
			if (fromUser) {
				settings().ETA_AVERAGE_SPEED_INTERVAL.setModeValue(appMode, INTERVALS_MIN[value.toInt()] * 60_000L)
				changed = true
				updateAverageSpeed(view)
			}
		}

		updateAverageSpeed(view)
	}

	private fun updateAverageSpeed(view: View) {
		val enabled = settings().ETA_USE_AVERAGE_SPEED.getModeValue(appMode)
		val intervalMinutes = settings().ETA_AVERAGE_SPEED_INTERVAL.getModeValue(appMode) / 60_000L
		val interval = formatMinutes(intervalMinutes)

		SettingRow(view.findViewById(R.id.average_speed_row)).setChecked(enabled)
		val intervalCard: View = view.findViewById(R.id.interval_card)
		intervalCard.visibility = if (enabled) View.VISIBLE else View.GONE
		intervalCard.findViewById<TextView>(R.id.value).text = interval
		val slider: Slider = intervalCard.findViewById(R.id.slider)
		val index = INTERVALS_MIN.indexOf(intervalMinutes).takeIf { it >= 0 } ?: INTERVALS_MIN.indexOf(15L)
		slider.value = index.toFloat()
		ViewCompat.setStateDescription(slider, interval)
		SegmentedList.apply(view.findViewById(R.id.eta_rows))

		val footer: GroupFooterView = view.findViewById(R.id.eta_footer)
		footer.visibility = if (enabled) View.VISIBLE else View.GONE
		footer.setText(getString(R.string.eta_use_average_speed_descr, interval))
		/* the footer keeps the gap to the next group; without it the group needs its own margin */
		val routingGroup: View = view.findViewById(R.id.routing_group)
		(routingGroup.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
			if (enabled) 0 else resources.getDimensionPixelSize(R.dimen.ui_group_gap)
		routingGroup.requestLayout()
	}

	private fun setupRoutingLimits(view: View) {
		val routingGroup: View = view.findViewById(R.id.routing_group)
		routingGroup.visibility = if (config.defaultSpeedOnly) View.GONE else View.VISIBLE
		if (config.defaultSpeedOnly) {
			return
		}
		bindSpeedCard(view.findViewById(R.id.min_speed_card), R.string.shared_string_min_speed,
			config.minSpeed) { value ->
			val speed = value.coerceAtMost(config.defaultSpeed)
			config.minSpeed = speed
			appMode.setMinSpeed(speed / config.ratio)
			speed
		}
		bindSpeedCard(view.findViewById(R.id.max_speed_card), R.string.shared_string_max_speed,
			config.maxSpeed) { value ->
			val speed = value.coerceAtLeast(config.defaultSpeed)
			config.maxSpeed = speed
			appMode.setMaxSpeed(speed / config.ratio)
			speed
		}
		SegmentedList.apply(view.findViewById(R.id.routing_rows))
		updateRoutingLimits(view)
	}

	private fun updateRoutingLimits(view: View) {
		val header: GroupHeaderView = view.findViewById(R.id.routing_header)
		header.setTrailingAction(
			if (limitsExpanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down,
			getString(if (limitsExpanded) R.string.shared_string_collapse else R.string.show_more)
		) {
			limitsExpanded = !limitsExpanded
			updateRoutingLimits(view)
		}
		val visibility = if (limitsExpanded) View.VISIBLE else View.GONE
		view.findViewById<View>(R.id.routing_rows).visibility = visibility
		view.findViewById<View>(R.id.routing_footer).visibility = visibility
	}

	/** @param commit stores the value and returns it, clamped to what the other speeds allow */
	private fun bindSpeedCard(card: View, titleId: Int, initial: Float, commit: (Float) -> Float) {
		card.findViewById<TextView>(R.id.title).setText(titleId)
		val valueView: TextView = card.findViewById(R.id.value)
		val slider: Slider = card.findViewById(R.id.slider)
		/* every slider card has the same child ids - restored view state would land in the wrong card */
		slider.isSaveEnabled = false
		slider.valueFrom = config.min.toFloat()
		slider.valueTo = config.max.toFloat()
		slider.stepSize = if (config.decimalPrecision) 0f else 1f
		slider.isTickVisible = false
		slider.value = initial.coerceIn(slider.valueFrom, slider.valueTo)
		valueView.text = formatSpeed(initial)
		ViewCompat.setStateDescription(slider, formatSpeed(initial))
		slider.addOnChangeListener { s, value, fromUser ->
			if (fromUser) {
				val rounded = if (config.decimalPrecision) Math.round(value * 10) / 10f else Math.round(value).toFloat()
				val stored = commit(rounded)
				changed = true
				if (stored != value) {
					s.value = stored.coerceIn(s.valueFrom, s.valueTo)
				}
				valueView.text = formatSpeed(stored)
				ViewCompat.setStateDescription(s, formatSpeed(stored))
			}
		}
	}

	private fun resetToDefault() {
		appMode.resetDefaultSpeed()
		appMode.setMinSpeed(0f)
		appMode.setMaxSpeed(0f)
		settings().ETA_USE_AVERAGE_SPEED.resetModeToDefault(appMode)
		settings().ETA_AVERAGE_SPEED_INTERVAL.resetModeToDefault(appMode)
		changed = true
		/* the slider bounds depend on the stored speeds, so the screen is built again */
		val manager = parentFragmentManager
		if (!manager.isStateSaved) {
			manager.beginTransaction().detach(this).commitNow()
			manager.beginTransaction().attach(this).commitNow()
		}
	}

	private fun settings() = osmandSettings

	private fun formatSpeed(speed: Float): String =
		getString(R.string.ltr_or_rtl_combine_via_space, speedHelper.formatSpeed(config, speed), config.units)

	private fun formatMinutes(minutes: Long): String =
		if (minutes >= 60 && minutes % 60 == 0L) {
			getString(R.string.ltr_or_rtl_combine_via_space, (minutes / 60).toString(), getString(R.string.int_hour))
		} else {
			getString(R.string.ltr_or_rtl_combine_via_space, minutes.toString(), getString(R.string.shared_string_minute_lowercase))
		}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}

	companion object {
		private const val STATE_LIMITS_EXPANDED = "limits_expanded"
		private val INTERVALS_MIN = listOf(1L, 5L, 10L, 15L, 30L, 60L)

		@JvmStatic
		fun showInstance(activity: FragmentActivity, appMode: ApplicationMode) {
			val manager = activity.supportFragmentManager
			val tag = DefaultSpeedFragment::class.java.name
			if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
				val fragment = DefaultSpeedFragment()
				fragment.arguments = Bundle().apply {
					putString(AppModeDependentComponent.APP_MODE_KEY, appMode.stringKey)
				}
				manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, tag)
					.addToBackStack(DRAWER_SETTINGS_ID)
					.commitAllowingStateLoss()
			}
		}
	}
}
