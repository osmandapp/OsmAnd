package net.osmand.plus.settings.backend.backup.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.SettingsItemType;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class OsmandSettingsItem extends SettingsItem {

	private final OsmandSettings settings;

	protected OsmandSettingsItem(@NonNull OsmandSettings settings) {
		super(settings.getContext());
		this.settings = settings;
	}

	protected OsmandSettingsItem(@NonNull OsmandSettings settings, @Nullable OsmandSettingsItem baseItem) {
		super(settings.getContext(), baseItem);
		this.settings = settings;
	}

	protected OsmandSettingsItem(@NonNull SettingsItemType type, @NonNull OsmandSettings settings, @NonNull JSONObject json) throws JSONException {
		super(settings.getContext(), json);
		this.settings = settings;
	}

	public OsmandSettings getSettings() {
		return settings;
	}
}
