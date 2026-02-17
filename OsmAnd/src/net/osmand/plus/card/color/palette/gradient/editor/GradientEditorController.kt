package net.osmand.plus.card.color.palette.gradient.editor

import androidx.fragment.app.FragmentManager
import net.osmand.OnResultCallback
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.card.color.palette.gradient.editor.contract.IGradientEditorController
import net.osmand.plus.card.color.palette.gradient.editor.contract.IGradientEditorView
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorDataState
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorStaticUiData
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientDraft
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientStepData
import net.osmand.plus.card.color.palette.gradient.editor.behaviour.FixedGradientBehaviour
import net.osmand.plus.card.color.palette.gradient.editor.behaviour.GradientEditorBehaviour
import net.osmand.plus.card.color.palette.gradient.editor.behaviour.RelativeGradientBehaviour
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientUpdateResult
import net.osmand.plus.card.color.palette.solid.SolidPaletteController
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.widgets.alert.AlertDialogData
import net.osmand.plus.widgets.alert.CustomAlert
import net.osmand.shared.palette.domain.GradientRangeType

class GradientEditorController(
	app: OsmandApplication,
	appMode: ApplicationMode,
	private val initialDraft: GradientDraft,
	private val callback: OnResultCallback<GradientDraft>
) : BaseDialogController(app), IGradientEditorController {

	companion object {
		private const val PROCESS_ID = "edit_gradient_palette"
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

	// --- State ---
	private var dataState: EditorDataState = EditorDataState(initialDraft, selectedIndex = 0)
	private val previousStates = ArrayDeque<EditorDataState>()

	// --- Specific Behaviour for the range type ---
	private val editorBehaviour: GradientEditorBehaviour = when (initialDraft.fileType.rangeType) {
		GradientRangeType.RELATIVE -> RelativeGradientBehaviour()
		GradientRangeType.FIXED_VALUES -> FixedGradientBehaviour()
	}

	private val uiBuilder = GradientEditorUiBuilder(app, editorBehaviour)

	private var view: IGradientEditorView? = null
	private var initialUiData: EditorStaticUiData? = null
	private var colorPaletteController: SolidPaletteController? = null

	override fun getId() = processId
	override fun getProcessId() = PROCESS_ID

	// --- Lifecycle ---

	override fun attachView(view: IGradientEditorView) {
		this.view = view
	}

	override fun detachView() {
		this.view = null
	}

	override fun onViewInitialized() {
		refreshView()
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
		// Check if this is the "No Data" item (value is NaN)
		val index = if (stepData.id == GradientEditorUiBuilder.NO_DATA_STEP_ID) {
			dataState.draft.points.size
		} else {
			stepData.id.toIntOrNull() ?: -1
		}
		if (index != -1 && index != dataState.selectedIndex) {
			// Navigation: Switch selection to the new index.
			// Important: Explicitly clear 'validationError' because the context of the error (previous field) is gone.
			val newState = dataState.copy(selectedIndex = index, validationError = null)
			applyState(newState, addToHistory = false)
		}
	}

	override fun onAddStepClick() {
		val newState = GradientEditorAlgorithms.addStep(dataState) ?: return
		applyState(newState)
	}

	override fun onValueInput(text: CharSequence) {
		val result = GradientEditorAlgorithms.updateValue(
			currentState = dataState,
			text = text.toString(),
			behaviour = editorBehaviour
		)

		when (result) {
			is GradientUpdateResult.Success -> {
				// Success: Update the state only if the data actually changed.
				// The validationError will be automatically cleared as it is null in the new state.
				if (result.newState != dataState) {
					applyState(result.newState)
				}
			}
			is GradientUpdateResult.Error -> {
				// Validation Error: Keep the existing data but inject the error message.
				// addToHistory = false: Typing an invalid character is a transient state and shouldn't be undoable.
				val errorState = dataState.copy(validationError = result.message)
				applyState(errorState, addToHistory = false)
			}
		}
	}

	override fun onRemoveStepClick() {
		val newState = GradientEditorAlgorithms.removeStep(dataState, editorBehaviour) ?: return
		applyState(newState)
	}

	override fun onColorSelected(colorInt: Int) {
		val newState = GradientEditorAlgorithms.updateColor(dataState, colorInt) ?: return
		applyState(newState)
	}

	private fun applyState(newState: EditorDataState, addToHistory: Boolean = true) {
		// Only push to the Undo stack if explicitly requested AND the core data (draft) has changed.
		// We do not track simple navigation changes or transient validation errors.
		if (addToHistory && newState.draft != dataState.draft) {
			pushState() // Pushes the *previous* valid state
		}
		dataState = newState
		refreshView()
	}

	override fun onSaveClick() {
		callback.onResult(dataState.draft)
	}

	override fun getColorPaletteController(): SolidPaletteController {
		var colorController = colorPaletteController
		if (colorController == null) {
			colorController = SolidPaletteController(app)
			colorPaletteController = colorController
		}
		return colorController
	}

	// --- Internal Helpers ---

	private fun pushState() {
		if (previousStates.size > UNDO_STACK_LIMIT) {
			previousStates.removeFirst()
		}
		previousStates.addLast(dataState.copy(validationError = null))
	}

	private fun refreshView() {
		val uiState = uiBuilder.buildUiState(
			dataState = dataState,
			isUndoAvailable = previousStates.isNotEmpty()
		)
		view?.render(uiState)
	}

	override fun getStaticUiData(): EditorStaticUiData {
		var data = initialUiData
		if (data == null) {
			data = uiBuilder.buildStaticUiData(initialDraft)
			initialUiData = data
		}
		return data
	}
}