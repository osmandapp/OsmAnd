package net.osmand.plus.views.mapwidgets.configure.appearance

import androidx.annotation.ColorInt
import androidx.fragment.app.FragmentActivity
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.chooseplan.ChoosePlanFragment
import net.osmand.plus.configmap.MapColorPaletteController
import net.osmand.plus.configmap.MapColorPaletteFragment
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceState
import net.osmand.plus.views.mapwidgets.appearance.PanelColorTarget
import net.osmand.shared.palette.domain.PaletteItem

class PanelColorController private constructor(
	app: OsmandApplication,
	private val appearanceSettings: PanelAppearanceSettings,
	private val appMode: ApplicationMode,
	private val target: PanelColorTarget,
	private val layoutMode: ScreenLayoutMode?,
	private val initialState: PanelAppearanceState
) : MapColorPaletteController(
	app,
	initialState.getColor(target, false),
	initialState.getColor(target, true)
) {

	companion object {

		@JvmStatic
		fun showDialog(activity: MapActivity, panel: WidgetsPanel, target: PanelColorTarget,
		               appMode: ApplicationMode, layoutMode: ScreenLayoutMode?) {
			val app = activity.app
			if (getExistedInstance(app) != null) {
				return
			}
			val appearanceSettings = app.panelAppearanceSettingsManager[panel]
			val initialState = appearanceSettings.readState(appMode, layoutMode)
			val controller = PanelColorController(
				app, appearanceSettings, appMode, target, layoutMode, initialState
			)

			if (MapColorPaletteFragment.showInstance(activity.supportFragmentManager)) {
				app.dialogManager.register(controller.processId, controller)
				controller.activate()
			}
		}
	}

	private val appearanceManager = app.panelAppearanceSettingsManager
	private var draftState = initialState.withCustomTarget(target)
	private var previewToken: Long? = null
	private var lastAvailability = isColorSelectionAvailable()
	private var disposed = false

	override fun getPreviewPanel(): WidgetsPanel = appearanceSettings.panel

	override fun getDialogTitle(): String {
		return app.getString(appearanceSettings.panel.getTitleId(AndroidUtils.isLayoutRtl(app)))
	}

	override fun getColorSectionTitleId(): Int = when (target) {
		PanelColorTarget.TEXT -> R.string.text_color
		PanelColorTarget.SECONDARY_TEXT -> R.string.secondary_text_color
		PanelColorTarget.BACKGROUND -> R.string.background_color
	}

	override fun isColorSelectionAvailable(): Boolean {
		return target != PanelColorTarget.BACKGROUND
				|| InAppPurchaseUtils.isCustomWidgetBackgroundColorAvailable(app)
	}

	override fun getUnavailableIconId(): Int = R.drawable.ic_action_widget_colored

	override fun getUnavailableTitleId(): Int = R.string.custom_widget_colors

	override fun getUnavailableDescriptionId(): Int = R.string.custom_widget_colors_description

	override fun getUnavailableActionTitleId(): Int = R.string.unlock_custom_colors

	override fun onUnavailableAction(activity: FragmentActivity) {
		ChoosePlanFragment.showDefaultInstance(activity)
	}

	override fun onCloseScreen(activity: MapActivity) {
		dispose()
		refreshWidgets()
		activity.supportFragmentManager.popBackStack()
	}

	override fun onApplyChanges() {
		if (!isColorSelectionAvailable()) {
			return
		}
		val saved = appearanceSettings.commitCustomColors(
			target, appMode, layoutMode, colorDay, colorNight
		)
		if (saved) {
			dispose()
			refreshWidgets()
		}
	}

	override fun hasChanges(): Boolean {
		return isColorSelectionAvailable && (!initialState.isCustom(target) || super.hasChanges())
	}

	override fun onResetToDefault() {
		if (!isColorSelectionAvailable) {
			return
		}
		colorDay = defaultColor(false)
		colorNight = defaultColor(true)
		draftState = draftState
			.withColor(target, false, colorDay)
			.withColor(target, true, colorNight)
		publishDraft()
		refreshWidgets()
		refreshSelectedPaletteColor()
	}

	override fun setSavedColor(@ColorInt color: Int, nightMode: Boolean) {
		if (nightMode) {
			colorNight = color
		} else {
			colorDay = color
		}
		draftState = draftState.withColor(target, nightMode, color)
		publishDraft()
	}

	@ColorInt
	override fun getSavedColor(nightMode: Boolean): Int {
		return draftState.getColor(target, nightMode)
	}

	override fun onColorSelectedFromPalette(paletteItem: PaletteItem) {
		if (paletteItem is PaletteItem.Solid) {
			setSavedColor(paletteItem.colorInt, isNightMap)
			refreshWidgets()
			externalListener?.onPaletteItemSelected(paletteItem)
		}
	}

	override fun onColorsPaletteModeChanged() {
		externalListener?.onPaletteModeChanged()
	}

	override fun onResume() {
		super.onResume()
		if (disposed) {
			return
		}
		handleAvailabilityChanged()
		if (lastAvailability) {
			startPreview()
		} else {
			finishPreview()
		}
	}

	override fun finishProcessIfNeeded(activity: FragmentActivity?): Boolean {
		val finished = super.finishProcessIfNeeded(activity)
		if (finished) {
			dispose()
			refreshWidgets()
		}
		return finished
	}

	private fun startPreview() {
		if (disposed || !isColorSelectionAvailable() || previewToken != null) {
			return
		}
		previewToken = appearanceManager.beginPreview(
			appearanceSettings.panel, appMode, layoutMode, draftState
		)
		refreshWidgets()
	}

	private fun activate() {
		startPreview()
	}

	private fun publishDraft() {
		previewToken?.let { appearanceManager.updatePreview(it, draftState) }
	}

	private fun finishPreview() {
		previewToken?.let(appearanceManager::endPreview)
		previewToken = null
	}

	private fun handleAvailabilityChanged() {
		val available = isColorSelectionAvailable()
		if (available == lastAvailability) {
			return
		}
		lastAvailability = available
		if (available) {
			startPreview()
		} else {
			finishPreview()
			refreshWidgets()
		}
		externalListener?.onAvailabilityChanged()
	}

	private fun dispose() {
		if (disposed) {
			return
		}
		disposed = true
		finishPreview()
	}

	private fun refreshSelectedPaletteColor() {
		val paletteController = colorsPaletteController
		val selectedMode = paletteController.selectedPaletteMode
		val newPaletteColor = paletteController.provideSelectedPaletteItemForMode(selectedMode)
		if (newPaletteColor != null) {
			paletteController.selectPaletteItem(newPaletteColor)
		}
	}

	private fun refreshWidgets() {
		app.osmandMap.mapLayers.mapInfoLayer?.refreshWidgetAppearance()
	}

	@ColorInt
	private fun defaultColor(nightMode: Boolean): Int {
		return PanelAppearanceSettings.getDefaultColor(app, appearanceSettings.panel, target, nightMode)
	}
}