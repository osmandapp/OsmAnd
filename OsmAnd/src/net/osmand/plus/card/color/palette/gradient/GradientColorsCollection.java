package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GradientColorsCollection extends ColorsCollection {

	private static final Log LOG = PlatformUtil.getLog(GradientColorsCollection.class);

	private static final String ATTR_ID = "id";
	private static final String ATTR_INDEX = "index";

	private final Map<String, Pair<ColorPalette, Long>> gradientPalettes;
	private final CommonPreference<String> preference;
	private final Object gradientType;
	private String type;

	public GradientColorsCollection(@NonNull OsmandApplication app,
	                                @NonNull Object gradientType) {
		OsmandSettings settings = app.getSettings();
		this.gradientType = gradientType;
		this.preference = settings.GRADIENT_PALETTES;
		this.gradientPalettes = app.getColorPaletteHelper().getPalletsForType(gradientType);

		if (gradientType instanceof ColorizationType) {
			type = ((ColorizationType) gradientType).name().toLowerCase();
		} else if (gradientType instanceof TerrainType) {
			type = ((TerrainType) gradientType).name();
		}
		loadColors();
	}

	@NonNull
	public List<PaletteColor> getPaletteColors() {
		return getColors(PaletteSortingMode.ORIGINAL);
	}

	@NonNull
	public Object getGradientType() {
		return gradientType;
	}

	@Nullable
	public PaletteGradientColor getDefaultGradientPalette() {
		for (PaletteColor paletteColor : getPaletteColors()) {
			PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
			if (gradientColor.getPaletteName().equals(PaletteGradientColor.DEFAULT_NAME)) {
				return gradientColor;
			}
		}
		return null;
	}

	@Override
	protected void loadColorsInLastUsedOrder() throws IOException {
		Map<String, Integer> loadedPreference = readPaletteColorsPreference();

		long now = System.currentTimeMillis();
		for (String key : gradientPalettes.keySet()) {
			Pair<ColorPalette, Long> pair = gradientPalettes.get(key);
			if (pair != null) {
				ColorPalette palette = pair.first;
				long creationTime = pair.second;
				String paletteId = createGradientPaletteId(type, key);
				Integer index = loadedPreference.get(paletteId);
				if (index == null) index = (int) creationTime;
				lastUsedOrder.add(new PaletteGradientColor(paletteId, palette, now++, index));
			}
		}
	}

	@NonNull
	private HashMap<String, Integer> readPaletteColorsPreference() {
		String jsonAsString = preference.get();
		HashMap<String, Integer> res = new HashMap<>();

		if (!Algorithms.isEmpty(jsonAsString)) {
			try {
				res = readFromJson(new JSONObject(jsonAsString), type);
			} catch (JSONException e) {
				LOG.debug("Error while reading palette colors from JSON ", e);
			}
		}
		return res;
	}

	@NonNull
	private static HashMap<String, Integer> readFromJson(@NonNull JSONObject json,
	                                                     @NonNull String type) throws JSONException {
		if (!json.has(type)) {
			return new HashMap<>();
		}
		HashMap<String, Integer> res = new HashMap<>();

		JSONArray typeGradients = json.getJSONArray(type);
		for (int i = 0; i < typeGradients.length(); i++) {
			try {
				JSONObject itemJson = typeGradients.getJSONObject(i);
				String id = itemJson.getString(ATTR_ID);
				int index = itemJson.getInt(ATTR_INDEX);
				res.put(id, index);
			} catch (JSONException e) {
				LOG.debug("Error while reading a palette color from JSON ", e);
			}
		}
		return res;
	}

	@Override
	protected void saveColors() {
		// Update indexes
		for (PaletteColor paletteColor : originalOrder) {
			int index = originalOrder.indexOf(paletteColor);
			paletteColor.setIndex(index + 1);
		}
		// Save colors to preference
		String savedGradientPreferences = preference.get();
		try {
			JSONObject jsonObject;
			if (!Algorithms.isEmpty(savedGradientPreferences)) {
				jsonObject = new JSONObject(savedGradientPreferences);
			} else {
				jsonObject = new JSONObject();
			}
			writeToJson(jsonObject, lastUsedOrder, type);
			String newGradientPreferences = jsonObject.toString();
			preference.set(newGradientPreferences);
		} catch (JSONException e) {
			LOG.debug("Error while reading palette colors from JSON ", e);
		}
	}

	private static void writeToJson(@NonNull JSONObject jsonObject,
	                                @NonNull List<PaletteColor> paletteColors,
	                                @NonNull String type) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (PaletteColor paletteColor : paletteColors) {
			PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
			JSONObject itemObject = new JSONObject();
			itemObject.put(ATTR_ID, gradientColor.getStringId());
			itemObject.put(ATTR_INDEX, gradientColor.getIndex());
			jsonArray.put(itemObject);
		}
		jsonObject.put(type, jsonArray);
	}

	@NonNull
	private String createGradientPaletteId(@NonNull String paletteName, @NonNull String colorizationName) {
		return colorizationName + GRADIENT_ID_SPLITTER + paletteName;
	}
}
