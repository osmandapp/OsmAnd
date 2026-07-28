package net.osmand.plus.views.mapwidgets.appearance

import android.content.res.ColorStateList
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import net.osmand.plus.settings.enums.PanelBackgroundMode
import net.osmand.plus.settings.enums.PanelIconMode
import net.osmand.plus.settings.enums.PanelSizeMode
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.mapwidgets.WidgetsPanel

enum class WidgetBackgroundShape {
	FLAT,
	RECTANGLE,
	ROUND
}

class ResolvedPanelBackground(
	val mode: PanelBackgroundMode,
	@DrawableRes private val flatDrawableRes: Int,
	@DrawableRes private val rectangleDrawableRes: Int,
	@DrawableRes private val roundDrawableRes: Int,
	val isTinted: Boolean,
	@ColorInt val color: Int,
	val tintColors: ColorStateList
) {
	val isOpaque: Boolean = color == ColorUtilities.removeAlpha(color)

	@DrawableRes
	fun getDrawableRes(shape: WidgetBackgroundShape): Int = when (shape) {
		WidgetBackgroundShape.FLAT -> flatDrawableRes
		WidgetBackgroundShape.RECTANGLE -> rectangleDrawableRes
		WidgetBackgroundShape.ROUND -> roundDrawableRes
	}
}

class ResolvedPanelAppearance(
	val panel: WidgetsPanel,
	val nightMode: Boolean,
	val boldText: Boolean,
	@ColorInt val primaryTextColor: Int,
	@ColorInt val secondaryTextColor: Int,
	@ColorInt val textShadowColor: Int,
	val textShadowRadius: Int,
	val background: ResolvedPanelBackground,
	@ColorInt val dividerColor: Int,
	@ColorInt val standaloneDividerColor: Int,
	@ColorInt val panelBorderColor: Int,
	val sizeMode: PanelSizeMode,
	val iconMode: PanelIconMode
)
