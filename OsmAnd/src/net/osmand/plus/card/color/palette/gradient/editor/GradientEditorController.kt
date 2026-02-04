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
import net.osmand.plus.card.color.palette.gradient.editor.data.RelativeConstants
import net.osmand.plus.card.color.palette.gradient.editor.data.RemoveButtonState
import net.osmand.plus.card.color.palette.gradient.editor.data.ToolbarState
import net.osmand.plus.card.color.palette.gradient.editor.data.ValueState
import net.osmand.plus.card.color.palette.solid.SolidPaletteController
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.GradientRangeType
import java.text.DecimalFormat
import kotlin.math.max

class GradientEditorController(
	app: OsmandApplication,
	appMode: ApplicationMode,
	private val initialDraft: GradientDraft,
	private val callback: OnResultCallback<GradientDraft>
) : BaseDialogController(app), IGradientEditorController {

	companion object {
		private const val PROCESS_ID = "edit_gradient"

		private const val UNDO_STACK_LIMIT = 50

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

	// --- Lifecycle & View Attachment ---

	override fun attachView(view: IGradientEditorView) {
		this.view = view
	}

	override fun detachView() {
		this.view = null
	}

	override fun onViewInitialized() {
		view?.render(buildUiState(dataState))
	}

	// --- User Actions ---

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
		if (previousStates.isNotEmpty()) {
			dataState = previousStates.removeLast()
			refreshView()
		}
	}

	override fun onStepClick(stepData: GradientStepData) {
		// TODO: check
		// Find index by unique ID or reference. Here we assume ID matches index string or value.
		// Safer way: find index of the point in the current draft.
		val index = dataState.draft.points.indexOf(stepData.point)
		if (index != -1 && index != dataState.selectedIndex) {
			// Navigation change doesn't need to be pushed to Undo stack
			dataState = dataState.copy(selectedIndex = index)
			refreshView()
		}
	}

	// TODO: improve it
	override fun onAddStepClick() {

		val draft = dataState.draft
		// "Relative" gradients usually have fixed number of steps (min/avg/max),
		// so we typically don't allow adding points there.
		if (draft.fileType.rangeType == GradientRangeType.RELATIVE) {
			// TODO: implement

		} else {
			pushState()

			val points = draft.points
			val selectedIndex = dataState.selectedIndex

			// Heuristic: Insert after selected, trying to take midpoint
			val newPointValue = if (points.isNotEmpty() && selectedIndex in points.indices) {
				val current = points[selectedIndex]
				if (selectedIndex < points.lastIndex) {
					// Insert between current and next
					val next = points[selectedIndex + 1]
					(current.value + next.value) / 2f
				} else {
					// Append after last. Try to use same step size as previous interval.
					val prev = if (selectedIndex > 0) points[selectedIndex - 1] else null
					val step = if (prev != null) current.value - prev.value else 10f
					current.value + max(1f, step)
				}
			} else {
				0f
			}

			// Create new point (defaulting to current color or Green)
			val baseColor = points.getOrNull(selectedIndex)?.color ?: 0xFF00FF00.toInt()
			val newPoint = GradientPoint(newPointValue, baseColor)

			val newDraft = draft.withPointAdded(newPoint)
			// Find the index of the newly added point (it might have shifted due to sorting)
			val newIndex = newDraft.points.indexOf(newPoint)

			dataState = EditorDataState(newDraft, newIndex)
		}
		refreshView()
	}

	override fun onValueInput(text: CharSequence) {
		val draft = dataState.draft
		val index = dataState.selectedIndex

		// Value editing is disabled for Relative gradients
		if (draft.fileType.rangeType == GradientRangeType.RELATIVE) return
		if (index !in draft.points.indices) return

		val newValue = text.toString().toFloatOrNull() ?: return
		val currentPoint = draft.points[index]

		// Avoid infinite loops if value is same
		if (currentPoint.value == newValue) return

		pushState()

		val newPoint = currentPoint.copy(value = newValue)
		val newDraft = draft.withPointUpdated(currentPoint, newPoint)

		// Re-calculate index as sort order might change
		val newIndex = newDraft.points.indexOfFirst {
			it.value == newPoint.value && it.color == newPoint.color
		}

		dataState = EditorDataState(newDraft, newIndex)
		refreshView()
	}

	override fun onColorSelected(colorInt: Int) {
		val index = dataState.selectedIndex
		val points = dataState.draft.points
		if (index !in points.indices) return

		val currentPoint = points[index]
		if (currentPoint.color == colorInt) return

		pushState()

		val newPoint = currentPoint.copy(color = colorInt)
		// Updating color preserves order, so index stays same
		val newDraft = dataState.draft.withPointUpdated(currentPoint, newPoint)

		dataState = dataState.copy(draft = newDraft)
		refreshView()
	}

	override fun onRemoveStepClick() {
		val draft = dataState.draft
		// Cannot remove steps in Relative mode (fixed min/avg/max structure)
		if (draft.fileType.rangeType == GradientRangeType.RELATIVE) return

		val index = dataState.selectedIndex
		if (index !in draft.points.indices) return
		if (draft.points.size <= 2) return // Minimum 2 points required

		pushState()

		val pointToRemove = draft.points[index]
		val newDraft = draft.withPointRemoved(pointToRemove)

		// Select neighbor
		val newIndex = if (newDraft.points.isNotEmpty()) {
			max(0, index - 1)
		} else {
			-1
		}

		dataState = EditorDataState(newDraft, newIndex)
		refreshView()
	}

	override fun onSaveClick() {
		callback.onResult(dataState.draft)
	}

	override fun getColorController(): SolidPaletteController {
		var colorController = colorCardController
		if (colorController == null) {
			colorController = SolidPaletteController(app)
			colorCardController = colorController
		}
		return colorController
	}

	// --- Internal Helpers ---

	private fun pushState() {
		// Limit stack size if needed, e.g., 50 states
		if (previousStates.size > UNDO_STACK_LIMIT) {
			previousStates.removeFirst()
		}
		previousStates.addLast(dataState)
	}

	private fun refreshView() {
		view?.render(buildUiState(dataState))
	}

	override fun getStaticUiData(): EditorStaticUiData {
		var data = initialUiData
		if (data == null) {
			val toolbarTitle = getString(
				if (initialDraft.originalId == null) {
					R.string.add_palette
				} else {
					R.string.edit_palette
				}
			)

			val fileType = initialDraft.fileType
			val paletteCategory = fileType.category
			val units = if (fileType.rangeType == GradientRangeType.FIXED_VALUES) {
				fileType.displayUnits.getSymbol()
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
		val draft = dataState.draft
		val rangeType = draft.fileType.rangeType
		val points = draft.points
		val selectedIndex = dataState.selectedIndex

		// 1. Build Gradient Steps (Chips)
		val stepData = points.mapIndexed { index, point ->
			val label = if (rangeType == GradientRangeType.RELATIVE) {
				when (point.value) {
					0f -> getString(R.string.shared_string_min) // "Min"
					0.5f -> getString(R.string.average) // "Average"
					1.0f -> getString(R.string.shared_string_max) // "Max"
					else -> "${(point.value * 100).toInt()}%"
				}
			} else {
				// Fixed values
				if (point.value % 1.0 == 0.0) {
					point.value.toInt().toString()
				} else {
					point.value.toString()
				}
			}

			// Use index as ID to ensure uniqueness even if values are duplicate during editing
			GradientStepData(
				id = index.toString(),
				label = label,
				point = point
			)
		}

		val selectedStep = stepData.getOrNull(selectedIndex)
		val selectedPoint = points.getOrNull(selectedIndex)

		// 2. Build Value State
		val isRelative = rangeType == GradientRangeType.RELATIVE
		val predefinedType = if (isRelative) RelativeConstants.valueOfRatio(selectedPoint?.value ?: -1f) else null

		val valueState = if (selectedPoint != null) {
			// Label next to input (e.g., "km/h" or empty for %)
			val inputLabel = if (isRelative) "" else draft.fileType.displayUnits.getSymbol()

			val label = if (isRelative) {
				predefinedType?.getName(true) ?: "%"
			} else {
				inputLabel
			}

			val decimalFormat = DecimalFormat("0.##")

			val text = if (isRelative && predefinedType != null) {
				""
			} else {
				decimalFormat.format(selectedPoint.value)
			}

			// Summary text logic
			val summaryText = if (predefinedType != null) {
				when (predefinedType) {
					RelativeConstants.MIN -> "Color for the minimum value found in the track." // TODO: Resource
					RelativeConstants.AVERAGE -> "Color for the average value of the track."   // TODO: Resource
					RelativeConstants.MAX -> "Color for the maximum value found in the track." // TODO: Resource
				}
			} else {
				null // Can add validation error text here if needed
			}

			ValueState(
				label = label,
				text = text,
				interactable = predefinedType == null, // Relative values are fixed (0, 0.5, 1)
				showTextField = true,
				summary = summaryText,
				error = null
			)
		} else {
			// Empty/No Selection State
			ValueState("", "", false, false, null, null)
		}

		// 3. Build Other States
		return EditorUiState(
			toolbarState = ToolbarState(
				showUndoButton = previousStates.isNotEmpty()
			),
			gradientState = GradientPreviewState(
				gradientFileType = draft.fileType,
				stepData = stepData,
				selectedItem = selectedStep
			),
			valueState = valueState,
			colorState = ColorState(
				colorInt = selectedPoint?.color ?: 0 // 0 or transparent if none selected
			),
			removeButtonState = RemoveButtonState(
				// Can remove only in Fixed mode and if we have > 2 points
				enabled = !isRelative && points.size > 2 && selectedIndex != -1
			)
		)
	}
}