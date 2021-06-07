package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;

public abstract class MapActivityCard extends BaseCard {

	protected MapActivity mapActivity;

	public MapActivityCard(@NonNull MapActivity mapActivity) {
		this(mapActivity, true);
	}

	public MapActivityCard(@NonNull MapActivity mapActivity, boolean usedOnMap) {
		super(mapActivity, usedOnMap);
		this.mapActivity = mapActivity;
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

}