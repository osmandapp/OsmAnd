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
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID
import net.osmand.plus.R
import net.osmand.plus.base.AppModeDependentComponent
import net.osmand.plus.base.BaseMaterialFragment
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.InsetTarget
import net.osmand.plus.utils.InsetTargetsCollection
import net.osmand.plus.views.mapwidgets.utils.AverageValueComputer.MEASURED_INTERVALS
import net.osmand.plus.widgets.ui.GroupFooterView
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow

/**
 * Travel speed of a profile: the default speed, the speed range it has to stay in, and whether
 * the arrival time follows the measured speed. The speed range starts collapsed.
 */
class DefaultSpeedFragment : BaseMaterialFragment() {

	private lateinit var speedHelper: VehicleSpeedHelper
	private lateinit var config: VehicleSpeedHelper.SpeedConfig
	private var rangeExpanded = false
	private var changed = false

	override fun getStatusBarColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		rangeExpanded = savedInstanceState?.getBoolean(STATE_RANGE_EXPANDED) == true
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
		setupSpeedRange(view)
		setupArrivalTime(view)
		return view
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putBoolean(STATE_RANGE_EXPANDED, rangeExpanded)
	}

	/* rows and slider cards share child ids, so restored view state lands in the wrong row -
	 * the screen state is taken from the preferences again after the restore */
	override fun onViewStateRestored(savedInstanceState: Bundle?) {
		super.onViewStateRestored(savedInstanceState)
		val view = view
		if (view != null) {
			updateArrivalTime(view)
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
		toolbar.setTitle(R.string.travel_speed)
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
		val card: View = view.findViewById(R.id.default_speed_card)
		card.findViewById<TextView>(R.id.title).setText(R.string.default_speed_setting_title)
		val slider: Slider = card.findViewById(R.id.slider)
		/* every slider card has the same child ids - restored view state would land in the wrong card */
		slider.isSaveEnabled = false
		slider.stepSize = if (config.decimalPrecision) 0f else 1f
		slider.isTickVisible = false
		slider.addOnChangeListener { _, value, fromUser ->
			if (fromUser) {
				config.defaultSpeed = roundSpeed(value).coerceIn(config.minSpeed, config.maxSpeed)
				appMode.setDefaultSpeed(config.defaultSpeed / config.ratio)
				changed = true
				updateDefaultSpeed(view)
			}
		}
		updateDefaultSpeed(view)
	}

	/** The default speed can't leave the speed range, so the range is the slider scale. */
	private fun updateDefaultSpeed(view: View) {
		val card: View = view.findViewById(R.id.default_speed_card)
		val slider: Slider = card.findViewById(R.id.slider)
		val from = if (config.defaultSpeedOnly) config.min.toFloat() else config.minSpeed
		val to = if (config.defaultSpeedOnly) config.max.toFloat() else config.maxSpeed
		slider.valueFrom = from
		slider.valueTo = to.coerceAtLeast(from + 1)
		slider.value = config.defaultSpeed.coerceIn(slider.valueFrom, slider.valueTo)
		val text = formatSpeed(config.defaultSpeed)
		card.findViewById<TextView>(R.id.value).text = text
		ViewCompat.setStateDescription(slider, text)
	}

	private fun setupSpeedRange(view: View) {
		val group: View = view.findViewById(R.id.speed_range_group)
		group.visibility = if (config.defaultSpeedOnly) View.GONE else View.VISIBLE
		if (config.defaultSpeedOnly) {
			return
		}
		val slider: RangeSlider = view.findViewById(R.id.speed_range_slider)
		slider.isSaveEnabled = false
		slider.valueFrom = config.min.toFloat()
		slider.valueTo = config.max.toFloat()
		slider.stepSize = if (config.decimalPrecision) 0f else 1f
		slider.setMinSeparationValue(1f)
		slider.values = listOf(
			config.minSpeed.coerceIn(slider.valueFrom, slider.valueTo),
			config.maxSpeed.coerceIn(slider.valueFrom, slider.valueTo))
		slider.addOnChangeListener { s, _, fromUser ->
			if (fromUser) {
				config.minSpeed = roundSpeed(s.values[0])
				config.maxSpeed = roundSpeed(s.values[1])
				appMode.setMinSpeed(config.minSpeed / config.ratio)
				appMode.setMaxSpeed(config.maxSpeed / config.ratio)
				val defaultSpeed = config.defaultSpeed.coerceIn(config.minSpeed, config.maxSpeed)
				if (defaultSpeed != config.defaultSpeed) {
					config.defaultSpeed = defaultSpeed
					appMode.setDefaultSpeed(defaultSpeed / config.ratio)
				}
				changed = true
				updateSpeedRange(view)
				updateDefaultSpeed(view)
			}
		}
		view.findViewById<View>(R.id.speed_range_header).setOnClickListener { toggleSpeedRange(view) }
		view.findViewById<View>(R.id.speed_range_expand).setOnClickListener { toggleSpeedRange(view) }
		updateSpeedRange(view)
	}

	private fun toggleSpeedRange(view: View) {
		rangeExpanded = !rangeExpanded
		updateSpeedRange(view)
	}

	private fun updateSpeedRange(view: View) {
		val text = getString(R.string.ltr_or_rtl_combine_via_space,
			getString(R.string.ltr_or_rtl_combine_via_dash,
				speedHelper.formatSpeed(config, config.minSpeed),
				speedHelper.formatSpeed(config, config.maxSpeed)),
			config.units)
		view.findViewById<TextView>(R.id.speed_range_value).text = text
		val slider: RangeSlider = view.findViewById(R.id.speed_range_slider)
		slider.visibility = if (rangeExpanded) View.VISIBLE else View.GONE
		ViewCompat.setStateDescription(slider, text)
		val expand: MaterialButton = view.findViewById(R.id.speed_range_expand)
		expand.setIconResource(if (rangeExpanded) R.drawable.ic_action_arrow_up else R.drawable.ic_action_arrow_down)
		expand.contentDescription = getString(if (rangeExpanded) R.string.shared_string_collapse else R.string.show_more)
	}

	private fun setupArrivalTime(view: View) {
		val adaptRow = SettingRow(view.findViewById(R.id.adapt_speed_row))
		adaptRow.setIcon(null)
		adaptRow.setTitle(R.string.eta_adapt_to_my_speed)
		adaptRow.setOnClickListener {
			val pref = settings().ETA_USE_AVERAGE_SPEED
			pref.setModeValue(appMode, !pref.getModeValue(appMode))
			changed = true
			updateArrivalTime(view)
		}

		/* the same steps as the Average speed widget */
		val intervalCard: View = view.findViewById(R.id.interval_card)
		intervalCard.findViewById<TextView>(R.id.title).setText(R.string.shared_string_interval)
		val slider: Slider = intervalCard.findViewById(R.id.slider)
		slider.isSaveEnabled = false
		slider.valueFrom = 0f
		slider.valueTo = (MEASURED_INTERVALS.size - 1).toFloat()
		slider.stepSize = 1f
		slider.isTickVisible = false
		slider.addOnChangeListener { _, value, fromUser ->
			if (fromUser) {
				settings().ETA_AVERAGE_SPEED_INTERVAL.setModeValue(appMode, MEASURED_INTERVALS[value.toInt()])
				changed = true
				updateArrivalTime(view)
			}
		}
		updateArrivalTime(view)
	}

	private fun updateArrivalTime(view: View) {
		val enabled = settings().ETA_USE_AVERAGE_SPEED.getModeValue(appMode)
		val intervalMillis = settings().ETA_AVERAGE_SPEED_INTERVAL.getModeValue(appMode)
		val interval = formatInterval(intervalMillis)

		SettingRow(view.findViewById(R.id.adapt_speed_row)).setChecked(enabled)
		val intervalCard: View = view.findViewById(R.id.interval_card)
		intervalCard.visibility = if (enabled) View.VISIBLE else View.GONE
		intervalCard.findViewById<TextView>(R.id.value).text = interval
		val slider: Slider = intervalCard.findViewById(R.id.slider)
		val index = MEASURED_INTERVALS.indexOf(intervalMillis).takeIf { it >= 0 }
			?: MEASURED_INTERVALS.indexOf(DEFAULT_INTERVAL_MILLIS)
		slider.value = index.toFloat()
		ViewCompat.setStateDescription(slider, interval)
		SegmentedList.apply(view.findViewById(R.id.eta_rows))

		val footer: GroupFooterView = view.findViewById(R.id.eta_footer)
		footer.setText(if (enabled) getString(R.string.eta_adapt_to_my_speed_descr, interval)
			else getString(R.string.eta_adapt_to_my_speed_off_descr))
	}

	private fun roundSpeed(value: Float): Float =
		if (config.decimalPrecision) Math.round(value * 10) / 10f else Math.round(value).toFloat()

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

	private fun formatInterval(millis: Long): String =
		if (millis < 60_000L) {
			getString(R.string.ltr_or_rtl_combine_via_space, (millis / 1000).toString(), getString(R.string.shared_string_sec))
		} else {
			getString(R.string.ltr_or_rtl_combine_via_space, (millis / 60_000L).toString(), getString(R.string.shared_string_minute_lowercase))
		}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}

	companion object {
		private const val STATE_RANGE_EXPANDED = "range_expanded"
		private const val DEFAULT_INTERVAL_MILLIS = 15 * 60_000L

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
