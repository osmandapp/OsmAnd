package net.osmand.plus.card.width;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.Objects;

public enum WidthMode {

	THIN("thin", R.drawable.ic_action_gpx_width_thin, R.string.rendering_value_thin_name),
	MEDIUM("medium", R.drawable.ic_action_gpx_width_medium, R.string.rendering_value_medium_name),
	BOLD("bold", R.drawable.ic_action_gpx_width_bold, R.string.rendering_value_bold_name),
	CUSTOM("", R.drawable.ic_action_filter, R.string.shared_string_custom);

	WidthMode(@NonNull String key, @DrawableRes int iconId, @StringRes int titleId) {
		this.key = key;
		this.iconId = iconId;
		this.titleId = titleId;
	}

	private final String key;
	private final int iconId;
	private final int titleId;

	public String getKey() {
		return key;
	}

	public int getIconId() {
		return iconId;
	}

	public int getTitleId() {
		return titleId;
	}

	@NonNull
	public static WidthMode valueOfKey(@Nullable String key) {
		if (key != null && Algorithms.isInt(key)) {
			return CUSTOM;
		}
		for (WidthMode widthMode : values()) {
			if (Objects.equals(widthMode.getKey(), key)) {
				return widthMode;
			}
		}
		return values()[0];
	}
}