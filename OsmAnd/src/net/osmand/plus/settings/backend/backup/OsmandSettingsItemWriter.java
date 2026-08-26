package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.items.OsmandSettingsItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class OsmandSettingsItemWriter<T extends OsmandSettingsItem> extends SettingsItemWriter<T> {

	private final OsmandSettings settings;

	public OsmandSettingsItemWriter(@NonNull T item, @NonNull OsmandSettings settings) {
		super(item);
		this.settings = settings;
	}

	protected abstract void writePreferenceToJson(@NonNull OsmandPreference<?> preference,
												  @NonNull JSONObject json) throws JSONException;

	@Override
	public void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException {
		JSONObject json = new JSONObject();
		List<Map.Entry<String, OsmandPreference<?>>> prefs = new ArrayList<>(settings.getRegisteredPreferences().entrySet());
		for (Map.Entry<String, OsmandPreference<?>> entry : prefs) {
			OsmandPreference<?> pref = entry.getValue();
			if (pref == null) {
				SettingsHelper.LOG.warn("No registered preference while exporting settings item: " + getItem().getName() + ", key: " + entry.getKey());
				continue;
			}
			try {
				writePreferenceToJson(pref, json);
			} catch (JSONException e) {
				SettingsHelper.LOG.error("Failed to write preference: " + pref.getId(), e);
			}
		}
		try {
			SettingsHelper.writeJson(json, outputStream, progress);
		} catch (JSONException e) {
			SettingsHelper.LOG.error("Failed to write json to stream", e);
		}
	}
}
