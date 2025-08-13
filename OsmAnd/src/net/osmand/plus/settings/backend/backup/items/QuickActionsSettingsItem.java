package net.osmand.plus.settings.backend.backup.items;

import static net.osmand.plus.settings.backend.backup.SettingsItemType.QUICK_ACTIONS;
import static net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState.DEFAULT_BUTTON_ID;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.views.mapwidgets.configure.buttons.ButtonStateBean;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QuickActionsSettingsItem extends SettingsItem {

	private static final int APPROXIMATE_QUICK_ACTION_SIZE_BYTES = 135;

	private MapButtonsHelper mapButtonsHelper;

	private ButtonStateBean stateBean;
	private QuickActionButtonState buttonState;

	public QuickActionsSettingsItem(@NonNull OsmandApplication app,
			@NonNull QuickActionButtonState buttonState) {
		super(app);
		this.buttonState = buttonState;
		this.stateBean = ButtonStateBean.toStateBean(buttonState);
	}

	public QuickActionsSettingsItem(@NonNull OsmandApplication app,
			@Nullable QuickActionsSettingsItem baseItem, @NonNull ButtonStateBean stateBean) {
		super(app, baseItem);
		this.stateBean = stateBean;
		this.buttonState = new QuickActionButtonState(app, stateBean.id);
	}

	public QuickActionsSettingsItem(@NonNull OsmandApplication app,
			@NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		mapButtonsHelper = app.getMapButtonsHelper();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return QUICK_ACTIONS;
	}

	@NonNull
	public QuickActionButtonState getButtonState() {
		return buttonState;
	}

	@NonNull
	public ButtonStateBean getStateBean() {
		return stateBean;
	}

	@Override
	public long getLocalModifiedTime() {
		return buttonState.getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		buttonState.setLastModifiedTime(lastModifiedTime);
	}

	@Override
	public boolean exists() {
		return mapButtonsHelper.getActionButtonStateById(stateBean.id) != null;
	}

	@Override
	public long getEstimatedSize() {
		return (long) stateBean.quickActions.size() * APPROXIMATE_QUICK_ACTION_SIZE_BYTES;
	}

	@Override
	public void apply() {
		if (exists()) {
			if (shouldReplace) {
				QuickActionButtonState state = mapButtonsHelper.getActionButtonStateById(stateBean.id);
				if (state != null) {
					mapButtonsHelper.removeQuickActionButtonState(state);
				}
			} else {
				renameButton();
			}
		}
		stateBean.setupButtonState(app, buttonState);
		mapButtonsHelper.addQuickActionButtonState(buttonState);
	}

	private void renameButton() {
		stateBean.id = mapButtonsHelper.createNewButtonStateId();
		stateBean.name = mapButtonsHelper.generateUniqueButtonName(stateBean.name);
		buttonState = new QuickActionButtonState(app, stateBean.id);
	}

	@NonNull
	@Override
	public String getName() {
		return stateBean.id;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return stateBean.getName(ctx);
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		readButtonState(json);
		super.readFromJson(json);
	}

	private void readButtonState(@NonNull JSONObject json) {
		try {
			if (json.has("buttonState")) {
				JSONObject object = json.getJSONObject("buttonState");
				String id = object.getString("id");
				buttonState = new QuickActionButtonState(app, id);
				stateBean = new ButtonStateBean(id);
				stateBean.name = object.optString("name");
				stateBean.enabled = object.optBoolean("enabled");

				String iconName = object.optString("icon");
				if (!Algorithms.isEmpty(iconName)) {
					stateBean.icon = iconName;
				}
				int size = object.optInt("size", -1);
				if (size > 0) {
					stateBean.size = size;
				}
				int cornerRadius = object.optInt("corner_radius", -1);
				if (cornerRadius >= 0) {
					stateBean.cornerRadius = cornerRadius;
				}
				float opacity = (float) object.optDouble("opacity", -1);
				if (opacity >= 0) {
					stateBean.opacity = opacity;
				}
			} else {
				stateBean = new ButtonStateBean(DEFAULT_BUTTON_ID);
				buttonState = new QuickActionButtonState(app, DEFAULT_BUTTON_ID);
			}
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@Override
	void writeToJson(@NonNull JSONObject json) throws JSONException {
		super.writeToJson(json);
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", stateBean.id);

			if (!Algorithms.isEmpty(stateBean.name)) {
				jsonObject.put("name", stateBean.name);
			}
			jsonObject.put("enabled", stateBean.enabled);

			if (!Algorithms.isEmpty(stateBean.icon)) {
				jsonObject.put("icon", stateBean.icon);
			}
			if (stateBean.size > 0) {
				jsonObject.put("size", stateBean.size);
			}
			if (stateBean.cornerRadius >= 0) {
				jsonObject.put("corner_radius", stateBean.cornerRadius);
			}
			if (stateBean.opacity >= 0) {
				jsonObject.put("opacity", stateBean.opacity);
			}
			json.put("buttonState", jsonObject);
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
			SettingsHelper.LOG.error("Failed write to json", e);
		}
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<String, String>>() {}.getType();

			List<QuickAction> actions = new ArrayList<>();
			JSONArray itemsJson = json.getJSONArray("items");
			for (int i = 0; i < itemsJson.length(); i++) {
				JSONObject object = itemsJson.getJSONObject(i);
				String name = object.getString("name");
				QuickAction action = null;
				if (object.has("actionType")) {
					action = mapButtonsHelper.newActionByStringType(object.getString("actionType"));
				} else if (object.has("type")) {
					action = mapButtonsHelper.newActionByType(object.getInt("type"));
				}
				if (action != null) {
					String paramsString = object.getString("params");
					HashMap<String, String> params = gson.fromJson(paramsString, type);

					if (!name.isEmpty()) {
						action.setName(name);
					}
					action.setParams(params);
					actions.add(action);
				} else {
					warnings.add(app.getString(R.string.settings_item_read_error, name));
				}
			}
			stateBean.quickActions = actions;
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		Gson gson = new Gson();
		JSONArray jsonArray = new JSONArray();
		Type type = new TypeToken<HashMap<String, String>>() {}.getType();

		try {
			for (QuickAction action : stateBean.quickActions) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", action.hasCustomName(app) ? action.getName(app) : "");
				jsonObject.put("actionType", action.getActionType().getStringId());
				jsonObject.put("params", gson.toJson(action.getParams(), type));
				jsonArray.put(jsonObject);
			}
			json.put("items", jsonArray);
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
			SettingsHelper.LOG.error("Failed write to json", e);
		}
		return json;
	}

	@Override
	public boolean shouldReadOnCollecting() {
		return true;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader(true);
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
