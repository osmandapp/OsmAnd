package net.osmand.plus.base.containers;

import androidx.annotation.DrawableRes;

public class ThemedIconId {

	private int iconDayId;
	private int iconNightId;

	public ThemedIconId(@DrawableRes int iconDayId, @DrawableRes int iconNightId) {
		this.iconDayId = iconDayId;
		this.iconNightId = iconNightId;
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? iconNightId : iconDayId;
	}
}
