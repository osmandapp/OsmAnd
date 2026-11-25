package net.osmand.plus.settings.bottomsheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.card.MaterialCardView
import net.osmand.plus.R
import net.osmand.plus.base.BaseMaterialBottomSheetWithHeader
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.settings.fragments.BaseSettingsFragment
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.ui.MainSwitchView
import net.osmand.plus.widgets.ui.SegmentedList
import net.osmand.plus.widgets.ui.SettingRow

/**
 * Fullscreen mode of a profile: whether a tap on the map hides the buttons and widgets,
 * and which system bars are hidden together with them.
 */
class FullscreenModeBottomSheet : BaseMaterialBottomSheetWithHeader() {

	private lateinit var targetAppMode: ApplicationMode
	private val initialValues = LinkedHashMap<String, Boolean>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		targetAppMode = ApplicationMode.valueOfStringKey(arguments?.getString(ARG_APP_MODE_KEY), currentAppMode)
			?: currentAppMode
		for (pref in listOf(osmandSettings.MAP_EMPTY_STATE_ALLOWED, osmandSettings.FULLSCREEN_HIDE_STATUS_BAR,
				osmandSettings.FULLSCREEN_HIDE_NAVIGATION_BAR)) {
			initialValues[pref.id] = savedInstanceState?.takeIf { it.containsKey(pref.id) }
				?.getBoolean(pref.id) ?: pref.getModeValue(targetAppMode)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		initialValues.forEach { (id, value) -> outState.putBoolean(id, value) }
	}

	override fun onDismiss(dialog: DialogInterface) {
		super.onDismiss(dialog)
		if (activity?.isChangingConfigurations == true) return
		val changes = HashMap<String, Boolean>()
		initialValues.forEach { (id, initial) ->
			val current = osmandSettings.getPreference(id)?.getModeValue(targetAppMode) as? Boolean
			if (current != null && current != initial) {
				changes[id] = current
			}
		}
		if (changes.isNotEmpty()) {
			@Suppress("DEPRECATION")
			(targetFragment as? BaseSettingsFragment)?.showApplyToAllProfilesSnackBar(changes)
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		// segmented rows are colorSurfaceContainer, so the sheet takes the screen surface under them
		mainView.findViewById<MaterialCardView>(R.id.bottomSheetCard)
			.setCardBackgroundColor(ColorUtilities.getColor(requireContext(), surfaceColorId()))
		mainView.findViewById<TextView>(R.id.title).setText(R.string.tap_on_map_to_hide_interface)
		// the dialog inflater carries the Material 3 theme the ui components need
		inflater.inflate(R.layout.fullscreen_mode_bottom_sheet_content, mainView.findViewById(R.id.itemsContainer), true)
		setupContent()
		return mainView
	}

	private fun surfaceColorId(): Int =
		if (nightMode) R.color.surface_dark else R.color.surface_light

	private fun setupContent() {
		val enabledPref = osmandSettings.MAP_EMPTY_STATE_ALLOWED
		val enabledContent = mainView.findViewById<View>(R.id.enabled_content)
		val mainSwitch = mainView.findViewById<MainSwitchView>(R.id.main_switch)
		mainSwitch.setChecked(enabledPref.getModeValue(targetAppMode), false)
		enabledContent.isVisible = mainSwitch.isChecked()
		mainSwitch.setOnCheckedChangeListener { checked ->
			enabledPref.setModeValue(targetAppMode, checked)
			enabledContent.isVisible = checked
			notifyTarget(enabledPref)
		}
		setupBarRow(R.id.status_bar_row, osmandSettings.FULLSCREEN_HIDE_STATUS_BAR,
			R.string.fullscreen_mode_status_bar, R.string.fullscreen_mode_status_bar_descr)
		setupBarRow(R.id.navigation_bar_row, osmandSettings.FULLSCREEN_HIDE_NAVIGATION_BAR,
			R.string.fullscreen_mode_navigation_bar, R.string.fullscreen_mode_navigation_bar_descr)
		SegmentedList.apply(mainView.findViewById<LinearLayout>(R.id.bars_group))
	}

	private fun setupBarRow(rowId: Int, pref: OsmandPreference<Boolean>, titleId: Int, descrId: Int) {
		val row = SettingRow(mainView.findViewById(rowId))
		row.setIcon(null)
		row.setTitle(titleId)
		row.setSubtitle(getString(descrId))
		row.setChecked(pref.getModeValue(targetAppMode))
		row.setOnClickListener {
			val checked = !pref.getModeValue(targetAppMode)
			pref.setModeValue(targetAppMode, checked)
			row.setChecked(checked)
			notifyTarget(pref)
		}
	}

	private fun notifyTarget(pref: OsmandPreference<*>) {
		@Suppress("DEPRECATION")
		(targetFragment as? BaseSettingsFragment)?.updateSetting(pref.id)
	}

	companion object {
		val TAG: String = FullscreenModeBottomSheet::class.java.simpleName

		private const val ARG_APP_MODE_KEY = "app_mode_key"

		@JvmStatic
		fun showInstance(manager: FragmentManager, appMode: ApplicationMode, target: Fragment?) {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = FullscreenModeBottomSheet()
				fragment.arguments = Bundle().apply { putString(ARG_APP_MODE_KEY, appMode.stringKey) }
				@Suppress("DEPRECATION")
				fragment.setTargetFragment(target, 0)
				fragment.show(manager, TAG)
			}
		}
	}
}
