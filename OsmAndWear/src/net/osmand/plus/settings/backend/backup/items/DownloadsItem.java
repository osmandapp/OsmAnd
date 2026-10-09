package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.map.WorldRegion;
import net.osmand.plus.plugins.custom.CustomOsmandPlugin;
import net.osmand.plus.plugins.custom.CustomRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class DownloadsItem extends SettingsItem {

	private static final int APPROXIMATE_DOWNLOAD_ITEM_SIZE_BYTES = 2048;

	private List<WorldRegion> items;

	public DownloadsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
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
		return ctx.getString(R.string.welmode_download_maps);
	}

	public List<WorldRegion> getItems() {
		return items;
	}

	@Override
	public long getLocalModifiedTime() {
		return 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
	}

	@Override
	public long getEstimatedSize() {
		return APPROXIMATE_DOWNLOAD_ITEM_SIZE_BYTES;
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

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
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
		return json;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return null;
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
