package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;

public abstract class MapBaseCard extends BaseCard {

	protected MapActivity mapActivity;

	public MapBaseCard(@NonNull MapActivity mapActivity) {
		this(mapActivity, true);
	}

	public MapBaseCard(@NonNull MapActivity mapActivity, boolean usedOnMap) {
		super(mapActivity, usedOnMap);
		this.mapActivity = mapActivity;
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

}