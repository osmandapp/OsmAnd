package net.osmand.plus.mapmarkers;

import androidx.annotation.DrawableRes;

public class GroupHeader {

	@DrawableRes
	private int iconRes;
	private MapMarkersGroup group;

	public GroupHeader(int iconRes, MapMarkersGroup group) {
		this.iconRes = iconRes;
		this.group = group;
	}

	@DrawableRes
	public int getIconRes() {
		return iconRes;
	}

	public MapMarkersGroup getGroup() {
		return group;
	}
}
