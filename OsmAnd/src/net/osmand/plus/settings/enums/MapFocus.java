package net.osmand.plus.settings.enums;

import static net.osmand.plus.settings.backend.OsmandSettings.POSITION_PLACEMENT_AUTOMATIC;
import static net.osmand.plus.settings.backend.OsmandSettings.POSITION_PLACEMENT_BOTTOM;
import static net.osmand.plus.settings.backend.OsmandSettings.POSITION_PLACEMENT_CENTER;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public enum MapFocus {

	CENTER(R.string.position_on_map_center, R.drawable.ic_action_display_position_center, POSITION_PLACEMENT_CENTER),
	BOTTOM(R.string.position_on_map_bottom, R.drawable.ic_action_display_position_bottom, POSITION_PLACEMENT_BOTTOM),
	AUTOMATIC(R.string.shared_string_automatic, R.drawable.ic_action_display_position_auto, POSITION_PLACEMENT_AUTOMATIC);

	private int titleId;
	private int iconId;
	private int value;

	MapFocus(int titleId, int iconId, int value) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.value = value;
	}

	public int getTitleId() {
		return titleId;
	}

	public int getIconId() {
		return iconId;
	}

	public int getValue() {
		return value;
	}

	@NonNull
	public static MapFocus getByValue(int value) {
		for (MapFocus position : values()) {
			if (position.value == value) {
				return position;
			}
		}
		return AUTOMATIC;
	}
}
