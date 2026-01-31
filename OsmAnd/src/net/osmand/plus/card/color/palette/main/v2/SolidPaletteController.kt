package net.osmand.plus.card.color.palette.main.v2

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.main.ColorsPaletteFragment
import net.osmand.plus.palette.contract.IPaletteView
import net.osmand.plus.palette.controller.BasePaletteController
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

open class SolidPaletteController(
	protected val app: OsmandApplication,
	private val paletteId: String = "user_palette_default" // TODO: don't use hardcoded ids
) : BasePaletteController(), ColorPickerDialogController.ColorPickerListener {

	private val repository = app.paletteRepository
	protected val views = ArrayList<WeakReference<IPaletteView>>()
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

	override fun attachView(view: IPaletteView) {
		views.add(WeakReference(view))
	}

	override fun detachView(view: IPaletteView) {
		val iterator = views.iterator()
		while (iterator.hasNext()) {
			if (iterator.next().get() == view) {
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
		var found = items.filterIsInstance<PaletteItem.Solid>().find { it.color == color }

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

	// TODO: interact with external listener / listeners
	protected open fun notifyPaletteItemSelected(item: PaletteItem?) {
		if (item != null) {
			listener?.onPaletteItemSelected(item)
		}
	}

	override fun renewLastUsedTime() {
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

	override fun isAddingNewItemsSupported(): Boolean {
		return true
	}

	override fun isPaletteItemSelected(item: PaletteItem): Boolean {
		return item.id == selectedItem?.id
	}

	override fun isAccentColorCanBeChanged(): Boolean = false

	override fun getControlsAccentColor(nightMode: Boolean): Int {
		return ColorUtilities.getActiveColor(app, nightMode)
	}

	// --- UI Interactions ---

	override fun onPaletteItemClick(item: PaletteItem, markAsUsed: Boolean) {
		if (markAsUsed) {
			renewLastUsedTime(item)
		}
		selectPaletteItem(item)
	}

	override fun onPaletteItemLongClick(anchorView: View, item: PaletteItem) {
		if (item is PaletteItem.Solid) {
			showItemPopUpMenu(anchorView, item)
		}
	}

	override fun onAddButtonClick(activity: FragmentActivity) {
		showColorPickerDialog(activity, null)
	}

	override fun onShowAllClick(activity: FragmentActivity) {
		ColorsPaletteFragment.showInstance(activity, this)
	}

	// TODO: should be individual for gradient and solid
	private fun showItemPopUpMenu(anchorView: View, item: PaletteItem.Solid) {
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
		val color = item?.color
		ColorPickerDialogController.showDialog(activity, this, color)
	}

	// --- Helpers ---

	private fun collectActivePalettes(): List<IPaletteView> {
		val result = ArrayList<IPaletteView>()
		val iterator = views.iterator()
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

	fun getColorName(color: Int): String {
		val found = findPaletteItem(color)
		return found?.displayName ?: app.getString(R.string.shared_string_custom)
	}

	protected fun getContentIcon(@DrawableRes id: Int): Drawable? {
		return app.uiUtilities.getThemedIcon(id)
	}
}