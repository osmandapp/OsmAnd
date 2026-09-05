package net.osmand.plus.card.color.palette.gradient;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.routing.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GradientColorsCollection extends ColorsCollection {

	private static final Log LOG = PlatformUtil.getLog(GradientColorsCollection.class);

	private static final String ATTR_TYPE_NAME = "type_name";
	private static final String ATTR_PALETTE_NAME = "palette_name";
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
		return getColors(PaletteSortingMode.LAST_USED_TIME);
	}

	@NonNull
	public Object getGradientType() {
		return gradientType;
	}

	@Nullable
	public PaletteGradientColor getDefaultGradientPalette() {
		for (PaletteColor paletteColor : getPaletteColors()) {
			PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
			if (Objects.equals(gradientColor.getPaletteName(), PaletteGradientColor.DEFAULT_NAME)) {
				return gradientColor;
			}
		}
		return null;
	}

	@Override
	protected void loadColorsInLastUsedOrder() throws IOException {
		Set<String> addedPaletteIds = new HashSet<>();
		List<GradientData> loadedPreference = readPaletteColorsPreference();

		// Firstly collect all items those already have last used order
		for (GradientData gradientData : loadedPreference) {
			int index = gradientData.index;
			String typeName = gradientData.typeName;
			String paletteName = gradientData.paletteName;
			Pair<ColorPalette, Long> paletteInfo = gradientPalettes.get(paletteName);
			if (paletteInfo != null) {
				ColorPalette palette = paletteInfo.first;
				PaletteGradientColor gradientColor = new PaletteGradientColor(typeName, paletteName, palette, index);
				lastUsedOrder.add(gradientColor);
				addedPaletteIds.add(gradientColor.getStringId());
			}
		}
		// Collect all new palette files, those are not in cache
		for (String key : gradientPalettes.keySet()) {
			Pair<ColorPalette, Long> pair = gradientPalettes.get(key);
			if (pair != null) {
				ColorPalette palette = pair.first;
				long creationTime = pair.second;
				PaletteGradientColor gradientColor = new PaletteGradientColor(type, key, palette, (int) creationTime);
				String id = gradientColor.getStringId();
				if (!addedPaletteIds.contains(id)) {
					lastUsedOrder.add(gradientColor);
					addedPaletteIds.add(id);
				}
			}
		}
	}

	@NonNull
	private List<GradientData> readPaletteColorsPreference() {
		String jsonAsString = preference.get();
		List<GradientData> res = new ArrayList<>();

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
	private static List<GradientData> readFromJson(@NonNull JSONObject json,
	                                               @NonNull String type) throws JSONException {
		if (!json.has(type)) {
			return new ArrayList<>();
		}
		List<GradientData> res = new ArrayList<>();

		JSONArray typeGradients = json.getJSONArray(type);
		for (int i = 0; i < typeGradients.length(); i++) {
			try {
				JSONObject itemJson = typeGradients.getJSONObject(i);
				String typeName = itemJson.getString(ATTR_TYPE_NAME);
				String paletteName = itemJson.getString(ATTR_PALETTE_NAME);
				int index = itemJson.getInt(ATTR_INDEX);
				res.add(new GradientData(typeName, paletteName, index));
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
			itemObject.put(ATTR_TYPE_NAME, gradientColor.getTypeName());
			itemObject.put(ATTR_PALETTE_NAME, gradientColor.getPaletteName());
			itemObject.put(ATTR_INDEX, gradientColor.getIndex());
			jsonArray.put(itemObject);
		}
		jsonObject.put(type, jsonArray);
	}

	private static class GradientData {
		private final String typeName;
		private final String paletteName;
		private final int index;

		public GradientData(@NonNull String typeName, @NonNull String paletteName, int index) {
			this.typeName = typeName;
			this.paletteName = paletteName;
			this.index = index;
		}
	}
}
