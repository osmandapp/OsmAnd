package net.osmand.plus.routing;

import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public class RouteLineDrawInfo {

	private static final String LINE_COLOR = "line_color";
	private static final String LINE_WIDTH = "line_width";

	@ColorInt
	private Integer color;
	private Integer width;

	public RouteLineDrawInfo(@Nullable @ColorInt Integer color,
	                         @Nullable Integer width) {
		this.color = color;
		this.width = width;
	}

	public RouteLineDrawInfo(@NonNull Bundle bundle) {
		readBundle(bundle);
	}

	public RouteLineDrawInfo(@NonNull RouteLineDrawInfo existed) {
		this(existed.getColor(), existed.getWidth());
	}

	@Nullable
	public Integer getColor() {
		return color;
	}

	@Nullable
	public Integer getWidth() {
		return width;
	}

	public void setColor(@Nullable Integer color) {
		this.color = color;
	}

	public void setWidth(@Nullable Integer width) {
		this.width = width;
	}

	private void readBundle(@NonNull Bundle bundle) {
		color = bundle.getInt(LINE_COLOR);
		width = bundle.getInt(LINE_WIDTH);
	}

	public void saveToBundle(@NonNull Bundle bundle) {
		if (color != null) {
			bundle.putInt(LINE_COLOR, color);
		}
		if (width != null) {
			bundle.putInt(LINE_WIDTH, width);
		}
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
