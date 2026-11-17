package net.osmand.plus.mapmarkers;

import androidx.annotation.DrawableRes;

import net.osmand.plus.R;

public class GroupHeader {

	private final MapMarkersGroup group;

	public GroupHeader(MapMarkersGroup group) {
		this.group = group;
	}

	@DrawableRes
	public int getIconRes() {
		ItineraryType type = group.getType();
		if (type != ItineraryType.MARKERS) {
			return type == ItineraryType.FAVOURITES ? R.drawable.ic_action_favorite : R.drawable.ic_action_polygom_dark;
		}
		return 0;
	}

	public MapMarkersGroup getGroup() {
		return group;
	}
}
