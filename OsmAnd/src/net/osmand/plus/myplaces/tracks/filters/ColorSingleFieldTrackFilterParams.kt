package net.osmand.plus.myplaces.tracks.filters

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.util.Algorithms

class ColorSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

	override fun getItemIcon(app: OsmandApplication, itemName: String): Drawable? {
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

	override fun getItemText(app: OsmandApplication, itemName: String): String {
		return if (Algorithms.isEmpty(itemName)) {
			app.getString(R.string.not_specified)
		} else {
			itemName
		}
	}

	override fun trackParamToString(trackParam: Any): String {
		return Algorithms.colorToString(trackParam as Int)
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}