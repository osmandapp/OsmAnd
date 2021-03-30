package net.osmand.plus.routing;

import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class RouteLineDrawInfo {

	private static final String LINE_COLOR_DAY = "line_color_day";
	private static final String LINE_COLOR_NIGHT = "line_color_night";
	private static final String LINE_WIDTH = "line_width";
	private static final String NAVIGATION_ICON_ID = "navigation_icon_id";
	private static final String NAVIGATION_ICON_COLOR = "navigation_icon_color";
	private static final String CENTER_X = "center_x";
	private static final String CENTER_Y = "center_y";
	private static final String SCREEN_HEIGHT = "screen_height";
	private static final String USE_DEFAULT_COLOR = "use_default_color";

	// parameters to save
	@ColorInt
	private Integer colorDay;
	private Integer colorNight;
	private String width;

	// temporally parameters to show in preview
	@ColorInt
	private int iconColor;
	private int iconId;
	private int centerX;
	private int centerY;
	private int screenHeight;
	private boolean useDefaultColor;

	public RouteLineDrawInfo(@Nullable @ColorInt Integer colorDay,
	                         @Nullable @ColorInt Integer colorNight,
	                         @Nullable String width) {
		this.colorDay = colorDay;
		this.colorNight = colorNight;
		this.width = width;
	}

	public RouteLineDrawInfo(@NonNull Bundle bundle) {
		readBundle(bundle);
	}

	public RouteLineDrawInfo(@NonNull RouteLineDrawInfo existed) {
		this.colorDay = existed.colorDay;
		this.colorNight = existed.colorNight;
		this.width = existed.width;
		this.iconId = existed.iconId;
		this.iconColor = existed.iconColor;
		this.centerX = existed.centerX;
		this.centerY = existed.centerY;
		this.screenHeight = existed.screenHeight;
		this.useDefaultColor = existed.useDefaultColor;
	}

	public void setColor(@ColorInt int color, boolean nightMode) {
		if (nightMode) {
			colorNight = color;
		} else {
			colorDay = color;
		}
	}

	public void setUseDefaultColor(boolean useDefaultColor) {
		this.useDefaultColor = useDefaultColor;
	}

	public void setWidth(@Nullable String width) {
		this.width = width;
	}

	public void setIconId(int iconId) {
		this.iconId = iconId;
	}

	public void setIconColor(int iconColor) {
		this.iconColor = iconColor;
	}

	public void setCenterX(int centerX) {
		this.centerX = centerX;
	}

	public void setCenterY(int centerY) {
		this.centerY = centerY;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}

	@Nullable
	public Integer getColor(boolean nightMode) {
		if (!useDefaultColor) {
			return getColorIgnoreDefault(nightMode);
		}
		return null;
	}

	@Nullable
	public Integer getColorIgnoreDefault(boolean nightMode) {
		return nightMode ? colorNight : colorDay;
	}

	@Nullable
	public String getWidth() {
		return width;
	}

	public int getIconId() {
		return iconId;
	}

	@ColorInt
	public int getIconColor() {
		return iconColor;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	private void readBundle(@NonNull Bundle bundle) {
		if (bundle.containsKey(LINE_COLOR_DAY)) {
			colorDay = bundle.getInt(LINE_COLOR_DAY);
		}
		if (bundle.containsKey(LINE_COLOR_NIGHT)) {
			colorNight = bundle.getInt(LINE_COLOR_NIGHT);
		}
		width = bundle.getString(LINE_WIDTH);
		iconId = bundle.getInt(NAVIGATION_ICON_ID);
		iconColor = bundle.getInt(NAVIGATION_ICON_COLOR);
		centerX = bundle.getInt(CENTER_X);
		centerY = bundle.getInt(CENTER_Y);
		screenHeight = bundle.getInt(SCREEN_HEIGHT);
		useDefaultColor = bundle.getBoolean(USE_DEFAULT_COLOR);
	}

	public void saveToBundle(@NonNull Bundle bundle) {
		if (colorDay != null) {
			bundle.putInt(LINE_COLOR_DAY, colorDay);
		}
		if (colorNight != null) {
			bundle.putInt(LINE_COLOR_NIGHT, colorNight);
		}
		if (width != null) {
			bundle.putString(LINE_WIDTH, width);
		}
		bundle.putInt(NAVIGATION_ICON_ID, iconId);
		bundle.putInt(NAVIGATION_ICON_COLOR, iconColor);
		bundle.putInt(CENTER_X, centerX);
		bundle.putInt(CENTER_Y, centerY);
		bundle.putInt(SCREEN_HEIGHT, screenHeight);
		bundle.putBoolean(USE_DEFAULT_COLOR, useDefaultColor);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RouteLineDrawInfo)) return false;

		RouteLineDrawInfo that = (RouteLineDrawInfo) o;

		if (!Algorithms.objectEquals(getColor(false), that.getColor(false))) return false;
		if (!Algorithms.objectEquals(getColor(true), that.getColor(true))) return false;
		return Algorithms.objectEquals(width, that.width);
	}

	@Override
	public int hashCode() {
		int result = colorDay != null ? colorDay.hashCode() : 0;
		result = 31 * result + (colorNight != null ? colorNight.hashCode() : 0);
		result = 31 * result + (width != null ? width.hashCode() : 0);
		return result;
	}
}
