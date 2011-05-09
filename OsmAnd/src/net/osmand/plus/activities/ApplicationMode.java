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

	public static boolean setAppMode(ApplicationMode preset, OsmandApplication app) {
		
		ApplicationMode old = OsmandSettings.getApplicationMode(OsmandSettings.getPrefs(app));
		if(preset == old){
			return false;
		}
		Editor edit = OsmandSettings.getWriteableEditor(app);
		edit.putString(OsmandSettings.APPLICATION_MODE, preset.toString());
		if (preset == ApplicationMode.CAR) {
			OsmandSettings.setUseInternetToDownloadTiles(true, edit);
			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, _);
			edit.putBoolean(OsmandSettings.SHOW_TRANSPORT_OVER_MAP, false);
			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_BEARING);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, true);
			edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, false);
			edit.putBoolean(OsmandSettings.USE_STEP_BY_STEP_RENDERING, true);
			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, true);
			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, 5);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.BOTTOM_CONSTANT);
			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);

		} else if (preset == ApplicationMode.BICYCLE) {
			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_CALCULATE_ROUTE, _);
			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_BEARING);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, true);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
			// edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, _);
			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, true);
			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, 30);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.BOTTOM_CONSTANT);
			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);

		} else if (preset == ApplicationMode.PEDESTRIAN) {
			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_COMPASS);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, true);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
			edit.putBoolean(OsmandSettings.USE_STEP_BY_STEP_RENDERING, false);
			// if(useInternetToDownloadTiles.isChecked()){
			// edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, true);
			// }
			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, false);
			// edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, _);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.CENTER_CONSTANT);
			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);

		} else if (preset == ApplicationMode.DEFAULT) {
			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_NONE);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
			edit.putBoolean(OsmandSettings.USE_STEP_BY_STEP_RENDERING, true);
			// edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, _);
			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, false);
			// edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, _);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.CENTER_CONSTANT);
			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);

		}

		BaseOsmandRender current = RendererRegistry.getRegistry().getCurrentSelectedRenderer();
		BaseOsmandRender defaultRender = RendererRegistry.getRegistry().defaultRender();
		BaseOsmandRender newRenderer;
		if (preset == ApplicationMode.CAR) {
			newRenderer = RendererRegistry.getRegistry().carRender();
		} else if (preset == ApplicationMode.BICYCLE) {
			newRenderer = RendererRegistry.getRegistry().bicycleRender();
		} else if (preset == ApplicationMode.PEDESTRIAN) {
			newRenderer = RendererRegistry.getRegistry().pedestrianRender();
		} else {
			newRenderer = defaultRender;
		}
		if (newRenderer != current) {
			RendererRegistry.getRegistry().setCurrentSelectedRender(newRenderer);
			app.getResourceManager().getRenderer().clearCache();
		}
		return edit.commit();
	}

}