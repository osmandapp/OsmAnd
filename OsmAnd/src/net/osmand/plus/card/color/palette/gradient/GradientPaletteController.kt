package net.osmand.plus.card.color.palette.gradient

import android.graphics.Typeface
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.fragment.app.FragmentActivity
import net.osmand.OnResultCallback
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientDraft
import net.osmand.plus.card.color.palette.gradient.editor.GradientEditorController
import net.osmand.plus.card.color.palette.gradient.editor.GradientRangeTypeController
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
import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.category.GradientPaletteCategory
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.filetype.GradientFileType

open class GradientPaletteController(
	app: OsmandApplication,
	private var paletteCategory: GradientPaletteCategory,
) : BasePaletteController(app, paletteCategory.id) {

	var analysis: GpxTrackAnalysis? = null

	constructor(
		app: OsmandApplication,
		paletteCategory: GradientPaletteCategory,
		analysis: GpxTrackAnalysis
	): this(app, paletteCategory) {
		this.analysis = analysis
	}

	// --- Content Update ---

	/**
	 * Updates the current palette category (e.g. switch from Speed to Elevation)
	 * and optionally selects an item by name.
	 */
	fun updatePalette(paletteCategory: GradientPaletteCategory, selectedItemName: String?) {
		this.paletteCategory = paletteCategory
		this.paletteId = paletteCategory.id
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
		return paletteCategory.editable && InAppPurchaseUtils.isGradientEditorAvailable(app)
	}

	override fun isAutoScrollSupported(): Boolean {
		return true
	}

	// --- Actions (Duplicate / Remove) ---

	override fun showItemPopUpMenu(anchorView: View, item: PaletteItem) {
		if (item !is PaletteItem.Gradient) return

		val paletteView = collectActivePalettes()[0]
		val activity = paletteView.getActivity() ?: return
		val nightMode = paletteView.isNightMode()
		val menuItems = ArrayList<PopUpMenuItem>()

		if (paletteCategory.editable && item.isEditable) {
			menuItems.add(PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_rename)
				.setIcon(getContentIcon(R.drawable.ic_action_edit_outlined))
				.setOnClickListener { showRenameDialog(activity, item) }
				.create()
			)

			menuItems.add(PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.shared_string_edit)
				.setIcon(getContentIcon(R.drawable.ic_action_appearance_outlined))
				.setOnClickListener { editGradient(item) }
				.create()
			)
		}

		// Duplicate (allowed for every item)
		menuItems.add(PopUpMenuItem.Builder(activity)
			.setTitleId(R.string.shared_string_duplicate)
			.setIcon(getContentIcon(R.drawable.ic_action_copy))
			.setOnClickListener { duplicateGradient(item) }
			.create()
		)

		// Remove (only if not default and not currently selected)
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

	private fun showRenameDialog(activity: FragmentActivity, item: PaletteItem.Gradient) {
		// TODO: implement
	}

	private fun editGradient(item: PaletteItem.Gradient) {
		editedItem = item
		showGradientEditor(
			GradientDraft(
			originalId = item.id,
			fileType = item.properties.fileType,
			points = item.points
		)
		)
	}

	private fun selectFileType(callback: OnResultCallback<GradientFileType>) {
		if (paletteCategory.isSupportDifferentRangeTypes()) {
			showRangeTypeDialog {
				callback.onResult(paletteCategory.getFileType(it))
			}
		} else {
			callback.onResult(paletteCategory.getFileType())
		}
	}

	private fun showRangeTypeDialog(callback: OnResultCallback<GradientRangeType>) {
		getFragmentActivity()?.let {
			GradientRangeTypeController.showDialog(
				app = app,
				fragmentManager = it.supportFragmentManager,
				appMode = app.settings.applicationMode,       // TODO: determine actual appMode
				usedOnMap = true,                             // TODO: determine actual usedOnMap
				supportedTypes = paletteCategory.getSupportedRangeTypes(),
				callback = callback
			)
		}
	}

	private fun showGradientEditor(gradientDraft: GradientDraft) {
		getFragmentActivity()?.let {
			GradientEditorController.showDialog(
				app = app,
				fragmentManager = it.supportFragmentManager,
				appMode = app.settings.applicationMode,       // TODO: determine actual appMode
				gradientDraft = gradientDraft,
				callback = { result -> onApplyGradientEdits(result) }
			)
		}
	}

	private fun onApplyGradientEdits(gradientDraft: GradientDraft) {
		// TODO: implement
	}

	private fun updateExternalDependencies() {
		if (paletteCategory.isTerrainRelated()) {
			TerrainMode.reloadAvailableModes(app)
		}
	}

	// --- UI Interactions ---

	override fun onAddButtonClick(activity: FragmentActivity) {
		selectFileType { fileType ->
			showGradientEditor(
				GradientDraft(
					originalId = null,
					fileType = fileType,
					points = fileType.getDefaultGradientPoints()
				)
			)
		}
	}

	override fun onShowAllClick(activity: FragmentActivity) {
		AllGradientsPaletteFragment.showInstance(activity, this)
	}
}