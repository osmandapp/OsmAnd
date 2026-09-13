package net.osmand.plus.plugins.aistracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.osmand.plus.R
import net.osmand.plus.plugins.aistracker.AisFormatter
import net.osmand.plus.widgets.ui.GroupFooterView

/**
 * Objects visibility screen: when a vessel without a signal is crossed out and when it is removed
 * from the map. Both sliders commit immediately.
 */
class AisObjectsVisibilityFragment : AisBaseFragment() {

	private lateinit var outdatedCard: AisSliderCard<Int>
	private lateinit var hideAfterCard: AisSliderCard<Int>

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		val view = inflater.inflate(R.layout.fragment_ais_objects_visibility, container, false)
		setupToolbar(view, R.string.ais_objects_visibility, R.string.ais_reset_sliders) {
			plugin.AIS_SHIP_LOST_TIMEOUT.resetToDefault()
			plugin.AIS_OBJ_LOST_TIMEOUT.resetToDefault()
			outdatedCard.setValue(plugin.AIS_SHIP_LOST_TIMEOUT.get())
			hideAfterCard.setValue(plugin.AIS_OBJ_LOST_TIMEOUT.get())
		}

		setupHeroImage(view, R.drawable.img_ais_object_visibility_day,
			R.drawable.img_ais_object_visibility_night)

		val hideAfterFooter: GroupFooterView = view.findViewById(R.id.hide_after_footer)
		hideAfterCard = AisSliderCard(
			view.findViewById(R.id.hide_after_card),
			hideAfterFooter,
			HIDE_AFTER_VALUES,
			{ AisFormatter.formatMinutes(osmandApp, it) },
			{ getString(R.string.ais_hide_after_desc, AisFormatter.formatMinutes(osmandApp, it)) },
			{
				plugin.AIS_OBJ_LOST_TIMEOUT.set(it)
				outdatedCard.refreshFooter()
			})
		hideAfterCard.setTitle(R.string.ais_hide_after)

		outdatedCard = AisSliderCard(
			view.findViewById(R.id.outdated_card),
			view.findViewById(R.id.outdated_footer),
			MARK_AS_OUTDATED_VALUES,
			{ if (it == OUTDATED_OFF) getString(R.string.shared_string_off) else AisFormatter.formatMinutes(osmandApp, it) },
			{ outdatedFooterText(it) },
			{ plugin.AIS_SHIP_LOST_TIMEOUT.set(it) })
		outdatedCard.setTitle(R.string.ais_mark_as_outdated)

		outdatedCard.setValue(plugin.AIS_SHIP_LOST_TIMEOUT.get())
		hideAfterCard.setValue(plugin.AIS_OBJ_LOST_TIMEOUT.get())
		return view
	}

	/**
	 * With "Mark as outdated" off the vessel symbol never changes, so the footer has to name the
	 * only timeout left - the one from the other slider.
	 */
	private fun outdatedFooterText(value: Int): CharSequence {
		val hideAfter = AisFormatter.formatMinutes(osmandApp, plugin.AIS_OBJ_LOST_TIMEOUT.get())
		return if (value == OUTDATED_OFF) {
			getString(R.string.ais_mark_as_outdated_off_desc, hideAfter)
		} else {
			getString(R.string.ais_mark_as_outdated_desc,
				AisFormatter.formatMinutes(osmandApp, value))
		}
	}

	companion object {
		/** Bigger than every "Hide after" value, so the symbol never changes before it is hidden. */
		private const val OUTDATED_OFF = 100
		private val MARK_AS_OUTDATED_VALUES = listOf(OUTDATED_OFF, 2, 3, 4, 5, 7, 10, 15)
		private val HIDE_AFTER_VALUES = listOf(3, 5, 7, 10, 12, 15, 20)
	}
}
