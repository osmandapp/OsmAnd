package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.backup.items.OsmandSettingsItem;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public abstract class OsmandSettingsItemReader<T extends OsmandSettingsItem> extends SettingsItemReader<T> {

	public OsmandSettingsItemReader(@NonNull T item) {
		super(item);
	}

	protected abstract void readPreferenceFromJson(@NonNull OsmandPreference<?> preference,
												   @NonNull JSONObject json) throws JSONException;

	@Override
	public void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
	                           @Nullable String entryName) throws IOException, IllegalArgumentException {
		StringBuilder buf = new StringBuilder();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
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
		JSONObject json;
		try {
			json = new JSONObject(jsonStr);
		} catch (JSONException e) {
			throw new IllegalArgumentException("Json parse error", e);
		}
		readPreferencesFromJson(json);
	}

	public abstract void readPreferencesFromJson(JSONObject json);
}
