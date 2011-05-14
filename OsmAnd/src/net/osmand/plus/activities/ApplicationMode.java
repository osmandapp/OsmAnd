package net.osmand.plus.activities;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.render.BaseOsmandRender;
import net.osmand.plus.render.RendererRegistry;
import android.content.Context;
import android.content.SharedPreferences.Editor;

public enum ApplicationMode {
	/*
	 * DEFAULT("Default"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
	 */

	DEFAULT(R.string.app_mode_default), 
	CAR(R.string.app_mode_car), 
	BICYCLE(R.string.app_mode_bicycle), 
	PEDESTRIAN(R.string.app_mode_pedestrian);

	private final int key;

	ApplicationMode(int key) {
		this.key = key;
	}

	public static String toHumanString(ApplicationMode m, Context ctx) {
		return ctx.getResources().getString(m.key);
	}

}