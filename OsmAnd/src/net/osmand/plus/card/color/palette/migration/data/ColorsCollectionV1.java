package net.osmand.plus.card.color.palette.migration.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorsCollectionV1 {

	private static final Log LOG = PlatformUtil.getLog(ColorsCollectionV1.class);

	private final ApplicationMode appMode;
	private final List<PaletteColorV1> paletteColors;
	private final CommonPreference<String> palettePreference;
	private final CommonPreference<String> customColorsPreference;

	public ColorsCollectionV1(@NonNull ColorsCollectionBundle bundle) {
		this.appMode = bundle.appMode;
		this.palettePreference = bundle.palettePreference;
		this.customColorsPreference = bundle.customColorsPreference;
		this.paletteColors = loadPaletteColors(bundle);
	}

	@NonNull
	public List<PaletteColorV1> getColors() {
		return new ArrayList<>(this.paletteColors);
	}

	@NonNull
	private List<PaletteColorV1> loadPaletteColors(@NonNull ColorsCollectionBundle bundle) {
		List<PaletteColorV1> paletteColors = bundle.paletteColors;
		if (paletteColors == null) {
			paletteColors = loadPaletteColors(bundle.predefinedColors);
		}
		return paletteColors;
	}

	@NonNull
	private List<PaletteColorV1> loadPaletteColors(@Nullable List<PaletteColorV1> predefinedColors) {
		List<PaletteColorV1> paletteColors;
		if (customColorsPreference != null) {
			List<PaletteColorV1> savedPredefinedColors = readPaletteColors(palettePreference);
			List<PaletteColorV1> savedCustomColors = readPaletteColors(customColorsPreference);
			paletteColors = CollectionUtils.asOneList(savedPredefinedColors, savedCustomColors);
		} else {
			paletteColors = readPaletteColors(palettePreference);
		}
		predefinedColors = predefinedColors != null ? new ArrayList<>(predefinedColors) : new ArrayList<>();
		return mergePredefinedAndSavedColors(predefinedColors, paletteColors);
	}

	@NonNull
	private List<PaletteColorV1> readPaletteColors(@NonNull CommonPreference<String> preference) {
		String jsonAsString;
		if (appMode == null) {
			jsonAsString = preference.get();
		} else {
			jsonAsString = preference.getModeValue(appMode);
		}
		List<PaletteColorV1> paletteColors = new ArrayList<>();
		if (!Algorithms.isEmpty(jsonAsString)) {
			try {
				paletteColors = readFromJson(new JSONObject(jsonAsString));
			} catch (JSONException e) {
				LOG.debug("Error while reading palette colors from JSON ", e);
			}
		}
		return paletteColors;
	}

	@NonNull
	private static List<PaletteColorV1> readFromJson(@NonNull JSONObject json) throws JSONException {
		if (!json.has("colors")) {
			return new ArrayList<>();
		}
		List<PaletteColorV1> res = new ArrayList<>();
		JSONArray jsonArray = json.getJSONArray("colors");
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				res.add(new PaletteColorV1(jsonArray.getJSONObject(i)));
			} catch (JSONException e) {
				LOG.debug("Error while reading a palette color from JSON ", e);
			} catch (IllegalArgumentException e) {
				LOG.error("Error while trying to parse color from its HEX value ", e);
			}
		}
		return res;
	}

	@NonNull
	private static List<PaletteColorV1> mergePredefinedAndSavedColors(@NonNull List<PaletteColorV1> allColors,
	                                                                  @NonNull List<PaletteColorV1> savedColors) {
		Map<String, PaletteColorV1> cachedPredefinedColors = new HashMap<>();
		for (PaletteColorV1 predefinedColor : allColors) {
			cachedPredefinedColors.put(predefinedColor.getId(), predefinedColor);
		}
		for (PaletteColorV1 color : savedColors) {
			if (color.isDefault()) {
				PaletteColorV1 defaultColor = cachedPredefinedColors.get(color.getId());
				if (defaultColor != null) {
					defaultColor.setLastUsedTime(color.getLastUsedTime());
				}
			} else {
				allColors.add(color);
			}
		}
		return allColors;
	}

	public void saveToPreferences() {
		syncSettings();
	}

	private void syncSettings() {
		if (customColorsPreference != null) {
			// Save custom and predefined colors separately
			List<PaletteColorV1> predefinedColors = new ArrayList<>();
			List<PaletteColorV1> customColors = new ArrayList<>();
			for (PaletteColorV1 paletteColor : paletteColors) {
				if (paletteColor.isDefault()) {
					predefinedColors.add(paletteColor);
				} else {
					customColors.add(paletteColor);
				}
			}
			syncSettings(palettePreference, predefinedColors);
			syncSettings(customColorsPreference, customColors);
		} else {
			// Save custom and predefined colors into the same preference
			syncSettings(palettePreference, paletteColors);
		}
	}

	private void syncSettings(@NonNull CommonPreference<String> preference,
	                          @NonNull List<PaletteColorV1> paletteColors) {
		JSONObject json = new JSONObject();
		try {
			writeToJson(json, paletteColors);
			String jsonAsString = json.toString();
			if (appMode == null) {
				preference.set(jsonAsString);
			} else {
				preference.setModeValue(appMode, jsonAsString);
			}
		} catch (JSONException e) {
			LOG.debug("Error while writing palette colors into JSON ", e);
		}
	}

	private static void writeToJson(@NonNull JSONObject jsonObject,
	                                @NonNull List<PaletteColorV1> paletteColors) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (PaletteColorV1 paletteColor : paletteColors) {
			jsonArray.put(paletteColor.toJson());
		}
		jsonObject.put("colors", jsonArray);
	}
}
