package net.osmand.plus.settings.backend;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_DASHBOARD_ID;

public class ContextMenuItemsSettings implements Serializable {

	private static final Log LOG = PlatformUtil.getLog(ContextMenuItemsSettings.class.getName());

	private static final String HIDDEN = "hidden";
	private static final String ORDER = "order";

	private List<String> hiddenIds = new ArrayList<>();
	private List<String> orderIds = new ArrayList<>();

	public ContextMenuItemsSettings() {

	}

	public ContextMenuItemsSettings(@NonNull List<String> hiddenIds, @NonNull List<String> orderIds) {
		this.hiddenIds = hiddenIds;
		this.orderIds = orderIds;
	}

	public ContextMenuItemsSettings newInstance() {
		return new ContextMenuItemsSettings();
	}

	public void readFromJsonString(String jsonString, @NonNull String idScheme) {
		if (Algorithms.isEmpty(jsonString)) {
			return;
		}
		try {
			JSONObject json = new JSONObject(jsonString);
			readFromJson(json, idScheme);
		} catch (JSONException e) {
			LOG.error("Error converting to json string: " + e);
		}
	}

	public void readFromJson(JSONObject json, String idScheme) {
		hiddenIds = readIdsList(json.optJSONArray(HIDDEN), idScheme);
		orderIds = readIdsList(json.optJSONArray(ORDER), idScheme);
	}

	protected List<String> readIdsList(JSONArray jsonArray, @NonNull String idScheme) {
		List<String> list = new ArrayList<>();
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				String id = jsonArray.optString(i);
				list.add(idScheme + id);
			}
		}
		return list;
	}

	public String writeToJsonString(@NonNull String idScheme) {
		try {
			JSONObject json = new JSONObject();
			writeToJson(json, idScheme);
			return json.toString();
		} catch (JSONException e) {
			LOG.error("Error converting to json string: " + e);
		}
		return "";
	}

	public void writeToJson(JSONObject json, String idScheme) throws JSONException {
		json.put(HIDDEN, getJsonArray(hiddenIds, idScheme));
		json.put(ORDER, getJsonArray(orderIds, idScheme));
	}

	protected JSONArray getJsonArray(List<String> ids, @NonNull String idScheme) {
		JSONArray jsonArray = new JSONArray();
		if (ids != null && !ids.isEmpty()) {
			for (String id : ids) {
				jsonArray.put(id.replace(idScheme, ""));
			}
		}
		return jsonArray;
	}

	public List<String> getHiddenIds() {
		return Collections.unmodifiableList(hiddenIds);
	}

	public List<String> getOrderIds() {
		return Collections.unmodifiableList(orderIds);
	}

	public static ContextMenuItemsSettings getDrawerDefaultInstance() {
		ArrayList<String> hiddenByDefault = new ArrayList<>();
		hiddenByDefault.add(DRAWER_DASHBOARD_ID);
		return new ContextMenuItemsSettings(hiddenByDefault, new ArrayList<String>());
	}
}
