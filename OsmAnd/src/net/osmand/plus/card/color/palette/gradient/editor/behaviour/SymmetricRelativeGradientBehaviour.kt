package net.osmand.plus.card.color.palette.gradient.editor.behaviour

import net.osmand.shared.palette.domain.GradientPoint
import kotlin.math.roundToInt

class SymmetricRelativeGradientBehaviour : GradientEditorBehaviour {
	
	private val mandatoryValues = setOf(-100, 0, 100)

	override fun isMandatoryPoint(point: GradientPoint): Boolean {
		val value = (point.value * 100).roundToInt()
		return mandatoryValues.contains(value)
	}
}