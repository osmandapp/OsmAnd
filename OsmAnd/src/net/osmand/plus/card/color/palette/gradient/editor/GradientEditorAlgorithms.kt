package net.osmand.plus.card.color.palette.gradient.editor

import net.osmand.plus.card.color.palette.gradient.editor.behaviour.GradientEditorBehaviour
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorDataState
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientUpdateResult
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.util.Localization
import kotlin.math.abs
import kotlin.math.max

object GradientEditorAlgorithms {

	// Technical tolerance to handle float precision issues (e.g. 50.0 vs 49.9999)
	private const val FLOAT_TOLERANCE = 0.005f
	private const val DEFAULT_STEP_INCREMENT = 10f

	/**
	 * Calculates and adds a new step based on the current selection.
	 * returns null if a valid step cannot be added (e.g. limit reached or duplicate).
	 */
	fun addStep(currentState: EditorDataState): EditorDataState? {
		val draft = currentState.draft
		val selectedIndex = currentState.selectedIndex
		val points = draft.points
		val fileType = draft.fileType

		// 1. Resolve limits from FileType configuration
		val minLimit = fileType.minLimit ?: Float.NEGATIVE_INFINITY
		val maxLimit = fileType.maxLimit ?: Float.POSITIVE_INFINITY

		if (selectedIndex !in points.indices) return null

		val currentPoint = points[selectedIndex]

		// 2. Calculate candidate value
		var newValue: Float = if (selectedIndex < points.lastIndex) {
			// INSERT MODE: Split the interval between the current and the next point
			val nextPoint = points[selectedIndex + 1]
			(currentPoint.value + nextPoint.value) / 2f
		} else {
			// APPEND MODE: Extrapolate forward
			val prevPoint = if (selectedIndex > 0) points[selectedIndex - 1] else null

			// Heuristic: Use the previous interval step or a default increment
			val step = if (prevPoint != null) {
				currentPoint.value - prevPoint.value
			} else {
				DEFAULT_STEP_INCREMENT
			}

			currentPoint.value + step
		}

		// 3. Apply constraints
		// If the value exceeds limits, clamp it to the boundary
		if (newValue > maxLimit) newValue = maxLimit
		if (newValue < minLimit) newValue = minLimit

		// 4. Validate uniqueness
		// If the calculated (or clamped) value already exists, the action is invalid
		val isDuplicate = points.any { abs(it.value - newValue) < FLOAT_TOLERANCE }
		if (isDuplicate) {
			return null
		}

		// 5. Commit changes
		val baseColor = currentPoint.color
		val newPoint = GradientPoint(newValue, baseColor)

		// GradientDraft is expected to auto-sort points upon addition
		val newDraft = draft.withPointAdded(newPoint)
		val newIndex = newDraft.points.indexOf(newPoint)

		return EditorDataState(newDraft, newIndex)
	}

	/**
	 * Removes the selected step if constraints allow.
	 * returns null if removal is not permitted (e.g. mandatory point or too few points).
	 */
	fun removeStep(currentState: EditorDataState, behaviour: GradientEditorBehaviour): EditorDataState? {
		val draft = currentState.draft
		val selectedIndex = currentState.selectedIndex
		val points = draft.points

		// 1. Validate selection exists
		if (selectedIndex !in points.indices) return null

		// 2. Validate minimum points count
		// A gradient must have at least 2 points to be valid.
		if (points.size <= 2) return null

		val pointToRemove = points[selectedIndex]

		// 3. Check behavioral constraints
		// Delegate to the behavior to protect mandatory points (e.g. Min/Max in Relative mode).
		if (behaviour.isMandatoryPoint(pointToRemove)) {
			return null
		}

		// 4. Commit changes
		val newDraft = draft.withPointRemoved(pointToRemove)

		// 5. Adjust Selection
		// Select the previous point (or stay at 0 if we removed the first one).
		// This provides a natural UX flow (cursor moves left).
		val newIndex = if (newDraft.points.isNotEmpty()) {
			max(0, selectedIndex - 1)
		} else {
			-1
		}

		return EditorDataState(newDraft, newIndex)
	}

