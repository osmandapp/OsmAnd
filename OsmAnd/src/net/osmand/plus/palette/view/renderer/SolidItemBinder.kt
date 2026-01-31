package net.osmand.plus.palette.view.renderer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import net.osmand.plus.card.color.palette.main.ColorsPaletteElements
import net.osmand.shared.palette.domain.PaletteItem

class SolidItemBinder(
	val context: Context,
	val nightMode: Boolean
) : PaletteItemBinder {

	private val paletteElements = ColorsPaletteElements(context, nightMode)

	override fun createView(parent: ViewGroup): View {
		return paletteElements.createCircleView(parent)
	}

	override fun bindView(itemView: View, item: PaletteItem, selected: Boolean) {
		if (item is PaletteItem.Solid) {
			paletteElements.updateColorItemView(itemView, item.color, selected)
		}
	}
}