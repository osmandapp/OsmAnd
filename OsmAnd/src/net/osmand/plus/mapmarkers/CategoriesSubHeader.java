package net.osmand.plus.mapmarkers;

import androidx.annotation.DrawableRes;

import net.osmand.plus.itinerary.ItineraryGroup;

public class CategoriesSubHeader {

	@DrawableRes
	private int iconRes;
	private ItineraryGroup group;

	public CategoriesSubHeader(int iconRes, ItineraryGroup group) {
		this.iconRes = iconRes;
		this.group = group;
	}

	@DrawableRes
	public int getIconRes() {
		return iconRes;
	}

	public ItineraryGroup getGroup() {
		return group;
	}
}
