package net.osmand.plus.card.color.palette.gradient.editor.behaviour

import net.osmand.plus.card.color.palette.gradient.editor.data.GradientDraft
import net.osmand.plus.card.color.palette.gradient.editor.data.RelativeConstants
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.filetype.GradientFileType

class RelativeGradientBehaviour : GradientEditorBehaviour {


	override fun isMandatoryPoint(point: GradientPoint): Boolean {
		return RelativeConstants.valueOfRatio(point.value) != null
	}

	override fun isValueEditable(point: GradientPoint): Boolean {
		return !isMandatoryPoint(point)
	}

	override fun isRemoveEnabled(draft: GradientDraft, selectedIndex: Int): Boolean {
		val point = draft.points.getOrNull(selectedIndex) ?: return false
		return RelativeConstants.valueOfRatio(point.value) == null && super.isRemoveEnabled(draft, selectedIndex)
	}

	override fun getStepLabel(
		point: GradientPoint,
		fileType: GradientFileType,
		useFullName: Boolean
	): String {
		val constant = RelativeConstants.valueOfRatio(point.value)
		return constant?.getName(useFullName) ?: super.getStepLabel(point, fileType, useFullName)
	}

	override fun getSummary(point: GradientPoint): String? {
		val constant = RelativeConstants.valueOfRatio(point.value)
		return constant?.getSummary()
	}
}