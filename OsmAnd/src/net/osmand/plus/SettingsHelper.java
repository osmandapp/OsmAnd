package net.osmand.plus;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class SettingsHelper {

	private static final Log LOG = PlatformUtil.getLog(SettingsHelper.class);
	private static final int BUFFER = 1024;

	public enum SettingsItemType {
		GLOBAL,
		PROFILE,
		PLUGIN,
		DATA,
		FILE,
	}

	public abstract static class SettingsItem {

		private SettingsItemType type;

		SettingsItem(@NonNull SettingsItemType type) {
			this.type = type;
		}

		public SettingsItemType getType() {
			return type;
		}

		public abstract String getName();

		public abstract SettingsItemReader getReader();

		public abstract SettingsItemWriter getWriter();
	}

	public abstract static class SettingsItemReader<T extends SettingsItem> {

		private T item;

		public SettingsItemReader(@NonNull T item) {
			this.item = item;
		}

		public abstract void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException;
	}

	public abstract static class SettingsItemWriter<T extends SettingsItem> {

		private T item;

		public SettingsItemWriter(T item) {
			this.item = item;
		}

		public T getItem() {
			return item;
		}

		public abstract boolean writeToStream(@NonNull OutputStream outputStream) throws IOException;
	}

	public abstract static class OsmandSettingsItem extends SettingsItem {

		private OsmandSettings settings;

		protected OsmandSettingsItem(@NonNull SettingsItemType type, @NonNull OsmandSettings settings) {
			super(type);
			this.settings = settings;
		}

		public OsmandSettings getSettings() {
			return settings;
		}
	}

	public abstract static class OsmandSettingsItemReader extends SettingsItemReader<OsmandSettingsItem> {

		private OsmandSettings settings;

		public OsmandSettingsItemReader(@NonNull OsmandSettingsItem item, @NonNull OsmandSettings settings) {
			super(item);
			this.settings = settings;
		}

		protected abstract void readPreferenceFromJson(@NonNull OsmandPreference<?> preference,
													   @NonNull JSONObject json) throws JSONException;

		@Override
		public void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException {
			StringBuilder buf = new StringBuilder();
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				String str;
				while ((str = in.readLine()) != null) {
					buf.append(str);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read json body", e);
			}
			String jsonStr = buf.toString();
			if (Algorithms.isEmpty(jsonStr)) {
				throw new IllegalArgumentException("Cannot find json body");
			}
			JSONObject json;
			try {
				json = new JSONObject(jsonStr);
			} catch (JSONException e) {
				throw new IllegalArgumentException("Json parse error", e);
			}
			Map<String, OsmandPreference<?>> prefs = settings.getRegisteredPreferences();
			Iterator<String> iter = json.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				OsmandPreference<?> p = prefs.get(key);
				try {
					readPreferenceFromJson(p, json);
				} catch (JSONException e) {
					LOG.error(null, e);
				}
			}
		}
	}

	public abstract static class OsmandSettingsItemWriter extends SettingsItemWriter<OsmandSettingsItem> {

		private OsmandSettings settings;

		public OsmandSettingsItemWriter(OsmandSettingsItem item, OsmandSettings settings) {
			super(item);
			this.settings = settings;
		}

		protected abstract void writePreferenceToJson(@NonNull OsmandPreference<?> preference,
													  @NonNull JSONObject json) throws JSONException;

		@Override
		public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
			JSONObject json = new JSONObject();
			Map<String, OsmandPreference<?>> prefs = settings.getRegisteredPreferences();
			for (OsmandPreference<?> pref : prefs.values()) {
				try {
					writePreferenceToJson(pref, json);
				} catch (JSONException e) {
					LOG.error(null, e);
				}
			}
			if (json.length() > 0) {
				try {
					String s = json.toString(2);
					outputStream.write(s.getBytes("UTF-8"));
				} catch (JSONException e) {
					LOG.error(null, e);
				}
				return true;
			}
			return false;
		}
	}

	public static class GlobalSettingsItem extends OsmandSettingsItem {

		public GlobalSettingsItem(@NonNull OsmandSettings settings) {
			super(SettingsItemType.GLOBAL, settings);
		}

		@Override
		public String getName() {
			return "global";
		}

		@Override
		public SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					preference.readFromJson(json, null);
				}
			};
		}

		@Override
		public SettingsItemWriter getWriter() {
			return new OsmandSettingsItemWriter(this, getSettings()) {
				@Override
				protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					preference.writeToJson(json, null);
				}
			};
		}
	}

	public static class ProfileSettingsItem extends OsmandSettingsItem {

		private ApplicationMode appMode;

		public ProfileSettingsItem(@NonNull OsmandSettings settings, @NonNull ApplicationMode appMode) {
			super(SettingsItemType.PROFILE, settings);
			this.appMode = appMode;
		}

		@Override
		public String getName() {
			return appMode.getStringKey();
		}

		@Override
		public SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					preference.readFromJson(json, appMode);
				}
			};
		}

		@Override
		public SettingsItemWriter getWriter() {
			return new OsmandSettingsItemWriter(this, getSettings()) {
				@Override
				protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					preference.writeToJson(json, appMode);
				}
			};
		}
	}

	public abstract static class StreamSettingsItemReader extends SettingsItemReader<StreamSettingsItem> {

		public StreamSettingsItemReader(@NonNull StreamSettingsItem item) {
			super(item);
		}

	}

	public static class StreamSettingsItemWriter extends SettingsItemWriter<StreamSettingsItem> {

		public StreamSettingsItemWriter(StreamSettingsItem item) {
			super(item);
		}

		@Override
		public boolean writeToStream(@NonNull OutputStream outputStream) throws IOException {
			boolean hasData = false;
			InputStream is = getItem().inputStream;
			if (is != null) {
				byte[] data = new byte[BUFFER];
				int count;
				while ((count = is.read(data, 0, BUFFER)) != -1) {
					outputStream.write(data, 0, count);
					if (!hasData) {
						hasData = true;
					}
				}
				Algorithms.closeStream(is);
			}
			return hasData;
		}
	}

	public abstract static class StreamSettingsItem extends SettingsItem {

		@Nullable
		private InputStream inputStream;
		private String name;

		public StreamSettingsItem(@NonNull SettingsItemType type, @NonNull String name) {
			super(type);
			this.name = name;
		}

		public StreamSettingsItem(@NonNull SettingsItemType type, @NonNull InputStream inputStream, @NonNull String name) {
			super(type);
			this.inputStream = inputStream;
			this.name = name;
		}

		@Nullable
		public InputStream getInputStream() {
			return inputStream;
		}

		protected void setInputStream(@Nullable InputStream inputStream) {
			this.inputStream = inputStream;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public SettingsItemWriter getWriter() {
			return new StreamSettingsItemWriter(this);
		}
	}

	public static class DataSettingsItem extends StreamSettingsItem {

		@Nullable
		private byte[] data;

		public DataSettingsItem(@NonNull String name) {
			super(SettingsItemType.DATA, name);
		}

		public DataSettingsItem(@NonNull byte[] data, @NonNull String name) {
			super(SettingsItemType.DATA, name);
			this.data = data;
		}

		@Nullable
		public byte[] getData() {
			return data;
		}

		@Override
		public SettingsItemReader getReader() {
			return new StreamSettingsItemReader(this) {
				@Override
				public void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					int nRead;
					byte[] data = new byte[BUFFER];
					while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
						buffer.write(data, 0, nRead);
					}

					buffer.flush();
					DataSettingsItem.this.data = buffer.toByteArray();
				}
			};
		}

		@Override
		public SettingsItemWriter getWriter() {
			setInputStream(new ByteArrayInputStream(data));
			return super.getWriter();
		}
	}

	public static class FileSettingsItem extends StreamSettingsItem {

		private File file;

		public FileSettingsItem(@NonNull OsmandApplication app, @NonNull File file) {
			super(SettingsItemType.FILE, file.getPath().replace(app.getAppPath(null).getPath(), ""));
			this.file = file;
		}

		public File getFile() {
			return file;
		}

		@Override
		public SettingsItemReader getReader() {
			return new StreamSettingsItemReader(this) {
				@Override
				public void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException {
					OutputStream output = new FileOutputStream(file);
					byte[] buffer = new byte[BUFFER];
					int count;
					while ((count = inputStream.read(buffer)) != -1) {
						output.write(buffer, 0, count);
					}
					output.flush();
				}
			};
		}

		@Override
		public SettingsItemWriter getWriter() {
			try {
				setInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				LOG.error(null, e);
			}
			return super.getWriter();
		}
	}

	private static class SettingsItemsFactory {

		private OsmandApplication app;

		SettingsItemsFactory(OsmandApplication app) {
			this.app = app;
		}

		@Nullable
		public SettingsItem createItem(@NonNull SettingsItemType type, @NonNull String name) {
			OsmandSettings settings = app.getSettings();
			switch (type) {
				case GLOBAL:
					return new GlobalSettingsItem(settings);
				case PROFILE:
					ApplicationMode appMode = ApplicationMode.valueOfStringKey(name, null);
					return appMode != null ? new ProfileSettingsItem(settings, appMode) : null;
				case PLUGIN:
					return null;
				case DATA:
					return new DataSettingsItem(name);
				case FILE:
					return new FileSettingsItem(app, new File(app.getAppPath(null), name));
			}
			return null;
		}
	}

	public static class SettingsExporter {

		private Map<String, SettingsItem> items;
		private Map<String, String> additionalParams;

		public SettingsExporter() {
			items = new LinkedHashMap<>();
			additionalParams = new LinkedHashMap<>();
		}

		public void addSettingsItem(SettingsItem item) throws IllegalArgumentException {
			if (items.containsKey(item.getName())) {
				throw new IllegalArgumentException("Already has such item: " + item.getName());
			}
			items.put(item.getName(), item);
		}

		public void addAdditionalParam(String key, String value) {
			additionalParams.put(key, value);
		}

		public void exportSettings(File zipFile) throws JSONException, IOException {
			JSONObject json = new JSONObject();
			json.put("osmand_settings_version", OsmandSettings.VERSION);
			for (Map.Entry<String, String> param : additionalParams.entrySet()) {
				json.put(param.getKey(), param.getValue());
			}
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(zipFile), BUFFER);
			ZipOutputStream zos = new ZipOutputStream(bos);
			try {
				for (SettingsItem item : items.values()) {
					ZipEntry entry = new ZipEntry(item.getName());
					entry.setExtra(item.getType().name().getBytes());
					zos.putNextEntry(entry);
					item.getWriter().writeToStream(zos);
					zos.closeEntry();
				}
				zos.flush();
				zos.finish();
			} finally {
				Algorithms.closeStream(zos);
				Algorithms.closeStream(bos);
			}
		}
	}

	public static class SettingsImporter {

		private OsmandApplication app;
		private List<SettingsItem> items;

		public SettingsImporter(@NonNull OsmandApplication app) {
			this.app = app;
		}

		public List<SettingsItem> getItems() {
			return Collections.unmodifiableList(items);
		}

		public void importSettings(File zipFile) throws IllegalArgumentException, IOException {
			items = new ArrayList<>();
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			InputStream ois = new BufferedInputStream(zis);
			SettingsItemsFactory itemsFactory = new SettingsItemsFactory(app);
			try {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String itemTypeStr = new String(entry.getExtra());
					if (!Algorithms.isEmpty(itemTypeStr)) {
						try {
							SettingsItemType type = SettingsItemType.valueOf(itemTypeStr);
							SettingsItem item = itemsFactory.createItem(type, entry.getName());
							if (item != null) {
								item.getReader().readFromStream(ois);
								items.add(item);
							}
						} catch (IllegalArgumentException e) {
							LOG.error("Wrong SettingsItemType: " + itemTypeStr, e);
						} finally {
							zis.closeEntry();
						}
					}
				}
			} catch (IOException ex) {
				LOG.error(ex);
			} finally {
				Algorithms.closeStream(ois);
				Algorithms.closeStream(zis);
			}
		}
	}
}
