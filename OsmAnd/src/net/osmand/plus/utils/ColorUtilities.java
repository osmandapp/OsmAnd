package net.osmand.plus.utils;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

public class ColorUtilities {

	public static float getColorsSquareDistance(@ColorInt int color1, @ColorInt int color2) {
		int redDiff = Color.red(color1) - Color.red(color2);
		int greenDiff = Color.green(color1) - Color.green(color2);
		int blueDiff = Color.blue(color1) - Color.blue(color2);
		float redmean = (Color.red(color1) + Color.red(color2)) / 2f;

		float redCoeff = 2.0f + redmean / 255.0f;
		float greenCoeff = 4.0f;
		float blueCoeff = 2.0f + (255.0f - redmean) / 255.0f;

		float redDelta = redCoeff * redDiff * redDiff;
		float greenDelta = greenCoeff * greenDiff * greenDiff;
		float blueDelta = blueCoeff * blueDiff * blueDiff;

		return redDelta + greenDelta + blueDelta;
	}

	@ColorInt
	public static int getContrastColor(Context context, @ColorInt int color, boolean transparent) {
		// Counting the perceptive luminance - human eye favors green color...
		double luminance = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
		return luminance < 0.5 ? transparent ? ContextCompat.getColor(context, R.color.color_black_transparent) : Color.BLACK : Color.WHITE;
	}

	public static float getProportionalAlpha(float startValue, float endValue, float currentValue) {
		currentValue = Math.min(currentValue, endValue);
		float proportion = (endValue - startValue) / 100;
		if (currentValue > startValue) {
			float currentInRange = currentValue - startValue;
			return 1.0f - (currentInRange / proportion) / 100;
		}
		return 1.0f;
	}

	@ColorInt
	public static int getProportionalColorMix(@ColorInt int startColor, @ColorInt int endColor,
	                                          float startValue, float endValue, float currentValue) {
		currentValue = Math.min(currentValue, endValue);
		float proportion = (endValue - startValue) / 100;
		if (currentValue > startValue) {
			float currentInRange = currentValue - startValue;
			float amount = (currentInRange / proportion) / 100;
			return mixTwoColors(endColor, startColor, amount);
		}
		return startColor;
	}

	@ColorInt
	public static int getColorWithAlpha(@ColorInt int color, float ratio) {
		int alpha = Math.round(Color.alpha(color) * ratio);
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
		return Color.argb(alpha, r, g, b);
	}

	@ColorInt
	public static int removeAlpha(@ColorInt int color) {
		return Color.rgb(Color.red(color), Color.green(color), Color.blue(color));
	}

	@ColorInt
	public static int mixTwoColors(@ColorInt int color1, @ColorInt int color2, float amount) {
		final byte ALPHA_CHANNEL = 24;
		final byte RED_CHANNEL = 16;
		final byte GREEN_CHANNEL = 8;
		final byte BLUE_CHANNEL = 0;

		float inverseAmount = 1.0f - amount;

		int a = ((int) (((float) (color1 >> ALPHA_CHANNEL & 0xff) * amount) +
				((float) (color2 >> ALPHA_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int r = ((int) (((float) (color1 >> RED_CHANNEL & 0xff) * amount) +
				((float) (color2 >> RED_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int g = ((int) (((float) (color1 >> GREEN_CHANNEL & 0xff) * amount) +
				((float) (color2 >> GREEN_CHANNEL & 0xff) * inverseAmount))) & 0xff;
		int b = ((int) (((float) (color1 & 0xff) * amount) +
				((float) (color2 & 0xff) * inverseAmount))) & 0xff;

		return a << ALPHA_CHANNEL | r << RED_CHANNEL | g << GREEN_CHANNEL | b << BLUE_CHANNEL;
	}

	/********************************* Basic colors *********************************/

	@ColorInt
	public static int getAppModeColor(@NonNull OsmandApplication app, boolean nightMode) {
		return getAppModeColor(app, nightMode, 1.0f);
	}

	@ColorInt
	public static int getAppModeColor(@NonNull OsmandApplication app, boolean nightMode, float alpha) {
		OsmandSettings settings = app.getSettings();
		ApplicationMode appMode = settings.getApplicationMode();
		return getAppModeColor(appMode, nightMode, alpha);
	}

	@ColorInt
	public static int getAppModeColor(@NonNull ApplicationMode appMode, boolean nightMode, float alpha) {
		int color = appMode.getProfileColor(nightMode);
		return alpha < 1.0f ? getColorWithAlpha(color, alpha) : color;
	}

	@ColorInt
	public static int getColor(@NonNull Context ctx, @ColorRes int colorId) {
		return getColor(ctx, colorId, 1.0f);
	}

	@ColorInt
	public static int getColor(@NonNull Context ctx, @ColorRes int colorId, float alpha) {
		int color = ContextCompat.getColor(ctx, colorId);
		return alpha < 1.0f ? getColorWithAlpha(color, alpha) : color;
	}

	@ColorInt
	public static int getActiveColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getActiveColorId(nightMode));
	}

	@ColorRes
	public static int getActiveColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorInt
	public static int getActiveTabTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getActiveTabTextColorId(nightMode));
	}

	@ColorRes
	public static int getActiveTabTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_tab_active_dark : R.color.text_color_tab_active_light;
	}

	@ColorInt
	public static int getPrimaryTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getPrimaryTextColorId(nightMode));
	}

	@ColorRes
	public static int getPrimaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_primary_dark : R.color.text_color_primary_light;
	}

