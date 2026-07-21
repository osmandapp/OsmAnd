package net.osmand.plus.views.mapwidgets.appearance

import androidx.annotation.ColorInt
import net.osmand.plus.settings.enums.PanelBackgroundMode
import net.osmand.plus.settings.enums.PanelIconMode
import net.osmand.plus.settings.enums.PanelSizeMode
import net.osmand.plus.settings.enums.PanelTextColorMode

enum class PanelColorTarget {
	TEXT,
	SECONDARY_TEXT,
	BACKGROUND
}

data class PanelAppearanceState(
	val sizeMode: PanelSizeMode,
	val iconMode: PanelIconMode,
	val textColorMode: PanelTextColorMode,
	val secondaryTextColorMode: PanelTextColorMode,
	val backgroundMode: PanelBackgroundMode,
	@ColorInt val textColorDay: Int,
	@ColorInt val textColorNight: Int,
	@ColorInt val secondaryTextColorDay: Int,
	@ColorInt val secondaryTextColorNight: Int,
	@ColorInt val backgroundColorDay: Int,
	@ColorInt val backgroundColorNight: Int
) {

	@ColorInt
	fun getColor(target: PanelColorTarget, nightMode: Boolean): Int = when (target) {
		PanelColorTarget.TEXT -> if (nightMode) textColorNight else textColorDay
		PanelColorTarget.SECONDARY_TEXT -> if (nightMode) secondaryTextColorNight else secondaryTextColorDay
		PanelColorTarget.BACKGROUND -> if (nightMode) backgroundColorNight else backgroundColorDay
	}

	fun isCustom(target: PanelColorTarget): Boolean = when (target) {
		PanelColorTarget.TEXT -> textColorMode == PanelTextColorMode.CUSTOM
		PanelColorTarget.SECONDARY_TEXT -> secondaryTextColorMode == PanelTextColorMode.CUSTOM
		PanelColorTarget.BACKGROUND -> backgroundMode == PanelBackgroundMode.CUSTOM
	}

	fun withCustomTarget(target: PanelColorTarget): PanelAppearanceState = when (target) {
		PanelColorTarget.TEXT -> copy(textColorMode = PanelTextColorMode.CUSTOM)
		PanelColorTarget.SECONDARY_TEXT -> copy(secondaryTextColorMode = PanelTextColorMode.CUSTOM)
		PanelColorTarget.BACKGROUND -> copy(backgroundMode = PanelBackgroundMode.CUSTOM)
	}

	fun withColor(target: PanelColorTarget, nightMode: Boolean,
	              @ColorInt color: Int): PanelAppearanceState = when (target) {
		PanelColorTarget.TEXT -> if (nightMode) copy(textColorNight = color) else copy(textColorDay = color)
		PanelColorTarget.SECONDARY_TEXT -> if (nightMode) {
			copy(secondaryTextColorNight = color)
		} else {
			copy(secondaryTextColorDay = color)
		}
		PanelColorTarget.BACKGROUND -> if (nightMode) {
			copy(backgroundColorNight = color)
		} else {
			copy(backgroundColorDay = color)
		}
	}
}