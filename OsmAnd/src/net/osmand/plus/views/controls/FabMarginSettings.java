package net.osmand.plus.views.controls;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.PlatformUtil;
import net.osmand.plus.settings.backend.preferences.FabMarginPreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

public class FabMarginSettings {
	private static final Log LOG = PlatformUtil.getLog(FabMarginSettings.class.getName());

	private static final String FAB_MARGIN_X_PORTRAIT = "fab_margin_x_portrait";
	private static final String FAB_MARGIN_Y_PORTRAIT = "fab_margin_y_portrait";
	private static final String FAB_MARGIN_X_LANDSCAPE = "fab_margin_x_landscape";
	private static final String FAB_MARGIN_Y_LANDSCAPE = "fab_margin_y_landscape";

	private Integer fabMarginXPortrait = null;
	private Integer fabMarginYPortrait = null;
	private Integer fabMarginXLandscape = null;
	private Integer fabMarginYLandscape = null;

	public void setPortraitFabMargin(FabMarginPreference preference, int x, int y) {
		fabMarginXPortrait = x;
		fabMarginYPortrait = y;
		preference.set(this);
	}

	public void setLandscapeFabMargin(FabMarginPreference preference, int x, int y) {
		fabMarginXLandscape = x;
		fabMarginYLandscape = y;
		preference.set(this);
	}

	@Nullable
	public Pair<Integer, Integer> getPortraitFabMargin() {
		if (fabMarginXPortrait != null && fabMarginYPortrait != null) {
			return new Pair<>(fabMarginXPortrait, fabMarginYPortrait);
		}
		return null;
	}

	@Nullable
	public Pair<Integer, Integer> getLandscapeFabMargin() {
		if (fabMarginXLandscape != null && fabMarginYLandscape != null) {
			return new Pair<>(fabMarginXLandscape, fabMarginYLandscape);
		}
		return null;
	}

	public FabMarginSettings newInstance() {
		return new FabMarginSettings();
	}

	public String writeToJsonString() {
		try {
			JSONObject json = new JSONObject();
			writeToJson(json);
			return json.toString();
		} catch (JSONException e) {
			LOG.error("Error converting to json string: " + e);
		}
		return "";
	}

	public void writeToJson(JSONObject json) throws JSONException {
		json.put(FAB_MARGIN_X_PORTRAIT, fabMarginXPortrait);
		json.put(FAB_MARGIN_Y_PORTRAIT, fabMarginYPortrait);
		json.put(FAB_MARGIN_X_LANDSCAPE, fabMarginXLandscape);
		json.put(FAB_MARGIN_Y_LANDSCAPE, fabMarginYLandscape);
	}

	public void readFromJsonString(String jsonString) {
		if (Algorithms.isEmpty(jsonString)) {
			return;
		}
		try {
			JSONObject json = new JSONObject(jsonString);
			readFromJson(json);
		} catch (JSONException e) {
			LOG.error("Error converting to json string: " + e);
		}
	}

	public void readFromJson(@NonNull JSONObject json) {
		fabMarginXPortrait = json.optInt(FAB_MARGIN_X_PORTRAIT);
		fabMarginYPortrait = json.optInt(FAB_MARGIN_Y_PORTRAIT);
		fabMarginXLandscape = json.optInt(FAB_MARGIN_X_LANDSCAPE);
		fabMarginYLandscape = json.optInt(FAB_MARGIN_Y_LANDSCAPE);
	}
}
