package net.osmand.plus.card.color.palette.gradient.editor

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.editor.behaviour.GradientEditorBehaviour
import net.osmand.plus.card.color.palette.gradient.editor.data.*
import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.GradientPoint
import java.text.DecimalFormat

class GradientEditorUiBuilder(
	private val app: OsmandApplication,
	private val behaviour: GradientEditorBehaviour
) {

	companion object {
		const val NO_DATA_STEP_ID = "no_data_step"
	}

	private val decimalFormat = DecimalFormat("0.#####")

	/**
	 * Builds static UI data (Toolbar title, subtitle) that rarely changes.
	 */
	fun buildStaticUiData(initialDraft: GradientDraft): EditorStaticUiData {
		val toolbarTitle = app.getString(
			if (initialDraft.originalId == null) {
				R.string.add_palette
			} else {
				R.string.edit_palette
			}
		)

		val fileType = initialDraft.fileType
		val units = fileType.displayUnits.getSymbol()

		val toolbarSubtitle = app.getString(
			R.string.ltr_or_rtl_combine_with_brackets,
			fileType.category.getDisplayName(),
			units
		)

		return EditorStaticUiData(
			toolbarTitle = toolbarTitle,
			toolbarSubtitle = toolbarSubtitle
		)
	}

	/**
	 * Maps the current data state into a View State.
	 * @param dataState Current data model.
	 * @param isUndoAvailable Boolean flag to show/hide Undo button.
	 */
	fun buildUiState(dataState: EditorDataState, isUndoAvailable: Boolean): EditorUiState {
		val draft = dataState.draft
		val points = draft.points
		val selectedIndex = dataState.selectedIndex
		val fileType = draft.fileType

		val noDataColor = draft.noDataColor ?: ColorPalette.LIGHT_GREY
		val noDataPoint = GradientPoint(Float.NaN, noDataColor)

		// 1. Build Gradient Steps (Chips)
		val stepData = buildList {

			// A. Add regular steps via Behaviour
			addAll(points.mapIndexed { index, point ->
				GradientStepData(
					id = index.toString(),
					label = behaviour.getStepLabel(point, fileType),
					point = point
				)
			})

			// B. Add "No Data" step
			add(GradientStepData(
				id = NO_DATA_STEP_ID,
				label = app.getString(R.string.gpx_logging_no_data),
				point = noDataPoint
			))
		}

		val isNoDataSelected = selectedIndex == points.size
		val selectedStep = stepData.getOrNull(selectedIndex)
		val selectedPoint = if (isNoDataSelected) noDataPoint else points.getOrNull(selectedIndex)

		val baseUnits = fileType.baseUnits
		val displayUnits = fileType.displayUnits

		// 2. Build Value State via Behaviour
		// We inject the validation error from the dataState here
		val valueState = if (selectedPoint != null) {
			if (isNoDataSelected) {
				ValueState(
					label = "",
					text = "",
					interactable = false,
					showTextField = false,
					summary = app.getString(R.string.gradient_no_data_point_summary),
					error = null
				)
			} else {
				val mandatory = behaviour.isMandatoryPoint(selectedPoint)
				ValueState(
					label = if (mandatory) {
						behaviour.getStepLabel(selectedPoint, fileType, useFullName = true)
					} else {
						fileType.displayUnits.getSymbol()
					},
					text = if (mandatory) {
						""
					} else {
						val valueInDisplayUnits =
							displayUnits.from(selectedPoint.value.toDouble(), baseUnits)
						decimalFormat.format(valueInDisplayUnits)
					},
					interactable = !mandatory,
					showTextField = true,
					summary = behaviour.getSummary(selectedPoint),
					error = dataState.validationError
				)
			}
		} else {
			ValueState(
				label = "",
				text = "",
				interactable = false,
				showTextField = false,
				summary = null,
				error = null
			)
		}

		// 3. Build complete UI State
		return EditorUiState(
			toolbarState = ToolbarState(
				showUndoButton = isUndoAvailable
			),
			gradientState = GradientPreviewState(
				gradientFileType = draft.fileType,
				stepData = stepData,
				selectedItem = selectedStep
			),
			valueState = valueState,
			colorState = ColorState(
				colorInt = selectedPoint?.color ?: 0
			),
			removeButtonState = RemoveButtonState(
				enabled = !isNoDataSelected && behaviour.isRemoveEnabled(draft, selectedIndex)
			)
		)
	}
}