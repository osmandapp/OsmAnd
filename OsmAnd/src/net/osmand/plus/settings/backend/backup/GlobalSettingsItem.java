package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;

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

	@NonNull
	@Override
	public String getName() {
		return "general_settings";
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return ctx.getString(R.string.general_settings_2);
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
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
	SettingsItemWriter<? extends SettingsItem> getWriter() {
		return new OsmandSettingsItemWriter<OsmandSettingsItem>(this, getSettings()) {
			@Override
			protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
				if ((preference instanceof CommonPreference) && (((CommonPreference<?>) preference).isShared())
						|| getSettings().APPLICATION_MODE.getId().equals(preference.getId())) {
					preference.writeToJson(json, null);
				}
			}
		};
	}
}
