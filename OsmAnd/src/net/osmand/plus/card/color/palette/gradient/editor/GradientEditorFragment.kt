package net.osmand.plus.card.color.palette.gradient.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import net.osmand.plus.R
import net.osmand.plus.base.BaseFullScreenDialogFragment
import net.osmand.plus.base.dialog.BaseDialogController
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog
import net.osmand.plus.card.color.palette.gradient.editor.contract.IGradientEditorController
import net.osmand.plus.card.color.palette.gradient.editor.contract.IGradientEditorView
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorStaticUiData
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.card.color.palette.gradient.editor.section.ActionsSection
import net.osmand.plus.card.color.palette.gradient.editor.section.ChartSection
import net.osmand.plus.card.color.palette.gradient.editor.section.ColorSection
import net.osmand.plus.card.color.palette.gradient.editor.section.ToolbarSection
import net.osmand.plus.card.color.palette.gradient.editor.section.UiSection
import net.osmand.plus.card.color.palette.gradient.editor.section.ValuesSection
import net.osmand.plus.palette.contract.IExternalPaletteListener
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.widgets.dialogbutton.DialogButton
import net.osmand.shared.palette.domain.PaletteItem

class GradientEditorFragment : BaseFullScreenDialogFragment(), IGradientEditorView, IDialog, IExternalPaletteListener {

	companion object {
		private const val TAG = "GradientEditorFragment"

		private const val CONTROLLER_ID_KEY = "controller_id"

		fun showInstance(
			manager: FragmentManager,
			appMode: ApplicationMode,
			controllerId: String
		): Boolean {
			if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
				val fragment = GradientEditorFragment()
				val arguments = Bundle()
				arguments.putString(APP_MODE_KEY, appMode.stringKey)
				arguments.putString(CONTROLLER_ID_KEY, controllerId)
				fragment.arguments = arguments
				fragment.show(manager, TAG)
				return true
			}
			return false
		}
	}

	private var controller: IGradientEditorController? = null

	private var cachedUiState: EditorUiState? = null

	// UI Sections
	private lateinit var uiSections: List<UiSection>
	private lateinit var toolbarSection: ToolbarSection
	private lateinit var chartSection: ChartSection
	private lateinit var valuesSection: ValuesSection
	private lateinit var colorSection: ColorSection
	private lateinit var actionsSection: ActionsSection

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val bundle = savedInstanceState ?: arguments
		val controllerId = bundle?.getString(CONTROLLER_ID_KEY)

		controllerId?.let { id ->
			controller = app.dialogManager.findController(id) as? IGradientEditorController
		}
		controller?.attachView(this) ?: dismiss()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		updateNightMode()
		val view = inflate(R.layout.fragment_gradient_editor, container, false)
		controller?.let { initView(it, view) }
		return view
	}

	private fun initView(controller: IGradientEditorController, view: View) {
		cachedUiState = null
		val staticUiData: EditorStaticUiData = controller.getStaticUiData()

		// 1. Toolbar Section
		toolbarSection = ToolbarSection(view, staticUiData, app, nightMode)
		toolbarSection.onBackClicked = {
			controller.onBackClick()
		}
		toolbarSection.onUndoClicked = {
			controller.onUndoClick()
		}

		// 2. Chart Section (Graph + Chips)
		chartSection = ChartSection(view, app, nightMode)
		chartSection.onStepClicked = { step ->
			controller.onStepClick(step)
		}
		chartSection.onAddClicked = {
			controller.onAddStepClick()
		}

		// 3. Values Section (Input)
		valuesSection = ValuesSection(view, app, nightMode) { newValue ->
			controller.onValueInput(newValue)
		}

		// 4. Color Section
		colorSection = ColorSection(view, requireActivity(), app, nightMode,
			this, controller.getColorPaletteController())

		// 5. Actions Section (Remove button)
		actionsSection = ActionsSection(view, app, nightMode) {
			controller.onRemoveStepClick()
		}

		// Apply Button
		view.findViewById<DialogButton>(R.id.apply_button)?.setOnClickListener {
			controller.onSaveClick()
			dismiss()
		}

		uiSections =
			listOf(toolbarSection, chartSection, valuesSection, colorSection, actionsSection)

		controller.onViewInitialized()
	}

	override fun render(uiState: EditorUiState) {
		val oldUiState = cachedUiState
		uiSections.forEach { it.update(oldUiState, uiState) }
		cachedUiState = uiState
	}

	override fun onPaletteItemSelected(item: PaletteItem) {
		if (item is PaletteItem.Solid) {
			controller?.onColorSelected(item.colorInt)
		}
	}

	override fun getThemeId(): Int {
		return if (nightMode) R.style.OsmandDarkTheme_DarkActionbar else R.style.OsmandLightTheme_DarkActionbar
	}

	override fun onResume() {
		super.onResume()
		callMapActivity { it.disableDrawer() }

		dialog?.setOnKeyListener { _, keyCode, event ->
			if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
				controller?.onBackClick()
				true
			} else {
				false
			}
		}
	}

	override fun onPause() {
		super.onPause()
		callMapActivity { it.enableDrawer() }
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putString(CONTROLLER_ID_KEY, controller?.getId())
	}

	override fun onDestroy() {
		super.onDestroy()
		controller?.detachView()
		(controller as? BaseDialogController)?.finishProcessIfNeeded(activity)
	}

	override fun getFragmentActivity() = activity
}