package net.osmand.plus.mapcontextmenu.builders;

import androidx.annotation.NonNull;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;

public class MapDataMenuBuilder extends MenuBuilder {

	public MapDataMenuBuilder(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	protected boolean needBuildCoordinatesRow() {
		return false;
	}
}
