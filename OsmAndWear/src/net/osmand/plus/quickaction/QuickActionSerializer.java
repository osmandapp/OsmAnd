package net.osmand.plus.quickaction;

import androidx.annotation.NonNull;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class QuickActionSerializer implements JsonDeserializer<QuickAction>, JsonSerializer<QuickAction> {

	private Map<String, QuickActionType> quickActionTypesStr = new TreeMap<>();
	private Map<Integer, QuickActionType> quickActionTypesInt = new TreeMap<>();

	protected void setQuickActionTypesStr(@NonNull Map<String, QuickActionType> quickActionTypesStr) {
		this.quickActionTypesStr = quickActionTypesStr;
	}

	protected void setQuickActionTypesInt(@NonNull Map<Integer, QuickActionType> quickActionTypesInt) {
		this.quickActionTypesInt = quickActionTypesInt;
	}

	@Override
	public QuickAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject obj = json.getAsJsonObject();
		QuickActionType found = null;
		if (obj.has("actionType")) {
			String actionType = obj.get("actionType").getAsString();
			found = quickActionTypesStr.get(actionType);
		} else if (obj.has("type")) {
			int type = obj.get("type").getAsInt();
			found = quickActionTypesInt.get(type);
		}
		if (found != null) {
			QuickAction qa = found.createNew();
			if (obj.has("name")) {
				qa.setName(obj.get("name").getAsString());
			}
			if (obj.has("id")) {
				qa.setId(obj.get("id").getAsLong());
			}
			if (obj.has("params")) {
				qa.setParams(context.deserialize(obj.get("params"), new TypeToken<HashMap<String, String>>() {}.getType()));
			}
			return qa;
		}
		return null;
	}

	@Override
	public JsonElement serialize(QuickAction src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("actionType", src.getActionType().getStringId());
		obj.addProperty("id", src.getId());
		if (src.getRawName() != null) {
			obj.addProperty("name", src.getRawName());
		}
		obj.add("params", context.serialize(src.getParams()));

		return obj;
	}
}