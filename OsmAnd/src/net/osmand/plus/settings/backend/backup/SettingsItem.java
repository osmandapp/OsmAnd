package net.osmand.plus.settings.backend.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class SettingsItem {

	protected OsmandApplication app;

	protected String pluginId;
	protected String fileName;

	boolean shouldReplace = false;

	protected List<String> warnings;

	SettingsItem(@NonNull OsmandApplication app) {
		this.app = app;
		init();
	}

	SettingsItem(@NonNull OsmandApplication app, @Nullable SettingsItem baseItem) {
		this.app = app;
		if (baseItem != null) {
			this.pluginId = baseItem.pluginId;
			this.fileName = baseItem.fileName;
		}
		init();
	}

	SettingsItem(OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		this.app = app;
		init();
		readFromJson(json);
	}

	protected void init() {
		warnings = new ArrayList<>();
	}

	public List<String> getWarnings() {
		return warnings;
	}

	@NonNull
	public abstract SettingsItemType getType();

	@NonNull
	public abstract String getName();

	@NonNull
	public abstract String getPublicName(@NonNull Context ctx);

	@NonNull
	public String getDefaultFileName() {
		return getName() + getDefaultFileExtension();
	}

	@NonNull
	public String getDefaultFileExtension() {
		return ".json";
	}

	public String getPluginId() {
		return pluginId;
	}

	@Nullable
	public String getFileName() {
		return fileName;
	}

	public boolean applyFileName(@NonNull String fileName) {
		String n = getFileName();
		return n != null && (n.endsWith(fileName) || fileName.startsWith(n + File.separator));
	}

	public boolean shouldReadOnCollecting() {
		return false;
	}

	public void setShouldReplace(boolean shouldReplace) {
		this.shouldReplace = shouldReplace;
	}

	static SettingsItemType parseItemType(@NonNull JSONObject json) throws IllegalArgumentException, JSONException {
		String type = json.has("type") ? json.getString("type") : null;
		if (type == null) {
			throw new IllegalArgumentException("No type field");
		}
		if (type.equals("QUICK_ACTION")) {
			type = "QUICK_ACTIONS";
		}
		return SettingsItemType.valueOf(type);
	}

	public boolean exists() {
		return false;
	}

	public void apply() {
		// non implemented
	}

	public void applyAdditionalParams() {
		// non implemented
	}

	void readFromJson(@NonNull JSONObject json) throws JSONException {
		pluginId = json.has("pluginId") ? json.getString("pluginId") : null;
		if (json.has("name")) {
			fileName = json.getString("name") + getDefaultFileExtension();
		}
		if (json.has("file")) {
			fileName = json.getString("file");
		}
		readItemsFromJson(json);
	}

	void writeToJson(@NonNull JSONObject json) throws JSONException {
		json.put("type", getType().name());
		String pluginId = getPluginId();
		if (!Algorithms.isEmpty(pluginId)) {
			json.put("pluginId", pluginId);
		}
		if (getWriter() != null) {
			String fileName = getFileName();
			if (Algorithms.isEmpty(fileName)) {
				fileName = getDefaultFileName();
			}
			json.put("file", fileName);
		}
	}

	String toJson() throws JSONException {
		JSONObject json = new JSONObject();
		writeToJson(json);
		return json.toString();
	}

	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		// override
	}

	void writeItemsToJson(@NonNull JSONObject json) {
		// override
	}

	@Nullable
	abstract SettingsItemReader<? extends SettingsItem> getReader();

	@Nullable
	abstract SettingsItemWriter<? extends SettingsItem> getWriter();

	@NonNull
	SettingsItemReader<? extends SettingsItem> getJsonReader() {
		return new SettingsItemReader<SettingsItem>(this) {
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
				String json = buf.toString();
				if (json.length() == 0) {
					throw new IllegalArgumentException("Json body is empty");
				}
				try {
					readItemsFromJson(new JSONObject(json));
				} catch (JSONException e) {
					throw new IllegalArgumentException("Json parsing error", e);
				}
			}
		};
	}

	@NonNull
	SettingsItemWriter<? extends SettingsItem> getJsonWriter() {
		return new SettingsItemWriter<SettingsItem>(this) {
			@Override
			public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
				JSONObject json = new JSONObject();
				writeItemsToJson(json);
				if (json.length() > 0) {
					try {
						String s = json.toString(2);
						outputStream.write(s.getBytes("UTF-8"));
					} catch (JSONException e) {
						warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
						SettingsHelper.LOG.error("Failed to write json to stream", e);
					}
					return true;
				}
				return false;
			}
		};
	}

	@NonNull
	SettingsItemWriter<? extends SettingsItem> getGpxWriter(@NonNull final GPXFile gpxFile) {
		return new SettingsItemWriter<SettingsItem>(this) {
			@Override
			public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
				Exception error = GPXUtilities.writeGpx(new OutputStreamWriter(outputStream, "UTF-8"), gpxFile);
				if (error != null) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed write to gpx file", error);
					return false;
				}
				return true;
			}
		};
	}

	@Override
	public int hashCode() {
		return (getType().name() + getName()).hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof SettingsItem)) {
			return false;
		}

		SettingsItem item = (SettingsItem) other;
		return item.getType() == getType()
				&& item.getName().equals(getName())
				&& Algorithms.stringsEqual(item.getFileName(), getFileName());
	}
}
