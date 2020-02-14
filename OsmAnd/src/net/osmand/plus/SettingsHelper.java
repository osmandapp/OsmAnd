package net.osmand.plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.ApplicationMode.ApplicationModeBean;
import net.osmand.plus.ApplicationMode.ApplicationModeBuilder;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionFactory;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;

/*
	Usage:

	SettingsHelper helper = app.getSettingsHelper();
	File file = new File(app.getAppPath(null), "settings.zip");

	List<SettingsItem> items = new ArrayList<>();
	items.add(new GlobalSettingsItem(app.getSettings()));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.DEFAULT));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.CAR));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.PEDESTRIAN));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.BICYCLE));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(GPX_INDEX_DIR), "Day 2.gpx")));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(GPX_INDEX_DIR), "Day 3.gpx")));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(RENDERERS_DIR), "default.render.xml")));
	items.add(new DataSettingsItem(new byte[] {'t', 'e', 's', 't', '1'}, "data1"));
	items.add(new DataSettingsItem(new byte[] {'t', 'e', 's', 't', '2'}, "data2"));

	helper.exportSettings(file, items);

	helper.importSettings(file);
 */

public class SettingsHelper {

	public static final String SETTINGS_LATEST_CHANGES_KEY = "settings_latest_changes";
	public static final String SETTINGS_VERSION_KEY = "settings_version";

	private static final Log LOG = PlatformUtil.getLog(SettingsHelper.class);
	private static final int BUFFER = 1024;

	private OsmandApplication app;
	private Activity activity;

	private boolean importing;
	private boolean importSuspended;
	private boolean collectOnly;
	private ImportAsyncTask importTask;

