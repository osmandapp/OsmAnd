package net.osmand.plus.card.color.palette.gradient.editor.contract

import androidx.fragment.app.FragmentActivity
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState

interface IGradientEditorView {

	fun render(uiState: EditorUiState)

	fun dismiss()

	fun getFragmentActivity(): FragmentActivity?

	fun isNightMode(): Boolean
}