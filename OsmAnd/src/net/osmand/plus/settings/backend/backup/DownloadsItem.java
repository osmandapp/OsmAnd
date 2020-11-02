package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.WorldRegion;
import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.CustomRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DownloadsItem extends SettingsItem {

	private List<WorldRegion> items;

	DownloadsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	protected void init() {
		super.init();
		items = new ArrayList<>();
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.DOWNLOADS;

	}

	@NonNull
	@Override
	public String getName() {
		return "downloads";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return "downloads";
	}

	public List<WorldRegion> getItems() {
		return items;
	}

	@Override
	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		try {
			if (!json.has("items")) {
				return;
			}
			JSONArray jsonArray = json.getJSONArray("items");
			items.addAll(CustomOsmandPlugin.collectRegionsFromJson(app, jsonArray));
		} catch (JSONException e) {
			warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
			throw new IllegalArgumentException("Json parse error", e);
		}
	}

	@Override
	void writeItemsToJson(@NonNull JSONObject json) {
		JSONArray jsonArray = new JSONArray();
		if (!items.isEmpty()) {
			try {
				for (WorldRegion region : items) {
					if (region instanceof CustomRegion) {
						JSONObject regionJson = ((CustomRegion) region).toJson();
						jsonArray.put(regionJson);
					}
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
		return null;
	}

	@Nullable
	@Override
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
