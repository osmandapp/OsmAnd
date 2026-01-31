package net.osmand.plus.palette.contract

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController
import net.osmand.plus.card.color.palette.main.v2.OnColorsPaletteListener
import net.osmand.plus.palette.view.binder.PaletteItemViewBinder
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

/**
 * Interface for Logic and Data Management.
 * Used by the Container (Activity/Fragment) to control the Palette.
 */
interface IPaletteController : IDialogController {

	// --- Lifecycle & View Management ---

	fun attachView(view: IPaletteView)

	fun detachView(view: IPaletteView)

	/**
	 * Called when the 'All items' full-screen dialog is closed.
	 */
	fun onAllColorsScreenClosed() {}

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

	/**
	 * Factory method for UI representation strategy.
	 */
	fun getItemBinder(activity: FragmentActivity, nightMode: Boolean): PaletteItemViewBinder

	fun getControlsAccentColor(nightMode: Boolean): Int

	fun isAccentColorCanBeChanged(): Boolean

	// --- Listeners ---

	fun setPaletteListener(listener: OnColorsPaletteListener?)
}