package net.osmand.plus.card.color.palette.gradient.v2

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.AllGradientsPaletteFragment
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

open class GradientPaletteController(
	app: OsmandApplication,
	paletteId: String, // TODO: here we can use GradientPaletteCategory
	val analysis: GpxTrackAnalysis?
) : BasePaletteController(app, paletteId) {

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

	override fun isAddingNewItemsSupported(): Boolean {
		return InAppPurchaseUtils.isGradientEditorAvailable(app)
	}

	// --- Actions (Duplicate / Remove) ---

	override fun showItemPopUpMenu(anchorView: View, item: PaletteItem) {
		if (item !is PaletteItem.Gradient) return

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

	override fun onAddButtonClick(activity: FragmentActivity) {
		// TODO: implement with new Gradient Editor UI
	}

	override fun onShowAllClick(activity: FragmentActivity) {
		AllGradientsPaletteFragment.showInstance(activity, this)
	}
}