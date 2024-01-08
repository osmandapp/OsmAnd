package net.osmand.plus.myplaces.tracks.filters

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.COLOR
import net.osmand.plus.track.helpers.GpxParameter
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.util.Algorithms

class ColorTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener?) :
	ListTrackFilter(app, R.string.shared_string_color, COLOR, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedItems)
	}

	@Expose
	private var selectedColors = ArrayList<String>()

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (color in selectedItems) {
			val trackItemColor: Int? = trackItem.dataItem?.getParameter(GpxParameter.COLOR)
			if (trackItemColor == 0 && Algorithms.isEmpty(color) ||
				!Algorithms.isEmpty(color) && trackItemColor == Color.parseColor(color)) {
				return true
			}
		}
		return false
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is ColorTrackFilter &&
				other.selectedColors.size == selectedColors.size &&
				areAllItemsSelected(other.selectedColors)
	}

	override fun getItemText(itemName: String): String {
		return if (Algorithms.isEmpty(itemName)) {
			app.getString(R.string.not_specified)
		} else {
			itemName
		}
	}

	override fun getItemIcon(itemName: String): Drawable? {
		return if (Algorithms.isEmpty(itemName)) {
			app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled)
		} else {
			val color = Color.parseColor(itemName)
			val transparencyIcon = getTransparencyIcon(app, color)
			val colorIcon = app.uiUtilities.getPaintedIcon(R.drawable.bg_point_circle, color)
			UiUtilities.getLayeredIcon(transparencyIcon, colorIcon)
		}
	}

	private fun getTransparencyIcon(app: OsmandApplication, @ColorInt color: Int): Drawable? {
		val colorWithoutAlpha = ColorUtilities.removeAlpha(color)
		val transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f)
		return app.uiUtilities.getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor)
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is ColorTrackFilter) {
			if (!Algorithms.isEmpty(value.selectedColors) || Algorithms.isEmpty(value.selectedItems)) {
				value.selectedItems = ArrayList(value.selectedColors)
			}
		}
		super.initWithValue(value)
	}
}