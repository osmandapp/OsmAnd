package net.osmand.plus.card.color.palette.main.v2

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.main.ColorsPaletteFragment
import net.osmand.plus.palette.view.renderer.PaletteItemBinder
import net.osmand.plus.palette.view.renderer.SolidItemBinder
import net.osmand.plus.track.fragments.controller.ColorPickerDialogController
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.shared.palette.data.PaletteSortMode
import net.osmand.shared.palette.data.PaletteUtils
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import java.lang.ref.WeakReference

// TODO: extract all methods related to solid colors in the separate controller
open class ColorsPaletteController(
	protected val app: OsmandApplication,
	private val paletteId: String = "user_palette_default" // TODO: don't use hardcoded ids
) : IColorsPaletteController {

	private val repository = app.paletteRepository
	protected val palettes = ArrayList<WeakReference<IColorsPalette>>()
	private var listener: OnColorsPaletteListener? = null

	private var editedItem: PaletteItem.Solid? = null
	protected var selectedItem: PaletteItem? = null

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

	// TODO: bindPaletteView
	override fun bindPalette(palette: IColorsPalette) {
		palettes.add(WeakReference(palette))
	}

	// TODO: unbindPaletteView
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

	// --- CRUD Operations ---

	// TODO: solid color
	override fun onApplyColorPickerSelection(oldColor: Int?, newColor: Int) {
		val currentPalette = repository.getPalette(paletteId) as? Palette.SolidCollection ?: return

		val itemToUpdate = editedItem
		val resultItem: PaletteItem.Solid

		if (itemToUpdate != null) {
			// Update Existing
			resultItem = itemToUpdate.copy(color = newColor)
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

	// TODO: solid color
	final override fun selectPaletteItem(color: Int, addIfNoFound: Boolean) {
		var found = findPaletteItem(color, addIfNoFound)
		if (found == null) {
			found = repository.getPaletteItems(paletteId, PaletteSortMode.ORIGINAL_ORDER)[0]
		}
		selectPaletteItem(found)
	}

	// TODO: solid color
	@JvmOverloads
	fun findPaletteItem(color: Int, addIfNoFound: Boolean = false): PaletteItem? {
		val items = repository.getPaletteItems(paletteId, PaletteSortMode.ORIGINAL_ORDER)
		var found = items.filterIsInstance<PaletteItem.Solid>().find { it.color == color }

		if (found == null && addIfNoFound) {
			val palette = repository.getPalette(paletteId)
			if (palette is Palette.SolidCollection) {
				val newItem = PaletteUtils.createSolidColor(palette, color, false)
				repository.addPaletteItem(paletteId, newItem)
				found = newItem
			}
		}
		return found
	}

	// TODO: in previous implementation it was a lot simpler:
	//  @Override
	//	 public void selectColor(@Nullable PaletteColor paletteColor) {
	//		selectedPaletteColor = paletteColor;
	//		onColorSelected(paletteColor);
	//	 }
	override fun selectPaletteItem(item: PaletteItem?) {
		if (selectedItem?.id != item?.id) {
			val oldSelected = selectedItem
			selectedItem = item
			notifyPaletteItemSelected(item)

			if (oldSelected != null && item != null) {
				notifyUpdatePaletteSelection(oldSelected, item)
			} else {
				notifyUpdatePaletteColors(item)
			}
		}
	}

	// TODO: maybe rename -- public interface that we call when click on item
	override fun onSelectItemFromPalette(item: PaletteItem, markAsUsed: Boolean) {
		if (selectedItem?.id != item.id) {
			val oldSelected = selectedItem
			selectPaletteItem(item)

			if (markAsUsed) {
				repository.markPaletteItemAsUsed(paletteId, item.id)
				notifyUpdatePaletteColors(item)
			} else {
				notifyUpdatePaletteSelection(oldSelected, item)
			}
		}
	}

	// TODO: interact with external listener / listeners
	protected open fun notifyPaletteItemSelected(item: PaletteItem?) {
		if (item != null) {
			listener?.onPaletteItemSelected(item)
		}
	}

	// TODO: change name to renewLastUsedTime
	override fun refreshLastUsedTime() {
		selectedItem?.let { renewLastUsedTime(it) }
	}

	fun renewLastUsedTime(item: PaletteItem) {
		repository.markPaletteItemAsUsed(paletteId, item.id)
	}

	// --- Data Access ---

	override fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem> {
		return repository.getPaletteItems(paletteId, sortMode)
	}

	override fun getSelectedPaletteItem(): PaletteItem? = selectedItem

	override fun isPaletteItemSelected(item: PaletteItem): Boolean {
		return item.id == selectedItem?.id
	}

	override fun isAccentColorCanBeChanged(): Boolean = false

	override fun getControlsAccentColor(nightMode: Boolean): Int {
		return ColorUtilities.getActiveColor(app, nightMode)
	}

	// --- UI Interactions ---

	override fun onAddColorButtonClicked(activity: FragmentActivity) {
		showColorPickerDialog(activity, null)
	}

	override fun onAllColorsButtonClicked(activity: FragmentActivity) {
		ColorsPaletteFragment.showInstance(activity, this) // TODO use AllPaletteItemsFragment
	}

	override fun onPaletteItemLongClick(activity: FragmentActivity, view: View, item: PaletteItem, nightMode: Boolean) {
		if (item is PaletteItem.Solid) { // TODO: don't check here
			showItemPopUpMenu(activity, view, item, nightMode)
		}
	}

	// TODO: should be individual for gradient and solid
	private fun showItemPopUpMenu(
		activity: FragmentActivity, view: View,
		item: PaletteItem.Solid, nightMode: Boolean
	) {
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
		displayData.anchorView = view
		displayData.menuItems = menuItems
		displayData.nightMode = nightMode
		PopUpMenu.show(displayData)
	}

	// TODO: only for solid colors
	private fun showColorPickerDialog(activity: FragmentActivity, item: PaletteItem.Solid?) {
		editedItem = item
		val color = item?.color
		ColorPickerDialogController.showDialog(activity, this, color)
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

	// TODO: add comment?
	//  This method updates palette view absolutely, each item will be updated
	private fun notifyUpdatePaletteColors(targetItem: PaletteItem?) {
		for (palette in collectActivePalettes()) {
			palette.updatePaletteItems(targetItem)
		}
	}

	// TODO: add comment?
	//  This method updates only palette selection, e.g. old and new selected items view
	protected fun notifyUpdatePaletteSelection(oldItem: PaletteItem?, newItem: PaletteItem) {
		for (palette in collectActivePalettes()) {
			palette.updatePaletteSelection(oldItem, newItem)
		}
	}

	override fun getItemBinder(
		activity: FragmentActivity,
		nightMode: Boolean
	): PaletteItemBinder {
		return SolidItemBinder(activity, nightMode)
	}

	fun getColorName(color: Int): String {
		val found = findPaletteItem(color)
		return found?.displayName ?: app.getString(R.string.shared_string_custom)
	}

	protected fun getContentIcon(@DrawableRes id: Int): Drawable? {
		return app.uiUtilities.getThemedIcon(id)
	}
}