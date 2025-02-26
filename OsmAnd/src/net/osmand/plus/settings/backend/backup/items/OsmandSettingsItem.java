package net.osmand.plus.settings.backend.backup.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class OsmandSettingsItem extends SettingsItem {

	protected static final int APPROXIMATE_PREFERENCE_SIZE_BYTES = 60;

	protected OsmandSettingsItem(@NonNull OsmandSettings settings) {
		super(settings.getContext());
	}

	protected OsmandSettingsItem(@NonNull OsmandSettings settings, @Nullable OsmandSettingsItem baseItem) {
		super(settings.getContext(), baseItem);
	}

	protected OsmandSettingsItem(@NonNull SettingsItemType type, @NonNull OsmandSettings settings, @NonNull JSONObject json) throws JSONException {
		super(settings.getContext(), json);
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public OsmandSettings getSettings() {
		return app.getSettings();
	}
}
