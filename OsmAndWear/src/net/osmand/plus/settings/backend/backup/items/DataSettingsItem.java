package net.osmand.plus.settings.backend.backup.items;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemWriter;
import net.osmand.plus.settings.backend.backup.StreamSettingsItemReader;
import net.osmand.util.Algorithms;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DataSettingsItem extends StreamSettingsItem {

	@Nullable
	private byte[] data;

	public DataSettingsItem(@NonNull OsmandApplication app, @NonNull String name) {
		super(app, name);
	}

	public DataSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app, json);
	}

	@Override
	public long getSize() {
		return data != null ? data.length : 0;
	}

	public DataSettingsItem(@NonNull OsmandApplication app, @NonNull byte[] data, @NonNull String name) {
		super(app, name);
		this.data = data;
	}

	@NonNull
	@Override
	public SettingsItemType getType() {
		return SettingsItemType.DATA;
	}

	@NonNull
	@Override
	public String getDefaultFileExtension() {
		return ".dat";
	}

	@Nullable
	public byte[] getData() {
		return data;
	}

	@Override
	public long getLocalModifiedTime() {
		return 0;
	}

	@Override
	public void setLocalModifiedTime(long lastModifiedTime) {
	}

	@Override
	void readFromJson(@NonNull JSONObject json) throws JSONException {
		super.readFromJson(json);
		String fileName = getFileName();
		if (!Algorithms.isEmpty(fileName)) {
			name = Algorithms.getFileNameWithoutExtension(new File(fileName));
		}
	}

	@Nullable
	@Override
	public SettingsItemReader<? extends SettingsItem> getReader() {
		return new StreamSettingsItemReader(this) {
			@Override
			public void readFromStream(@NonNull InputStream inputStream, @Nullable File inputFile,
			                           @Nullable String entryName) throws IOException, IllegalArgumentException {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int nRead;
				byte[] data = new byte[SettingsHelper.BUFFER];
				while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, nRead);
				}

				buffer.flush();
				DataSettingsItem.this.data = buffer.toByteArray();
			}
		};
	}

	@Nullable
	@Override
	public SettingsItemWriter<? extends SettingsItem> getWriter() {
		setInputStream(new ByteArrayInputStream(data));
		return super.getWriter();
	}
}
