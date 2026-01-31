package net.osmand.plus.palette.contract

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController
import net.osmand.plus.card.color.palette.main.v2.OnColorsPaletteListener
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

/**
 * Interface for Logic and Data Management.
 * Used by the Container (Activity/Fragment) to control the Palette.
 */
interface IPaletteController : IDialogController {

	companion object {
		const val ALL_PALETTE_ITEMS_PROCESS_ID = "show_all_palette_items_screen"
	}

	// --- Lifecycle & View Management ---

	fun attachView(view: IPaletteView)

	fun detachView(view: IPaletteView)

	fun onPaletteScreenClosed() {}


	// --- External Control (Commands from outside) ---

	/**
	 * Programmatically select an item (e.g. "Reset to default").
	 */
	fun selectPaletteItem(item: PaletteItem?)

	fun renewLastUsedTime()


	// --- Data & Configuration Provider (Used by View/Adapter) ---

	fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem>

	fun isPaletteItemSelected(item: PaletteItem): Boolean

	fun getSelectedPaletteItem(): PaletteItem?

	fun isAddingNewItemsSupported(): Boolean

	fun getControlsAccentColor(nightMode: Boolean): Int

	fun isAccentColorCanBeChanged(): Boolean


	// --- Listeners ---

	fun setPaletteListener(listener: OnColorsPaletteListener?)
}