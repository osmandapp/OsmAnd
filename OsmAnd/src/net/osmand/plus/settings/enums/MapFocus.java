package net.osmand.plus.settings.enums;

import static net.osmand.plus.settings.backend.OsmandSettings.POSITION_PLACEMENT_AUTOMATIC;
import static net.osmand.plus.settings.backend.OsmandSettings.POSITION_PLACEMENT_BOTTOM;
import static net.osmand.plus.settings.backend.OsmandSettings.POSITION_PLACEMENT_CENTER;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum MapFocus {

	CENTER(R.string.position_on_map_center, R.drawable.ic_action_display_position_center, POSITION_PLACEMENT_CENTER),
	BOTTOM(R.string.position_on_map_bottom, R.drawable.ic_action_display_position_bottom, POSITION_PLACEMENT_BOTTOM),
	AUTOMATIC(R.string.shared_string_automatic, R.drawable.ic_action_display_position_auto, POSITION_PLACEMENT_AUTOMATIC);

	private final int titleId;
	private final int iconId;
	private final int value;

	MapFocus(@StringRes int titleId, @DrawableRes int iconId, int value) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.value = value;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public int getValue() {
		return value;
	}

	@NonNull
	public static MapFocus valueOf(int value) {
		for (MapFocus position : values()) {
			if (position.value == value) {
				return position;
			}
		}
		return AUTOMATIC;
	}
}