	@ColorInt
	public static int getSecondaryTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getSecondaryTextColorId(nightMode));
	}

	@ColorRes
	public static int getSecondaryTextColorId() {
		return R.color.text_color_secondary_dark;
	}

	@ColorRes
	public static int getSecondaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
	}

	@ColorInt
	public static int getTertiaryTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getTertiaryTextColorId(nightMode));
	}

	@ColorRes
	public static int getTertiaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.text_color_tertiary_dark : R.color.text_color_tertiary_light;
	}

	@ColorInt
	public static int getDisabledTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getDisabledTextColorId(nightMode));
	}

	@ColorRes
	public static int getDisabledTextColorId(boolean nightMode) {
		return nightMode ? R.color.ctx_menu_controller_disabled_text_color_dark : R.color.ctx_menu_controller_disabled_text_color_light;
	}

	@ColorInt
	public static int getActiveIconColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getActiveIconColorId(nightMode));
	}

	@ColorRes
	public static int getActiveIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light;
	}

	@ColorInt
	public static int getDefaultIconColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getDefaultIconColorId(nightMode));
	}

	@ColorRes
	public static int getDefaultIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
	}

	@ColorInt
	public static int getSecondaryIconColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getSecondaryIconColorId(nightMode));
	}

	@ColorRes
	public static int getSecondaryIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
	}

	@ColorInt
	public static int getPrimaryIconColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getPrimaryIconColorId(nightMode));
	}

	@ColorRes
	public static int getPrimaryIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_primary_dark : R.color.icon_color_primary_light;
	}

	@ColorInt
	public static int getButtonSecondaryTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getButtonSecondaryTextColorId(nightMode));
	}

	@ColorRes
	public static int getButtonSecondaryTextColorId(boolean nightMode) {
		return nightMode ? R.color.dlg_btn_secondary_text_dark : R.color.dlg_btn_secondary_text_light;
	}

	@ColorInt
	public static int getActivityBgColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getActivityBgColorId(nightMode));
	}

	@ColorRes
	public static int getActivityBgColorId(boolean nightMode) {
		return nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
	}

	@ColorInt
	public static int getListBgColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getListBgColorId(nightMode));
	}

	@ColorRes
	public static int getListBgColorId(boolean nightMode) {
		return nightMode ? R.color.list_background_color_dark : R.color.list_background_color_light;
	}

	@ColorInt
	public static int getAppBarColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getAppBarColorId(nightMode));
	}

	@ColorRes
	public static int getAppBarColorId(boolean nightMode) {
		return nightMode ? R.color.app_bar_main_dark : R.color.app_bar_main_light;
	}

	@ColorInt
	public static int getDividerColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getDividerColorId(nightMode));
	}

	@ColorRes
	public static int getDividerColorId(boolean nightMode) {
		return nightMode ? R.color.divider_color_dark : R.color.divider_color_light;
	}

	@ColorInt
	public static int getActiveButtonsAndLinksTextColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getActiveButtonsAndLinksTextColorId(nightMode));
	}

	@ColorRes
	public static int getActiveButtonsAndLinksTextColorId(boolean nightMode) {
		return nightMode ? R.color.active_buttons_and_links_text_dark : R.color.active_buttons_and_links_text_light;
	}

	@ColorInt
	public static int getActiveTransparentColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getActiveTransparentColorId(nightMode));
	}

	@ColorRes
	public static int getActiveTransparentColorId(boolean nightMode) {
		return nightMode ? R.color.switch_button_active_dark : R.color.switch_button_active_light;
	}

	@ColorInt
	public static int getCardAndListBackgroundColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getCardAndListBackgroundColorId(nightMode));
	}

	@ColorRes
	public static int getCardAndListBackgroundColorId(boolean nightMode) {
		return nightMode ? R.color.card_and_list_background_dark : R.color.card_and_list_background_light;
	}

	@ColorInt
	public static int getStrokedButtonsOutlineColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getStrokedButtonsOutlineColorId(nightMode));
	}

	@ColorRes
	public static int getStrokedButtonsOutlineColorId(boolean nightMode) {
		return nightMode ?
				R.color.stroked_buttons_and_links_outline_dark :
				R.color.stroked_buttons_and_links_outline_light;
	}

	@ColorInt
	public static int getInactiveButtonsAndLinksColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getInactiveButtonsAndLinksColorId(nightMode));
	}

	@ColorRes
	public static int getInactiveButtonsAndLinksColorId(boolean nightMode) {
		return nightMode ? R.color.inactive_buttons_and_links_bg_dark : R.color.inactive_buttons_and_links_bg_light;
	}

	@ColorInt
	public static int getMapButtonIconColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getMapButtonIconColorId(nightMode));
	}

	@ColorRes
	public static int getMapButtonIconColorId(boolean nightMode) {
		return nightMode ? R.color.map_button_icon_color_dark : R.color.map_button_icon_color_light;
	}

	@ColorInt
	public static int getToolbarActiveColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getToolbarActiveColorId(nightMode));
	}

	@ColorRes
	public static int getToolbarActiveColorId(boolean nightMode) {
		return nightMode ? R.color.app_bar_active_dark : R.color.app_bar_active_light;
	}

	@ColorRes
	public static int getStatusBarColorId(boolean nightMode) {
		return nightMode ? R.color.status_bar_main_dark : R.color.status_bar_main_light;
	}

	@ColorRes
	public static int getStatusBarActiveColorId(boolean nightMode) {
		return nightMode ? R.color.status_bar_selection_color_dark : R.color.status_bar_selection_color_light;
	}

	@ColorInt
	public static int getStatusBarSecondaryColor(@NonNull Context context, boolean nightMode) {
		return getColor(context, getStatusBarSecondaryColorId(nightMode));
	}

	@ColorRes
	public static int getStatusBarSecondaryColorId(boolean nightMode) {
		return nightMode ? R.color.status_bar_secondary_dark : R.color.status_bar_secondary_light;
	}

	@ColorInt
	public static int getLinksColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getLinksColorId(nightMode));
	}

	@ColorRes
	public static int getLinksColorId(boolean nightMode) {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorInt
	public static int getOsmandIconColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getOsmandIconColorId(nightMode));
	}

	@ColorRes
	public static int getOsmandIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_osmand_dark : R.color.icon_color_osmand_light;
	}

	@ColorInt
	public static int getWarningColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getWarningColorId(nightMode));
	}

	@ColorRes
	public static int getWarningColorId(boolean nightMode) {
		return R.color.deletion_color_warning;
	}

	@ColorInt
	public static int getWidgetBackgroundColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getWidgetBackgroundColorId(nightMode));
	}

	@ColorRes
	public static int getWidgetBackgroundColorId(boolean nightMode) {
		return nightMode ? R.color.widget_background_color_dark : R.color.widget_background_color_light;
	}

	@ColorInt
	public static int getWidgetSecondaryBackgroundColor(@NonNull Context ctx, boolean nightMode) {
		return getColor(ctx, getWidgetSecondaryBackgroundColorId(nightMode));
	}

	@ColorRes
	public static int getWidgetSecondaryBackgroundColorId(boolean nightMode) {
		return nightMode ? R.color.widget_secondary_background_color_dark : R.color.widget_secondary_background_color_light;
	}
}
