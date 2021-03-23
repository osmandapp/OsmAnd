package net.osmand.plus.routing;

import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class RouteLineDrawInfo {

	private static final String LINE_COLOR = "line_color";
	private static final String LINE_WIDTH = "line_width";
	private static final String NAVIGATION_ICON_ID = "navigation_icon_id";
	private static final String NAVIGATION_ICON_COLOR = "navigation_icon_color";
	private static final String CENTER_X = "center_x";
	private static final String CENTER_Y = "center_y";
	private static final String SCREEN_HEIGHT = "screen_height";

	// parameters to save
	@ColorInt
	private Integer color;
	private String width;

	// temporally parameters to show in preview
	@ColorInt
	private int iconColor;
	private int iconId;
	private int centerX;
	private int centerY;
	private int screenHeight;

	public RouteLineDrawInfo(@Nullable @ColorInt Integer color,
	                         @Nullable String width) {
		this.color = color;
		this.width = width;
	}

	public RouteLineDrawInfo(@NonNull Bundle bundle) {
		readBundle(bundle);
	}

	public RouteLineDrawInfo(@NonNull RouteLineDrawInfo existed) {
		this.color = existed.getColor();
		this.width = existed.getWidth();
		this.iconId = existed.iconId;
		this.iconColor = existed.iconColor;
		this.centerX = existed.centerX;
		this.centerY = existed.centerY;
		this.screenHeight = existed.screenHeight;
	}

	@Nullable
	public Integer getColor() {
		return color;
	}

	@Nullable
	public String getWidth() {
		return width;
	}

	public void setColor(@Nullable Integer color) {
		this.color = color;
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
		color = bundle.getInt(LINE_COLOR);
		width = bundle.getString(LINE_WIDTH);
		iconId = bundle.getInt(NAVIGATION_ICON_ID);
		iconColor = bundle.getInt(NAVIGATION_ICON_COLOR);
		centerX = bundle.getInt(CENTER_X);
		centerY = bundle.getInt(CENTER_Y);
		screenHeight = bundle.getInt(SCREEN_HEIGHT);
	}

	public void saveToBundle(@NonNull Bundle bundle) {
		if (color != null) {
			bundle.putInt(LINE_COLOR, color);
		}
		if (width != null) {
			bundle.putString(LINE_WIDTH, width);
		}
		bundle.putInt(NAVIGATION_ICON_ID, iconId);
		bundle.putInt(NAVIGATION_ICON_COLOR, iconColor);
		bundle.putInt(CENTER_X, (int) centerX);
		bundle.putInt(CENTER_Y, (int) centerY);
		bundle.putInt(SCREEN_HEIGHT, screenHeight);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RouteLineDrawInfo)) return false;

		RouteLineDrawInfo that = (RouteLineDrawInfo) o;

		if (!Algorithms.objectEquals(getColor(), that.getColor())) return false;
		return Algorithms.objectEquals(getWidth(), that.getWidth());
	}

	@Override
	public int hashCode() {
		int result = color != null ? color.hashCode() : 0;
		result = 31 * result + (width != null ? width.hashCode() : 0);
		return result;
	}
}
