package net.osmand.plus.palette.contract

import androidx.fragment.app.FragmentActivity
import net.osmand.shared.palette.domain.PaletteItem

interface IPaletteView {
	fun updatePaletteItems(targetItem: PaletteItem?)
	fun updatePaletteSelection(oldItem: PaletteItem?, newItem: PaletteItem, selectionDone: Boolean)
	fun askScrollToPaletteItemPosition(targetItem: PaletteItem?, smoothScroll: Boolean = false)

	fun isNightMode(): Boolean
	fun getActivity(): FragmentActivity?
}