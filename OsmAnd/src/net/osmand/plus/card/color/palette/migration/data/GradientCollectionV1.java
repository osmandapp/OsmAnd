package net.osmand.plus.card.color.palette.migration.data;

import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;

import android.util.Pair;

import androidx.annotation.NonNull;

import net.osmand.ColorPalette;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradientCollectionV1 {

	private static final Log LOG = PlatformUtil.getLog(GradientCollectionV1.class);

	private final List<PaletteGradientColorV1> paletteColors = new ArrayList<>();
	private final CommonPreference<String> gradientPalettesPreference;
	private final Object gradientType;
	private String type;

	public GradientCollectionV1(@NonNull OsmandApplication app,
	                            @NonNull CommonPreference<String> preference,
	                            @NonNull Object gradientType) {
		this.gradientPalettesPreference = preference;
		this.gradientType = gradientType;

		if (gradientType instanceof ColorizationType) {
			type = ((ColorizationType) gradientType).name().toLowerCase();
		} else if (gradientType instanceof TerrainType) {
			type = ((TerrainType) gradientType).name();
		}
		loadPaletteColors(app.getColorPaletteHelper().getPalletsForType(gradientType));
	}

	private void loadPaletteColors(@NonNull Map<String, Pair<ColorPalette, Long>> gradientPalettes) {
		Map<String, PaletteColorV1> loadedPreferences = readPaletteColorsPreferences(gradientPalettesPreference);
		for (String key : gradientPalettes.keySet()) {
			Pair<ColorPalette, Long> pair = gradientPalettes.get(key);
			if (pair != null) {
				ColorPalette palette = pair.first;
				long creationTime = pair.second;
				String paletteId = createGradientPaletteId(type, key);
				PaletteColorV1 savedPaletteColorPreference = loadedPreferences.get(paletteId);
				long lastUsedTime = savedPaletteColorPreference != null ? savedPaletteColorPreference.getLastUsedTime() : 0;
				paletteColors.add(new PaletteGradientColorV1(key, type, palette, creationTime, lastUsedTime));
			}
		}
	}

	@NonNull
	private HashMap<String, PaletteColorV1> readPaletteColorsPreferences(@NonNull CommonPreference<String> preference) {
		String jsonAsString = preference.get();
		HashMap<String, PaletteColorV1> paletteColors = new HashMap<>();

		if (!Algorithms.isEmpty(jsonAsString)) {
			try {
				paletteColors = readFromJson(new JSONObject(jsonAsString), type);
			} catch (JSONException e) {
				LOG.debug("Error while reading palette colors from JSON ", e);
			}
		}
		return paletteColors;
	}

	@NonNull
	private static HashMap<String, PaletteColorV1> readFromJson(@NonNull JSONObject json, @NonNull String type) throws JSONException {
		if (!json.has(type)) {
			return new HashMap<>();
		}
		HashMap<String, PaletteColorV1> res = new HashMap<>();

		JSONArray typeGradients = json.getJSONArray(type);
		for (int i = 0; i < typeGradients.length(); i++) {
			try {
				PaletteGradientColorV1 paletteColor = new PaletteGradientColorV1(typeGradients.getJSONObject(i));
				res.put(paletteColor.getId(), paletteColor);
			} catch (JSONException e) {
				LOG.debug("Error while reading a palette color from JSON ", e);
			} catch (IllegalArgumentException e) {
				LOG.error("Error while trying to parse color from its HEX value ", e);
			}
		}
		return res;
	}

	@NonNull
	private String createGradientPaletteId(@NonNull String paletteName, @NonNull String colorizationName) {
		return colorizationName + GRADIENT_ID_SPLITTER + paletteName;
	}
}
