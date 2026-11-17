package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.items.OsmandSettingsItem;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
		List<OsmandPreference<?>> prefs = new ArrayList<>(settings.getRegisteredPreferences().values());
		for (OsmandPreference<?> pref : prefs) {
			try {
				writePreferenceToJson(pref, json);
			} catch (JSONException e) {
				SettingsHelper.LOG.error("Failed to write preference: " + pref.getId(), e);
			}
		}
		if (json.length() > 0) {
			try {
				int bytesDivisor = 1024;
				byte[] bytes = json.toString(2).getBytes("UTF-8");
				if (progress != null) {
					progress.startWork(bytes.length / bytesDivisor);
				}
				Algorithms.streamCopy(new ByteArrayInputStream(bytes), outputStream, progress, bytesDivisor);
			} catch (JSONException e) {
				SettingsHelper.LOG.error("Failed to write json to stream", e);
			}
		}
		if (progress != null) {
			progress.finishTask();
		}
	}
}
