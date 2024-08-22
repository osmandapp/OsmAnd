package net.osmand.shared.filters

import net.osmand.plus.card.width.WidthMode
import net.osmand.plus.myplaces.tracks.filters.SingleFieldTrackFilterParams
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil

class WidthSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

//	override fun getItemIcon(app: OsmandApplication, itemName: String): Drawable? {
//		return if (Algorithms.isEmpty(itemName)) {
//			app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled)
//		} else {
//			val iconColor = when (itemName) {
//				WidthMode.THIN.key,
//				WidthMode.MEDIUM.key,
//				WidthMode.BOLD.key -> R.color.track_filter_width_standard
//
//				else -> {
//					R.color.track_filter_width_custom
//				}
//			}
//			TrackAppearanceFragment.getTrackIcon(
//				app,
//				itemName,
//				false,
//				app.getColor(iconColor))
//		}
//	}

	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			PlatformUtil.getStringResource("not_specified")
		} else {
			when (itemName) {
				WidthMode.THIN.key -> PlatformUtil.getStringResource("rendering_value_thin_name")
				WidthMode.MEDIUM.key -> PlatformUtil.getStringResource("rendering_value_medium_name")
				WidthMode.BOLD.key -> PlatformUtil.getStringResource("rendering_value_bold_name")
				else -> {
					"${PlatformUtil.getStringResource("shared_string_custom")}: $itemName"
				}

			}
		}
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}