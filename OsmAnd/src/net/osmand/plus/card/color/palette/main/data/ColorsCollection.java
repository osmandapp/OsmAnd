package net.osmand.plus.card.color.palette.main.data;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.card.color.palette.main.PaletteColorsComparator;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorsCollection {

	private static final Log LOG = PlatformUtil.getLog(ColorsCollection.class);

	private final ApplicationMode appMode;
	private final List<PaletteColor> paletteColors;
	private final CommonPreference<String> palettePreference;
	private final CommonPreference<String> customColorsPreference;

	public ColorsCollection(@NonNull ColorsCollectionBundle bundle) {
		this.appMode = bundle.appMode;
		this.palettePreference = bundle.palettePreference;
		this.customColorsPreference = bundle.customColorsPreference;
		this.paletteColors = loadPaletteColors(bundle);
	}

	@Nullable
	public PaletteColor findPaletteColor(@ColorInt int colorInt) {
		for (PaletteColor paletteColor : paletteColors) {
			if (paletteColor.getColor() == colorInt) {
				return paletteColor;
			}
		}
		return null;
	}

	@NonNull
	public List<PaletteColor> getColors(@NonNull PaletteSortingMode sortingMode) {
		List<PaletteColor> sortedPaletteColors = new ArrayList<>(this.paletteColors);
		Collections.sort(sortedPaletteColors, new PaletteColorsComparator(sortingMode));
		return sortedPaletteColors;
	}

	@NonNull
	public PaletteColor duplicateColor(@NonNull PaletteColor paletteColor) {
		PaletteColor colorDuplicate = paletteColor.duplicate();
		if (paletteColor.isCustom()) {
			int index = paletteColors.indexOf(paletteColor);
			if (index >= 0 && index < paletteColors.size()) {
				paletteColors.add(index + 1, colorDuplicate);
			} else {
				paletteColors.add(colorDuplicate);
			}
		} else {
			paletteColors.add(colorDuplicate);
		}
		syncSettings();
		return colorDuplicate;
	}

	public boolean askRemoveColor(@NonNull PaletteColor paletteColor) {
		if (paletteColor.isCustom()) {
			paletteColors.remove(paletteColor);
			syncSettings();
			return true;
		}
		return false;
	}

	@Nullable
	public PaletteColor addOrUpdateColor(@Nullable PaletteColor paletteColor, @ColorInt int newColor) {
		if (paletteColor == null) {
			return addNewColor(newColor);
		}
		if (paletteColor.isCustom()) {
			updateColor(paletteColor, newColor);
			return paletteColor;
		}
		return null;
	}

	@NonNull
	private PaletteColor addNewColor(@ColorInt int newColor) {
		long now = System.currentTimeMillis();
		String id = PaletteColor.generateId(now);
		PaletteColor paletteColor = new PaletteColor(id, newColor, now);
		paletteColors.add(paletteColor);
		syncSettings();
		return paletteColor;
	}

	private void updateColor(@NonNull PaletteColor paletteColor, @ColorInt int newColor) {
		paletteColor.setColor(newColor);
		syncSettings();
	}

	@NonNull
	private List<PaletteColor> loadPaletteColors(@NonNull ColorsCollectionBundle bundle) {
		List<PaletteColor> paletteColors = bundle.paletteColors;
		if (paletteColors == null) {
			paletteColors = loadPaletteColors(bundle.predefinedColors);
		}
		return paletteColors;
	}

	@NonNull
	private List<PaletteColor> loadPaletteColors(@Nullable List<PaletteColor> predefinedColors) {
		List<PaletteColor> paletteColors;
		if (customColorsPreference != null) {
			List<PaletteColor> savedPredefinedColors = readPaletteColors(palettePreference);
			List<PaletteColor> savedCustomColors = readPaletteColors(customColorsPreference);
			paletteColors = CollectionUtils.asOneList(savedPredefinedColors, savedCustomColors);
		} else {
			paletteColors = readPaletteColors(palettePreference);
		}
		predefinedColors = predefinedColors != null ? new ArrayList<>(predefinedColors) : new ArrayList<>();
		return mergePredefinedAndSavedColors(predefinedColors, paletteColors);
	}

	@NonNull
	private List<PaletteColor> readPaletteColors(@NonNull CommonPreference<String> preference) {
		String jsonAsString;
		if (appMode == null) {
			jsonAsString = preference.get();
		} else {
			jsonAsString = preference.getModeValue(appMode);
		}
		List<PaletteColor> paletteColors = new ArrayList<>();
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
	private static List<PaletteColor> readFromJson(@NonNull JSONObject json) throws JSONException {
		if (!json.has("colors")) {
			return new ArrayList<>();
		}
		List<PaletteColor> res = new ArrayList<>();
		JSONArray jsonArray = json.getJSONArray("colors");
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				res.add(new PaletteColor(jsonArray.getJSONObject(i)));
			} catch (JSONException e) {
				LOG.debug("Error while reading a palette color from JSON ", e);
			} catch (IllegalArgumentException e) {
				LOG.error("Error while trying to parse color from its HEX value ", e);
			}
		}
		return res;
	}

	@NonNull
	private static List<PaletteColor> mergePredefinedAndSavedColors(@NonNull List<PaletteColor> allColors,
	                                                                @NonNull List<PaletteColor> savedColors) {
		Map<String, PaletteColor> cachedPredefinedColors = new HashMap<>();
		for (PaletteColor predefinedColor : allColors) {
			cachedPredefinedColors.put(predefinedColor.getId(), predefinedColor);
		}
		for (PaletteColor color : savedColors) {
			if (color.isDefault()) {
				PaletteColor defaultColor = cachedPredefinedColors.get(color.getId());
				if (defaultColor != null) {
					defaultColor.setLastUsedTime(color.getLastUsedTime());
				}
			} else {
				allColors.add(color);
			}
		}
		return allColors;
	}

	public void syncSettings() {
		if (customColorsPreference != null) {
			// Save custom and predefined colors separately
			List<PaletteColor> predefinedColors = new ArrayList<>();
			List<PaletteColor> customColors = new ArrayList<>();
			for (PaletteColor paletteColor : paletteColors) {
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
	                          @NonNull List<PaletteColor> paletteColors) {
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
	                                @NonNull List<PaletteColor> paletteColors) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (PaletteColor paletteColor : paletteColors) {
			jsonArray.put(paletteColor.toJson());
		}
		jsonObject.put("colors", jsonArray);
	}
}
