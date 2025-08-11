package net.osmand.plus.settings.enums;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.base.containers.ThemedIconId;
import net.osmand.plus.settings.backend.OsmandSettings;

public enum CompassMode {
	MANUALLY_ROTATED(
			OsmandSettings.ROTATE_MAP_MANUAL,
			R.string.rotate_map_manual_opt,
			new ThemedIconId(R.drawable.ic_compass_manual, R.drawable.ic_compass_manual_white)
	),

	MOVEMENT_DIRECTION(
			OsmandSettings.ROTATE_MAP_BEARING,
			R.string.rotate_map_bearing_opt,
			new ThemedIconId(R.drawable.ic_compass_bearing, R.drawable.ic_compass_bearing_white)
	),

	COMPASS_DIRECTION(
			OsmandSettings.ROTATE_MAP_COMPASS,
			R.string.rotate_map_compass_opt,
			new ThemedIconId(R.drawable.ic_compass, R.drawable.ic_compass_white)
	),

	NORTH_IS_UP(
			OsmandSettings.ROTATE_MAP_NONE,
			R.string.rotate_map_north_opt,
			new ThemedIconId(R.drawable.ic_compass_niu, R.drawable.ic_compass_niu_white)
	);

	@StringRes
	private final int titleId;
	private final ThemedIconId themedIconId;
	private final int value;

	CompassMode(int value, @StringRes int titleId, @NonNull ThemedIconId themedIconId) {
		this.value = value;
		this.titleId = titleId;
		this.themedIconId = themedIconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	public String getTitle(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}

	@NonNull
	public ThemedIconId getIconId() {
		return themedIconId;
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return themedIconId.getIconId(nightMode);
	}

	public int getValue() {
		return value;
	}

	@NonNull
	public String getKey() {
		return name();
	};

	@NonNull
	public CompassMode next() {
		CompassMode[] values = values();
		int nextModeIndex = (ordinal() + 1) % values.length;
		return values[nextModeIndex];
	}

	@NonNull
	public static CompassMode getByValue(int value) {
		for (CompassMode compassMode : values()) {
			if (compassMode.value == value) {
				return compassMode;
			}
		}
		return NORTH_IS_UP;
	}

	public static boolean isCompassIconId(@DrawableRes int iconId) {
		for (CompassMode compassMode : CompassMode.values()) {
			ThemedIconId icon = compassMode.getIconId();
			if (icon.getIconId(true) == iconId || icon.getIconId(false) == iconId) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public static CompassMode getModeForKey(@Nullable String key) {
		for (CompassMode mode : CompassMode.values()) {
			if (mode.getKey().equals(key)) {
				return mode;
			}
		}
		return null;
	}
}
