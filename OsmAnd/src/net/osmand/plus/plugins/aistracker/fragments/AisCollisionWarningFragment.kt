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
	private lateinit var tcpaCard: AisSliderCard<Int>
	private lateinit var cpaCard: AisSliderCard<Float>

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_ais_collision_warning, container, false)
		setupToolbar(view, R.string.ais_collision_warning, R.string.reset_to_default) {
			plugin.AIS_CPA_WARNING_TIME.resetModeToDefault(appMode)
			plugin.AIS_CPA_WARNING_DISTANCE.resetModeToDefault(appMode)
			tcpaCard.setValue(plugin.AIS_CPA_WARNING_TIME.getModeValue(appMode))
			cpaCard.setValue(plugin.AIS_CPA_WARNING_DISTANCE.getModeValue(appMode))
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

		tcpaCard = AisSliderCard(
			view.findViewById(R.id.tcpa_card),
			view.findViewById(R.id.tcpa_footer),
			WARNING_TIME_VALUES,
			{ AisFormatter.formatMinutes(osmandApp, it) },
			{ getString(R.string.ais_cpa_warning_time_desc, AisFormatter.formatMinutes(osmandApp, it)) },
			{
				plugin.AIS_CPA_WARNING_TIME.setModeValue(appMode, it)
				updateMainSwitchFooter()
			})
		tcpaCard.setTitle(R.string.ais_cpa_warning_time_title)

		cpaCard = AisSliderCard(
			view.findViewById(R.id.cpa_card),
			view.findViewById(R.id.cpa_footer),
			SAFE_DISTANCE_VALUES,
			{ AisFormatter.formatNauticalMiles(osmandApp, it, appMode) },
			{ getString(R.string.ais_cpa_safe_distance_desc, AisFormatter.formatNauticalMiles(osmandApp, it, appMode)) },
			{
				plugin.AIS_CPA_WARNING_DISTANCE.setModeValue(appMode, it)
				updateMainSwitchFooter()
			})
		cpaCard.setTitle(R.string.ais_cpa_safe_distance)

		tcpaCard.setValue(plugin.AIS_CPA_WARNING_TIME.getModeValue(appMode))
		cpaCard.setValue(plugin.AIS_CPA_WARNING_DISTANCE.getModeValue(appMode))
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
		(tcpaCard.view.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
			if (bannerShown) resources.getDimensionPixelSize(R.dimen.ui_group_gap) else 0
		tcpaCard.view.requestLayout()
		updateMainSwitchFooter()
	}

	private fun updateMainSwitchFooter() {
		mainSwitchFooter.setText(
			if (plugin.AIS_CPA_ENABLED.getModeValue(appMode)) {
				getString(R.string.ais_cpa_on_desc,
					AisFormatter.formatNauticalMiles(osmandApp, plugin.AIS_CPA_WARNING_DISTANCE.getModeValue(appMode), appMode),
					AisFormatter.formatMinutes(osmandApp, plugin.AIS_CPA_WARNING_TIME.getModeValue(appMode)))
			} else {
				getString(R.string.ais_cpa_off_desc)
			})
	}

	companion object {
		private val WARNING_TIME_VALUES = listOf(1, 5, 10, 20, 30, 60)
		private val SAFE_DISTANCE_VALUES = listOf(0.02f, 0.05f, 0.1f, 0.2f, 0.5f, 1.0f, 2.0f)
	}
}
