package net.osmand.plus.plugins.aistracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import net.osmand.plus.R
import net.osmand.plus.plugins.aistracker.AisFormatter
import net.osmand.plus.widgets.ui.GroupFooterView
import net.osmand.plus.widgets.ui.MainSwitchView
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SliderListItem

/**
 * Collision warning (CPA) screen.
 *
 * With the main switch off the content below its footer is hidden rather than disabled - a screen
 * full of greyed out sliders gives the user nothing to act on.
 */
class AisCollisionWarningFragment : AisBaseFragment() {

	private lateinit var mainSwitch: MainSwitchView
	private lateinit var mainSwitchFooter: GroupFooterView
	private lateinit var enabledContent: View
	private lateinit var mmsiBanner: View
	private lateinit var tcpaRows: View
	private lateinit var tcpaRow: SliderListItem
	private lateinit var cpaRow: SliderListItem
	private lateinit var tcpaFooter: GroupFooterView
	private lateinit var cpaFooter: GroupFooterView

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_ais_collision_warning, container, false)
		setupToolbar(view, R.string.ais_collision_warning)
		addResetAction(view) {
			plugin.AIS_CPA_WARNING_TIME.resetModeToDefault(appMode)
			plugin.AIS_CPA_WARNING_DISTANCE.resetModeToDefault(appMode)
			bindSliders()
			updateState()
		}

		setupHeroImage(view, R.drawable.img_ais_cpa_day, R.drawable.img_ais_cpa_night)

		mainSwitch = view.findViewById(R.id.main_switch)
		mainSwitchFooter = view.findViewById(R.id.main_switch_footer)
		enabledContent = view.findViewById(R.id.enabled_content)
		mmsiBanner = view.findViewById(R.id.mmsi_banner)

		mainSwitch.setLabel(getString(R.string.ais_collision_warning_short), false)
		mainSwitch.setChecked(plugin.AIS_CPA_ENABLED.getModeValue(appMode), false)
		mainSwitch.setOnCheckedChangeListener { checked ->
			plugin.AIS_CPA_ENABLED.setModeValue(appMode, checked)
			updateState()
		}

		mmsiBanner.findViewById<MaterialButton>(R.id.set_mmsi_button).setOnClickListener {
			AisMmsiDialog.show(this) { updateState() }
		}

		tcpaRows = view.findViewById(R.id.tcpa_rows)
		tcpaRow = SliderListItem(view.findViewById(R.id.tcpa_row))
		tcpaRow.setTitle(R.string.ais_cpa_warning_time_title)
		tcpaRow.setTickVisible(true)
		tcpaFooter = view.findViewById(R.id.tcpa_footer)
		SegmentedList.apply(tcpaRows as ViewGroup)

		cpaRow = SliderListItem(view.findViewById(R.id.cpa_row))
		cpaRow.setTitle(R.string.ais_cpa_safe_distance)
		cpaRow.setTickVisible(true)
		cpaFooter = view.findViewById(R.id.cpa_footer)
		SegmentedList.apply(view.findViewById(R.id.cpa_rows))

		bindSliders()
		updateState()
		return view
	}

	private fun updateState() {
		val enabled = plugin.AIS_CPA_ENABLED.getModeValue(appMode)
		enabledContent.visibility = if (enabled) View.VISIBLE else View.GONE
		val bannerShown = plugin.AIS_OWN_MMSI.getModeValue(appMode) == 0
		mmsiBanner.visibility = if (bannerShown) View.VISIBLE else View.GONE
		/* the footer of the main switch already keeps 16dp to the next card; the gap of the
		 * first card is only needed below the banner */
		(tcpaRows.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
			if (bannerShown) resources.getDimensionPixelSize(R.dimen.ui_group_gap) else 0
		tcpaRows.requestLayout()
		updateFooters()
	}

	/** Puts the stored values on the sliders - also after a reset. */
	private fun bindSliders() {
		tcpaRow.setStops(WARNING_TIME_VALUES, plugin.AIS_CPA_WARNING_TIME.getModeValue(appMode),
			{ AisFormatter.formatMinutes(osmandApp, it) }) {
			plugin.AIS_CPA_WARNING_TIME.setModeValue(appMode, it)
			updateFooters()
		}
		cpaRow.setStops(SAFE_DISTANCE_VALUES, plugin.AIS_CPA_WARNING_DISTANCE.getModeValue(appMode),
			{ AisFormatter.formatNauticalMiles(osmandApp, it, appMode) }) {
			plugin.AIS_CPA_WARNING_DISTANCE.setModeValue(appMode, it)
			updateFooters()
		}
	}

	private fun updateFooters() {
		val time = AisFormatter.formatMinutes(osmandApp, plugin.AIS_CPA_WARNING_TIME.getModeValue(appMode))
		val distance = AisFormatter.formatNauticalMiles(osmandApp, plugin.AIS_CPA_WARNING_DISTANCE.getModeValue(appMode), appMode)
		tcpaFooter.setText(getString(R.string.ais_cpa_warning_time_desc, time))
		cpaFooter.setText(getString(R.string.ais_cpa_safe_distance_desc, distance))
		mainSwitchFooter.setText(
			if (plugin.AIS_CPA_ENABLED.getModeValue(appMode)) {
				getString(R.string.ais_cpa_on_desc, distance, time)
			} else {
				getString(R.string.ais_cpa_off_desc)
			})
	}

	companion object {
		private val WARNING_TIME_VALUES = listOf(1, 5, 10, 20, 30, 60)
		private val SAFE_DISTANCE_VALUES = listOf(0.02f, 0.05f, 0.1f, 0.2f, 0.5f, 1.0f, 2.0f)
	}
}
