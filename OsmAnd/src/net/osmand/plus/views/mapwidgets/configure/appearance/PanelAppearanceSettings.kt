package net.osmand.plus.views.mapwidgets.configure.appearance

import android.content.Context
import androidx.annotation.ColorInt
import net.osmand.StateChangedListener
import net.osmand.plus.R
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.settings.enums.PanelBackgroundMode
import net.osmand.plus.settings.enums.PanelIconMode
import net.osmand.plus.settings.enums.PanelSizeMode
import net.osmand.plus.settings.enums.PanelTextColorMode
import net.osmand.plus.settings.enums.ScreenLayoutMode
import net.osmand.plus.settings.enums.WidgetSize
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceState
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceResolver
import net.osmand.plus.views.mapwidgets.appearance.PanelColorTarget

class PanelAppearanceSettings internal constructor(
	private val settings: OsmandSettings,
	val panel: WidgetsPanel
) {

	companion object {
		private val SIZE_MODE_VALUES = PanelSizeMode.entries.toTypedArray()
		private val ICON_MODE_VALUES = PanelIconMode.entries.toTypedArray()
		private val TEXT_COLOR_MODE_VALUES = PanelTextColorMode.entries.toTypedArray()
		private val BACKGROUND_MODE_VALUES = PanelBackgroundMode.entries.toTypedArray()

		@JvmStatic
		@ColorInt
		fun getDefaultColor(app: OsmandApplication, panel: WidgetsPanel, target: PanelColorTarget,
		                    nightMode: Boolean): Int {
			return when (target) {
				PanelColorTarget.TEXT -> if (panel.isPanelVertical) {
					ColorUtilities.getPrimaryTextColor(app, nightMode)
				} else {
					ColorUtilities.getColor(app, if (nightMode) R.color.widgettext_night else R.color.widgettext_day)
				}
				PanelColorTarget.SECONDARY_TEXT -> ColorUtilities.getSecondaryTextColor(app, nightMode)
				PanelColorTarget.BACKGROUND ->
					PanelAppearanceResolver.getDefaultBackgroundColor(app, panel, nightMode)
			}
		}

		@JvmStatic
		fun resolveWidgetSize(app: OsmandApplication, panel: WidgetsPanel,
		                      individualSize: WidgetSize, ctx: Context): WidgetSize {
			val layoutMode = ScreenLayoutMode.getDefault(ctx)
			val mode = app.panelAppearanceSettingsManager[panel].getSizeModePref(layoutMode).get()
			return mode.widgetSize ?: individualSize
		}

		@JvmStatic
		fun getCommittedCustomBackgroundColor(app: OsmandApplication, panel: WidgetsPanel,
		                                      layoutMode: ScreenLayoutMode?, nightMode: Boolean): Int? {
			val background = app.panelAppearanceSettingsManager
				.resolveCommittedBackground(panel, layoutMode, nightMode)
			return if (background.mode == PanelBackgroundMode.CUSTOM) {
				background.color
			} else {
				null
			}
		}

		@JvmStatic
		fun resolveIconVisibility(app: OsmandApplication, panel: WidgetsPanel,
		                          individualShowIcon: Boolean, ctx: Context): Boolean {
			val layoutMode = ScreenLayoutMode.getDefault(ctx)
			return when (app.panelAppearanceSettingsManager[panel].getIconModePref(layoutMode).get()) {
				PanelIconMode.ON -> true
				PanelIconMode.OFF -> false
				else -> individualShowIcon
			}
		}
	}

	private val suffix = "_" + panel.name.lowercase()
	private val app = settings.context

	private val sizeMode: CommonPreference<PanelSizeMode> = settings.registerEnumStringPreference(
		"widget_panel_size_mode$suffix", PanelSizeMode.ORIGINAL,
		SIZE_MODE_VALUES, PanelSizeMode::class.java).makeProfile().cache()

	private val iconMode: CommonPreference<PanelIconMode> = settings.registerEnumStringPreference(
		"widget_panel_icon_mode$suffix", PanelIconMode.ORIGINAL,
		ICON_MODE_VALUES, PanelIconMode::class.java).makeProfile().cache()

	private val textColorMode: CommonPreference<PanelTextColorMode> = settings.registerEnumStringPreference(
		"widget_panel_text_color_mode$suffix", PanelTextColorMode.DEFAULT,
		TEXT_COLOR_MODE_VALUES, PanelTextColorMode::class.java).makeProfile().cache()

	private val secondaryTextColorMode: CommonPreference<PanelTextColorMode> = settings.registerEnumStringPreference(
		"widget_panel_secondary_text_color_mode$suffix", PanelTextColorMode.DEFAULT,
		TEXT_COLOR_MODE_VALUES, PanelTextColorMode::class.java).makeProfile().cache()

	private val backgroundMode: CommonPreference<PanelBackgroundMode> = settings.registerEnumStringPreference(
		"widget_panel_background_mode$suffix", PanelBackgroundMode.DEFAULT,
		BACKGROUND_MODE_VALUES, PanelBackgroundMode::class.java).makeProfile().cache()

	private val textColorDay: CommonPreference<Int> = settings.registerIntPreference(
		"widget_panel_text_color_day$suffix",
		getDefaultColor(app, panel, PanelColorTarget.TEXT, false)).makeProfile().cache()

	private val textColorNight: CommonPreference<Int> = settings.registerIntPreference(
		"widget_panel_text_color_night$suffix",
		getDefaultColor(app, panel, PanelColorTarget.TEXT, true)).makeProfile().cache()

	private val secondaryTextColorDay: CommonPreference<Int> = settings.registerIntPreference(
		"widget_panel_secondary_text_color_day$suffix",
		getDefaultColor(app, panel, PanelColorTarget.SECONDARY_TEXT, false)).makeProfile().cache()

	private val secondaryTextColorNight: CommonPreference<Int> = settings.registerIntPreference(
		"widget_panel_secondary_text_color_night$suffix",
		getDefaultColor(app, panel, PanelColorTarget.SECONDARY_TEXT, true)).makeProfile().cache()

	private val backgroundColorDay: CommonPreference<Int> = settings.registerIntPreference(
		"widget_panel_background_color_day$suffix",
		getDefaultColor(app, panel, PanelColorTarget.BACKGROUND, false)).makeProfile().cache()

	private val backgroundColorNight: CommonPreference<Int> = settings.registerIntPreference(
		"widget_panel_background_color_night$suffix",
		getDefaultColor(app, panel, PanelColorTarget.BACKGROUND, true)).makeProfile().cache()

	private class LayoutPreferences(
		val sizeMode: CommonPreference<PanelSizeMode>,
		val iconMode: CommonPreference<PanelIconMode>,
		val textColorMode: CommonPreference<PanelTextColorMode>,
		val secondaryTextColorMode: CommonPreference<PanelTextColorMode>,
		val backgroundMode: CommonPreference<PanelBackgroundMode>,
		val textColorDay: CommonPreference<Int>,
		val textColorNight: CommonPreference<Int>,
		val secondaryTextColorDay: CommonPreference<Int>,
		val secondaryTextColorNight: CommonPreference<Int>,
		val backgroundColorDay: CommonPreference<Int>,
		val backgroundColorNight: CommonPreference<Int>
	) {
		val all: Array<CommonPreference<*>> = arrayOf(
			sizeMode, iconMode, textColorMode, secondaryTextColorMode, backgroundMode,
			textColorDay, textColorNight, secondaryTextColorDay, secondaryTextColorNight,
			backgroundColorDay, backgroundColorNight
		)
	}

	private val basePreferences = createLayoutPreferences(null)
	private val portraitPreferences = createLayoutPreferences(ScreenLayoutMode.PORTRAIT)
	private val landscapePreferences = createLayoutPreferences(ScreenLayoutMode.LANDSCAPE)

	private fun createLayoutPreferences(layoutMode: ScreenLayoutMode?): LayoutPreferences = LayoutPreferences(
		settings.getLayoutPreference(sizeMode, layoutMode),
		settings.getLayoutPreference(iconMode, layoutMode),
		settings.getLayoutPreference(textColorMode, layoutMode),
		settings.getLayoutPreference(secondaryTextColorMode, layoutMode),
		settings.getLayoutPreference(backgroundMode, layoutMode),
		settings.getLayoutPreference(textColorDay, layoutMode),
		settings.getLayoutPreference(textColorNight, layoutMode),
		settings.getLayoutPreference(secondaryTextColorDay, layoutMode),
		settings.getLayoutPreference(secondaryTextColorNight, layoutMode),
		settings.getLayoutPreference(backgroundColorDay, layoutMode),
		settings.getLayoutPreference(backgroundColorNight, layoutMode)
	)

	private fun getLayoutPreferences(layoutMode: ScreenLayoutMode?): LayoutPreferences = when (layoutMode) {
		ScreenLayoutMode.PORTRAIT -> portraitPreferences
		ScreenLayoutMode.LANDSCAPE -> landscapePreferences
		null -> basePreferences
	}

	fun getSizeModePref(layoutMode: ScreenLayoutMode?): CommonPreference<PanelSizeMode> =
		getLayoutPreferences(layoutMode).sizeMode

	fun getIconModePref(layoutMode: ScreenLayoutMode?): CommonPreference<PanelIconMode> =
		getLayoutPreferences(layoutMode).iconMode

	fun getTextColorModePref(layoutMode: ScreenLayoutMode?): CommonPreference<PanelTextColorMode> =
		getLayoutPreferences(layoutMode).textColorMode

	fun getSecondaryTextColorModePref(layoutMode: ScreenLayoutMode?): CommonPreference<PanelTextColorMode> =
		getLayoutPreferences(layoutMode).secondaryTextColorMode

	fun getBackgroundModePref(layoutMode: ScreenLayoutMode?): CommonPreference<PanelBackgroundMode> =
		getLayoutPreferences(layoutMode).backgroundMode

	fun getTextColorPref(layoutMode: ScreenLayoutMode?, nightMode: Boolean): CommonPreference<Int> =
		getLayoutPreferences(layoutMode).let { if (nightMode) it.textColorNight else it.textColorDay }

	fun getSecondaryTextColorPref(layoutMode: ScreenLayoutMode?, nightMode: Boolean): CommonPreference<Int> =
		getLayoutPreferences(layoutMode).let {
			if (nightMode) it.secondaryTextColorNight else it.secondaryTextColorDay
		}

	fun getBackgroundColorPref(layoutMode: ScreenLayoutMode?, nightMode: Boolean): CommonPreference<Int> =
		getLayoutPreferences(layoutMode).let { if (nightMode) it.backgroundColorNight else it.backgroundColorDay }

	fun readState(appMode: ApplicationMode, layoutMode: ScreenLayoutMode?): PanelAppearanceState {
		val preferences = getLayoutPreferences(layoutMode)
		return PanelAppearanceState(
			sizeMode = preferences.sizeMode.getModeValue(appMode),
			iconMode = preferences.iconMode.getModeValue(appMode),
			textColorMode = preferences.textColorMode.getModeValue(appMode),
			secondaryTextColorMode = preferences.secondaryTextColorMode.getModeValue(appMode),
			backgroundMode = preferences.backgroundMode.getModeValue(appMode),
			textColorDay = preferences.textColorDay.getModeValue(appMode),
			textColorNight = preferences.textColorNight.getModeValue(appMode),
			secondaryTextColorDay = preferences.secondaryTextColorDay.getModeValue(appMode),
			secondaryTextColorNight = preferences.secondaryTextColorNight.getModeValue(appMode),
			backgroundColorDay = preferences.backgroundColorDay.getModeValue(appMode),
			backgroundColorNight = preferences.backgroundColorNight.getModeValue(appMode)
		)
	}

	fun commitCustomColors(target: PanelColorTarget, appMode: ApplicationMode,
	                       layoutMode: ScreenLayoutMode?, @ColorInt dayColor: Int,
	                       @ColorInt nightColor: Int): Boolean {
		val preferences = getLayoutPreferences(layoutMode)
		return when (target) {
			PanelColorTarget.TEXT -> commitCustomColors(
				preferences.textColorMode, PanelTextColorMode.CUSTOM,
				preferences.textColorDay, preferences.textColorNight,
				appMode, dayColor, nightColor
			)
			PanelColorTarget.SECONDARY_TEXT -> commitCustomColors(
				preferences.secondaryTextColorMode, PanelTextColorMode.CUSTOM,
				preferences.secondaryTextColorDay, preferences.secondaryTextColorNight,
				appMode, dayColor, nightColor
			)
			PanelColorTarget.BACKGROUND -> commitCustomColors(
				preferences.backgroundMode, PanelBackgroundMode.CUSTOM,
				preferences.backgroundColorDay, preferences.backgroundColorNight,
				appMode, dayColor, nightColor
			)
		}
	}

	private fun <T> commitCustomColors(modePref: CommonPreference<T>, customMode: T,
	                                   dayColorPref: CommonPreference<Int>,
	                                   nightColorPref: CommonPreference<Int>,
	                                   appMode: ApplicationMode, @ColorInt dayColor: Int,
	                                   @ColorInt nightColor: Int): Boolean {
		val originalMode = modePref.getModeValue(appMode)
		val originalDayColor = dayColorPref.getModeValue(appMode)
		val originalNightColor = nightColorPref.getModeValue(appMode)

		val saved = dayColorPref.setModeValue(appMode, dayColor)
				&& nightColorPref.setModeValue(appMode, nightColor)
				&& modePref.setModeValue(appMode, customMode)
		if (!saved) {
			dayColorPref.setModeValue(appMode, originalDayColor)
			nightColorPref.setModeValue(appMode, originalNightColor)
			modePref.setModeValue(appMode, originalMode)
		}
		return saved
	}

	private fun allPrefs(layoutMode: ScreenLayoutMode?): Array<CommonPreference<*>> =
		getLayoutPreferences(layoutMode).all

	internal fun addListener(listener: StateChangedListener<Any?>) {
		basePreferences.addListener(listener)
		portraitPreferences.addListener(listener)
		landscapePreferences.addListener(listener)
	}

	internal fun removeListener(listener: StateChangedListener<Any?>) {
		basePreferences.removeListener(listener)
		portraitPreferences.removeListener(listener)
		landscapePreferences.removeListener(listener)
	}

	@Suppress("UNCHECKED_CAST")
	private fun LayoutPreferences.addListener(listener: StateChangedListener<Any?>) {
		for (preference in all) {
			(preference as CommonPreference<Any?>).addListener(listener)
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun LayoutPreferences.removeListener(listener: StateChangedListener<Any?>) {
		for (preference in all) {
			(preference as CommonPreference<Any?>).removeListener(listener)
		}
	}

	fun resetToDefault(appMode: ApplicationMode, layoutMode: ScreenLayoutMode?) {
		allPrefs(layoutMode).forEach { it.resetModeToDefault(appMode) }
	}

	fun copyFromProfile(fromAppMode: ApplicationMode, appMode: ApplicationMode, layoutMode: ScreenLayoutMode?) {
		allPrefs(layoutMode).forEach { copyPrefFromAppMode(it, fromAppMode, appMode) }
	}

	fun copyFromPanel(fromPanel: WidgetsPanel, appMode: ApplicationMode, layoutMode: ScreenLayoutMode?) {
		val from = app.panelAppearanceSettingsManager[fromPanel].allPrefs(layoutMode)
		val to = allPrefs(layoutMode)
		for (i in to.indices) {
			copyPrefValue(from[i], to[i], appMode)
		}
	}

	private fun <T> copyPrefFromAppMode(pref: CommonPreference<T>, fromAppMode: ApplicationMode,
	                                    appMode: ApplicationMode) {
		pref.setModeValue(appMode, pref.getModeValue(fromAppMode))
	}

	@Suppress("UNCHECKED_CAST")
	private fun copyPrefValue(from: CommonPreference<*>, to: CommonPreference<*>, appMode: ApplicationMode) {
		(to as CommonPreference<Any>).setModeValue(appMode, (from as CommonPreference<Any>).getModeValue(appMode))
	}
}