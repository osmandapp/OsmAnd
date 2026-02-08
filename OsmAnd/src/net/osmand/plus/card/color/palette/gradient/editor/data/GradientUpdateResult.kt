package net.osmand.plus.card.color.palette.gradient.editor.data

sealed class GradientUpdateResult {
	data class Success(val newState: EditorDataState) : GradientUpdateResult()
	data class Error(val message: String) : GradientUpdateResult()
}