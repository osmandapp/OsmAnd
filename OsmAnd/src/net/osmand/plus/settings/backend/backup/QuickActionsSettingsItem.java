package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuickActionsSettingsItem extends CollectionSettingsItem<QuickAction> {

	private QuickActionRegistry actionRegistry;

	public QuickActionsSettingsItem(@NonNull OsmandApplication app, @NonNull List<QuickAction> items) {
		super(app, null, items);
	}

	public QuickActionsSettingsItem(@NonNull OsmandApplication app, @Nullable QuickActionsSettingsItem baseItem, @NonNull List<QuickAction> items) {
		super(app, baseItem, items);
	}

	QuickActionsSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		actionRegistry = app.getQuickActionRegistry();
		existingItems = actionRegistry.getQuickActions();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.QUICK_ACTIONS;
	}

	@Override
	public boolean isDuplicate(@NonNull QuickAction item) {
		return !actionRegistry.isNameUnique(item, app);
	}

	@NonNull
	@Override
	public QuickAction renameItem(@NonNull QuickAction item) {
		return actionRegistry.generateUniqueName(item, app);
	}

	@Override
	public void apply() {
		List<QuickAction> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);
			List<QuickAction> newActions = new ArrayList<>(existingItems);
			if (!duplicateItems.isEmpty()) {
				if (shouldReplace) {
					for (QuickAction duplicateItem : duplicateItems) {
						for (QuickAction savedAction : existingItems) {
							if (duplicateItem.getName(app).equals(savedAction.getName(app))) {
								newActions.remove(savedAction);
							}
						}
					}
				} else {
					for (QuickAction duplicateItem : duplicateItems) {
						renameItem(duplicateItem);
					}
				}
				appliedItems.addAll(duplicateItems);
			}
			newActions.addAll(appliedItems);
			actionRegistry.updateQuickActions(newActions);
		}
	}

	@NonNull
	@Override
	public String getName() {
		return "quick_actions";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return "quick_actions";
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<String, String>>() {
			}.getType();
			QuickActionRegistry quickActionRegistry = app.getQuickActionRegistry();
			JSONArray itemsJson = json.getJSONArray("items");
			for (int i = 0; i < itemsJson.length(); i++) {
				JSONObject object = itemsJson.getJSONObject(i);
				String name = object.getString("name");
				QuickAction quickAction = null;
				if (object.has("actionType")) {
					quickAction = quickActionRegistry.newActionByStringType(object.getString("actionType"));
				} else if (object.has("type")) {
					quickAction = quickActionRegistry.newActionByType(object.getInt("type"));
				}
				if (quickAction != null) {
					String paramsString = object.getString("params");
					HashMap<String, String> params = gson.fromJson(paramsString, type);

					if (!name.isEmpty()) {
						quickAction.setName(name);
					}
					quickAction.setParams(params);
					items.add(quickAction);
				} else {
					warnings.add(app.getString(R.string.settings_item_read_error, name));
				}
			}
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@Override
	void writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();
		if (!items.isEmpty()) {
			try {
				for (QuickAction action : items) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", action.hasCustomName(app)
							? action.getName(app) : "");
					jsonObject.put("actionType", action.getActionType().getStringId());
					jsonObject.put("params", gson.toJson(action.getParams(), type));
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader();
	}

	@Nullable
	@Override
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
