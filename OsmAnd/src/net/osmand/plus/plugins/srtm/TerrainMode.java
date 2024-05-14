package net.osmand.plus.plugins.srtm;

import androidx.annotation.StringRes;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TerrainMode {


	private static final String HILLSHADE_KEY = "hillshade";
	private static final String SLOPE_KEY = "slope";
	public static final String HILLSHADE_PREFIX = "hs_main_";
	public static final String HILLSHADE_SCND_PREFIX = "hs_scnd_";
	public static final String COLOR_SLOPE_PREFIX = "clr_";
	public static final String EXT = ".txt";
//	public static final String SLOPE_MAIN_COLOR_FILENAME = "clr_slope.txt";
//	public static final String HILLSHADE_MAIN_COLOR_FILENAME = "hs_main_hillshade.txt";
//	public static final String SLOPE_SECONDARY_COLOR_FILENAME = "hs_scnd_hillshade.txt";
	private static TerrainMode[] terrainModes;

	String translateName;
	private final CommonPreference<Integer> MIN_ZOOM;
	private final CommonPreference<Integer> MAX_ZOOM;
	private final CommonPreference<Integer> TRANSPARENCY;
	private final boolean hilshade;
	private final String key;
	// hillshade, slope
//	HILLSHADE_MIN_ZOOM = registerIntPreference("hillshade_min_zoom", 3).makeProfile();
//	HILLSHADE_MAX_ZOOM = registerIntPreference("hillshade_max_zoom", 17).makeProfile();
//	HILLSHADE_TRANSPARENCY = registerIntPreference("hillshade_transparency", 100).makeProfile();
//	SLOPE_MIN_ZOOM = registerIntPreference("slope_min_zoom", 3).makeProfile();
//	SLOPE_MAX_ZOOM = registerIntPreference("slope_max_zoom", 17).makeProfile();
//	SLOPE_TRANSPARENCY = registerIntPreference("slope_transparency", 80).makeProfile();
	public TerrainMode(OsmandApplication app, String key, String translateName, boolean hillshade) {
		this.hilshade = hillshade;
		this.key = key;
		this.translateName = translateName;
		MIN_ZOOM = app.getSettings().registerIntPreference(key+"_min_zoom", 3).makeProfile();
		MAX_ZOOM = app.getSettings().registerIntPreference(key+"_max_zoom", 17).makeProfile();
		TRANSPARENCY = app.getSettings().registerIntPreference(key+"_transparency",
				hillshade ? 100 : 80).makeProfile();
	}


	public static TerrainMode[] values(OsmandApplication app) {
		if (terrainModes != null) {
			return terrainModes;
		}
		TerrainMode hillshade =
				new TerrainMode(app, HILLSHADE_KEY, app.getString(R.string.shared_string_hillshade), true);
		TerrainMode slope =
				new TerrainMode(app, SLOPE_KEY, app.getString(R.string.shared_string_slope), false);
		List<TerrainMode> tms = new ArrayList<>();
		// HILLSHADE first
		tms.add(hillshade);
		tms.add(slope);
		File dir = app.getAppPath(IndexConstants.HEIGHTMAP_INDEX_DIR);
		if (dir.exists() && dir.listFiles() != null) {
			for (File lf : dir.listFiles()) {
				if(lf == null || !lf.getName().endsWith(EXT) ) {
					continue;
				}
				String nm = lf.getName();
				if (nm.startsWith(HILLSHADE_PREFIX)) {
					String key = nm.substring(HILLSHADE_PREFIX.length());
					key = key.substring(0, key.length() - EXT.length());
					if (!HILLSHADE_KEY.equals(key)) {
						tms.add(new TerrainMode(app, key, Algorithms.capitalizeFirstLetter(key), true));
					}
				} else if (nm.startsWith(COLOR_SLOPE_PREFIX)) {
					String key = nm.substring(COLOR_SLOPE_PREFIX.length());
					key = key.substring(0, key.length() - EXT.length());
					if (!SLOPE_KEY.equals(key)) {
						tms.add(new TerrainMode(app, key, Algorithms.capitalizeFirstLetter(key), false));
					}
				}
			}
		}
		terrainModes = tms.toArray(new TerrainMode[tms.size()]);
		return terrainModes;
	}

	public static TerrainMode getByKey(String key) {
		TerrainMode hillshade = null;
		for (TerrainMode m : terrainModes) {
			if (Algorithms.stringsEqual(m.getKey(), key)) {
				return m;
			} else if (m.hilshade && hillshade == null) {
				hillshade = m;
			}
		}
		return hillshade;
	}

	public boolean isHillshade() {
		return hilshade;
	}

	public boolean isColor() {
		return !hilshade;
	}

	public String getMainFile() {
		return (hilshade ? HILLSHADE_PREFIX : COLOR_SLOPE_PREFIX) + key + EXT;
	}

	public String getSecondFile() {
		return (hilshade ? HILLSHADE_SCND_PREFIX : "") + key + EXT;
	}

	public String getKey() {
		return key;
	}

	//	private static final String HILLSHADE_CACHE = "hillshade.cache";
	//	private static final String SLOPE_CACHE = "slope.cache";
	public String getCacheFileName() {
		return key +".cache";
	}

	public void setZoomValues(int minZoom, int maxZoom) {
		MIN_ZOOM.set(minZoom);
		MAX_ZOOM.set(maxZoom);
	}

	public void setTransparency(int transparency) {
		TRANSPARENCY.set(transparency);
	}

	public int getTransparency() {
		return TRANSPARENCY.get();
	}

	public void resetZoomsToDefault() {
		MIN_ZOOM.resetToDefault();
		MAX_ZOOM.resetToDefault();
	}

	public void resetTransparencyToDefault() {
		TRANSPARENCY.resetToDefault();
	}

	public int getMinZoom() {
		return MIN_ZOOM.get();
	}

	public int getMaxZoom() {
		return MAX_ZOOM.get();
	}

	public String getDescription() {
		return translateName;
	}
}
