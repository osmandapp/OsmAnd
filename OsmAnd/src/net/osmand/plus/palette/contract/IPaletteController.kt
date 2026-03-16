package net.osmand.plus.palette.contract

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController
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
	 * Selects the specified [item] as the active element and notifies listeners.
	 * This method should be used for standard interactions where the selection change
	 * needs to be propagated to the rest of the application (e.g., saving settings or updating the map).
	 */
	fun selectPaletteItem(item: PaletteItem?)

	/**
	 * Selects the specified [item] without triggering the main selection callback.
	 * * This is useful for synchronizing the UI with an external state (source of truth)
	 * to update the visual selection (highlight) without causing circular update loops
	 * or re-triggering logic that has already been executed.
	 */
	fun selectPaletteItemSilently(item: PaletteItem?)

	fun scrollToPaletteItem(item: PaletteItem?, smoothScroll: Boolean)

	fun renewLastUsedTime()


	// --- Data & Configuration Provider (Used by View/Adapter) ---

	fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem>

	fun isPaletteItemSelected(item: PaletteItem): Boolean

	fun getSelectedPaletteItem(): PaletteItem?

	fun isAddingNewItemsSupported(): Boolean

	fun isAutoScrollSupported(): Boolean

	fun getControlsAccentColor(nightMode: Boolean): Int

	fun isAccentColorCanBeChanged(): Boolean

	/**
	 * Returns a stable unique Long ID for the given String item ID.
	 * Required for RecyclerView adapters with setHasStableIds(true).
	 */
	fun getStableId(itemId: String): Long


	// --- Listeners ---

	fun setPaletteListener(listener: IExternalPaletteListener?)
}