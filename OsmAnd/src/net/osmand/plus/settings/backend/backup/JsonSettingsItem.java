package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

import org.json.JSONObject;

public class JsonSettingsItem extends SettingsItem {

	private final String name;
	private final JSONObject json;

	public JsonSettingsItem(@NonNull OsmandApplication app, @NonNull String name, @NonNull JSONObject json) {
		super(app);
		this.name = name;
		this.json = json;
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.JSON;
	}

	@NonNull
	@Override
	public String getName() {
		return name;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return name;
	}

	@NonNull
	@Override
	public String getDefaultFileExtension() {
		return ".json";
	}

	@NonNull
	@Override
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		return this.json;
	}

	@Nullable
	@Override
	SettingsItemReader<? extends SettingsItem> getReader() {
		return null;
	}

	@NonNull
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return getJsonWriter();
	}
}
