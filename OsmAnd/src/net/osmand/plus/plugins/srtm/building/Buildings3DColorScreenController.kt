package net.osmand.plus.plugins.srtm.building

import android.view.View
import androidx.fragment.app.FragmentManager
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.configmap.MultiStateColorPaletteController
import net.osmand.plus.dashboard.DashboardType.BUILDINGS_3D
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.plugins.srtm.SRTMPlugin
import net.osmand.plus.settings.enums.DayNightMode
import net.osmand.plus.settings.enums.ThemeUsageContext
import net.osmand.plus.widgets.popup.PopUpMenu
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.shared.palette.domain.PaletteItem

class Buildings3DColorScreenController(
	app: OsmandApplication,
	val plugin: SRTMPlugin
) : MultiStateColorPaletteController(
	app,
	plugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.get() ?: SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR,
	plugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get() ?: SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR
) {

	companion object {
		const val PROCESS_ID = "buildings_3d_color_screen"

		@JvmStatic
		fun showDialog(fragmentManager: FragmentManager, app: OsmandApplication) {
			val plugin = PluginsHelper.getPlugin(SRTMPlugin::class.java) ?: return
			val controller = Buildings3DColorScreenController(app, plugin)

			app.dialogManager.register(PROCESS_ID, controller)
			if (!Buildings3DColorFragment.showInstance(fragmentManager)) {
				app.dialogManager.unregister(PROCESS_ID)
			}
		}

		fun getExistedInstance(app: OsmandApplication): Buildings3DColorScreenController? {
			return app.dialogManager.findController(PROCESS_ID) as? Buildings3DColorScreenController
		}
	}

	var colorType: Buildings3DColorType =
		Buildings3DColorType.getById(plugin.BUILDINGS_3D_COLOR_STYLE.get() ?: 1)
	private val initialColorType: Buildings3DColorType = colorType
	private var applyChanges = false

	override fun getProcessId(): String = PROCESS_ID

	override fun getDialogTitle(): String = app.getString(R.string.enable_3d_objects)

	// --- IMultiStateCardController Implementation ---

	override fun getCardTitle(): String = app.getString(R.string.shared_string_color)

	override fun getCardStateSelectorTitle(): String = app.getString(colorType.labelId)

	override fun hasSelector(): Boolean = true

	override fun onSelectorButtonClicked(selectorView: View) {
		val items = mutableListOf<PopUpMenuItem>()

		val types = Buildings3DColorType.entries.toTypedArray()
		types.forEachIndexed { index, type ->
			items.add(PopUpMenuItem.Builder(app)
				.setTitleId(type.labelId)
				.setOnClickListener  {
					colorType = types[index]
					notifyCardStateChanged()
					plugin.apply3DBuildingsColorStyle(colorType)
					externalListener?.onPaletteModeChanged()
				}
				.create()
			)
		}

		val displayData = PopUpMenuDisplayData()
		displayData.anchorView = selectorView
		displayData.menuItems = items
		displayData.nightMode = isNightMode
		PopUpMenu.show(displayData)
	}

	// --- Lifecycle Logic ---

	override fun onCloseScreen(activity: MapActivity) {
		setSavedColors(applyChanges)
		if (!applyChanges) {
			if (initialColorType != colorType) {
				plugin.BUILDINGS_3D_COLOR_STYLE.set(initialColorType.id)
			}
			plugin.apply3DBuildingsColorStyle(Buildings3DColorType.getById(plugin.BUILDINGS_3D_COLOR_STYLE.get()))

			val isAppNightMode = app.daynightHelper.isNightMode(app.settings.applicationMode, ThemeUsageContext.APP)
			val colorToRestore = if (isAppNightMode) {
				plugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get()
			} else {
				plugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.get()
			}
			plugin.apply3DBuildingsColor(colorToRestore ?: SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR)
		}
		activity.supportFragmentManager.popBackStack()
		activity.dashboard.setDashboardVisibility(true, BUILDINGS_3D, false)
	}

	override fun onApplyChanges() {
		applyChanges = true
		plugin.BUILDINGS_3D_COLOR_STYLE.set(colorType.id)
		plugin.apply3DBuildingsColorStyle(colorType)
		loadSavedColors()
	}

	override fun onResetToDefault() {
		colorDay = SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR
		colorNight = SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR
		colorType = Buildings3DColorType.MAP_STYLE
		plugin.apply3DBuildingsColorStyle(colorType)
		setSavedColors(true)

		notifyCardStateChanged()
		externalListener?.onPaletteModeChanged()
	}

	override fun hasChanges(): Boolean {
		return initialColorType != colorType
				|| (colorType == Buildings3DColorType.CUSTOM && super.hasChanges())
	}

	override fun getMapTheme(): DayNightMode? {
		return if (colorType == Buildings3DColorType.CUSTOM) super.getMapTheme() else null
	}

	// --- Color Persistence Logic ---

	override fun setSavedColor(color: Int, nightMode: Boolean) {
		if (nightMode) {
			plugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.set(color)
			colorNight = color
		} else {
			plugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.set(color)
			colorDay = color
		}
	}

	override fun getSavedColor(nightMode: Boolean): Int {
		return if (nightMode) {
			plugin.BUILDINGS_3D_CUSTOM_NIGHT_COLOR.get() ?: SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR
		} else {
			plugin.BUILDINGS_3D_CUSTOM_DAY_COLOR.get() ?: SRTMPlugin.BUILDINGS_3D_DEFAULT_COLOR
		}
	}

	private fun loadSavedColors() {
		colorDay = getSavedColor(false)
		colorNight = getSavedColor(true)
	}

	override fun onColorSelectedFromPalette(paletteItem: PaletteItem) {
		if (paletteItem is PaletteItem.Solid) {
			setSavedColor(paletteItem.colorInt, isNightMap)
			loadSavedColors()
			plugin.apply3DBuildingsColor(paletteItem.colorInt)
			externalListener?.onPaletteItemSelected(paletteItem)
		}
	}

	override fun onColorsPaletteModeChanged() {
		externalListener?.onPaletteModeChanged()
	}
}