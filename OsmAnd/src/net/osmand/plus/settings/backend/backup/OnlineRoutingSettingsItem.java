package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine.EngineParameterType;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OnlineRoutingSettingsItem extends CollectionSettingsItem<OnlineRoutingEngine> {

	private OnlineRoutingHelper onlineRoutingHelper;

	public OnlineRoutingSettingsItem(@NonNull OsmandApplication app, @NonNull List<OnlineRoutingEngine> items) {
		super(app, null, items);
	}

	public OnlineRoutingSettingsItem(@NonNull OsmandApplication app, @Nullable OnlineRoutingSettingsItem baseItem, @NonNull List<OnlineRoutingEngine> items) {
		super(app, baseItem, items);
	}

	public OnlineRoutingSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		onlineRoutingHelper = app.getOnlineRoutingHelper();
		existingItems = new ArrayList<>(onlineRoutingHelper.getEngines());
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.ONLINE_ROUTING_ENGINES;
	}

	@NonNull
	@Override
	public String getName() {
		return "online_routing_engines";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.online_routing_engine);
	}

	@Override
	public void apply() {
		List<OnlineRoutingEngine> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (OnlineRoutingEngine duplicate : duplicateItems) {
				if (shouldReplace) {
					onlineRoutingHelper.deleteEngine(duplicate.getStringKey());
				}
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
			for (OnlineRoutingEngine engine : appliedItems) {
				onlineRoutingHelper.saveEngine(engine);
			}
		}
	}

	@Override
	public boolean isDuplicate(@NonNull OnlineRoutingEngine routingEngine) {
		for (OnlineRoutingEngine engine : existingItems) {
			if (engine.getStringKey().equals(routingEngine.getStringKey())
					|| engine.getName(app).equals(routingEngine.getName(app))) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public OnlineRoutingEngine renameItem(@NonNull OnlineRoutingEngine item) {
		int number = 0;
		while (true) {
			number++;
			OnlineRoutingEngine renamedItem = OnlineRoutingEngine.createNewEngine(item.getServerType(), item.getVehicleKey(), item.getParams());
			renamedItem.putParameter(EngineParameterType.CUSTOM_NAME, renamedItem.getName(app) + "_" + number);
			if (!isDuplicate(renamedItem)) {
				return renamedItem;
			}
		}
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			OnlineRoutingHelper.readFromJson(json, items);
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@Override
	void writeItemsToJson(@NonNull JSONObject json) {
		if (!items.isEmpty()) {
			try {
				OnlineRoutingHelper.writeToJson(json, items);
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
