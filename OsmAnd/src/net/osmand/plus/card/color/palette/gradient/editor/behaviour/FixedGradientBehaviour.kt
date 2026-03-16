package net.osmand.plus.card.color.palette.gradient.editor.behaviour

import net.osmand.shared.palette.domain.GradientPoint

class FixedGradientBehaviour : GradientEditorBehaviour {

	override fun isMandatoryPoint(point: GradientPoint): Boolean {
		return false
	}
}