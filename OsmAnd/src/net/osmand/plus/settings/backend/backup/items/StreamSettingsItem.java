package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.backup.StreamSettingsItemWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public abstract class StreamSettingsItem extends SettingsItem {

	@Nullable
	private InputStream inputStream;
	protected String name;

	public StreamSettingsItem(@NonNull OsmandApplication app, @NonNull String name) {
		super(app);
		this.name = name;
		this.fileName = name;
	}

	public StreamSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	public StreamSettingsItem(@NonNull OsmandApplication app, @NonNull InputStream inputStream, @NonNull String name) {
		super(app);
		this.inputStream = inputStream;
		this.name = name;
		this.fileName = name;
	}

	@Nullable
	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(@Nullable InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public abstract long getSize();

	@NonNull
	@Override
	public String getName() {
		return name;
	}

	@NonNull
	@Override
	public String getPublicName(@NonNull Context ctx) {
		return getName();
	}

	@NonNull
	@Override
	public String getDefaultFileExtension() {
		return "";
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		super.readFromJson(json);
		name = json.has("name") ? json.getString("name") : null;
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		return new StreamSettingsItemWriter(this);
	}
}
