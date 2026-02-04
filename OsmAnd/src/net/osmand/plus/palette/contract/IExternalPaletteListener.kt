package net.osmand.plus.palette.contract

import net.osmand.shared.palette.domain.PaletteItem

interface IExternalPaletteListener {

	fun onPaletteItemSelected(item: PaletteItem)

	fun onPaletteItemAdded(oldItem: PaletteItem?, newItem: PaletteItem) {
	}
}