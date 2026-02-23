package net.osmand.plus.card.color.palette.gradient.editor.behaviour

import net.osmand.plus.card.color.palette.gradient.editor.data.GradientDraft
import net.osmand.shared.palette.domain.GradientPoint

class FixedGradientBehaviour : GradientEditorBehaviour {

	override fun isMandatoryPoint(point: GradientPoint): Boolean {
		return false
	}

	override fun isValueEditable(point: GradientPoint): Boolean {
		return true
	}

	override fun isRemoveEnabled(draft: GradientDraft, selectedIndex: Int): Boolean {
		return draft.points.size > 2 && selectedIndex != -1
	}
}