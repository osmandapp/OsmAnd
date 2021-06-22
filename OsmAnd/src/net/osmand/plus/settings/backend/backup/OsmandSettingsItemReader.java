package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.items.OsmandSettingsItem;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

public abstract class OsmandSettingsItemReader<T extends OsmandSettingsItem> extends SettingsItemReader<T> {

	private final OsmandSettings settings;

	public OsmandSettingsItemReader(@NonNull T item, @NonNull OsmandSettings settings) {
		super(item);
		this.settings = settings;
	}

	protected abstract void readPreferenceFromJson(@NonNull OsmandPreference<?> preference,
												   @NonNull JSONObject json) throws JSONException;

	@Override
	public void readFromStream(@NonNull InputStream inputStream, String entryName) throws IOException, IllegalArgumentException {
		StringBuilder buf = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			String str;
			while ((str = in.readLine()) != null) {
				buf.append(str);
			}
		} catch (IOException e) {
			throw new IOException("Cannot read json body", e);
		}
		String jsonStr = buf.toString();
		if (Algorithms.isEmpty(jsonStr)) {
			throw new IllegalArgumentException("Cannot find json body");
		}
		final JSONObject json;
		try {
			json = new JSONObject(jsonStr);
		} catch (JSONException e) {
			throw new IllegalArgumentException("Json parse error", e);
		}
		readPreferencesFromJson(json);
	}

	public void readPreferencesFromJson(final JSONObject json) {
		settings.getContext().runInUIThread(new Runnable() {
			@Override
			public void run() {
				Map<String, OsmandPreference<?>> prefs = settings.getRegisteredPreferences();
				Iterator<String> iter = json.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					OsmandPreference<?> p = prefs.get(key);
					if (p != null) {
						try {
							readPreferenceFromJson(p, json);
						} catch (Exception e) {
							SettingsHelper.LOG.error("Failed to read preference: " + key, e);
						}
					} else {
						SettingsHelper.LOG.warn("No preference while importing settings: " + key);
					}
				}
			}
		});
	}
}
