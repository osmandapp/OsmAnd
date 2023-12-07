package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.WIDTH
import net.osmand.plus.routing.cards.RouteLineWidthCard
import net.osmand.plus.track.fragments.TrackAppearanceFragment
import net.osmand.plus.track.helpers.GpxParameter
import net.osmand.util.Algorithms

class WidthTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener?) :
	ListTrackFilter(app, R.string.shared_string_width, WIDTH, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedItems)
	}

	@Expose
	private var selectedWidths = ArrayList<String>()

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (width in selectedItems) {
			val trackItemWidth: String? = trackItem.dataItem?.getParameter(GpxParameter.WIDTH)
			if (Algorithms.stringsEqual(trackItemWidth, width) ||
				Algorithms.isEmpty(trackItemWidth) && Algorithms.isEmpty(width)) {
				return true
			}
		}
		return false
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is WidthTrackFilter &&
				other.selectedItems.size == selectedItems.size &&
				areAllItemsSelected(other.selectedItems)
	}

	override fun getItemText(itemName: String): String {
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

	override fun getItemIcon(itemName: String): Drawable? {
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

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is WidthTrackFilter) {
			if (!Algorithms.isEmpty(value.selectedWidths) || Algorithms.isEmpty(value.selectedItems)) {
				value.selectedItems = ArrayList(value.selectedWidths)
			}
		}
		super.initWithValue(value)
	}
}