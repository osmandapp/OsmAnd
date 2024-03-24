package net.osmand.plus.configmap.tracks.appearance;

import androidx.annotation.ColorInt;

public class AppearanceData {

	@ColorInt
	private int customColor;

	public void setCustomColor(@ColorInt int customColor) {
		this.customColor = customColor;
	}

	@ColorInt
	public int getCustomColor() {
		return customColor;
	}
}
