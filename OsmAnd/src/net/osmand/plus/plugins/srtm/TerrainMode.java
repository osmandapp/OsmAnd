package net.osmand.plus.plugins.srtm;

import androidx.annotation.NonNull;
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


	private static final String DEFAULT_KEY = "default";
	public static final String HILLSHADE_PREFIX = "hillshade_main_";
	public static final String HILLSHADE_SCND_PREFIX = "hillshade_color_";
	public static final String COLOR_SLOPE_PREFIX = "slope_";

	public static final String HEIGHT_PREFIX = "height_";
	public static final String EXT = ".txt";
	private static TerrainMode[] terrainModes;

	public enum TerrainType {
		HILLSHADE, SLOPE, HEIGHT
	}

	String translateName;
	private final CommonPreference<Integer> MIN_ZOOM;
	private final CommonPreference<Integer> MAX_ZOOM;
	private final CommonPreference<Integer> TRANSPARENCY;
	private final TerrainType type;
	private final String key;

	public TerrainMode(OsmandApplication app, String key, String translateName, TerrainType type) {
		this.type = type;
		this.key = key;
		this.translateName = translateName;
		MIN_ZOOM = app.getSettings().registerIntPreference(key+"_min_zoom", 3).makeProfile();
		MAX_ZOOM = app.getSettings().registerIntPreference(key+"_max_zoom", 17).makeProfile();
		TRANSPARENCY = app.getSettings().registerIntPreference(key+"_transparency",
				type == TerrainType.HILLSHADE ? 100 : 80).makeProfile();
	}


	public static TerrainMode[] values(OsmandApplication app) {
		if (terrainModes != null) {
			return terrainModes;
		}
		TerrainMode hillshade =
				new TerrainMode(app, DEFAULT_KEY, app.getString(R.string.shared_string_hillshade), TerrainType.HILLSHADE);
		TerrainMode slope =
				new TerrainMode(app, DEFAULT_KEY, app.getString(R.string.shared_string_slope), TerrainType.SLOPE);
		List<TerrainMode> tms = new ArrayList<>();
		// HILLSHADE first
		tms.add(hillshade);
		tms.add(slope);
		File dir = app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
		if (dir.exists() && dir.listFiles() != null) {
			for (File lf : dir.listFiles()) {
				if (lf == null || !lf.getName().endsWith(EXT)) {
					continue;
				}
				String nm = lf.getName();
				if (nm.startsWith(HILLSHADE_PREFIX)) {
					String key = nm.substring(HILLSHADE_PREFIX.length());
					key = key.substring(0, key.length() - EXT.length());
					String name = Algorithms.capitalizeFirstLetter(key).replace('_', ' ');
					if (!DEFAULT_KEY.equals(key)) {
						tms.add(new TerrainMode(app, key, name, TerrainType.HILLSHADE));
					}
				} else if (nm.startsWith(COLOR_SLOPE_PREFIX)) {
					String key = nm.substring(COLOR_SLOPE_PREFIX.length());
					key = key.substring(0, key.length() - EXT.length());
					String name = Algorithms.capitalizeFirstLetter(key).replace('_', ' ');
					if (!DEFAULT_KEY.equals(key)) {
						tms.add(new TerrainMode(app, key, name, TerrainType.SLOPE));
					}
				} else if (nm.startsWith(HEIGHT_PREFIX)) {
					String key = nm.substring(HEIGHT_PREFIX.length());
					key = key.substring(0, key.length() - EXT.length());
					String name = Algorithms.capitalizeFirstLetter(key).replace('_', ' ');
					if (!DEFAULT_KEY.equals(key)) {
						tms.add(new TerrainMode(app, key, name, TerrainType.HEIGHT));
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
			if (Algorithms.stringsEqual(m.getKeyName(), key)) {
				return m;
			} else if (m.type == TerrainType.HILLSHADE && hillshade == null) {
				hillshade = m;
			}
		}
		return hillshade;
	}

	public static boolean isModeExist(@NonNull String key) {
		for (TerrainMode m : terrainModes) {
			if (Algorithms.stringsEqual(m.getKeyName(), key)) {
				return true;
			}
		}
		return false;
	}

	public boolean isHillshade() {
		return type == TerrainType.HILLSHADE;
	}

	public TerrainType getType() {
		return type;
	}

	public String getMainFile() {
		String prefix = HILLSHADE_PREFIX;
		if (type == TerrainType.HEIGHT) {
			prefix = HEIGHT_PREFIX;
		} else if(type == TerrainType.SLOPE) {
			prefix = COLOR_SLOPE_PREFIX;
		}
		return prefix + key + EXT;
	}

	public String getSecondFile() {
		return (isHillshade() ? HILLSHADE_SCND_PREFIX : "") + key + EXT;
	}

	public String getKeyName() {
		if (key.equals(DEFAULT_KEY)) {
			return type.name().toLowerCase();
		}
		return key;
	}

	//	private static final String HILLSHADE_CACHE = "hillshade.cache";
	//	private static final String SLOPE_CACHE = "slope.cache";
	public String getCacheFileName() {
		return type.name().toLowerCase() +".cache";
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