	public interface SettingsImportListener {
		void onSettingsImportFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items);
	}

	public interface SettingsExportListener {
		void onSettingsExportFinished(@NonNull File file, boolean succeed);
	}

	public SettingsHelper(OsmandApplication app) {
		this.app = app;
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
		if (importing && !collectOnly) {
			importTask.processNextItem();
		}
	}

	public void resetActivity(Activity activity) {
		if (this.activity == activity) {
			if (importing) {
				importTask.suspendImport();
				importSuspended = true;
			}
			this.activity = null;
		}
	}

	public boolean isImporting() {
		return importing;
	}

	public enum SettingsItemType {
		GLOBAL,
		PROFILE,
		PLUGIN,
		DATA,
		FILE,
		QUICK_ACTION,
		POI_UI_FILTERS,
		MAP_SOURCES,
	}

	public abstract static class SettingsItem {

		private SettingsItemType type;

		SettingsItem(@NonNull SettingsItemType type) {
			this.type = type;
		}

		SettingsItem(@NonNull SettingsItemType type, @NonNull JSONObject json) throws JSONException {
			this.type = type;
			readFromJson(json);
		}

		@NonNull
		public SettingsItemType getType() {
			return type;
		}

		@NonNull
		public abstract String getName();

		@NonNull
		public abstract String getPublicName(@NonNull Context ctx);

		@NonNull
		public abstract String getFileName();

		public Boolean shouldReadOnCollecting() {
			return false;
		}

		static SettingsItemType parseItemType(@NonNull JSONObject json) throws IllegalArgumentException, JSONException {
			return SettingsItemType.valueOf(json.getString("type"));
		}

		public boolean exists() {
			return false;
		}

		public void apply() {
			// non implemented
		}

		void readFromJson(@NonNull JSONObject json) throws JSONException {
		}

		void writeToJson(@NonNull JSONObject json) throws JSONException {
			json.put("type", type.name());
			json.put("name", getName());
		}

		String toJson() throws JSONException {
			JSONObject json = new JSONObject();
			writeToJson(json);
			return json.toString();
		}

		@NonNull
		abstract SettingsItemReader getReader();

		@NonNull
		abstract SettingsItemWriter getWriter();

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
			return item.getType() == getType() && item.getName().equals(getName());
		}
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

		protected OsmandSettingsItem(@NonNull SettingsItemType type, @NonNull OsmandSettings settings, @NonNull JSONObject json) throws JSONException {
			super(type, json);
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
				throw new IOException("Cannot read json body", e);
			}
			String jsonStr = buf.toString();
			if (Algorithms.isEmpty(jsonStr)) {
				throw new IllegalArgumentException("Cannot find json body");
			}
			final JSONObject json;
			try {
				json = new JSONObject(jsonStr);
			} catch (JSONException e) {
				throw new IllegalArgumentException("Json parse error", e);
			}
			settings.getContext().runInUIThread(new Runnable() {
				@Override
				public void run() {
					Map<String, OsmandPreference<?>> prefs = settings.getRegisteredPreferences();
					Iterator<String> iter = json.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						OsmandPreference<?> p = prefs.get(key);
						if (p != null) {
							try {
								readPreferenceFromJson(p, json);
							} catch (Exception e) {
								LOG.error("Failed to read preference: " + key, e);
							}
						} else {
							LOG.warn("No preference while importing settings: " + key);
						}
					}
				}
			});
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
					LOG.error("Failed to write preference: " + pref.getId(), e);
				}
			}
			if (json.length() > 0) {
				try {
					String s = json.toString(2);
					outputStream.write(s.getBytes("UTF-8"));
				} catch (JSONException e) {
					LOG.error("Failed to write json to stream", e);
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

		@NonNull
		@Override
		public String getFileName() {
			return getName() + ".json";
		}

		@Override
		public boolean exists() {
			return true;
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					preference.readFromJson(json, null);
				}
			};
		}

		@NonNull
		@Override
		SettingsItemWriter getWriter() {
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
		private ApplicationModeBuilder builder;
		private ApplicationModeBean modeBean;
		private Set<String> appModeBeanPrefsIds;


		public ProfileSettingsItem(@NonNull OsmandSettings settings, @NonNull ApplicationMode appMode) {
			super(SettingsItemType.PROFILE, settings);
			this.appMode = appMode;
			appModeBeanPrefsIds = new HashSet<>(Arrays.asList(settings.appModeBeanPrefsIds));
		}

		public ProfileSettingsItem(@NonNull OsmandSettings settings, @NonNull JSONObject json) throws JSONException {
			super(SettingsItemType.PROFILE, settings, json);
			readFromJson(settings.getContext(), json);
			appModeBeanPrefsIds = new HashSet<>(Arrays.asList(settings.appModeBeanPrefsIds));
		}

		public ApplicationMode getAppMode() {
			return appMode;
		}

		@NonNull
		@Override
		public String getName() {
			return appMode.getStringKey();
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			if (appMode.isCustomProfile()) {
				return modeBean.userProfileName;
			} else if (appMode.getNameKeyResource() != -1) {
				return ctx.getString(appMode.getNameKeyResource());
			} else {
				return getName();
			}
		}

		@NonNull
		@Override
		public String getFileName() {
			return "profile_" + getName() + ".json";
		}

		void readFromJson(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			String appModeJson = json.getString("appMode");
			modeBean = ApplicationMode.fromJson(appModeJson);
			builder = ApplicationMode.fromModeBean(app, modeBean);
			ApplicationMode appMode = builder.getApplicationMode();
			if (!appMode.isCustomProfile()) {
				appMode = ApplicationMode.valueOfStringKey(appMode.getStringKey(), appMode);
			}
			this.appMode = appMode;
		}

		@Override
		public boolean exists() {
			return builder != null && ApplicationMode.valueOfStringKey(getName(), null) != null;
		}

		@Override
		public void apply() {
			if (appMode.isCustomProfile()) {
				appMode = ApplicationMode.saveProfile(builder, getSettings().getContext());
			}
		}

		@Override
		void writeToJson(@NonNull JSONObject json) throws JSONException {
			super.writeToJson(json);
			json.put("appMode", new JSONObject(appMode.toJson()));
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					if (!appModeBeanPrefsIds.contains(preference.getId())) {
						preference.readFromJson(json, appMode);
					}
				}
			};
		}

		@NonNull
		@Override
		SettingsItemWriter getWriter() {
			return new OsmandSettingsItemWriter(this, getSettings()) {
				@Override
				protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					if (!appModeBeanPrefsIds.contains(preference.getId())) {
						preference.writeToJson(json, appMode);
					}
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
		protected String name;

		public StreamSettingsItem(@NonNull SettingsItemType type, @NonNull String name) {
			super(type);
			this.name = name;
		}

		StreamSettingsItem(@NonNull SettingsItemType type, @NonNull JSONObject json) throws JSONException {
			super(type, json);
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

		@Override
		void readFromJson(@NonNull JSONObject json) throws JSONException {
			super.readFromJson(json);
			name = json.getString("name");
		}

		@NonNull
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

		DataSettingsItem(@NonNull JSONObject json) throws JSONException {
			super(SettingsItemType.DATA, json);
		}

		public DataSettingsItem(@NonNull byte[] data, @NonNull String name) {
			super(SettingsItemType.DATA, name);
			this.data = data;
		}

		@NonNull
		@Override
		public String getFileName() {
			return getName() + ".dat";
		}

		@Nullable
		public byte[] getData() {
			return data;
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
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

		@NonNull
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

		FileSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(SettingsItemType.FILE, json);
			this.file = new File(app.getAppPath(null), name);
		}

		@NonNull
		@Override
		public String getFileName() {
			return getName();
		}

		public File getFile() {
			return file;
		}

		@Override
		public boolean exists() {
			return file.exists();
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
			return new StreamSettingsItemReader(this) {
				@Override
				public void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException {
					OutputStream output = new FileOutputStream(file);
					byte[] buffer = new byte[BUFFER];
					int count;
					try {
						while ((count = inputStream.read(buffer)) != -1) {
							output.write(buffer, 0, count);
						}
						output.flush();
					} finally {
						Algorithms.closeStream(output);
					}
				}
			};
		}

		@NonNull
		@Override
		public SettingsItemWriter getWriter() {
			try {
				setInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				LOG.error("Failed to set input stream from file: " + file.getName(), e);
			}
			return super.getWriter();
		}
	}

	public static class QuickActionSettingsItem extends OsmandSettingsItem {

		private List<QuickAction> quickActions;

		private OsmandApplication app;

		public QuickActionSettingsItem(@NonNull OsmandApplication app,
									   @NonNull List<QuickAction> quickActions) {
			super(SettingsItemType.QUICK_ACTION, app.getSettings());
			this.app = app;
			this.quickActions = quickActions;
		}

		public QuickActionSettingsItem(@NonNull OsmandApplication app,
									   @NonNull JSONObject jsonObject) throws JSONException {
			super(SettingsItemType.QUICK_ACTION, app.getSettings(), jsonObject);
			this.app = app;
		}

		public List<QuickAction> getQuickActions() {
			return quickActions;
		}

		@Override
		public void apply() {
			if (!quickActions.isEmpty()) {
				QuickActionFactory factory = new QuickActionFactory();
				List<QuickAction> savedActions = factory.parseActiveActionsList(getSettings().QUICK_ACTION_LIST.get());
				List<QuickAction> newActions = new ArrayList<>(savedActions);
				for (QuickAction action : quickActions) {
					for (QuickAction savedAction : savedActions) {
						if (action.getName(app).equals(savedAction.getName(app))) {
							newActions.remove(savedAction);
						}
					}
				}
				newActions.addAll(quickActions);
				((MapActivity) app.getSettingsHelper().getActivity()).getMapLayers().getQuickActionRegistry().updateQuickActions(newActions);
			}
		}

		@Override
		public Boolean shouldReadOnCollecting() {
			return true;
		}

		@NonNull
		@Override
		public String getName() {
			return "quick_actions";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return "quick_actions";
		}

		@NonNull
		@Override
		public String getFileName() {
			return getName() + ".json";
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {

				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {

				}

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
						throw new IOException("Cannot read json body", e);
					}
					String jsonStr = buf.toString();
					if (Algorithms.isEmpty(jsonStr)) {
						throw new IllegalArgumentException("Cannot find json body");
					}
					final JSONObject json;
					try {
						quickActions = new ArrayList<>();
						Gson gson = new Gson();
						Type type = new TypeToken<HashMap<String, String>>() {
						}.getType();
						json = new JSONObject(jsonStr);
						JSONArray items = json.getJSONArray("items");
						for (int i = 0; i < items.length(); i++) {
							JSONObject object = items.getJSONObject(i);
							String name = object.getString("name");
							int actionType = object.getInt("type");
							String paramsString = object.getString("params");
							HashMap<String, String> params = gson.fromJson(paramsString, type);
							QuickAction quickAction = new QuickAction(actionType);
							quickAction.setName(name);
							quickAction.setParams(params);
							quickActions.add(quickAction);
						}
					} catch (JSONException e) {
						throw new IllegalArgumentException("Json parse error", e);
					}
				}
			};
		}

		@NonNull
		@Override
		SettingsItemWriter getWriter() {
			return new OsmandSettingsItemWriter(this, getSettings()) {
				@Override
				protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					JSONArray items = new JSONArray();
					Gson gson = new Gson();
					Type type = new TypeToken<HashMap<String, String>>() {
					}.getType();
					if (!quickActions.isEmpty()) {
						for (QuickAction action : quickActions) {
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("name", action.getName(app));
							jsonObject.put("type", action.getType());
							jsonObject.put("params", gson.toJson(action.getParams(), type));
							items.put(jsonObject);
						}
						json.put("items", items);
					}
				}
			};
		}
	}

	public static class PoiUiFilterSettingsItem extends OsmandSettingsItem {

		private List<PoiUIFilter> poiUIFilters;

		private OsmandApplication app;

		public PoiUiFilterSettingsItem(OsmandApplication app, List<PoiUIFilter> poiUIFilters) {
			super(SettingsItemType.POI_UI_FILTERS, app.getSettings());
			this.app = app;
			this.poiUIFilters = poiUIFilters;
		}

		public PoiUiFilterSettingsItem(OsmandApplication app, JSONObject jsonObject) throws JSONException {
			super(SettingsItemType.POI_UI_FILTERS, app.getSettings(), jsonObject);
			this.app = app;
		}

		public List<PoiUIFilter> getPoiUIFilters() {
			return this.poiUIFilters != null ? this.poiUIFilters : new ArrayList<PoiUIFilter>();
		}

		@Override
		public void apply() {
			if (!poiUIFilters.isEmpty()) {
				for (PoiUIFilter filter : poiUIFilters) {
					app.getPoiFilters().createPoiFilter(filter, false);
				}
				app.getSearchUICore().refreshCustomPoiFilters();
			}
		}

		@NonNull
		@Override
		public String getName() {
			return "poi_ui_filters";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return null;
		}

		@Override
		public Boolean shouldReadOnCollecting() {
			return true;
		}

		@NonNull
		@Override
		public String getFileName() {
			return getName() + ".json";
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {

				}

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
						throw new IOException("Cannot read json body", e);
					}
					String jsonStr = buf.toString();
					if (Algorithms.isEmpty(jsonStr)) {
						throw new IllegalArgumentException("Cannot find json body");
					}
					final JSONObject json;
					try {
						poiUIFilters = new ArrayList<>();
						json = new JSONObject(jsonStr);
						JSONArray items = json.getJSONArray("items");
						Gson gson = new Gson();
						Type type = new TypeToken<HashMap<String, LinkedHashSet<String>>>() {
						}.getType();
						MapPoiTypes poiTypes = app.getPoiTypes();
						for (int i = 0; i < items.length(); i++) {
							JSONObject object = items.getJSONObject(i);
							String name = object.getString("name");
							String filterId = object.getString("filterId");
							String acceptedTypesString = object.getString("acceptedTypes");
							HashMap<String, LinkedHashSet<String>> acceptedTypes = gson.fromJson(acceptedTypesString, type);
							Map<PoiCategory, LinkedHashSet<String>> acceptedTypesDone = new HashMap<>();
							for (Map.Entry<String, LinkedHashSet<String>> mapItem : acceptedTypes.entrySet()) {
								final PoiCategory a = poiTypes.getPoiCategoryByName(mapItem.getKey());
								acceptedTypesDone.put(a, mapItem.getValue());
							}
							PoiUIFilter filter = new PoiUIFilter(name, filterId, acceptedTypesDone, app);
							poiUIFilters.add(filter);
						}
					} catch (JSONException e) {
						throw new IllegalArgumentException("Json parse error", e);
					}
				}
			};
		}

		@NonNull
		@Override
		SettingsItemWriter getWriter() {
			return new OsmandSettingsItemWriter(this, getSettings()) {
				@Override
				protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					JSONArray items = new JSONArray();
					Gson gson = new Gson();
					Type type = new TypeToken<HashMap<PoiCategory, LinkedHashSet<String>>>() {
					}.getType();
					if (!poiUIFilters.isEmpty()) {
						for (PoiUIFilter filter : poiUIFilters) {
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("name", filter.getName());
							jsonObject.put("filterId", filter.getFilterId());
							jsonObject.put("acceptedTypes", gson.toJson(filter.getAcceptedTypes(), type));
							items.put(jsonObject);
						}
						json.put("items", items);
					}
				}
			};
		}
	}

	public static class MapSourcesSettingsItem extends OsmandSettingsItem {

		private OsmandApplication app;

		private List<ITileSource> mapSources;

		public MapSourcesSettingsItem(OsmandApplication app, List<ITileSource> mapSources) {
			super(SettingsItemType.MAP_SOURCES, app.getSettings());
			this.app = app;
			this.mapSources = mapSources;
		}

		public MapSourcesSettingsItem(OsmandApplication app, JSONObject jsonObject) throws JSONException {
			super(SettingsItemType.MAP_SOURCES, app.getSettings(), jsonObject);
			this.app = app;
		}

		public List<ITileSource> getMapSources() {
			return this.mapSources;
		}

		@Override
		public void apply() {
			if (!mapSources.isEmpty()) {
				for (ITileSource template : mapSources) {
					if (template instanceof TileSourceManager.TileSourceTemplate) {
						getSettings().installTileSource((TileSourceManager.TileSourceTemplate) template);
					} else {
						((SQLiteTileSource) template).createDataBase();
					}
				}
			}
		}

		@NonNull
		@Override
		public String getName() {
			return "map_sources";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return null;
		}

		@Override
		public Boolean shouldReadOnCollecting() {
			return true;
		}

		@NonNull
		@Override
		public String getFileName() {
			return getName() + ".json";
		}

		@NonNull
		@Override
		SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {

				}

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
						throw new IOException("Cannot read json body", e);
					}
					String jsonStr = buf.toString();
					if (Algorithms.isEmpty(jsonStr)) {
						throw new IllegalArgumentException("Cannot find json body");
					}
					final JSONObject json;
					try {
						mapSources = new ArrayList<>();
						json = new JSONObject(jsonStr);
						JSONArray items = json.getJSONArray("items");
						for (int i = 0; i < items.length(); i++) {
							JSONObject object = items.getJSONObject(i);
							boolean sql = object.optBoolean("sql");
							String name = object.optString("name");
							int minZoom = object.optInt("minZoom");
							int maxZoom = object.optInt("maxZoom");
							String url = object.optString("url");
							String randoms = object.optString("randoms");
							boolean ellipsoid = object.optBoolean("ellipsoid", false);
							boolean invertedY = object.optBoolean("inverted_y", false);
							String referer = object.optString("referer");
							boolean timesupported = object.optBoolean("timesupported", false);
							long expire = object.optLong("expire");
							boolean inversiveZoom = object.optBoolean("inversiveZoom", false);

							String ext = object.optString("ext");
							int tileSize = object.optInt("tileSize");
							int bitDensity = object.optInt("bitDensity");
							int avgSize = object.optInt("avgSize");
							String rule = object.optString("rule");

							ITileSource template;
							if (!sql) {
								template = new TileSourceManager.TileSourceTemplate(name, url, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
							} else {
								template = new SQLiteTileSource(app, name, minZoom, maxZoom, url, randoms, ellipsoid, invertedY, referer, timesupported, expire, inversiveZoom);
							}
							mapSources.add(template);
						}
					} catch (JSONException e) {
						throw new IllegalArgumentException("Json parse error", e);
					}
				}
			};
		}

		@NonNull
		@Override
		SettingsItemWriter getWriter() {
			return new OsmandSettingsItemWriter(this, getSettings()) {
				@Override
				protected void writePreferenceToJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					JSONArray items = new JSONArray();
					if (!mapSources.isEmpty()) {
						for (ITileSource template : mapSources) {
							JSONObject jsonObject = new JSONObject();
							boolean sql = template instanceof SQLiteTileSource;
							jsonObject.put("sql", sql);
							jsonObject.put("name", template.getName());
							jsonObject.put("minZoom", template.getMinimumZoomSupported());
							jsonObject.put("maxZoom", template.getMaximumZoomSupported());
							jsonObject.put("url", template.getUrlTemplate());
							jsonObject.put("randoms", template.getRandoms());
							jsonObject.put("ellipsoid", template.isEllipticYTile());
							jsonObject.put("inverted_y", template.isInvertedYTile());
							jsonObject.put("referer", template.getReferer());
							jsonObject.put("timesupported", template.isTimeSupported());
							jsonObject.put("expire", template.getExpirationTimeMillis());
							jsonObject.put("inversiveZoom", template.getInversiveZoom());

							jsonObject.put("ext", template.getTileFormat());
							jsonObject.put("tileSize", template.getTileSize());
							jsonObject.put("bitDensity", template.getBitDensity());
							jsonObject.put("avgSize", template.getAvgSize());
							jsonObject.put("rule", template.getRule());

							items.put(jsonObject);
						}
						json.put("items", items);
					}
				}
			};
		}
	}

	private static class SettingsItemsFactory {

		private OsmandApplication app;
		private List<SettingsItem> items = new ArrayList<>();

		SettingsItemsFactory(OsmandApplication app, String jsonStr) throws IllegalArgumentException, JSONException {
			this.app = app;
			JSONObject json = new JSONObject(jsonStr);
			JSONArray itemsJson = json.getJSONArray("items");
			for (int i = 0; i < itemsJson.length(); i++) {
				JSONObject itemJson = itemsJson.getJSONObject(i);
				SettingsItem item = createItem(itemJson);
				if (item != null) {
					items.add(item);
				}
			}
			if (items.size() == 0) {
				throw new IllegalArgumentException("No items");
			}
		}

		@NonNull
		public List<SettingsItem> getItems() {
			return items;
		}

		@Nullable
		public SettingsItem getItemByFileName(@NonNull String fileName) {
			for (SettingsItem item : items) {
				if (item.getFileName().equals(fileName)) {
					return item;
				}
			}
			return null;
		}

		@Nullable
		private SettingsItem createItem(@NonNull JSONObject json) throws IllegalArgumentException, JSONException {
			SettingsItem item = null;
			SettingsItemType type = SettingsItem.parseItemType(json);
			OsmandSettings settings = app.getSettings();
			switch (type) {
				case GLOBAL:
					item = new GlobalSettingsItem(settings);
					break;
				case PROFILE:
					item = new ProfileSettingsItem(settings, json);
					break;
				case PLUGIN:
					break;
				case DATA:
					item = new DataSettingsItem(json);
					break;
				case FILE:
					item = new FileSettingsItem(app, json);
					break;
				case QUICK_ACTION:
					item = new QuickActionSettingsItem(app, json);
					break;
				case POI_UI_FILTERS:
					item = new PoiUiFilterSettingsItem(app, json);
					break;
				case MAP_SOURCES:
					item = new MapSourcesSettingsItem(app, json);
					break;
			}
			return item;
		}
	}

	private static class SettingsExporter {

		private Map<String, SettingsItem> items;
		private Map<String, String> additionalParams;

		SettingsExporter() {
			items = new LinkedHashMap<>();
			additionalParams = new LinkedHashMap<>();
		}

		void addSettingsItem(SettingsItem item) throws IllegalArgumentException {
			if (items.containsKey(item.getName())) {
				throw new IllegalArgumentException("Already has such item: " + item.getName());
			}
			items.put(item.getName(), item);
		}

		void addAdditionalParam(String key, String value) {
			additionalParams.put(key, value);
		}

		void exportSettings(File file) throws JSONException, IOException {
			JSONObject json = new JSONObject();
			json.put("osmand_settings_version", OsmandSettings.VERSION);
			for (Map.Entry<String, String> param : additionalParams.entrySet()) {
				json.put(param.getKey(), param.getValue());
			}
			JSONArray itemsJson = new JSONArray();
			for (SettingsItem item : items.values()) {
				itemsJson.put(new JSONObject(item.toJson()));
			}
			json.put("items", itemsJson);
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUFFER);
			ZipOutputStream zos = new ZipOutputStream(os);
			try {
				ZipEntry entry = new ZipEntry("items.json");
				zos.putNextEntry(entry);
				zos.write(json.toString(2).getBytes("UTF-8"));
				zos.closeEntry();
				for (SettingsItem item : items.values()) {
					entry = new ZipEntry(item.getFileName());
					zos.putNextEntry(entry);
					item.getWriter().writeToStream(zos);
					zos.closeEntry();
				}
				zos.flush();
				zos.finish();
			} finally {
				Algorithms.closeStream(zos);
				Algorithms.closeStream(os);
			}
		}
	}

	private static class SettingsImporter {

		private OsmandApplication app;

		SettingsImporter(@NonNull OsmandApplication app) {
			this.app = app;
		}

		List<SettingsItem> collectItems(@NonNull File file) throws IllegalArgumentException, IOException {
			return processItems(file, null);
		}

		void importItems(@NonNull File file, @NonNull List<SettingsItem> items) throws IllegalArgumentException, IOException {
			processItems(file, items);
		}

		private List<SettingsItem> processItems(@NonNull File file, @Nullable List<SettingsItem> items) throws IllegalArgumentException, IOException {
			boolean collecting = items == null;
			if (collecting) {
				items = new ArrayList<>();
			} else {
				if (items.size() == 0) {
					throw new IllegalArgumentException("No items");
				}
			}
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			InputStream ois = new BufferedInputStream(zis);
			try {
				ZipEntry entry = zis.getNextEntry();
				if (entry != null && entry.getName().equals("items.json")) {
					String itemsJson = null;
					try {
						itemsJson = Algorithms.readFromInputStream(ois).toString();
					} catch (IOException e) {
						LOG.error("Error reading items.json: " + itemsJson, e);
						throw new IllegalArgumentException("No items");
					} finally {
						zis.closeEntry();
					}
					SettingsItemsFactory itemsFactory;
					try {
						itemsFactory = new SettingsItemsFactory(app, itemsJson);
						if (collecting) {
							items.addAll(itemsFactory.getItems());
						}
					} catch (IllegalArgumentException e) {
						LOG.error("Error parsing items: " + itemsJson, e);
						throw new IllegalArgumentException("No items");
					} catch (JSONException e) {
						LOG.error("Error parsing items: " + itemsJson, e);
						throw new IllegalArgumentException("No items");
					}
					while ((entry = zis.getNextEntry()) != null) {
						String fileName = entry.getName();
						SettingsItem item = itemsFactory.getItemByFileName(fileName);
						if (item != null && collecting && item.shouldReadOnCollecting()
								|| item != null && !collecting && !item.shouldReadOnCollecting()) {
							try {
								item.getReader().readFromStream(ois);
							} catch (IllegalArgumentException e) {
								LOG.error("Error reading item data: " + item.getName(), e);
							} catch (IOException e) {
								LOG.error("Error reading item data: " + item.getName(), e);
							} finally {
								zis.closeEntry();
							}
						}
					}
				} else {
					throw new IllegalArgumentException("No items found");
				}
			} catch (IOException ex) {
				LOG.error("Failed to read next entry", ex);
			} finally {
				Algorithms.closeStream(ois);
				Algorithms.closeStream(zis);
			}
			return items;
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ImportAsyncTask extends AsyncTask<Void, Void, List<SettingsItem>> {

		private File file;
		private String latestChanges;
		private boolean askBeforeImport;
		private int version;

		private SettingsImportListener listener;
		private SettingsImporter importer;
		private List<SettingsItem> items;
		private List<SettingsItem> processedItems = new ArrayList<>();
		private SettingsItem currentItem;
		private AlertDialog dialog;

		ImportAsyncTask(@NonNull File settingsFile, String latestChanges, int version, boolean askBeforeImport,
						@Nullable SettingsImportListener listener) {
			this.file = settingsFile;
			this.listener = listener;
			this.latestChanges = latestChanges;
			this.version = version;
			this.askBeforeImport = askBeforeImport;
			importer = new SettingsImporter(app);
			collectOnly = true;
		}

		ImportAsyncTask(@NonNull File settingsFile, @NonNull List<SettingsItem> items, String latestChanges, int version, @Nullable SettingsImportListener listener) {
			this.file = settingsFile;
			this.listener = listener;
			this.items = items;
			this.latestChanges = latestChanges;
			this.version = version;
			importer = new SettingsImporter(app);
			collectOnly = false;
		}

		@Override
		protected void onPreExecute() {
			if (importing) {
				finishImport(listener, false, false, items);
			}
			importing = true;
			importSuspended = false;
			importTask = this;
		}

		@Override
		protected List<SettingsItem> doInBackground(Void... voids) {
			if (collectOnly) {
				try {
					return importer.collectItems(file);
				} catch (IllegalArgumentException e) {
					LOG.error("Failed to collect items from: " + file.getName(), e);
				} catch (IOException e) {
					LOG.error("Failed to collect items from: " + file.getName(), e);
				}
			} else {
				return this.items;
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<SettingsItem> items) {
			this.items = items;
			if (collectOnly) {
				listener.onSettingsImportFinished(true, false, items);
			} else {
				if (items != null && items.size() > 0) {
					processNextItem();
				}
			}
		}

		private void processNextItem() {
			if (activity == null) {
				return;
			}
			if (items.size() == 0 && !importSuspended) {
				if (processedItems.size() > 0) {
					new ImportItemsAsyncTask(file, listener, processedItems).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					finishImport(listener, false, true, items);
				}
				return;
			}
			final SettingsItem item;
			if (importSuspended && currentItem != null) {
				item = currentItem;
			} else if (items.size() > 0) {
				item = items.remove(0);
				currentItem = item;
			} else {
				item = null;
			}
			importSuspended = false;
			if (item != null) {
				if (item.exists()) {
					switch (item.getType()) {
						case PROFILE: {
							String title = activity.getString(R.string.overwrite_profile_q, item.getPublicName(app));
							dialog = showConfirmDialog(item, title, latestChanges);
							break;
						}
						case FILE:
							// overwrite now
							acceptItem(item);
							break;
						default:
							acceptItem(item);
							break;
					}
				} else {
					if (item.getType() == SettingsItemType.PROFILE && askBeforeImport) {
						String title = activity.getString(R.string.add_new_profile_q, item.getPublicName(app));
						dialog = showConfirmDialog(item, title, latestChanges);
					} else {
						acceptItem(item);
					}
				}
			} else {
				processNextItem();
			}
		}

		private AlertDialog showConfirmDialog(final SettingsItem item, String title, String message) {
			AlertDialog.Builder b = new AlertDialog.Builder(activity);
			b.setTitle(title);
			b.setMessage(message);
			b.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					acceptItem(item);
				}
			});
			b.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					processNextItem();
				}
			});
			b.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					ImportAsyncTask.this.dialog = null;
				}
			});
			b.setCancelable(false);
			return b.show();
		}

		private void suspendImport() {
			if (dialog != null) {
				dialog.dismiss();
				dialog = null;
			}
		}

		private void acceptItem(SettingsItem item) {
			item.apply();
			processedItems.add(item);
			processNextItem();
		}

		public List<SettingsItem> getItems() {
			return this.items;
		}

		public File getFile() {
			return this.file;
		}
	}

	@Nullable
	public List<SettingsItem> getSettingsItems() {
		return this.importTask.getItems();
	}

	@Nullable
	public File getSettingsFile() {
		return this.importTask.getFile();
	}

	@SuppressLint("StaticFieldLeak")
	private class ImportItemsAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private SettingsImporter importer;
		private File file;
		private SettingsImportListener listener;
		private List<SettingsItem> items;

		ImportItemsAsyncTask(@NonNull File file,
							 @Nullable SettingsImportListener listener,
							 @NonNull List<SettingsItem> items) {
			importer = new SettingsImporter(app);
			this.file = file;
			this.listener = listener;
			this.items = items;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				importer.importItems(file, items);
				return true;
			} catch (IllegalArgumentException e) {
				LOG.error("Failed to import items from: " + file.getName(), e);
			} catch (IOException e) {
				LOG.error("Failed to import items from: " + file.getName(), e);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			finishImport(listener, success, false, items);
		}
	}

	private void finishImport(@Nullable SettingsImportListener listener, boolean success, boolean empty, List<SettingsItem> items) {
		importing = false;
		importSuspended = false;
		importTask = null;
		if (listener != null) {
			listener.onSettingsImportFinished(success, empty, items);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ExportAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private SettingsExporter exporter;
		private File file;
		private SettingsExportListener listener;

		ExportAsyncTask(@NonNull File settingsFile,
						@Nullable SettingsExportListener listener,
						@NonNull List<SettingsItem> items) {
			this.file = settingsFile;
			this.listener = listener;
			this.exporter = new SettingsExporter();
			for (SettingsItem item : items) {
				exporter.addSettingsItem(item);
			}
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				exporter.exportSettings(file);
				return true;
			} catch (JSONException e) {
				LOG.error("Failed to export items to: " + file.getName(), e);
			} catch (IOException e) {
				LOG.error("Failed to export items to: " + file.getName(), e);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (listener != null) {
				listener.onSettingsExportFinished(file, success);
			}
		}
	}

	public void importSettings(@NonNull File settingsFile, String latestChanges, int version,
							   boolean askBeforeImport, @Nullable SettingsImportListener listener) {
		new ImportAsyncTask(settingsFile, latestChanges, version, askBeforeImport, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void importSettings(@NonNull File settingsFile, @NonNull List<SettingsItem> items, String latestChanges, int version, @Nullable SettingsImportListener listener) {
		new ImportAsyncTask(settingsFile, items, latestChanges, version, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName,
							   @Nullable SettingsExportListener listener,
							   @NonNull List<SettingsItem> items) {
		new ExportAsyncTask(new File(fileDir, fileName + OSMAND_SETTINGS_FILE_EXT), listener, items)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName, @Nullable SettingsExportListener listener,
							   @NonNull SettingsItem... items) {
		exportSettings(fileDir, fileName, listener, new ArrayList<>(Arrays.asList(items)));
	}
}