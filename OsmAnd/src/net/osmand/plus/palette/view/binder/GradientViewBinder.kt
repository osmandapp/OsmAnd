package net.osmand.plus.palette.view.binder

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.shared.ColorPalette.ColorValue
import net.osmand.shared.palette.domain.PaletteItem

class GradientViewBinder(
	val context: Context,
	val nightMode: Boolean
) : PaletteItemViewBinder {

	private val app = context.applicationContext as OsmandApplication
	private val themedInflater = UiUtilities.getInflater(context, nightMode)

	override fun createView(parent: ViewGroup): View {
		return createRectangleView(parent)
	}

	override fun bindView(itemView: View, item: PaletteItem, selected: Boolean) {
		if (item is PaletteItem.Gradient) {
			updateColorItemView(itemView, item, selected)
		}
	}

	private fun updateColorItemView(view: View, gradient: PaletteItem.Gradient, showOutline: Boolean) {
		val icon = view.findViewById<ImageView>(R.id.icon)
		val background = view.findViewById<AppCompatImageView>(R.id.background)
		val outline = view.findViewById<AppCompatImageView>(R.id.outline)

		val colors: List<ColorValue> = gradient.getColorPalette().colors
		background.setImageDrawable(createGradientDrawable(app, colors, GradientDrawable.RECTANGLE))

		if (showOutline) {
			val border = getPaintedIcon(
				R.drawable.bg_point_square_contour,
				ColorUtilities.getActiveIconColor(app, nightMode)
			)
			outline.setImageDrawable(border)
			outline.visibility = View.VISIBLE
		} else {
			outline.visibility = View.INVISIBLE
			val defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode)
			icon.setImageDrawable(UiUtilities.tintDrawable(icon.drawable, defaultColor))
		}
	}

	private fun createRectangleView(view: ViewGroup): View {
		return themedInflater.inflate(R.layout.point_editor_button, view, false)
	}

	private fun getPaintedIcon(@DrawableRes id: Int, @ColorInt color: Int): Drawable? {
		return app.uiUtilities.getPaintedIcon(id, color)
	}

	companion object {

		@JvmStatic
		fun createGradientDrawable(
			app: OsmandApplication,
			values: List<ColorValue>,
			shape: Int
		): GradientDrawable {
			val colors = IntArray(values.size)
			for (i in values.indices) {
				val value = values[i]
				colors[i] = Color.argb(value.a, value.r, value.g, value.b)
			}
			val drawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
			drawable.gradientType = GradientDrawable.LINEAR_GRADIENT
			drawable.shape = shape
			if (shape == GradientDrawable.RECTANGLE) {
				drawable.cornerRadius = AndroidUtils.dpToPx(app, 2f).toFloat()
			}
			return drawable
		}
	}
}