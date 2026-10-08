package net.osmand.plus.settings.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.MaterialToolbar
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
import net.osmand.plus.widgets.ui.SliderListItem

/**
 * Travel speed of a profile: the default speed, the speed range it has to stay in, and whether
 * the arrival time follows the measured speed.
 */
class DefaultSpeedFragment : BaseMaterialFragment() {

	private lateinit var speedHelper: VehicleSpeedHelper
	private lateinit var config: VehicleSpeedHelper.SpeedConfig
	private lateinit var defaultSpeedRow: SliderListItem
	private lateinit var speedRangeRow: SliderListItem
	private lateinit var adaptSpeedRow: SettingRow
	private lateinit var intervalRow: SliderListItem
	private var routingChanged = false

	override fun getStatusBarColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		speedHelper = VehicleSpeedHelper(osmandApp, appMode)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_default_speed, container, false)
		setupToolbar(view)

		defaultSpeedRow = SliderListItem(view.findViewById(R.id.default_speed_row))
		defaultSpeedRow.setTitle(R.string.default_speed_setting_title)
		speedRangeRow = SliderListItem(view.findViewById(R.id.speed_range_row))
		speedRangeRow.setTitle(R.string.speed_range)

		adaptSpeedRow = SettingRow(view.findViewById(R.id.adapt_speed_row))
		adaptSpeedRow.setIcon(null)
		adaptSpeedRow.setTitle(R.string.eta_adapt_to_my_speed)
		adaptSpeedRow.setOnClickListener {
			val pref = settings().ETA_USE_AVERAGE_SPEED
			pref.setModeValue(appMode, !pref.getModeValue(appMode))
			updateArrivalTime(view)
		}
		intervalRow = SliderListItem(view.findViewById(R.id.interval_row))
		intervalRow.setTitle(R.string.shared_string_interval)

		bindValues(view)
		return view
	}

	override fun onDestroyView() {
		super.onDestroyView()
		if (routingChanged) {
			osmandApp.routingHelper.onSettingsChanged(appMode)
			routingChanged = false
		}
	}

	private fun setupToolbar(view: View) {
		val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
		toolbar.setTitle(R.string.travel_speed)
		toolbar.setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
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

	/** Puts the stored values on the screen - also after a reset, the slider bounds depend on them. */
	private fun bindValues(view: View) {
		config = speedHelper.createSpeedConfig()
		updateDefaultSpeed()
		SegmentedList.apply(view.findViewById(R.id.default_speed_rows))
		bindSpeedRange(view)

		intervalRow.setStops(MEASURED_INTERVALS,
			settings().ETA_AVERAGE_SPEED_INTERVAL.getModeValue(appMode), ::formatInterval) { interval ->
			settings().ETA_AVERAGE_SPEED_INTERVAL.setModeValue(appMode, interval)
			updateArrivalTime(view)
		}
		updateArrivalTime(view)
	}

	/** The default speed can't leave the speed range, so the range is the slider scale. */
	private fun updateDefaultSpeed() {
		val from = if (config.defaultSpeedOnly) config.min.toFloat() else config.minSpeed
		val to = if (config.defaultSpeedOnly) config.max.toFloat() else config.maxSpeed
		defaultSpeedRow.setContinuous(from, to.coerceAtLeast(from + 1), speedStep(),
			config.defaultSpeed, ::formatSpeed) { value ->
			config.defaultSpeed = roundSpeed(value).coerceIn(config.minSpeed, config.maxSpeed)
			appMode.setDefaultSpeed(config.defaultSpeed / config.ratio)
			routingChanged = true
		}
	}

	/** Straight line profiles have no router to limit the speed, so the range is hidden. */
	private fun bindSpeedRange(view: View) {
		val rows: ViewGroup = view.findViewById(R.id.speed_range_rows)
		val visibility = if (config.defaultSpeedOnly) View.GONE else View.VISIBLE
		rows.visibility = visibility
		view.findViewById<View>(R.id.speed_range_footer).visibility = visibility
		if (config.defaultSpeedOnly) {
			return
		}
		speedRangeRow.setRange(config.min.toFloat(), config.max.toFloat(), speedStep(), 1f,
			config.minSpeed, config.maxSpeed, ::formatSpeedRange) { start, end ->
			val minSpeed = roundSpeed(start)
			val maxSpeed = roundSpeed(end)
			if (minSpeed != config.minSpeed) {
				config.minSpeed = minSpeed
				appMode.setMinSpeed(minSpeed / config.ratio)
				routingChanged = true
			}
			if (maxSpeed != config.maxSpeed) {
				config.maxSpeed = maxSpeed
				appMode.setMaxSpeed(maxSpeed / config.ratio)
				routingChanged = true
			}
			val defaultSpeed = config.defaultSpeed.coerceIn(config.minSpeed, config.maxSpeed)
			if (defaultSpeed != config.defaultSpeed) {
				config.defaultSpeed = defaultSpeed
				appMode.setDefaultSpeed(defaultSpeed / config.ratio)
			}
			updateDefaultSpeed()
		}
		SegmentedList.apply(rows)
	}

	private fun updateArrivalTime(view: View) {
		val enabled = settings().ETA_USE_AVERAGE_SPEED.getModeValue(appMode)
		adaptSpeedRow.setChecked(enabled)
		intervalRow.view.visibility = if (enabled) View.VISIBLE else View.GONE
		SegmentedList.apply(view.findViewById(R.id.eta_rows))

		val interval = formatInterval(settings().ETA_AVERAGE_SPEED_INTERVAL.getModeValue(appMode))
		val footer: GroupFooterView = view.findViewById(R.id.eta_footer)
		footer.setText(if (enabled) getString(R.string.eta_adapt_to_my_speed_descr, interval)
			else getString(R.string.eta_adapt_to_my_speed_off_descr))
	}

	private fun speedStep(): Float = if (config.decimalPrecision) 0f else 1f

	private fun roundSpeed(value: Float): Float =
		if (config.decimalPrecision) Math.round(value * 10) / 10f else Math.round(value).toFloat()

	private fun resetToDefault() {
		appMode.resetDefaultSpeed()
		appMode.setMinSpeed(0f)
		appMode.setMaxSpeed(0f)
		settings().ETA_USE_AVERAGE_SPEED.resetModeToDefault(appMode)
		settings().ETA_AVERAGE_SPEED_INTERVAL.resetModeToDefault(appMode)
		routingChanged = true
		view?.let { bindValues(it) }
	}

	private fun settings() = osmandSettings

	private fun formatSpeed(speed: Float): String =
		getString(R.string.ltr_or_rtl_combine_via_space, speedHelper.formatSpeed(config, speed), config.units)

	private fun formatSpeedRange(minSpeed: Float, maxSpeed: Float): String =
		getString(R.string.ltr_or_rtl_combine_via_space,
			getString(R.string.ltr_or_rtl_combine_via_dash,
				speedHelper.formatSpeed(config, minSpeed),
				speedHelper.formatSpeed(config, maxSpeed)),
			config.units)

	private fun formatInterval(millis: Long): String =
		if (millis < 60_000L) {
			getString(R.string.ltr_or_rtl_combine_via_space, (millis / 1000).toString(), getString(R.string.shared_string_sec))
		} else {
			getString(R.string.ltr_or_rtl_combine_via_space, (millis / 60_000L).toString(), getString(R.string.shared_string_minute_lowercase))
		}

	override fun getInsetTargets(): InsetTargetsCollection {
		val collection = InsetTargetsCollection()
		collection.add(InsetTarget.createRootInset())
		collection.add(InsetTarget.createHorizontalLandscape(R.id.toolbar))
		collection.add(InsetTarget.createScrollable(R.id.scroll_view))
		return collection
	}

	companion object {

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
