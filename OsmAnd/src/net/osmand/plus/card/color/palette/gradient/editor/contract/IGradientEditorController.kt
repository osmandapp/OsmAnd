package net.osmand.plus.card.color.palette.gradient.editor.contract

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorStaticUiData
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientStepData
import net.osmand.plus.card.color.palette.solid.SolidPaletteController

interface IGradientEditorController : IDialogController {

	fun getId(): String

	fun attachView(view: IGradientEditorView)

	fun detachView()

	fun onViewInitialized()

	fun getStaticUiData(): EditorStaticUiData

	fun onBackClick()

	fun onUndoClick()

	fun onStepClick(stepData: GradientStepData)

	fun onAddStepClick()

	fun onValueInput(text: CharSequence)

	fun getColorController(): SolidPaletteController

	fun onColorSelected(colorInt: Int)

	fun onRemoveStepClick()

	fun onSaveClick()
}