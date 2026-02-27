package net.osmand.plus.plugins.srtm;

import static net.osmand.IndexConstants.TXT_EXT;
import static net.osmand.plus.plugins.srtm.TerrainMode.TerrainType.HEIGHT;
import static net.osmand.plus.plugins.srtm.TerrainMode.TerrainType.HILLSHADE;
import static net.osmand.plus.plugins.srtm.TerrainMode.TerrainType.SLOPE;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.palette.domain.PaletteConstants;
import net.osmand.shared.palette.domain.category.GradientPaletteCategory;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TerrainMode {

	public static final String DEFAULT_KEY = PaletteConstants.DEFAULT_NAME;
	public static final String ALTITUDE_DEFAULT_KEY = PaletteConstants.ALTITUDE_DEFAULT_NAME;
	public static final String HILLSHADE_PREFIX = "hillshade_main_";
	public static final String HILLSHADE_SCND_PREFIX = "hillshade_color_";
	public static final String COLOR_SLOPE_PREFIX = "slope_";

	public static final String HEIGHT_PREFIX = "height_";
	private static TerrainMode[] cachedTerrainModes;

	public enum TerrainType {
		HILLSHADE(R.string.shared_string_hillshade),
		SLOPE(R.string.shared_string_slope),
		HEIGHT(R.string.altitude);

		final int nameRes;

		TerrainType(@StringRes int nameRes) {
			this.nameRes = nameRes;
		}

		@NonNull
		public String getName(@NonNull Context ctx) {
			return ctx.getString(nameRes);
		}

		@NonNull
		public GradientPaletteCategory toPaletteCategory() {
			return switch (this) {
				case SLOPE -> GradientPaletteCategory.TERRAIN_SLOPE;
				case HILLSHADE -> GradientPaletteCategory.TERRAIN_HILLSHADE;
				case HEIGHT -> GradientPaletteCategory.TERRAIN_ALTITUDE;
			};
		}
	}

	private final String translateName;
	private final CommonPreference<Integer> MIN_ZOOM;
	private final CommonPreference<Integer> MAX_ZOOM;
	private final CommonPreference<Integer> TRANSPARENCY;
	private final TerrainType type;
	private final String key;

	private TerrainMode(@NonNull OsmandApplication app, @NonNull String key, @NonNull TerrainType type, @NonNull String translateName) {
		this.key = key;
		this.type = type;
		this.translateName = translateName;

		OsmandSettings settings = app.getSettings();
		MIN_ZOOM = settings.registerIntPreference(type + "_min_zoom", 3).makeProfile();
		MAX_ZOOM = settings.registerIntPreference(type + "_max_zoom", 17).makeProfile();
		TRANSPARENCY = settings.registerIntPreference(type + "_transparency", type == HILLSHADE ? 100 : 80).makeProfile();
	}

	@NonNull
	public static TerrainMode[] values(OsmandApplication app) {
		if (cachedTerrainModes == null) {
			reloadAvailableModes(app);
		}
		return cachedTerrainModes;
	}

	public static void reloadAvailableModes(@NonNull OsmandApplication app) {
		List<TerrainMode> modes = new ArrayList<>();
		// HILLSHADE first
		modes.add(new TerrainMode(app, DEFAULT_KEY, HILLSHADE, HILLSHADE.getName(app)));
		modes.add(new TerrainMode(app, DEFAULT_KEY, SLOPE, SLOPE.getName(app)));

		File dir = app.getAppPath(IndexConstants.CLR_PALETTE_DIR);
		File[] files = dir.exists() ? dir.listFiles() : null;
		if (files != null) {
			for (File file : files) {
				if (file == null || !file.getName().endsWith(TXT_EXT)) {
					continue;
				}
				String fileName = file.getName();
				if (fileName.startsWith(HILLSHADE_PREFIX)) {
					String paletteName = fileName.substring(HILLSHADE_PREFIX.length());
					paletteName = paletteName.substring(0, paletteName.length() - TXT_EXT.length());
					String name = Algorithms.capitalizeFirstLetter(paletteName).replace('_', ' ');
					if (!DEFAULT_KEY.equals(paletteName)) {
						modes.add(new TerrainMode(app, paletteName, HILLSHADE, name));
					}
				} else if (fileName.startsWith(COLOR_SLOPE_PREFIX)) {
					String paletteName = fileName.substring(COLOR_SLOPE_PREFIX.length());
					paletteName = paletteName.substring(0, paletteName.length() - TXT_EXT.length());
					String name = Algorithms.capitalizeFirstLetter(paletteName).replace('_', ' ');
					if (!DEFAULT_KEY.equals(paletteName)) {
						modes.add(new TerrainMode(app, paletteName, SLOPE, name));
					}
				} else if (fileName.startsWith(HEIGHT_PREFIX)) {
					String paletteName = fileName.substring(HEIGHT_PREFIX.length());
					paletteName = paletteName.substring(0, paletteName.length() - TXT_EXT.length());
					String name = Algorithms.capitalizeFirstLetter(paletteName).replace('_', ' ');
					if (!DEFAULT_KEY.equals(paletteName)) {
						modes.add(new TerrainMode(app, paletteName, HEIGHT, name));
					}
				}
			}
		}
		cachedTerrainModes = modes.toArray(new TerrainMode[0]);
	}

	@Nullable
	public static TerrainMode valueOf(@NonNull TerrainType type, @NonNull String keyName) {
		for (TerrainMode mode : cachedTerrainModes) {
			if (mode.type == type && Algorithms.stringsEqual(mode.getKeyName(), keyName)) {
				return mode;
			}
		}
		return null;
	}

	@Nullable
	public static TerrainMode getDefaultMode(@NonNull TerrainType type) {
		for (TerrainMode mode : cachedTerrainModes) {
			if (mode.type == type && mode.isDefaultMode()) {
				return mode;
			}
		}
		return null;
	}

	public static TerrainMode getByKey(String key) {
		TerrainMode hillshade = null;
		for (TerrainMode mode : cachedTerrainModes) {
			if (Algorithms.stringsEqual(mode.getKeyName(), key)) {
				return mode;
			} else if (mode.isHillshade() && hillshade == null) {
				hillshade = mode;
			}
		}
		return hillshade;
	}

	public static boolean isModeExist(@NonNull String key) {
		for (TerrainMode m : cachedTerrainModes) {
			if (Algorithms.stringsEqual(m.getKeyName(), key)) {
				return true;
			}
		}
		return false;
	}

	public boolean isHillshade() {
		return type == HILLSHADE;
	}

	public TerrainType getType() {
		return type;
	}

	public String getMainFileName() {
		String prefix = HILLSHADE_PREFIX;
		if (type == HEIGHT) {
			prefix = HEIGHT_PREFIX;
		} else if (type == SLOPE) {
			prefix = COLOR_SLOPE_PREFIX;
		}
		return prefix + key + TXT_EXT;
	}

	public String getSecondFileName() {
		return (isHillshade() ? HILLSHADE_SCND_PREFIX : "") + key + TXT_EXT;
	}

	public String getKeyName() {
		if (key.equals(DEFAULT_KEY) || key.equals(ALTITUDE_DEFAULT_KEY)) {
			return type.name().toLowerCase();
		}
		return key;
	}

	public boolean isDefaultMode() {
		return type == HEIGHT ? key.equals(ALTITUDE_DEFAULT_KEY) : key.equals(DEFAULT_KEY);
	}

	//	private static final String HILLSHADE_CACHE = "hillshade.cache";
	//	private static final String SLOPE_CACHE = "slope.cache";
	@NonNull
	public String getCacheFileName() {
		return type.name().toLowerCase() + ".cache";
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

	@NonNull
	public String getDescription() {
		return translateName;
	}
}
