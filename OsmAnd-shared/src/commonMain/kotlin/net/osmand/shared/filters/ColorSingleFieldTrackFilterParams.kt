package net.osmand.shared.filters

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil

class ColorSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

//	override fun getItemIcon(app: OsmandApplication, itemName: String): Drawable? {
//		return if (Algorithms.isEmpty(itemName)) {
//			app.uiUtilities.getThemedIcon(R.drawable.ic_action_appearance_disabled)
//		} else {
//			val color = Color.parseColor(itemName)
//			val transparencyIcon = getTransparencyIcon(app, color)
//			val colorIcon = app.uiUtilities.getPaintedIcon(R.drawable.bg_point_circle, color)
//			UiUtilities.getLayeredIcon(transparencyIcon, colorIcon)
//		}
//	}
//
//	private fun getTransparencyIcon(app: OsmandApplication, @ColorInt color: Int): Drawable? {
//		val colorWithoutAlpha = ColorUtilities.removeAlpha(color)
//		val transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f)
//		return app.uiUtilities.getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor)
//	}

	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			PlatformUtil.getStringResource("not_specified")
		} else {
			itemName
		}
	}

	override fun trackParamToString(trackParam: Any): String {
		return KAlgorithms.colorToString(trackParam as Int)
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}