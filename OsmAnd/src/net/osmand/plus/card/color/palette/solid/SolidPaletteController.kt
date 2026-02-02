package net.osmand.plus.card.color.palette.solid

import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.palette.controller.BasePaletteController
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.data.PaletteUtils
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem

open class SolidPaletteController(
	app: OsmandApplication,
	paletteId: String = "user_palette_default" // TODO: don't use hardcoded ids
) : BasePaletteController(app, paletteId), ColorPickerDialogController.ColorPickerListener {

	@JvmOverloads
	constructor(
		app: OsmandApplication,
		startColor: Int?,
		addIfNoFound: Boolean = true
	) : this(app) {
		if (startColor != null) {
			selectPaletteItem(startColor, addIfNoFound)
		}
	}

	// --- CRUD Operations ---

	override fun onApplyColorPickerSelection(oldColor: Int?, newColor: Int) {
		val currentPalette = repository.getPalette(paletteId) as? Palette.SolidCollection ?: return

		val itemToUpdate = editedItem as? PaletteItem.Solid
		val resultItem: PaletteItem.Solid

		if (itemToUpdate != null) {
			// Update Existing
			resultItem = PaletteUtils.updateSolidColor(itemToUpdate, newColor)
			repository.updatePaletteItem(resultItem)
		} else {
			// Add New
			// TODO: here we mark this color as last used (it works like in previous version),
			//  but I think we shouldn't renew last used time before user confirm his selection
			//  by clicking on the "Apply" button. We can select this item on palette view,
			//  but don't pick it on the first position in the palette items list
			resultItem = PaletteUtils.createSolidColor(currentPalette, newColor, markAsUsed = true)
			repository.addPaletteItem(paletteId, resultItem)
		}

		notifyUpdatePaletteColors(resultItem)
		listener?.onPaletteItemAdded(itemToUpdate, resultItem)

		if (oldColor == null || itemToUpdate?.id == selectedItem?.id) {
			val oldSelected = selectedItem
			selectPaletteItem(resultItem)
			notifyUpdatePaletteSelection(oldSelected, resultItem)
		}
		editedItem = null
	}

	// TODO: in this case only for solid color (the same should be implemented for gradient,
	//  maybe use the same method, but with different palette utils createDuplicate calls)
	private fun duplicateColor(item: PaletteItem.Solid) {
		val currentPalette = repository.getPalette(paletteId) as? Palette.SolidCollection ?: return
		val newItem = PaletteUtils.createSolidDuplicate(currentPalette, item.id)

		if (newItem != null) {
			repository.insertPaletteItemAfter(paletteId, item.id, newItem)
			notifyUpdatePaletteColors(newItem)
		}
	}

	private fun removeCustomColor(item: PaletteItem.Solid) {
		repository.removePaletteItem(paletteId, item.id)

		if (selectedItem?.id == item.id) {
			selectedItem = null
		}
		notifyUpdatePaletteColors(null)
	}

	// --- Selection Logic ---

	fun selectPaletteItem(color: Int, addIfNoFound: Boolean) {
		var found = findPaletteItem(color, addIfNoFound)
		if (found == null) {
			found = repository.getPaletteItems(paletteId, PaletteSortMode.ORIGINAL_ORDER)[0]
		}
		selectPaletteItem(found)
	}

	@JvmOverloads
	fun findPaletteItem(color: Int, addIfNotFound: Boolean = false): PaletteItem? {
		val items = repository.getPaletteItems(paletteId, PaletteSortMode.ORIGINAL_ORDER)
		var found = items.filterIsInstance<PaletteItem.Solid>().find { it.colorInt == color }

		if (found == null && addIfNotFound) {
			val palette = repository.getPalette(paletteId)
			if (palette is Palette.SolidCollection) {
				val newItem = PaletteUtils.createSolidColor(palette, color, false)
				repository.addPaletteItem(paletteId, newItem)
				found = newItem
			}
		}
		return found
	}

	override fun isAddingNewItemsSupported(): Boolean {
		return true
	}

	// --- UI Interactions ---

	override fun onAddButtonClick(activity: FragmentActivity) {
		showColorPickerDialog(activity, null)
	}

	override fun onShowAllClick(activity: FragmentActivity) {
		ColorsPaletteFragment.showInstance(activity, this)
	}

	override fun showItemPopUpMenu(anchorView: View, item: PaletteItem) {
		if (item !is PaletteItem.Solid) return

		val paletteView = collectActivePalettes()[0]
		val activity = paletteView.getActivity() ?: return
		val nightMode = paletteView.isNightMode()

		val menuItems = ArrayList<PopUpMenuItem>()

		menuItems.add(PopUpMenuItem.Builder(activity)
			.setTitleId(R.string.shared_string_edit)
			.setIcon(getContentIcon(R.drawable.ic_action_appearance_outlined))
			.setOnClickListener { showColorPickerDialog(activity, item) }
			.create()
		)
		menuItems.add(PopUpMenuItem.Builder(activity)
			.setTitleId(R.string.shared_string_duplicate)
			.setIcon(getContentIcon(R.drawable.ic_action_copy))
			.setOnClickListener { duplicateColor(item) }
			.create()
		)
		menuItems.add(PopUpMenuItem.Builder(activity)
			.setTitleId(R.string.shared_string_remove)
			.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
			.showTopDivider(menuItems.isNotEmpty())
			.setOnClickListener { removeCustomColor(item) }
			.create()
		)

		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = anchorView
		displayData.menuItems = menuItems
		displayData.nightMode = nightMode
		PopUpMenu.show(displayData)
	}

	private fun showColorPickerDialog(activity: FragmentActivity, item: PaletteItem.Solid?) {
		editedItem = item
		val color = item?.colorInt
		ColorPickerDialogController.showDialog(activity, this, color)
	}

	fun getColorName(color: Int): String {
		val found = findPaletteItem(color)
		return found?.displayName ?: app.getString(R.string.shared_string_custom)
	}
}