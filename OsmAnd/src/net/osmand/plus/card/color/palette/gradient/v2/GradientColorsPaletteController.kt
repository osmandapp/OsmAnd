package net.osmand.plus.card.color.palette.gradient.v2

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.AllGradientsPaletteFragment
import net.osmand.plus.card.color.palette.main.v2.IColorsPalette
import net.osmand.plus.card.color.palette.main.v2.IColorsPaletteController
import net.osmand.plus.card.color.palette.main.v2.OnColorsPaletteListener
import net.osmand.plus.palette.view.renderer.GradientItemBinder
import net.osmand.plus.palette.view.renderer.PaletteItemBinder
import net.osmand.plus.plugins.srtm.TerrainMode
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.data.PaletteUtils
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteCategory
import net.osmand.shared.palette.domain.PaletteItem
import java.lang.ref.WeakReference

open class GradientColorsPaletteController(
	private val app: OsmandApplication,
	private var paletteId: String, // TODO: here we can use GradientPaletteCategory
	val analysis: GpxTrackAnalysis?
) : IColorsPaletteController {

	private val repository = app.paletteRepository
	private val palettes = ArrayList<WeakReference<IColorsPalette>>()
	private var listener: OnColorsPaletteListener? = null

	private var selectedItem: PaletteItem? = null

	// --- Content Update ---

	/**
	 * Updates the current palette category (e.g. switch from Speed to Elevation)
	 * and optionally selects an item by name.
	 */
	fun updatePalette(newPaletteId: String, selectedItemName: String?) {
		this.paletteId = newPaletteId
		selectPaletteItemByName(selectedItemName)
		notifyUpdatePaletteColors(null)
	}

	// --- Binding ---

	override fun bindPalette(palette: IColorsPalette) {
		palettes.add(WeakReference(palette))
	}

	override fun unbindPalette(palette: IColorsPalette) {
		val iterator = palettes.iterator()
		while (iterator.hasNext()) {
			if (iterator.next().get() == palette) {
				iterator.remove()
				break
			}
		}
	}

	override fun setPaletteListener(listener: OnColorsPaletteListener?) {
		this.listener = listener
	}

	// --- Selection & State ---

	private fun selectPaletteItemByName(name: String?) {
		if (name == null) {
			selectDefault()
			return
		}
		val items = getPaletteItems(PaletteSortMode.ORIGINAL_ORDER)

		val found = items.filterIsInstance<PaletteItem.Gradient>().find { it.paletteName == name }

		if (found != null) {
			selectPaletteItem(found)
		} else {
			selectDefault()
		}
	}

	private fun selectDefault() {
		val items = getPaletteItems(PaletteSortMode.ORIGINAL_ORDER)
		val defaultItem = items.filterIsInstance<PaletteItem.Gradient>().find { it.isDefault }

		if (defaultItem != null) {
			selectPaletteItem(defaultItem)
		}
	}

	override fun selectPaletteItem(item: PaletteItem?) {
		if (selectedItem?.id != item?.id) {
			val oldSelected = selectedItem
			selectedItem = item

			listener?.onPaletteItemSelected(item ?: return)

			if (oldSelected != null && item != null) {
				notifyUpdatePaletteSelection(oldSelected, item)
			} else {
				notifyUpdatePaletteColors(null)
			}
		}
	}

	override fun selectPaletteItem(color: Int, addIfNoFound: Boolean) {
		// TODO: extract and use only for solid colors
		// Not applicable for Gradients
	}

	override fun onSelectItemFromPalette(item: PaletteItem, renewLastUsedTime: Boolean) {
		if (selectedItem?.id != item.id) {
			val oldSelected = selectedItem
			selectPaletteItem(item)

			if (renewLastUsedTime) {
				repository.markPaletteItemAsUsed(paletteId, item.id)
				notifyUpdatePaletteColors(item)
			} else {
				notifyUpdatePaletteSelection(oldSelected, item)
			}
		}
	}

	override fun getSelectedPaletteItem(): PaletteItem? = selectedItem

	override fun isPaletteItemSelected(item: PaletteItem): Boolean {
		return item.id == selectedItem?.id
	}

	override fun refreshLastUsedTime() {
		selectedItem?.let {
			repository.markPaletteItemAsUsed(paletteId, it.id)
		}
	}

	// --- Data Access ---

	override fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem> {
		return repository.getPaletteItems(paletteId, sortMode)
	}

	// --- Actions (Duplicate / Remove) ---

	override fun onPaletteItemLongClick(activity: FragmentActivity, view: View, item: PaletteItem, nightMode: Boolean) {
		if (item is PaletteItem.Gradient) {
			showItemPopUpMenu(activity, view, item, nightMode)
		}
	}

	private fun showItemPopUpMenu(
		activity: FragmentActivity, view: View,
		item: PaletteItem.Gradient, nightMode: Boolean
	) {
		val menuItems = ArrayList<PopUpMenuItem>()

		// Duplicate
		menuItems.add(PopUpMenuItem.Builder(activity)
			.setTitleId(R.string.shared_string_duplicate)
			.setIcon(getContentIcon(R.drawable.ic_action_copy))
			.setOnClickListener { duplicateGradient(item) }
			.create()
		)

		// Remove (only if not default and not currently selected, mirroring legacy logic)
		val isSelected = isPaletteItemSelected(item)
		if (!item.isDefault && !isSelected) {
			menuItems.add(PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_remove)
				.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
				.setOnClickListener { showDeleteDialog(activity, item, nightMode) }
				.create()
			)
		}

		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = view
		displayData.menuItems = menuItems
		displayData.nightMode = nightMode
		PopUpMenu.show(displayData)
	}

	private fun showDeleteDialog(activity: FragmentActivity, item: PaletteItem.Gradient, nightMode: Boolean) {
		val warningColor = ColorUtilities.getColor(app, R.color.deletion_color_warning)
		val textColor = ColorUtilities.getSecondaryTextColor(activity, nightMode)
		val displayName = item.displayName

		val dialogData = AlertDialogData(activity, nightMode)
			.setTitle(activity.getString(R.string.delete_palette))
			.setNegativeButton(R.string.shared_string_cancel, null)
			.setPositiveButton(R.string.shared_string_delete) { _, _ ->
				removeGradient(item)
			}
			.setPositiveButtonTextColor(warningColor)

		val description = activity.getString(R.string.delete_colors_palette_dialog_summary, displayName)
		val spannable = UiUtilities.createSpannableString(description, Typeface.BOLD, displayName)
		UiUtilities.setSpan(spannable, ForegroundColorSpan(textColor), description, description)

		CustomAlert.showSimpleMessage(dialogData, spannable)
	}

	private fun duplicateGradient(item: PaletteItem.Gradient) {
		val currentPalette = repository.getPalette(paletteId) as? Palette.GradientCollection ?: return

		// 1. Factory create
		val newItem = PaletteUtils.createGradientDuplicate(currentPalette, item.id) ?: return

		// 2. Repository insert
		repository.insertPaletteItemAfter(paletteId, item.id, newItem)

		notifyUpdatePaletteColors(newItem)

		updateExternalDependencies()
	}

	private fun removeGradient(item: PaletteItem.Gradient) {
		repository.removePaletteItem(paletteId, item.id)
		notifyUpdatePaletteColors(null)

		updateExternalDependencies()
	}

	private fun updateExternalDependencies() {
		val category = PaletteCategory.fromKey(paletteId)

		if (category != null && category.isTerrainRelated()) {
			TerrainMode.reloadAvailableModes(app)
		}
	}

	// --- UI Interactions ---

	override fun onAllColorsButtonClicked(activity: FragmentActivity) {
		 AllGradientsPaletteFragment.showInstance(activity, this)
	}

	override fun onAddColorButtonClicked(activity: FragmentActivity) {
		// TODO: implement with new Gradient Editor UI
		// Not applicable for Gradients usually
	}

	override fun getControlsAccentColor(nightMode: Boolean): Int {
		return ColorUtilities.getActiveColor(app, nightMode)
	}

	override fun isAccentColorCanBeChanged(): Boolean = false

	override fun onApplyColorPickerSelection(oldColor: Int?, newColor: Int) {
		// Not applicable
	}

	// --- Helpers ---

	private fun collectActivePalettes(): List<IColorsPalette> {
		val result = ArrayList<IColorsPalette>()
		val iterator = palettes.iterator()
		while (iterator.hasNext()) {
			val palette = iterator.next().get()
			if (palette != null) {
				result.add(palette)
			} else {
				iterator.remove()
			}
		}
		return result
	}

	private fun notifyUpdatePaletteColors(targetItem: PaletteItem?) {
		for (palette in collectActivePalettes()) {
			palette.updatePaletteItems(targetItem)
		}
	}

	private fun notifyUpdatePaletteSelection(oldItem: PaletteItem?, newItem: PaletteItem) {
		for (palette in collectActivePalettes()) {
			palette.updatePaletteSelection(oldItem, newItem)
		}
	}

	override fun getItemBinder(
		activity: FragmentActivity,
		nightMode: Boolean
	): PaletteItemBinder {
		return GradientItemBinder(activity, nightMode)
	}

	private fun getContentIcon(@DrawableRes id: Int): Drawable? {
		return app.uiUtilities.getThemedIcon(id)
	}
}