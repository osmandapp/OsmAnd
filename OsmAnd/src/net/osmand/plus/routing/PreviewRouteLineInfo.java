package net.osmand.plus.routing;

import android.graphics.Rect;
import android.os.Bundle;

import net.osmand.plus.track.GradientScaleType;
import net.osmand.util.Algorithms;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PreviewRouteLineInfo {

	private static final String LINE_COLOR_DAY = "line_color_day";
	private static final String LINE_COLOR_NIGHT = "line_color_night";
	private static final String LINE_COLOR_GRADIENT = "line_color_gradient";
	private static final String LINE_WIDTH = "line_width";
	private static final String NAVIGATION_ICON_ID = "navigation_icon_id";
	private static final String NAVIGATION_ICON_COLOR = "navigation_icon_color";
	private static final String LINE_BOUNDS = "line_bounds";
	private static final String CENTER_X = "center_x";
	private static final String CENTER_Y = "center_y";
	private static final String SCREEN_HEIGHT = "screen_height";
	private static final String USE_DEFAULT_COLOR = "use_default_color";

	// parameters to save
	@ColorInt
	private Integer colorDay;
	private Integer colorNight;
	private GradientScaleType scaleType;
	private String width;

	// temporally parameters to show in preview
	@ColorInt
	private int iconColor;
	private int iconId;
	private Rect lineBounds;
	private int centerX;
	private int centerY;
	private int screenHeight;
	private boolean useDefaultColor;

	public PreviewRouteLineInfo(@Nullable @ColorInt Integer colorDay,
								@Nullable @ColorInt Integer colorNight,
								@Nullable GradientScaleType gradientScaleType,
								@Nullable String width) {
		this.colorDay = colorDay;
		this.colorNight = colorNight;
		this.scaleType = gradientScaleType;
		this.width = width;
	}

	public PreviewRouteLineInfo(@NonNull Bundle bundle) {
		readBundle(bundle);
	}

	public PreviewRouteLineInfo(@NonNull PreviewRouteLineInfo existed) {
		this.colorDay = existed.colorDay;
		this.colorNight = existed.colorNight;
		this.scaleType = existed.scaleType;
		this.width = existed.width;
		this.iconId = existed.iconId;
		this.iconColor = existed.iconColor;
		this.lineBounds = existed.lineBounds;
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

	public void setGradientScaleType(@Nullable GradientScaleType scaleType) {
		this.scaleType = scaleType;
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

	public void setLineBounds(Rect lineBounds) {
		this.lineBounds = lineBounds;
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
	public GradientScaleType getGradientScaleType() {
		return scaleType;
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

	public Rect getLineBounds() {
		return lineBounds;
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
		if (bundle.containsKey(LINE_COLOR_GRADIENT)) {
			String scaleTypeName = bundle.getString(LINE_COLOR_GRADIENT);
			if (!Algorithms.isEmpty(scaleTypeName)) {
				scaleType = GradientScaleType.getGradientTypeByName(scaleTypeName);
			}
		}
		width = bundle.getString(LINE_WIDTH);
		iconId = bundle.getInt(NAVIGATION_ICON_ID);
		iconColor = bundle.getInt(NAVIGATION_ICON_COLOR);
		lineBounds = (Rect) bundle.getParcelable(LINE_BOUNDS);
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
		if (scaleType != null) {
			bundle.putString(LINE_COLOR_GRADIENT, scaleType.getTypeName());
		}
		if (width != null) {
			bundle.putString(LINE_WIDTH, width);
		}
		bundle.putInt(NAVIGATION_ICON_ID, iconId);
		bundle.putInt(NAVIGATION_ICON_COLOR, iconColor);
		bundle.putParcelable(LINE_BOUNDS, lineBounds);
		bundle.putInt(CENTER_X, centerX);
		bundle.putInt(CENTER_Y, centerY);
		bundle.putInt(SCREEN_HEIGHT, screenHeight);
		bundle.putBoolean(USE_DEFAULT_COLOR, useDefaultColor);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof PreviewRouteLineInfo)) return false;

		PreviewRouteLineInfo that = (PreviewRouteLineInfo) o;

		if (!Algorithms.objectEquals(getColor(false), that.getColor(false))) return false;
		if (!Algorithms.objectEquals(getColor(true), that.getColor(true))) return false;
		if (!Algorithms.objectEquals(scaleType, that.scaleType)) return false;
		return Algorithms.objectEquals(width, that.width);
	}

	@Override
	public int hashCode() {
		int result = colorDay != null ? colorDay.hashCode() : 0;
		result = 31 * result + (colorNight != null ? colorNight.hashCode() : 0);
		result = 31 * result + (scaleType != null ? scaleType.getTypeName().hashCode() : 0);
		result = 31 * result + (width != null ? width.hashCode() : 0);
		return result;
	}
}