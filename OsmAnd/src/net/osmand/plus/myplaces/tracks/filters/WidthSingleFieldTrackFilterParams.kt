package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.routing.cards.RouteLineWidthCard
import net.osmand.plus.track.fragments.TrackAppearanceFragment
import net.osmand.util.Algorithms

class WidthSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

	override fun getItemIcon(app: OsmandApplication, itemName: String): Drawable? {
		return if (Algorithms.isEmpty(itemName)) {
			app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled)
		} else {
			val iconColor = when (itemName) {
				RouteLineWidthCard.WidthMode.THICK.widthKey,
				RouteLineWidthCard.WidthMode.THIN.widthKey,
				RouteLineWidthCard.WidthMode.MEDIUM.widthKey -> R.color.track_filter_width_standard

				else -> {
					R.color.track_filter_width_custom
				}
			}
			TrackAppearanceFragment.getTrackIcon(
				app,
				itemName,
				false,
				app.getColor(iconColor))
		}
	}

	override fun getItemText(app: OsmandApplication, itemName: String): String {
		return if (Algorithms.isEmpty(itemName)) {
			app.getString(R.string.not_specified)
		} else {
			when (itemName) {
				RouteLineWidthCard.WidthMode.THICK.widthKey -> app.getString(R.string.rendering_value_bold_name)
				RouteLineWidthCard.WidthMode.THIN.widthKey -> app.getString(R.string.rendering_value_thin_name)
				RouteLineWidthCard.WidthMode.MEDIUM.widthKey -> app.getString(R.string.rendering_value_medium_name)
				else -> {
					"${app.getString(R.string.shared_string_custom)}: $itemName"
				}
			}
		}
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}