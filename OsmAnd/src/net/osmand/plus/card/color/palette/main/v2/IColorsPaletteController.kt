package net.osmand.plus.card.color.palette.main.v2

import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController
import net.osmand.plus.palette.view.renderer.PaletteItemBinder
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.domain.PaletteItem

interface IColorsPaletteController : IDialogController,
	ColorPickerDialogController.ColorPickerListener {

	companion object {
		const val ALL_COLORS_PROCESS_ID = "show_all_colors_screen"
	}

	fun bindPalette(palette: IColorsPalette)
	fun unbindPalette(palette: IColorsPalette)
	fun onAllColorsScreenClosed() {}

	fun setPaletteListener(listener: OnColorsPaletteListener?)

	fun getControlsAccentColor(nightMode: Boolean): Int
	fun isAccentColorCanBeChanged(): Boolean

	fun onSelectItemFromPalette(item: PaletteItem, renewLastUsedTime: Boolean)

	fun selectPaletteItem(item: PaletteItem?)
	fun selectPaletteItem(color: Int, addIfNoFound: Boolean)

	// --- Adapter interaction ---

	fun onPaletteItemLongClick(activity: FragmentActivity, view: View, item: PaletteItem, nightMode: Boolean)

	fun onAllColorsButtonClicked(activity: FragmentActivity)
	fun onAddColorButtonClicked(activity: FragmentActivity)

	fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem>
	fun getSelectedPaletteItem(): PaletteItem?
	fun isPaletteItemSelected(item: PaletteItem): Boolean

	fun refreshLastUsedTime()

	fun getItemBinder(
		activity: FragmentActivity,
		nightMode: Boolean
	): PaletteItemBinder
}