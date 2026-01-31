package net.osmand.plus.palette.view.binder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import net.osmand.plus.card.color.palette.gradient.GradientUiHelper
import net.osmand.shared.palette.domain.PaletteItem

class GradientViewBinder(
	val context: Context,
	val nightMode: Boolean
) : PaletteItemViewBinder {

	private val gradientUiHelper = GradientUiHelper(context, nightMode)

	override fun createView(parent: ViewGroup): View {
		return gradientUiHelper.createRectangleView(parent)
	}

	override fun bindView(itemView: View, item: PaletteItem, selected: Boolean) {
		if (item is PaletteItem.Gradient) {
			gradientUiHelper.updateColorItemView(itemView, item, selected)
		}
	}
}