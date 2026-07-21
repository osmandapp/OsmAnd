package net.osmand.plus.views.mapwidgets.appearance

import android.R.attr.state_pressed
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.enums.PanelBackgroundMode
import net.osmand.plus.settings.enums.PanelTextColorMode
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.DynamicWidgetColors
import net.osmand.plus.views.mapwidgets.WidgetsPanel

object PanelAppearanceResolver {

	private const val LIGHT_BACKGROUND_PRESSED_OVERLAY_OPACITY = 0.1f
	private const val DARK_BACKGROUND_PRESSED_OVERLAY_OPACITY = 0.15f

	@JvmStatic
	fun resolve(
		app: OsmandApplication,
		state: PanelAppearanceState,
		panel: WidgetsPanel,
		nightMode: Boolean,
		boldText: Boolean,
		density: Float,
		applyPanelOverrides: Boolean
	): ResolvedPanelAppearance {
		val verticalWidget = panel.isPanelVertical
		val backgroundMode = state.backgroundMode
		val transparent = backgroundMode == PanelBackgroundMode.TRANSPARENT

		var primaryTextColor = if (verticalWidget) {
			ColorUtilities.getPrimaryTextColor(app, nightMode)
		} else {
			ColorUtilities.getColor(app, if (nightMode) R.color.widgettext_night else R.color.widgettext_day)
		}
		var secondaryTextColor = ColorUtilities.getSecondaryTextColor(app, nightMode)
		val textShadowColor = ContextCompat.getColor(
			app,
			if (nightMode) R.color.widgettext_shadow_night else R.color.widgettext_shadow_day
		)
		val textShadowRadius = if (!transparent && !nightMode) 0 else (4 * density).toInt()

		val flatDrawableRes: Int
		val rectangleDrawableRes: Int
		val roundDrawableRes: Int
		val dividerColorRes: Int
		val panelBorderColorRes: Int
		when {
			transparent -> {
				flatDrawableRes = R.drawable.btn_flat_transparent
				rectangleDrawableRes = R.drawable.bg_side_widget_transparent
				roundDrawableRes = R.drawable.btn_round_transparent
				dividerColorRes = R.color.widget_divider_transparent
				panelBorderColorRes = R.color.widget_panel_border_transparent
			}
			nightMode -> {
				flatDrawableRes = R.drawable.btn_flat_night
				rectangleDrawableRes = if (verticalWidget) {
					R.drawable.bs_vertical_widget_night
				} else {
					R.drawable.bs_side_widget_night
				}
				roundDrawableRes = R.drawable.btn_round_night
				dividerColorRes = R.color.divider_color_dark
				panelBorderColorRes = R.color.icon_color_secondary_dark
			}
			else -> {
				flatDrawableRes = R.drawable.btn_flat
				rectangleDrawableRes = R.drawable.bg_side_widget_day
				roundDrawableRes = R.drawable.btn_round
				dividerColorRes = if (verticalWidget) {
					R.color.widget_background_color_light
				} else {
					R.color.divider_color_light
				}
				panelBorderColorRes = R.color.stroked_buttons_and_links_outline_light
			}
		}

		var dividerColor = ContextCompat.getColor(app, dividerColorRes)
		var backgroundColor = when (backgroundMode) {
			PanelBackgroundMode.TRANSPARENT -> Color.TRANSPARENT
			else -> getDefaultBackgroundColor(app, panel, nightMode)
		}
		var tintBackground = false

		if (applyPanelOverrides) {
			if (backgroundMode == PanelBackgroundMode.CUSTOM) {
				backgroundColor = state.getColor(PanelColorTarget.BACKGROUND, nightMode)
				tintBackground = true
			}

			val dynamicColors = if (backgroundMode != PanelBackgroundMode.TRANSPARENT) {
				DynamicWidgetColors.resolve(backgroundColor)
			} else {
				null
			}
			if (dynamicColors != null) {
				dividerColor = dynamicColors.divider
			}
			primaryTextColor = when (state.textColorMode) {
				PanelTextColorMode.DEFAULT -> primaryTextColor
				PanelTextColorMode.AUTOMATIC -> dynamicColors?.primaryText ?: primaryTextColor
				PanelTextColorMode.CUSTOM -> state.getColor(PanelColorTarget.TEXT, nightMode)
			}
			secondaryTextColor = when (state.secondaryTextColorMode) {
				PanelTextColorMode.DEFAULT -> secondaryTextColor
				PanelTextColorMode.AUTOMATIC -> dynamicColors?.secondaryText ?: secondaryTextColor
				PanelTextColorMode.CUSTOM -> state.getColor(PanelColorTarget.SECONDARY_TEXT, nightMode)
			}
		}
		val standaloneDividerColor = if (backgroundMode == PanelBackgroundMode.TRANSPARENT) {
			dividerColor
		} else {
			ColorUtils.compositeColors(dividerColor, backgroundColor)
		}

		val backgroundTint = if (tintBackground) {
			AndroidUtils.createColorStateList(
				state_pressed,
				resolvePressedBackgroundColor(backgroundColor),
				backgroundColor
			)
		} else {
			ColorStateList.valueOf(backgroundColor)
		}
		val background = ResolvedPanelBackground(
			backgroundMode,
			flatDrawableRes,
			rectangleDrawableRes,
			roundDrawableRes,
			tintBackground,
			backgroundColor,
			backgroundTint
		)
		return ResolvedPanelAppearance(
			panel = panel,
			nightMode = nightMode,
			boldText = boldText,
			primaryTextColor = primaryTextColor,
			secondaryTextColor = secondaryTextColor,
			textShadowColor = textShadowColor,
			textShadowRadius = textShadowRadius,
			background = background,
			dividerColor = dividerColor,
			standaloneDividerColor = standaloneDividerColor,
			panelBorderColor = ContextCompat.getColor(app, panelBorderColorRes),
			sizeMode = state.sizeMode,
			iconMode = state.iconMode
		)
	}

	@JvmStatic
	@ColorInt
	fun getDefaultBackgroundColor(app: OsmandApplication, panel: WidgetsPanel,
	                              nightMode: Boolean): Int {
		return if (nightMode && !panel.isPanelVertical) {
			ColorUtilities.getColor(app, R.color.map_widget_dark)
		} else {
			ColorUtilities.getWidgetBackgroundColor(app, nightMode)
		}
	}

	private fun resolvePressedBackgroundColor(@ColorInt backgroundColor: Int): Int {
		val alpha = Color.alpha(backgroundColor)
		val opaqueBackground = ColorUtilities.removeAlpha(backgroundColor)
		val lightBackground = ColorUtils.calculateLuminance(opaqueBackground) > 0.5
		val overlay = if (lightBackground) {
			ColorUtilities.getColorWithAlpha(Color.BLACK, LIGHT_BACKGROUND_PRESSED_OVERLAY_OPACITY)
		} else {
			ColorUtilities.getColorWithAlpha(Color.WHITE, DARK_BACKGROUND_PRESSED_OVERLAY_OPACITY)
		}
		val pressedColor = ColorUtils.compositeColors(overlay, opaqueBackground)
		return ColorUtils.setAlphaComponent(pressedColor, alpha)
	}
}