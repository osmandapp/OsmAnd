package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public abstract class OsmandSettingsItemWriter<T extends OsmandSettingsItem> extends SettingsItemWriter<T> {

	private OsmandSettings settings;

	public OsmandSettingsItemWriter(@NonNull T item, @NonNull OsmandSettings settings) {
		super(item);
		this.settings = settings;
	}

	protected abstract void writePreferenceToJson(@NonNull OsmandPreference<?> preference,
												  @NonNull JSONObject json) throws JSONException;

	@Override
	public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
		JSONObject json = new JSONObject();
		Map<String, OsmandPreference<?>> prefs = settings.getRegisteredPreferences();
		for (OsmandPreference<?> pref : prefs.values()) {
			try {
				writePreferenceToJson(pref, json);
			} catch (JSONException e) {
				SettingsHelper.LOG.error("Failed to write preference: " + pref.getId(), e);
			}
		}
		if (json.length() > 0) {
			try {
				String s = json.toString(2);
				outputStream.write(s.getBytes("UTF-8"));
			} catch (JSONException e) {
				SettingsHelper.LOG.error("Failed to write json to stream", e);
			}
			return true;
		}
		return false;
	}
}
