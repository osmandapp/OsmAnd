package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingUtils;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OnlineRoutingSettingsItem extends CollectionSettingsItem<OnlineRoutingEngine> {

	private static final int APPROXIMATE_ONLINE_ROUTING_SIZE_BYTES = 150;

	private OnlineRoutingHelper helper;
	private List<OnlineRoutingEngine> otherEngines;

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
		helper = app.getOnlineRoutingHelper();
		existingItems = new ArrayList<>(helper.getOnlyCustomEngines());
		otherEngines = new ArrayList<>(existingItems);
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
	public long getLocalModifiedTime() {
		return helper.getLastModifiedTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		helper.setLastModifiedTime(lastModifiedTime);
	}

	@Override
	public void apply() {
		List<OnlineRoutingEngine> newItems = getNewItems();
		if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
			appliedItems = new ArrayList<>(newItems);

			for (OnlineRoutingEngine duplicate : duplicateItems) {
				if (shouldReplace) {
					OnlineRoutingEngine cachedEngine = helper.getEngineByKey(duplicate.getStringKey());
					if (cachedEngine == null) {
						cachedEngine = helper.getEngineByName(duplicate.getName(app));
					}
					if (cachedEngine != null) {
						helper.deleteEngine(cachedEngine);
					}
				}
				appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
			}
			for (OnlineRoutingEngine engine : appliedItems) {
				helper.saveEngine(engine);
			}
		}
	}

	@Override
	protected void deleteItem(OnlineRoutingEngine item) {
		// TODO: delete settings item
	}

	@Override
	public boolean isDuplicate(@NonNull OnlineRoutingEngine routingEngine) {
		for (OnlineRoutingEngine engine : existingItems) {
			if (Algorithms.objectEquals(engine.getStringKey(), routingEngine.getStringKey())
					|| Algorithms.objectEquals(engine.getName(app), routingEngine.getName(app))) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	@Override
	public OnlineRoutingEngine renameItem(@NonNull OnlineRoutingEngine item) {
		OnlineRoutingEngine renamedItem = (OnlineRoutingEngine) item.clone();
		OnlineRoutingUtils.generateUniqueName(app, renamedItem, otherEngines);
		renamedItem.remove(EngineParameter.KEY);
		otherEngines.add(renamedItem);
		return renamedItem;
	}

	@Override
	public long getEstimatedItemSize(@NonNull OnlineRoutingEngine item) {
		return APPROXIMATE_ONLINE_ROUTING_SIZE_BYTES;
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			OnlineRoutingUtils.readFromJson(json, items);
		} catch (JSONException | IllegalArgumentException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		if (!items.isEmpty()) {
			try {
				OnlineRoutingUtils.writeToJson(json, items);
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
				SettingsHelper.LOG.error("Failed write to json", e);
			}
		}
		return json;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return getJsonReader();
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
