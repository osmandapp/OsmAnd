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
import net.osmand.plus.palette.contract.IPaletteView
import net.osmand.plus.card.color.palette.main.v2.OnColorsPaletteListener
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.palette.controller.BasePaletteController
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

open class GradientPaletteController(
	private val app: OsmandApplication,
	private var paletteId: String, // TODO: here we can use GradientPaletteCategory
	val analysis: GpxTrackAnalysis?
) : BasePaletteController() {

	private val repository = app.paletteRepository
	private val palettes = ArrayList<WeakReference<IPaletteView>>()
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

	override fun attachView(view: IPaletteView) {
		palettes.add(WeakReference(view))
	}

	override fun detachView(view: IPaletteView) {
		val iterator = palettes.iterator()
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

	override fun renewLastUsedTime() {
		selectedItem?.let { renewLastUsedTime(it) }
	}

	private fun renewLastUsedTime(item: PaletteItem) {
		repository.markPaletteItemAsUsed(paletteId, item.id)
	}

	override fun getSelectedPaletteItem(): PaletteItem? = selectedItem

	override fun isAddingNewItemsSupported(): Boolean {
		return InAppPurchaseUtils.isGradientEditorAvailable(app)
	}

	override fun isPaletteItemSelected(item: PaletteItem): Boolean {
		return item.id == selectedItem?.id
	}

	// --- Data Access ---

	override fun getPaletteItems(sortMode: PaletteSortMode): List<PaletteItem> {
		return repository.getPaletteItems(paletteId, sortMode)
	}

	// --- Actions (Duplicate / Remove) ---

	private fun showItemPopUpMenu(anchorView: View, item: PaletteItem.Gradient) {
		val paletteView = collectActivePalettes()[0]
		val activity = paletteView.getActivity() ?: return
		val nightMode = paletteView.isNightMode()
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
		displayData.anchorView = anchorView
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

	override fun onPaletteItemClick(item: PaletteItem, markAsUsed: Boolean) {
		if (markAsUsed) {
			renewLastUsedTime(item)
		}
		selectPaletteItem(item)
	}

	override fun onPaletteItemLongClick(anchorView: View, item: PaletteItem) {
		if (item is PaletteItem.Gradient) {
			showItemPopUpMenu(anchorView, item)
		}
	}

	override fun onAddButtonClick(activity: FragmentActivity) {
		// TODO: implement with new Gradient Editor UI
		// Not applicable for Gradients usually
	}

	override fun onShowAllClick(activity: FragmentActivity) {
		AllGradientsPaletteFragment.showInstance(activity, this)
	}

	override fun getControlsAccentColor(nightMode: Boolean): Int {
		return ColorUtilities.getActiveColor(app, nightMode)
	}

	override fun isAccentColorCanBeChanged(): Boolean = false

	// --- Helpers ---

	private fun collectActivePalettes(): List<IPaletteView> {
		val result = ArrayList<IPaletteView>()
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

	private fun getContentIcon(@DrawableRes id: Int): Drawable? {
		return app.uiUtilities.getThemedIcon(id)
	}
}