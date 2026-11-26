package net.osmand.plus.onlinerouting;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public class OnlineRoutingUtils {

	private static final String ITEMS = "items";
	private static final String TYPE = "type";
	private static final String PARAMS = "params";

	public static void generateUniqueName(@NonNull Context ctx,
	                                      @NonNull OnlineRoutingEngine engine,
	                                      @NonNull List<OnlineRoutingEngine> otherEngines) {
		engine.remove(EngineParameter.NAME_INDEX);
		if (hasNameDuplicate(ctx, engine.getName(ctx), otherEngines)) {
			int index = 0;
			do {
				engine.put(EngineParameter.NAME_INDEX, String.valueOf(++index));
			} while (hasNameDuplicate(ctx, engine.getName(ctx), otherEngines));
		}
	}

	public static boolean hasNameDuplicate(@NonNull Context ctx,
	                                       @NonNull String engineName,
	                                       @NonNull List<OnlineRoutingEngine> otherEngines) {
		for (OnlineRoutingEngine engine : otherEngines) {
			if (Algorithms.objectEquals(engine.getName(ctx), engineName)) {
				return true;
			}
		}
		return false;
	}

	public static void readFromJson(@NonNull JSONObject json,
	                                @NonNull List<OnlineRoutingEngine> engines) throws JSONException {
		if (!json.has("items")) {
			return;
		}
		Gson gson = new Gson();
		Type typeToken = new TypeToken<HashMap<String, String>>() {
		}.getType();
		JSONArray itemsJson = json.getJSONArray(ITEMS);
		for (int i = 0; i < itemsJson.length(); i++) {
			JSONObject object = itemsJson.getJSONObject(i);
			if (object.has(TYPE) && object.has(PARAMS)) {
				OnlineRoutingEngine type = EngineType.getTypeByName(object.getString(TYPE));
				String paramsString = object.getString(PARAMS);
				HashMap<String, String> params = gson.fromJson(paramsString, typeToken);
				OnlineRoutingEngine engine = type.newInstance(params);
				if (!Algorithms.isEmpty(engine.getStringKey())) {
					engines.add(engine);
				}
			}
		}
	}

	public static void writeToJson(@NonNull JSONObject json,
	                               @NonNull List<OnlineRoutingEngine> engines) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();
		for (OnlineRoutingEngine engine : engines) {
			if (Algorithms.isEmpty(engine.getStringKey())) {
				continue;
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(TYPE, engine.getTypeName());
			jsonObject.put(PARAMS, gson.toJson(engine.getParams(), type));
			jsonArray.put(jsonObject);
		}
		json.put(ITEMS, jsonArray);
	}
}
