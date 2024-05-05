package net.osmand.plus.base.containers;

import androidx.annotation.DrawableRes;

public class ThemedIconId {

	private final int iconDayId;
	private final int iconNightId;

	public ThemedIconId(@DrawableRes int iconDayId, @DrawableRes int iconNightId) {
		this.iconDayId = iconDayId;
		this.iconNightId = iconNightId;
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? iconNightId : iconDayId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o instanceof ThemedIconId) {
			ThemedIconId that = (ThemedIconId) o;
			return iconDayId == that.iconDayId
					&& iconNightId == that.iconNightId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 * iconDayId + iconNightId;
	}
}
