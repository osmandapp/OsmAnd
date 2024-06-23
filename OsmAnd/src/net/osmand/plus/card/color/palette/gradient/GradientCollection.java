package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.PlatformUtil;
import net.osmand.plus.card.color.palette.main.PaletteColorsComparator;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradientCollection {
	private static final Log LOG = PlatformUtil.getLog(GradientCollection.class);

	private final List<PaletteGradientColor> paletteColors = new ArrayList<>();
	private final CommonPreference<String> gradientPalettesPreference;
	private String type;
	private final Object gradientType;

	public GradientCollection(@NonNull Map<String, Pair<ColorPalette, Long>> palettes,
							  @NonNull CommonPreference<String> preference, @NonNull Object gradientType) {
		this.gradientPalettesPreference = preference;
		this.gradientType = gradientType;

		if (gradientType instanceof ColorizationType) {
			type = ((ColorizationType) gradientType).name().toLowerCase();
		} else if (gradientType instanceof TerrainType) {
			type = ((TerrainType) gradientType).name();
		}
		loadPaletteColors(palettes);
	}

	private void loadPaletteColors(@NonNull Map<String, Pair<ColorPalette, Long>> gradientPalettes) {
		Map<String, PaletteColor> loadedPreferences = readPaletteColorsPreferences(gradientPalettesPreference);
		for (String key : gradientPalettes.keySet()) {
			Pair<ColorPalette, Long> pair = gradientPalettes.get(key);
			if (pair != null) {
				ColorPalette palette = pair.first;
				long creationTime = pair.second;
				PaletteColor savedPaletteColorPreference = loadedPreferences.get(type + GRADIENT_ID_SPLITTER + key);
				long lastUsedTime = savedPaletteColorPreference != null ? savedPaletteColorPreference.getLastUsedTime() : 0;
				paletteColors.add(new PaletteGradientColor(key, type, palette, creationTime, lastUsedTime));
			}
		}
	}

	@NonNull
	public Object getGradientType() {
		return gradientType;
	}

	@Nullable
	public PaletteGradientColor getDefaultGradientPalette() {
		for (PaletteGradientColor gradientColor : paletteColors) {
			if (gradientColor.getPaletteName().equals(PaletteGradientColor.DEFAULT_NAME)) {
				return gradientColor;
			}
		}
		return null;
	}

	public void askRenewLastUsedTime(@Nullable PaletteColor paletteColor) {
		if (paletteColor != null) {
			renewLastUsedTime(Collections.singletonList(paletteColor));
		}
	}

	public void renewLastUsedTime(@NonNull List<PaletteColor> paletteColors) {
		long now = System.currentTimeMillis();
		for (PaletteColor paletteColor : paletteColors) {
			paletteColor.setLastUsedTime(now++);
		}
		syncSettings();
	}

	@NonNull
	public List<PaletteGradientColor> getPaletteColors() {
		return paletteColors;
	}

	@NonNull
	public List<PaletteColor> getColors(@NonNull PaletteSortingMode sortingMode) {
		List<PaletteColor> sortedPaletteColors = new ArrayList<>(this.paletteColors);
		sortedPaletteColors.sort(new PaletteColorsComparator(sortingMode));
		return sortedPaletteColors;
	}

	@NonNull
	private HashMap<String, PaletteColor> readPaletteColorsPreferences(@NonNull CommonPreference<String> preference) {
		String jsonAsString = preference.get();
		HashMap<String, PaletteColor> paletteColors = new HashMap<>();

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
	private static HashMap<String, PaletteColor> readFromJson(@NonNull JSONObject json, @NonNull String type) throws JSONException {
		if (!json.has(type)) {
			return new HashMap<>();
		}
		HashMap<String, PaletteColor> res = new HashMap<>();

		JSONArray typeGradients = json.getJSONArray(type);
		for (int i = 0; i < typeGradients.length(); i++) {
			try {
				PaletteGradientColor paletteColor = new PaletteGradientColor(typeGradients.getJSONObject(i));
				res.put(paletteColor.getId(), paletteColor);
			} catch (JSONException e) {
				LOG.debug("Error while reading a palette color from JSON ", e);
			} catch (IllegalArgumentException e) {
				LOG.error("Error while trying to parse color from its HEX value ", e);
			}
		}
		return res;
	}

	private void syncSettings() {
		if (gradientPalettesPreference != null) {
			syncSettings(gradientPalettesPreference, paletteColors);
		}
	}

	private void syncSettings(@NonNull CommonPreference<String> preference,
							  @NonNull List<PaletteGradientColor> paletteColors) {
		String savedGradientPreferences = preference.get();

		try {
			JSONObject jsonObject;
			if (!Algorithms.isEmpty(savedGradientPreferences)) {
				jsonObject = new JSONObject(savedGradientPreferences);
			} else {
				jsonObject = new JSONObject();
			}
			writeToJson(jsonObject, paletteColors, type);
			String newGradientPreferences = jsonObject.toString();
			preference.set(newGradientPreferences);
		} catch (JSONException e) {
			LOG.debug("Error while reading palette colors from JSON ", e);
		}
	}


	private static void writeToJson(@NonNull JSONObject jsonObject,
									@NonNull List<PaletteGradientColor> paletteColors, @NonNull String type) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (PaletteGradientColor paletteColor : paletteColors) {
			jsonArray.put(paletteColor.toJson());
		}
		jsonObject.put(type, jsonArray);
	}
}
