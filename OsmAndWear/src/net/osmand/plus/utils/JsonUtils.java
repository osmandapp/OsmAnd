package net.osmand.plus.utils;

import android.content.Context;
import android.content.res.Configuration;

import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonUtils {

	public static String getLocalizedResFromMap(Context ctx, Map<String, String> localizedMap, String defVal) {
		if (!Algorithms.isEmpty(localizedMap)) {
			Configuration config = ctx.getResources().getConfiguration();
			String lang = config.locale.getLanguage();
			String name = localizedMap.get(lang);
			if (Algorithms.isEmpty(name)) {
				name = localizedMap.get("");
			}
			if (!Algorithms.isEmpty(name)) {
				return name;
			}
		}
		return defVal;
	}

	public static List<String> jsonArrayToList(String key, JSONObject json) throws JSONException {
		List<String> items = new ArrayList<>();
		JSONArray jsonArray = json.optJSONArray(key);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				items.add(jsonArray.getString(i));
			}
		}
		return items;
	}

	public static Map<String, String> getLocalizedMapFromJson(String key, JSONObject json) throws JSONException {
		JSONObject jsonObject = json.optJSONObject(key);
		return getLocalizedMapFromJson(jsonObject);
	}

	public static Map<String, String> getLocalizedMapFromJson(JSONObject json) throws JSONException {
		Map<String, String> localizedMap = new HashMap<>();
		if (json != null) {
			for (Iterator<String> it = json.keys(); it.hasNext(); ) {
				String localeKey = it.next();
				String name = json.getString(localeKey);
				localizedMap.put(localeKey, name);
			}
		}
		return localizedMap;
	}

	public static void writeStringListToJson(String key, JSONObject json, List<String> items) throws JSONException {
		if (!Algorithms.isEmpty(items)) {
			JSONArray jsonArray = new JSONArray();
			for (String render : items) {
				jsonArray.put(render);
			}
			json.put(key, jsonArray);
		}
	}

	public static void writeLocalizedMapToJson(String jsonKey, JSONObject json, Map<String, String> map) throws JSONException {
		if (!Algorithms.isEmpty(map)) {
			JSONObject jsonObject = new JSONObject();
			for (Map.Entry<String, String> entry : map.entrySet()) {
				jsonObject.put(entry.getKey(), entry.getValue());
			}
			json.put(jsonKey, jsonObject);
		}
	}
}