package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.SettingsHelper.ExportProgressListener;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.osmand.plus.settings.backend.backup.SettingsHelper.VERSION;

public abstract class Exporter {

	private final Map<String, SettingsItem> items;
	private final Map<String, String> additionalParams;
	private ExportProgressListener progressListener;

	private boolean cancelled;

	public Exporter(@Nullable ExportProgressListener progressListener) {
		this.progressListener = progressListener;
		this.items = new LinkedHashMap<>();
		this.additionalParams = new LinkedHashMap<>();
	}

	public void addSettingsItem(SettingsItem item) throws IllegalArgumentException {
		if (items.containsKey(item.getName())) {
			throw new IllegalArgumentException("Already has such item: " + item.getName());
		}
		items.put(item.getName(), item);
	}

	public Map<String, SettingsItem> getItems() {
		return items;
	}

	@Nullable
	public ExportProgressListener getProgressListener() {
		return progressListener;
	}

	public void setProgressListener(@Nullable ExportProgressListener progressListener) {
		this.progressListener = progressListener;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public void addAdditionalParam(String key, String value) {
		additionalParams.put(key, value);
	}

	public abstract void export() throws JSONException, IOException;

	protected void writeItems(@NonNull AbstractWriter writer) throws IOException {
		for (SettingsItem item : getItems().values()) {
			writer.write(item);
			if (isCancelled()) {
				break;
			}
		}
	}

	protected JSONObject createItemsJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("version", VERSION);
		for (Map.Entry<String, String> param : additionalParams.entrySet()) {
			json.put(param.getKey(), param.getValue());
		}
		JSONArray itemsJson = new JSONArray();
		for (SettingsItem item : items.values()) {
			itemsJson.put(new JSONObject(item.toJson()));
		}
		json.put("items", itemsJson);
		return json;
	}
}
