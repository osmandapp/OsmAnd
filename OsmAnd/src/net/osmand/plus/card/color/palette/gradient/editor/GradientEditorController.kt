package net.osmand.plus.card.color.palette.gradient.editor

import androidx.fragment.app.FragmentManager
import net.osmand.OnResultCallback
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.card.color.palette.gradient.editor.contract.IGradientEditorController
import net.osmand.plus.card.color.palette.gradient.editor.contract.IGradientEditorView
import net.osmand.plus.card.color.palette.gradient.editor.data.ColorState
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorDataState
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorStaticUiData
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientDraft
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientPreviewState
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientStepData
import net.osmand.plus.card.color.palette.gradient.editor.data.RemoveButtonState
import net.osmand.plus.card.color.palette.gradient.editor.data.ToolbarState
import net.osmand.plus.card.color.palette.gradient.editor.data.ValueState
import net.osmand.plus.card.color.palette.solid.SolidPaletteController
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.GradientRangeType
import kotlin.math.max

class GradientEditorController(
	app: OsmandApplication,
	appMode: ApplicationMode,
	private val initialDraft: GradientDraft,
	private val callback: OnResultCallback<GradientDraft>
) : BaseDialogController(app), IGradientEditorController {

	companion object {
		private const val PROCESS_ID = "edit_gradient"

		fun showDialog(
			app: OsmandApplication,
			fragmentManager: FragmentManager,
			appMode: ApplicationMode,
			gradientDraft: GradientDraft,
			callback: OnResultCallback<GradientDraft>
		) {
			val controller = GradientEditorController(app, appMode, gradientDraft, callback)
			app.dialogManager.register(PROCESS_ID, controller)
			if (!GradientEditorFragment.showInstance(fragmentManager, appMode, PROCESS_ID)) {
				app.dialogManager.unregister(PROCESS_ID)
			}
		}
	}

	private var dataState: EditorDataState = EditorDataState(initialDraft, 0)

	private val previousStates = ArrayDeque<EditorDataState>()

	private var view: IGradientEditorView? = null
	private var initialUiData: EditorStaticUiData? = null
	private var colorCardController: SolidPaletteController? = null // TODO: use interface

	override fun getId() = processId

	override fun getProcessId() = PROCESS_ID

	// --- State ---

	var draft: GradientDraft = initialDraft
		private set

	var selectedPointIndex: Int = -1
		private set

	init {
		if (draft.points.isNotEmpty()) {
			selectedPointIndex = 0
		}
	}

	override fun attachView(view: IGradientEditorView) {
		this.view = view
	}

	override fun detachView() {
		this.view = null
	}

	override fun onViewInitialized() {
		view?.render(buildUiState(dataState))
	}

	override fun onBackClick() {
		if (initialDraft != dataState.draft) {
			val nightMode = view?.isNightMode() ?: false
			view?.getFragmentActivity()?.let { activity ->
				val dialogData = AlertDialogData(activity, nightMode)
					.setTitle(R.string.exit_without_saving)
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_exit) { _, _ ->
						view?.dismiss()
					}
				CustomAlert.showSimpleMessage(dialogData, R.string.dismiss_changes_descr)
			}
		} else {
			view?.dismiss()
		}
	}

	override fun onUndoClick() {
		TODO("Not yet implemented")
	}

	override fun onStepClick(stepData: GradientStepData) {
		TODO("Not yet implemented")
	}

	override fun onAddStepClick() {
		TODO("Not yet implemented")
	}

	override fun onValueInput(text: CharSequence) {
		TODO("Not yet implemented")
	}

	override fun getColorController(): SolidPaletteController {
		var colorController = colorCardController
		if (colorController == null) {
			colorController = SolidPaletteController(app)
			colorCardController = colorController
		}
		return colorController
	}

	override fun onColorSelected(colorInt: Int) {
	}

	override fun onRemoveStepClick() {
		TODO("Not yet implemented")
	}

	override fun onSaveClick() {
		TODO("Not yet implemented")
	}

	// --- Actions ---

	fun selectPoint(index: Int) {
		selectedPointIndex = if (index in draft.points.indices) index else -1
	}

	fun updatePointValue(newValue: Float) {
		if (selectedPointIndex == -1 || selectedPointIndex >= draft.points.size) return

		val oldPoint = draft.points[selectedPointIndex]
		if (oldPoint.value == newValue) return

		val newPoint = oldPoint.copy(value = newValue)

		draft = draft.withPointUpdated(oldPoint, newPoint)

		selectedPointIndex = draft.points.indexOfFirst { it.value == newPoint.value && it.color == newPoint.color }
	}

	fun updatePointColor(newColor: Int) {
		if (selectedPointIndex == -1 || selectedPointIndex >= draft.points.size) return

		val oldPoint = draft.points[selectedPointIndex]
		if (oldPoint.color == newColor) return

		val newPoint = oldPoint.copy(color = newColor)

		draft = draft.withPointUpdated(oldPoint, newPoint)
		selectedPointIndex = draft.points.indexOfFirst { it.value == newPoint.value && it.color == newPoint.color }
	}

	fun addNewPoint() {
		val points = draft.points
		val newPoint = if (points.isNotEmpty()) {
			val last = points.last()
			val addValue = if (last.value == 0f) 10f else last.value * 0.1f
			last.copy(value = last.value + max(1f, addValue))
		} else {
			GradientPoint(0f, 0xFF00FF00.toInt()) // Default Green
		}

		draft = draft.withPointAdded(newPoint)
		selectedPointIndex = draft.points.indexOf(newPoint)
	}

	fun removeSelectedPoint() {
		if (selectedPointIndex == -1 || selectedPointIndex >= draft.points.size) return
		val points = draft.points

		if (points.size <= 2) return

		val pointToRemove = points[selectedPointIndex]
		draft = draft.withPointRemoved(pointToRemove)

		selectedPointIndex = if (points.isNotEmpty()) {
			max(0, selectedPointIndex - 1)
		} else {
			-1
		}
	}

	fun saveChanges() {
		callback.onResult(draft)
	}

	override fun getStaticUiData(): EditorStaticUiData {
		var data = initialUiData
		if (data == null) {
			val toolbarTitle = getString(
				if (draft.originalId == null) {
					R.string.add_palette
				} else {
					R.string.edit_palette
				}
			)

			val fileType = draft.fileType
			val paletteCategory = fileType.category
			val units = if (fileType.rangeType == GradientRangeType.FIXED_VALUES) {
				paletteCategory.baseUnit.getSymbol()
			} else {
				"%"
			}

			val toolbarSubtitle = getString(
				R.string.ltr_or_rtl_combine_with_brackets,
				paletteCategory.getDisplayName(),
				units
			)

			data = EditorStaticUiData(
				toolbarTitle = toolbarTitle,
				toolbarSubtitle = toolbarSubtitle
			)
			initialUiData = data
		}
		return data
	}

	private fun buildUiState(dataState: EditorDataState): EditorUiState {

		val selectedPointIndex = dataState.selectedIndex
		val points = dataState.draft.points

		val stepData = mutableListOf<GradientStepData>()
		points.forEach { point ->
			val value = point.value
			val id = value.toString()
			val label = when (value) {
				0f -> {
					"Min"
				}
				0.5f -> {
					"Average"
				}
				1.0f -> {
					"Max"
				}
				else -> {
					value.toString()
				}
			}
			stepData.add(
				GradientStepData(
					id = id,
					label = label,
					point = point
				)
			)
		}

		val selectedStep = if (stepData.isNotEmpty() && selectedPointIndex >= 0) {
			stepData[selectedPointIndex]
		} else {
			null
		}

		val label = "Minimum"
		val summaryText = "Color for the minimum value found in the track."
		val hasError = true
		val summaryColor = if (hasError) {
			ColorUtilities.getColor(app, R.color.design_default_color_error)
		} else {
			ColorUtilities.getSecondaryTextColor(app, isNightMode)
		}

		return EditorUiState(
			toolbarState = ToolbarState(
				showUndoButton = previousStates.isNotEmpty()
			),
			gradientState = GradientPreviewState(
				paletteCategory = initialDraft.fileType.category,
				stepData = stepData,
				selectedItem = selectedStep
			),
			valueState = ValueState(
				label = label,
				text = "",
				interactable = false,
				showTextField = true,
				summaryText = summaryText,
				summaryColor = summaryColor
			),
			colorState = ColorState(
				colorInt = points[selectedPointIndex].color
			),
			removeButtonState = RemoveButtonState(
				enabled = true
			)
		)
	}
}