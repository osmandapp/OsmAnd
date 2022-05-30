package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.OsmandSettingsItemReader;
import net.osmand.plus.settings.backend.backup.OsmandSettingsItemWriter;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;

import org.json.JSONException;
import org.json.JSONObject;

public class GlobalSettingsItem extends OsmandSettingsItem {

	public GlobalSettingsItem(@NonNull OsmandSettings settings) {
		super(settings);
	}

	public GlobalSettingsItem(@NonNull OsmandSettings settings, @NonNull JSONObject json) throws JSONException {
		super(SettingsItemType.GLOBAL, settings, json);
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.GLOBAL;
	}

	@Override
	public long getLocalModifiedTime() {
		return getSettings().getLastGlobalPreferencesEditTime();
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
		getSettings().setLastGlobalPreferencesEditTime(lastModifiedTime);
	}

	@Override
	public long getEstimatedSize() {
		return getSettings().getSavedGlobalPrefsCount() * APPROXIMATE_PREFERENCE_SIZE_BYTES;
	}

	@NonNull
	@Override
	public String getName() {
		return "general_settings";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.global_settings);
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return new OsmandSettingsItemReader<OsmandSettingsItem>(this, getSettings()) {
			@Override
			protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
				if ((preference instanceof CommonPreference) && (((CommonPreference<?>) preference).isShared())
						|| getSettings().APPLICATION_MODE.getId().equals(preference.getId())) {
					preference.readFromJson(json, null);
				}
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return new OsmandSettingsItemWriter<OsmandSettingsItem>(this, getSettings()) {
			@Override
			protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
				if (getSettings().isExportAvailableForPref(preference)) {
					preference.writeToJson(json, null);
				}
			}
		};
	}
}
