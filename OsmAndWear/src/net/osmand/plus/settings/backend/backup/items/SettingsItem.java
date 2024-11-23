package net.osmand.plus.settings.backend.backup.items;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.IProgress;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class SettingsItem {

	protected OsmandApplication app;

	protected String pluginId;
	protected String fileName;
	protected long lastModifiedTime;
	private boolean fromJson;

	protected boolean shouldReplace;

	protected List<String> warnings;

	public SettingsItem(@NonNull OsmandApplication app) {
		this.app = app;
		init();
	}

	public SettingsItem(@NonNull OsmandApplication app, @Nullable SettingsItem baseItem) {
		this.app = app;
		if (baseItem != null) {
			this.pluginId = baseItem.pluginId;
			this.fileName = baseItem.fileName;
		}
		init();
	}

	public SettingsItem(OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		this.app = app;
		this.fromJson = true;
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

	public long getLastModifiedTime() {
		if (fromJson) {
			return lastModifiedTime;
		} else if (lastModifiedTime == 0) {
			lastModifiedTime = getLocalModifiedTime();
		}
		return lastModifiedTime;
	}

	public void setLastModifiedTime(long lastModified) {
		this.lastModifiedTime = lastModified;
	}

	public abstract long getLocalModifiedTime();

	public abstract void setLocalModifiedTime(long lastModifiedTime);

	public abstract long getEstimatedSize();

	public boolean applyFileName(@NonNull String fileName) {
		String n = getFileName();
		return n != null && (n.endsWith(fileName) || fileName.startsWith(n + File.separator));
	}

	public boolean shouldReadOnCollecting() {
		return false;
	}

	public boolean isShouldReplace() {
		return shouldReplace;
	}

	public void setShouldReplace(boolean shouldReplace) {
		this.shouldReplace = shouldReplace;
	}

	@Nullable
	public static SettingsItemType parseItemType(@NonNull JSONObject json) throws IllegalArgumentException, JSONException {
		String typeName = json.has("type") ? json.getString("type") : null;
		return typeName == null ? null : SettingsItemType.fromName(typeName);
	}

	public boolean exists() {
		return false;
	}

	public void apply() {
		// non implemented
	}

	public void delete() {
		// non implemented
	}

	public void applyAdditionalParams(@Nullable SettingsItemReader<? extends SettingsItem> reader) {
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

	public String toJson() throws JSONException {
		JSONObject json = new JSONObject();
		writeToJson(json);
		return json.toString();
	}

	public JSONObject toJsonObj() throws JSONException {
		JSONObject json = new JSONObject();
		writeToJson(json);
		return json;
	}

	void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
		// override
	}

	@NonNull
	JSONObject writeItemsToJson(@NonNull JSONObject json) {
		// override
		return json;
	}

	@Nullable
	public abstract SettingsItemReader<? extends SettingsItem> getReader();

	@Nullable
	public abstract SettingsItemWriter<? extends SettingsItem> getWriter();

	@NonNull
	protected SettingsItemReader<? extends SettingsItem> getJsonReader() {
		return new SettingsItemReader<SettingsItem>(this) {
			@Override
			public void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
			                           @Nullable String entryName) throws IOException, IllegalArgumentException {
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
	protected SettingsItemWriter<? extends SettingsItem> getJsonWriter() {
		return new SettingsItemWriter<SettingsItem>(this) {
			@Override
			public void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException {
				JSONObject json = writeItemsToJson(new JSONObject());
				if (json.length() > 0) {
					try {
						int bytesDivisor = 1024;
						byte[] bytes = json.toString(2).getBytes("UTF-8");
						if (progress != null) {
							progress.startWork(bytes.length / bytesDivisor);
						}
						Algorithms.streamCopy(new ByteArrayInputStream(bytes), outputStream, progress, bytesDivisor);
					} catch (JSONException e) {
						warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
						SettingsHelper.LOG.error("Failed to write json to stream", e);
					}
				}
				if (progress != null) {
					progress.finishTask();
				}
			}
		};
	}

	@NonNull
	protected SettingsItemWriter<? extends SettingsItem> getGpxWriter(@NonNull GpxFile gpxFile) {
		return new SettingsItemWriter<SettingsItem>(this) {
			@Override
			public void writeToStream(@NonNull OutputStream outputStream, @Nullable IProgress progress) throws IOException {
				Exception error = SharedUtil.writeGpx(outputStream, gpxFile, SharedUtil.kIProgress(progress));
				if (error != null) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					SettingsHelper.LOG.error("Failed write to gpx file", error);
				}
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

	@NonNull
	@Override
	public String toString() {
		return "SettingsItem { " + getType().name() + ", " + getName() + ", " + getFileName() + " }";
	}
}
