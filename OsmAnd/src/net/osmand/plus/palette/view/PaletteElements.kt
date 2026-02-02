package net.osmand.plus.palette.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities

class PaletteElements(context: Context, private val nightMode: Boolean) {

	private val app: OsmandApplication = context.applicationContext as OsmandApplication
	private val themedInflater: LayoutInflater = UiUtilities.getInflater(context, nightMode)

	fun updateColorItemView(view: View, color: Int, showOutline: Boolean) {
		val icon = view.findViewById<ImageView>(R.id.icon)
		val background = view.findViewById<AppCompatImageView>(R.id.background)
		val outline = view.findViewById<AppCompatImageView>(R.id.outline)

		val transparencyIcon = getTransparencyIcon(color)
		val colorIcon = app.uiUtilities.getPaintedIcon(R.drawable.bg_point_circle, color)
		val layeredIcon = UiUtilities.getLayeredIcon(transparencyIcon, colorIcon)
		background.setImageDrawable(layeredIcon)

		if (showOutline) {
			val border = getPaintedIcon(
				R.drawable.bg_point_circle_contour,
				ColorUtilities.getActiveIconColor(app, nightMode)
			)
			outline.setImageDrawable(border)
			outline.visibility = View.VISIBLE
		} else {
			outline.visibility = View.INVISIBLE
			icon.setImageDrawable(
				UiUtilities.tintDrawable(
					icon.drawable, ColorUtilities.getDefaultIconColor(app, nightMode)
				)
			)
		}
	}

	fun createAddButtonView(rootView: ViewGroup): View {
		val itemView = createCircleView(rootView)
		val icon = itemView.findViewById<ImageView>(R.id.icon)
		val outline = itemView.findViewById<View>(R.id.outline)
		val background = itemView.findViewById<ImageView>(R.id.background)

		val bgColorId = ColorUtilities.getActivityBgColorId(nightMode)
		val backgroundIcon = getIcon(R.drawable.bg_point_circle, bgColorId)
		background.setImageDrawable(backgroundIcon)

		val activeColorResId = if (nightMode) R.color.icon_color_active_dark else R.color.icon_color_active_light
		icon.setImageDrawable(getIcon(R.drawable.ic_action_plus, activeColorResId))
		icon.visibility = View.VISIBLE
		outline.visibility = View.INVISIBLE
		return itemView
	}

	fun createCircleView(rootView: ViewGroup): View {
		return themedInflater.inflate(R.layout.point_editor_button, rootView, false)
	}

	private fun getTransparencyIcon(@ColorInt color: Int): Drawable? {
		val colorWithoutAlpha = ColorUtilities.removeAlpha(color)
		val transparencyColor = ColorUtilities.getColorWithAlpha(colorWithoutAlpha, 0.8f)
		return getPaintedIcon(R.drawable.ic_bg_transparency, transparencyColor)
	}

	private fun getPaintedIcon(@DrawableRes id: Int, @ColorInt color: Int): Drawable? {
		return app.uiUtilities.getPaintedIcon(id, color)
	}

	fun getIcon(@DrawableRes id: Int, @ColorRes colorId: Int): Drawable? {
		return app.uiUtilities.getIcon(id, colorId)
	}

	companion object {
		const val MINIMUM_CONTRAST_RATIO = 1.5
	}
}