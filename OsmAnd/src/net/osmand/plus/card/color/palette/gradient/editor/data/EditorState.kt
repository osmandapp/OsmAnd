package net.osmand.plus.card.color.palette.gradient.editor.data

import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.filetype.GradientFileType

data class EditorDataState(
	val draft: GradientDraft,
	val selectedIndex: Int,
	val validationError: String? = null
)

data class EditorStaticUiData(
	val toolbarTitle: String,
	val toolbarSubtitle: String
)

data class EditorUiState(
	val gradientState: GradientPreviewState,
	val valueState: ValueState,
	val colorState: ColorState,
	val toolbarState: ToolbarState,
	val removeButtonState: RemoveButtonState
)

data class ToolbarState(
	val showUndoButton: Boolean
)

data class GradientPreviewState(
	val gradientFileType: GradientFileType,
	val stepData: List<GradientStepData>,
	val selectedItem: GradientStepData?
)

data class ValueState(
	val label: String?,           // text for units part (e.g. km/h)
	val text: String? = null,     // main text / input from user, if null than don't update (e.g. when we input new value from user)
	val interactable: Boolean,    // indicates if text field is enabled or disabled (for values those are not allowed to edit)
	val showTextField: Boolean,   // hide text field if "No data" selected
	val summary: String?,         // additional summary to describe specific value
	val error: String?            // error message displayed for invalid input values
)

data class ColorState(
	val colorInt: Int
)

data class RemoveButtonState(
	val enabled: Boolean
)

data class GradientStepData(
	val id: String,
	val point: GradientPoint,
	val label: String,
)