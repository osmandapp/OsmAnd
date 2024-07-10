package net.osmand.plus.views.controls.maphudbuttons

import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.base.containers.ThemedIconId

class CustomMapButton(
	mapActivity: MapActivity,
	view: ImageView,
	id: String,
	@DrawableRes nightIconRes: Int,
	@DrawableRes dayIconRes: Int,
	@ColorRes nightColor: Int,
	@ColorRes dayColor: Int
) : MapButton(mapActivity, view, id, true) {
	init {
		setIconId(ThemedIconId(dayIconRes, nightIconRes))
		setIconColorId(dayColor, nightColor);
		setRoundTransparentBackground()
		updateIcon(app.daynightHelper.isNightModeForMapControls)
	}

	override fun shouldShow(): Boolean {
		return true
	}
}