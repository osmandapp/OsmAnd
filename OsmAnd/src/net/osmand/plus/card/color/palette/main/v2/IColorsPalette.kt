package net.osmand.plus.card.color.palette.main.v2

import net.osmand.shared.palette.domain.PaletteItem

// TODO: IPaletteView
interface IColorsPalette {
	fun updatePaletteItems(targetItem: PaletteItem?)
	fun updatePaletteSelection(oldItem: PaletteItem?, newItem: PaletteItem)
}