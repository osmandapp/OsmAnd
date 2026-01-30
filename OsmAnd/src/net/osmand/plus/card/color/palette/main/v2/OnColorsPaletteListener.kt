package net.osmand.plus.card.color.palette.main.v2

import net.osmand.shared.palette.domain.PaletteItem

interface OnColorsPaletteListener {

	fun onPaletteItemSelected(item: PaletteItem)

	fun onPaletteItemAdded(oldItem: PaletteItem?, newItem: PaletteItem) {
	}
}