package net.osmand.plus.card.color.palette.gradient.editor.behaviour

import net.osmand.plus.card.color.palette.gradient.GradientFormatter
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientDraft
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.filetype.GradientFileType

/**
 * Defines the behavior for editing different types of gradients (Fixed vs Relative).
 */
interface GradientEditorBehaviour {

	/**
	 * Checks if a specific point is structural/mandatory and cannot be removed.
	 */
	fun isMandatoryPoint(point: GradientPoint): Boolean

	/**
	 * Determines if the user can manually edit the value of this point.
	 * Usually false for Min/Avg/Max in Relative mode.
	 */
	fun isValueEditable(point: GradientPoint): Boolean

	/**
	 * Checks if the remove button should be enabled.
	 */
	fun isRemoveEnabled(draft: GradientDraft, selectedIndex: Int): Boolean {
		return draft.points.size > 2 && selectedIndex != -1
	}

	/**
	 * Returns the label for a specific point to be displayed in the Chips/Steps list.
	 */
	fun getStepLabel(
		point: GradientPoint,
		fileType: GradientFileType,
		useFullName: Boolean = false
	): String {
		return GradientFormatter.formatValue(
			value = point.value,
			fileType = fileType,
			showUnits = false
		)
	}

	fun getSummary(point: GradientPoint): String? = null
}