	/**
	 * Updates the value of the currently selected step based on user input.
	 * Handles conversion from Display Units (user input) to Base Units (storage).
	 */
	fun updateValue(
		currentState: EditorDataState,
		text: String,
		behaviour: GradientEditorBehaviour
	): GradientUpdateResult {
		val draft = currentState.draft
		val selectedIndex = currentState.selectedIndex
		val points = draft.points
		val fileType = draft.fileType

		// 1. Validate selection
		if (selectedIndex !in points.indices) {
			return GradientUpdateResult.Error(Localization.getString("unexpected_error_occurred_warn"))
		}

		val currentPoint = points[selectedIndex]

		// 2. Check edit ability
		// Returns the same object without indication if value isn't editable
		if (!behaviour.isValueEditable(currentPoint)) {
			return GradientUpdateResult.Success(currentState)
		}

		// 3. Parse Input (Display Value)
		val inputDisplayValue = text.toFloatOrNull()
			?: return GradientUpdateResult.Error(
				Localization.getString("gradient_input_invalid_value_warn",)
			)

		// 4. Convert: Display Units -> Base Units
		val newValueBase = fileType.baseUnits.from(
			value = inputDisplayValue.toDouble(),
			sourceUnit = fileType.displayUnits
		).toFloat()

		// 5. Validate Limits (Limits are defined in Base Units)
		val minLimitBase = fileType.minLimit
		val maxLimitBase = fileType.maxLimit

		if (minLimitBase != null && newValueBase < minLimitBase) {
			// Convert limit back to Display Units for the error message
			// We use 'from' on Display Unit to convert FROM Base Unit.
			val minLimitDisplay = fileType.displayUnits.from(
				value = minLimitBase.toDouble(),
				sourceUnit = fileType.baseUnits
			).toFloat()

			return GradientUpdateResult.Error(
				Localization.getString(
					"gradient_input_value_too_low_warn",
					formatNumber(minLimitDisplay)
				)
			)
		}

		if (maxLimitBase != null && newValueBase > maxLimitBase) {
			// Convert limit back to Display Units
			val maxLimitDisplay = fileType.displayUnits.from(
				value = maxLimitBase.toDouble(),
				sourceUnit = fileType.baseUnits
			).toFloat()

			return GradientUpdateResult.Error(
				Localization.getString(
					"gradient_input_value_too_high_warn",
					formatNumber(maxLimitDisplay)
				)
			)
		}

		// 6. Validate Uniqueness (Compare in Base Units)
		// Use tolerance to handle potential float precision issues after conversion
		val isDuplicate = points.any {
			it !== currentPoint && abs(it.value - newValueBase) < FLOAT_TOLERANCE
		}

		if (isDuplicate) {
			return GradientUpdateResult.Error(Localization.getString("gradient_input_value_duplicate_warn"))
		}

		// 7. Success - Apply Update
		if (abs(currentPoint.value - newValueBase) < FLOAT_TOLERANCE) {
			return GradientUpdateResult.Success(currentState)
		}

		val newPoint = currentPoint.copy(value = newValueBase)
		val newDraft = draft.withPointUpdated(currentPoint, newPoint)

		val newIndex = newDraft.points.indexOfFirst {
			it.value == newPoint.value && it.color == newPoint.color
		}

		return GradientUpdateResult.Success(EditorDataState(newDraft, newIndex))
	}

	/**
	 * Updates the color of the currently selected step.
	 * @return A new state with the updated color, or null if the action is invalid (e.g. no selection)
	 * or redundant (same color selected).
	 */
	fun updateColor(currentState: EditorDataState, newColor: Int): EditorDataState? {
		val draft = currentState.draft
		val selectedIndex = currentState.selectedIndex
		val points = draft.points

		// Case 1: "No Data" selected
		if (selectedIndex == points.size) {
			val currentColor = draft.noDataColor ?: net.osmand.shared.ColorPalette.LIGHT_GREY
			if (currentColor == newColor) return null
			val newDraft = draft.copy(noDataColor = newColor)
			return currentState.copy(draft = newDraft)
		}

		// Case 2: Regular Gradient Point selected
		// 1. Validate selection
		if (selectedIndex !in points.indices) return null

		val currentPoint = points[selectedIndex]

		// 2. Check for redundancy
		// Avoid creating new state if the color hasn't effectively changed.
		if (currentPoint.color == newColor) return null

		// 3. Apply changes
		val newPoint = currentPoint.copy(color = newColor)

		// Since we modify only the color, the value (position) remains the same.
		// Therefore, the sorting order and the selected index should remain unchanged.
		val newDraft = draft.withPointUpdated(currentPoint, newPoint)

		return EditorDataState(newDraft, selectedIndex)
	}

	// Helper to format limits nicely (remove trailing zeros: 50.0 -> "50")
	private fun formatNumber(value: Float): String {
		return if (value % 1.0 == 0.0) {
			value.toInt().toString()
		} else {
			value.toString()
		}
	}
}