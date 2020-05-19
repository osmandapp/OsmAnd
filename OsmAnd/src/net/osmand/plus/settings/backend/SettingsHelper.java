package net.osmand.plus.settings.backend;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.map.WorldRegion;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.settings.backend.ApplicationMode.ApplicationModeBean;
import net.osmand.plus.settings.backend.ApplicationMode.ApplicationModeBuilder;
import net.osmand.plus.CustomOsmandPlugin;
import net.osmand.plus.CustomOsmandPlugin.SuggestedDownloadItem;
import net.osmand.plus.CustomRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings.OsmandPreference;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
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

	public static final int VERSION = 1;

	public static final String SETTINGS_LATEST_CHANGES_KEY = "settings_latest_changes";
	public static final String SETTINGS_VERSION_KEY = "settings_version";

	private static final Log LOG = PlatformUtil.getLog(SettingsHelper.class);
	private static final int BUFFER = 1024;

	private OsmandApplication app;

	private ImportAsyncTask importTask;
	private Map<File, ExportAsyncTask> exportAsyncTasks = new HashMap<>();

	public interface SettingsImportListener {
		void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items);
	}

	public interface SettingsCollectListener {
		void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items);
	}

	public interface CheckDuplicatesListener {
		void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items);
	}

	public interface SettingsExportListener {
		void onSettingsExportFinished(@NonNull File file, boolean succeed);
	}

	public SettingsHelper(OsmandApplication app) {
		this.app = app;
	}

	public enum SettingsItemType {
		GLOBAL,
		PROFILE,
		PLUGIN,
		DATA,
		FILE,
		RESOURCES,
		QUICK_ACTIONS,
		POI_UI_FILTERS,
		MAP_SOURCES,
		AVOID_ROADS,
		SUGGESTED_DOWNLOADS,
		DOWNLOADS
	}

	public abstract static class SettingsItem {

		protected OsmandApplication app;

		private String pluginId;
		private String fileName;

		boolean shouldReplace = false;

		protected List<String> warnings;

		SettingsItem(OsmandApplication app) {
			this.app = app;
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

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public boolean applyFileName(@NonNull String fileName) {
			String n = getFileName();
			return n != null && n.endsWith(fileName);
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
			writeItemsToJson(json);
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
		abstract SettingsItemReader getReader();

		@Nullable
		abstract SettingsItemWriter getWriter();

		@NonNull
		SettingsItemReader getJsonReader() {
			return new SettingsItemReader<SettingsItem>(this) {
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
		SettingsItemWriter getJsonWriter() {
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
							LOG.error("Failed to write json to stream", e);
						}
						return true;
					}
					return false;
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

	public static class PluginSettingsItem extends SettingsItem {

		private CustomOsmandPlugin plugin;
		private List<SettingsItem> pluginDependentItems;

		PluginSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			pluginDependentItems = new ArrayList<>();
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.PLUGIN;
		}

		@NonNull
		@Override
		public String getName() {
			return plugin.getId();
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return plugin.getName();
		}

		@NonNull
		@Override
		public String getDefaultFileName() {
			return getName();
		}

		public CustomOsmandPlugin getPlugin() {
			return plugin;
		}

		public List<SettingsItem> getPluginDependentItems() {
			return pluginDependentItems;
		}

		@Override
		public boolean exists() {
			return OsmandPlugin.getPlugin(getPluginId()) != null;
		}

		@Override
		public void apply() {
			if (shouldReplace || !exists()) {
				for (SettingsHelper.SettingsItem item : pluginDependentItems) {
					if (item instanceof SettingsHelper.FileSettingsItem) {
						FileSettingsItem fileItem = (FileSettingsItem) item;
						if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.RENDERING_STYLE) {
							plugin.addRenderer(fileItem.getName());
						} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.ROUTING_CONFIG) {
							plugin.addRouter(fileItem.getName());
						} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.OTHER) {
							plugin.setResourceDirName(item.getFileName());
						}
					} else if (item instanceof SuggestedDownloadsItem) {
						plugin.updateSuggestedDownloads(((SuggestedDownloadsItem) item).getItems());
					} else if (item instanceof DownloadsItem) {
						plugin.updateDownloadItems(((DownloadsItem) item).getItems());
					}
				}
				OsmandPlugin.addCustomPlugin(app, plugin);
			}
		}

		@Override
		void readFromJson(@NonNull JSONObject json) throws JSONException {
			super.readFromJson(json);
			plugin = new CustomOsmandPlugin(app, json);
		}

		@Override
		void writeToJson(@NonNull JSONObject json) throws JSONException {
			super.writeToJson(json);
			plugin.writeAdditionalDataToJson(json);
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return null;
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	public static class SuggestedDownloadsItem extends SettingsItem {

		private List<SuggestedDownloadItem> items;

		SuggestedDownloadsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			items = new ArrayList<>();
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.SUGGESTED_DOWNLOADS;

		}

		@NonNull
		@Override
		public String getName() {
			return "suggested_downloads";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return "suggested_downloads";
		}

		public List<SuggestedDownloadItem> getItems() {
			return items;
		}

		@Override
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			try {
				if (!json.has("items")) {
					return;
				}
				JSONArray jsonArray = json.getJSONArray("items");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject object = jsonArray.getJSONObject(i);
					String scopeId = object.optString("scope-id");
					String searchType = object.optString("search-type");
					int limit = object.optInt("limit", -1);

					List<String> names = new ArrayList<>();
					if (object.has("names")) {
						JSONArray namesArray = object.getJSONArray("names");
						for (int j = 0; j < namesArray.length(); j++) {
							names.add(namesArray.getString(j));
						}
					}
					SuggestedDownloadItem suggestedDownload = new SuggestedDownloadItem(scopeId, searchType, names, limit);
					items.add(suggestedDownload);
				}
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
				throw new IllegalArgumentException("Json parse error", e);
			}
		}

		@Override
		void writeItemsToJson(@NonNull JSONObject json) {
			JSONArray jsonArray = new JSONArray();
			if (!items.isEmpty()) {
				try {
					for (SuggestedDownloadItem downloadItem : items) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("scope-id", downloadItem.getScopeId());
						if (downloadItem.getLimit() != -1) {
							jsonObject.put("limit", downloadItem.getLimit());
						}
						if (!Algorithms.isEmpty(downloadItem.getSearchType())) {
							jsonObject.put("search-type", downloadItem.getSearchType());
						}
						if (!Algorithms.isEmpty(downloadItem.getNames())) {
							JSONArray namesArray = new JSONArray();
							for (String downloadName : downloadItem.getNames()) {
								namesArray.put(downloadName);
							}
							jsonObject.put("names", namesArray);
						}
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					LOG.error("Failed write to json", e);
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return null;
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	public static class DownloadsItem extends SettingsItem {

		private List<WorldRegion> items;

		DownloadsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			items = new ArrayList<>();
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.DOWNLOADS;

		}

		@NonNull
		@Override
		public String getName() {
			return "downloads";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return "downloads";
		}

		public List<WorldRegion> getItems() {
			return items;
		}

		@Override
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			try {
				if (!json.has("items")) {
					return;
				}
				JSONArray jsonArray = json.getJSONArray("items");
				items.addAll(CustomOsmandPlugin.collectRegionsFromJson(app, jsonArray));
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
				throw new IllegalArgumentException("Json parse error", e);
			}
		}

		@Override
		void writeItemsToJson(@NonNull JSONObject json) {
			JSONArray jsonArray = new JSONArray();
			if (!items.isEmpty()) {
				try {
					for (WorldRegion region : items) {
						if (region instanceof CustomRegion) {
							JSONObject regionJson = ((CustomRegion) region).toJson();
							jsonArray.put(regionJson);
						}
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					LOG.error("Failed write to json", e);
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return null;
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	public abstract static class CollectionSettingsItem<T> extends SettingsItem {

		protected List<T> items;
		protected List<T> appliedItems;
		protected List<T> duplicateItems;
		protected List<T> existingItems;

		@Override
		protected void init() {
			super.init();
			items = new ArrayList<>();
			appliedItems = new ArrayList<>();
			duplicateItems = new ArrayList<>();
		}

		CollectionSettingsItem(OsmandApplication app, @NonNull List<T> items) {
			super(app);
			this.items = items;
		}

		CollectionSettingsItem(OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@NonNull
		public List<T> getItems() {
			return items;
		}

		@NonNull
		public List<T> getAppliedItems() {
			return appliedItems;
		}

		@NonNull
		public List<T> getDuplicateItems() {
			return duplicateItems;
		}

		@NonNull
		public List<T> processDuplicateItems() {
			if (!items.isEmpty()) {
				for (T item : items) {
					if (isDuplicate(item)) {
						duplicateItems.add(item);
					}
				}
			}
			return duplicateItems;
		}

		public List<T> getNewItems() {
			List<T> res = new ArrayList<>(items);
			res.removeAll(duplicateItems);
			return res;
		}

		public abstract boolean isDuplicate(@NonNull T item);

		@NonNull
		public abstract T renameItem(@NonNull T item);
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

		protected OsmandSettingsItem(@NonNull OsmandSettings settings) {
			super(settings.getContext());
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
			readPreferencesFromJson(json);
		}

		void readPreferencesFromJson(final JSONObject json) {
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
			super(settings);
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
		SettingsItemReader getReader() {
			return new OsmandSettingsItemReader(this, getSettings()) {
				@Override
				protected void readPreferenceFromJson(@NonNull OsmandPreference<?> preference, @NonNull JSONObject json) throws JSONException {
					preference.readFromJson(json, null);
				}
			};
		}

		@Nullable
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

		private JSONObject additionalPrefsJson;
		private Set<String> appModeBeanPrefsIds;

		public ProfileSettingsItem(@NonNull OsmandApplication app, @NonNull ApplicationMode appMode) {
			super(app.getSettings());
			this.appMode = appMode;
		}

		public ProfileSettingsItem(@NonNull OsmandApplication app, @NonNull ApplicationModeBean modeBean) {
			super(app.getSettings());
			this.modeBean = modeBean;
			builder = ApplicationMode.fromModeBean(app, modeBean);
			appMode = builder.getApplicationMode();
		}

		public ProfileSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(SettingsItemType.PROFILE, app.getSettings(), json);
		}

		@Override
		protected void init() {
			super.init();
			appModeBeanPrefsIds = new HashSet<>(Arrays.asList(app.getSettings().appModeBeanPrefsIds));
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.PROFILE;
		}

		public ApplicationMode getAppMode() {
			return appMode;
		}

		public ApplicationModeBean getModeBean() {
			return modeBean;
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
		public String getDefaultFileName() {
			return "profile_" + getName() + getDefaultFileExtension();
		}

		@Override
		void readFromJson(@NonNull JSONObject json) throws JSONException {
			super.readFromJson(json);
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
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			additionalPrefsJson = json.optJSONObject("prefs");
		}

		@Override
		public boolean exists() {
			return builder != null && ApplicationMode.valueOfStringKey(getName(), null) != null;
		}

		private void renameProfile() {
			List<ApplicationMode> values = ApplicationMode.allPossibleValues();
			if (Algorithms.isEmpty(modeBean.userProfileName)) {
				ApplicationMode appMode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
				if (appMode != null) {
					modeBean.userProfileName = app.getString(appMode.getNameKeyResource());
				}
			}
			int number = 0;
			while (true) {
				number++;
				String key = modeBean.stringKey + "_" + number;
				String name = modeBean.userProfileName + '_' + number;
				if (ApplicationMode.valueOfStringKey(key, null) == null && isNameUnique(values, name)) {
					modeBean.userProfileName = name;
					modeBean.stringKey = key;
					break;
				}
			}
		}

		private boolean isNameUnique(List<ApplicationMode> values, String name) {
			for (ApplicationMode mode : values) {
				if (mode.getUserProfileName().equals(name)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void apply() {
			if (!appMode.isCustomProfile() && !shouldReplace) {
				ApplicationMode parent = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
				renameProfile();
				ApplicationMode.ApplicationModeBuilder builder = ApplicationMode
						.createCustomMode(parent, modeBean.stringKey, app)
						.setIconResName(modeBean.iconName)
						.setUserProfileName(modeBean.userProfileName)
						.setRoutingProfile(modeBean.routingProfile)
						.setRouteService(modeBean.routeService)
						.setIconColor(modeBean.iconColor)
						.setLocationIcon(modeBean.locIcon)
						.setNavigationIcon(modeBean.navIcon);
				app.getSettings().copyPreferencesFromProfile(parent, builder.getApplicationMode());
				appMode = ApplicationMode.saveProfile(builder, app);
			} else if (!shouldReplace && exists()) {
				renameProfile();
				builder = ApplicationMode.fromModeBean(app, modeBean);
				appMode = ApplicationMode.saveProfile(builder, app);
			} else {
				builder = ApplicationMode.fromModeBean(app, modeBean);
				appMode = ApplicationMode.saveProfile(builder, app);
			}
			ApplicationMode.changeProfileAvailability(appMode, true, app);
		}

		public void applyAdditionalPrefs() {
			if (additionalPrefsJson != null) {
				updatePluginResPrefs();

				SettingsItemReader reader = getReader();
				if (reader instanceof OsmandSettingsItemReader) {
					((OsmandSettingsItemReader) reader).readPreferencesFromJson(additionalPrefsJson);
				}
			}
		}

		private void updatePluginResPrefs() {
			String pluginId = getPluginId();
			if (Algorithms.isEmpty(pluginId)) {
				return;
			}
			OsmandPlugin plugin = OsmandPlugin.getPlugin(pluginId);
			if (plugin instanceof CustomOsmandPlugin) {
				CustomOsmandPlugin customPlugin = (CustomOsmandPlugin) plugin;
				String resDirPath = IndexConstants.PLUGINS_DIR + pluginId + "/" + customPlugin.getResourceDirName();

				for (Iterator<String> it = additionalPrefsJson.keys(); it.hasNext(); ) {
					try {
						String prefId = it.next();
						Object value = additionalPrefsJson.get(prefId);
						if (value instanceof JSONObject) {
							JSONObject jsonObject = (JSONObject) value;
							for (Iterator<String> iterator = jsonObject.keys(); iterator.hasNext(); ) {
								String key = iterator.next();
								Object val = jsonObject.get(key);
								if (val instanceof String) {
									val = checkPluginResPath((String) val, resDirPath);
								}
								jsonObject.put(key, val);
							}
						} else if (value instanceof String) {
							value = checkPluginResPath((String) value, resDirPath);
							additionalPrefsJson.put(prefId, value);
						}
					} catch (JSONException e) {
						LOG.error(e);
					}
				}
			}
		}

		private String checkPluginResPath(String path, String resDirPath) {
			if (path.startsWith("@")) {
				return resDirPath + "/" + path.substring(1);
			}
			return path;
		}

		@Override
		void writeToJson(@NonNull JSONObject json) throws JSONException {
			super.writeToJson(json);
			json.put("appMode", new JSONObject(appMode.toJson()));
		}

		@Nullable
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

		@Nullable
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

		public StreamSettingsItem(@NonNull OsmandApplication app, @NonNull String name) {
			super(app);
			this.name = name;
			setFileName(name);
		}

		StreamSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		public StreamSettingsItem(@NonNull OsmandApplication app, @NonNull InputStream inputStream, @NonNull String name) {
			super(app);
			this.inputStream = inputStream;
			this.name = name;
			setFileName(name);
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
		public SettingsItemWriter getWriter() {
			return new StreamSettingsItemWriter(this);
		}
	}

	public static class DataSettingsItem extends StreamSettingsItem {

		@Nullable
		private byte[] data;

		public DataSettingsItem(@NonNull OsmandApplication app, @NonNull String name) {
			super(app, name);
		}

		DataSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
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
		void readFromJson(@NonNull JSONObject json) throws JSONException {
			super.readFromJson(json);
			String fileName = getFileName();
			if (!Algorithms.isEmpty(fileName)) {
				name = Algorithms.getFileNameWithoutExtension(new File(fileName));
			}
		}

		@Nullable
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

		@Nullable
		@Override
		public SettingsItemWriter getWriter() {
			setInputStream(new ByteArrayInputStream(data));
			return super.getWriter();
		}
	}

	public static class FileSettingsItem extends StreamSettingsItem {

		public enum FileSubtype {
			UNKNOWN("", null),
			OTHER("other", ""),
			ROUTING_CONFIG("routing_config", IndexConstants.ROUTING_PROFILES_DIR),
			RENDERING_STYLE("rendering_style", IndexConstants.RENDERERS_DIR),
			OBF_MAP("obf_map", IndexConstants.MAPS_PATH),
			TILES_MAP("tiles_map", IndexConstants.TILES_INDEX_DIR),
			GPX("gpx", IndexConstants.GPX_INDEX_DIR),
			VOICE("voice", IndexConstants.VOICE_INDEX_DIR),
			TRAVEL("travel", IndexConstants.WIKIVOYAGE_INDEX_DIR);

			private String subtypeName;
			private String subtypeFolder;

			FileSubtype(String subtypeName, String subtypeFolder) {
				this.subtypeName = subtypeName;
				this.subtypeFolder = subtypeFolder;
			}

			public String getSubtypeName() {
				return subtypeName;
			}

			public String getSubtypeFolder() {
				return subtypeFolder;
			}

			public static FileSubtype getSubtypeByName(@NonNull String name) {
				for (FileSubtype subtype : FileSubtype.values()) {
					if (name.equals(subtype.subtypeName)) {
						return subtype;
					}
				}
				return null;
			}

			public static FileSubtype getSubtypeByFileName(@NonNull String fileName) {
				String name = fileName;
				if (fileName.startsWith(File.separator)) {
					name = fileName.substring(1);
				}
				for (FileSubtype subtype : FileSubtype.values()) {
					switch (subtype) {
						case UNKNOWN:
						case OTHER:
							break;
						case OBF_MAP:
							if (name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
								return subtype;
							}
							break;
						default:
							if (name.startsWith(subtype.subtypeFolder)) {
								return subtype;
							}
							break;
					}
				}
				return UNKNOWN;
			}

			@Override
			public String toString() {
				return subtypeName;
			}
		}

		protected File file;
		private File appPath;
		protected FileSubtype subtype;

		public FileSettingsItem(@NonNull OsmandApplication app, @NonNull File file) throws IllegalArgumentException {
			super(app, file.getPath().replace(app.getAppPath(null).getPath(), ""));
			this.file = file;
			this.appPath = app.getAppPath(null);
			String fileName = getFileName();
			if (fileName != null) {
				this.subtype = FileSubtype.getSubtypeByFileName(fileName);
			}
			if (subtype == FileSubtype.UNKNOWN || subtype == null) {
				throw new IllegalArgumentException("Unknown file subtype: " + fileName);
			}
		}

		FileSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
			this.appPath = app.getAppPath(null);
			if (subtype == FileSubtype.OTHER) {
				this.file = new File(appPath, name);
			} else if (subtype == FileSubtype.UNKNOWN || subtype == null) {
				throw new IllegalArgumentException("Unknown file subtype: " + getFileName());
			} else {
				this.file = new File(app.getAppPath(subtype.subtypeFolder), name);
			}
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.FILE;
		}

		public File getPluginPath() {
			String pluginId = getPluginId();
			if (!Algorithms.isEmpty(pluginId)) {
				return new File(appPath, IndexConstants.PLUGINS_DIR + pluginId);
			}
			return appPath;
		}

		@Override
		void readFromJson(@NonNull JSONObject json) throws JSONException {
			super.readFromJson(json);
			String fileName = getFileName();
			if (subtype == null) {
				String subtypeStr = json.has("subtype") ? json.getString("subtype") : null;
				if (!Algorithms.isEmpty(subtypeStr)) {
					subtype = FileSubtype.getSubtypeByName(subtypeStr);
				} else if (!Algorithms.isEmpty(fileName)) {
					subtype = FileSubtype.getSubtypeByFileName(fileName);
				} else {
					subtype = FileSubtype.UNKNOWN;
				}
			}
			if (!Algorithms.isEmpty(fileName)) {
				if (subtype == FileSubtype.OTHER) {
					name = fileName;
				} else if (subtype != null && subtype != FileSubtype.UNKNOWN) {
					name = Algorithms.getFileWithoutDirs(fileName);
				}
			}
		}

		@Override
		void writeToJson(@NonNull JSONObject json) throws JSONException {
			super.writeToJson(json);
			if (subtype != null) {
				json.put("subtype", subtype.getSubtypeName());
			}
		}

		@NonNull
		public File getFile() {
			return file;
		}

		@NonNull
		public FileSubtype getSubtype() {
			return subtype;
		}

		@Override
		public boolean exists() {
			return file.exists();
		}

		private File renameFile(File file) {
			int number = 0;
			String path = file.getAbsolutePath();
			while (true) {
				number++;
				String copyName = path.replaceAll(file.getName(), file.getName().replaceFirst("[.]", "_" + number + "."));
				File copyFile = new File(copyName);
				if (!copyFile.exists()) {
					return copyFile;
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return new StreamSettingsItemReader(this) {
				@Override
				public void readFromStream(@NonNull InputStream inputStream) throws IOException, IllegalArgumentException {
					OutputStream output;
					File dest = FileSettingsItem.this.file;
					if (dest.exists() && !shouldReplace) {
						dest = renameFile(dest);
					}
					if (dest.getParentFile() != null && !dest.getParentFile().exists()) {
						dest.getParentFile().mkdirs();
					}
					output = new FileOutputStream(dest);
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

		@Nullable
		@Override
		public SettingsItemWriter getWriter() {
			try {
				setInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, file.getName()));
				LOG.error("Failed to set input stream from file: " + file.getName(), e);
			}
			return super.getWriter();
		}
	}

	public static class ResourcesSettingsItem extends FileSettingsItem {

		ResourcesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
			shouldReplace = true;
			String fileName = getFileName();
			if (!Algorithms.isEmpty(fileName) && !fileName.endsWith(File.separator)) {
				setFileName(fileName + File.separator);
			}
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.RESOURCES;
		}

		@Override
		void readFromJson(@NonNull JSONObject json) throws JSONException {
			subtype = FileSubtype.OTHER;
			super.readFromJson(json);
		}

		@Override
		void writeToJson(@NonNull JSONObject json) throws JSONException {
			super.writeToJson(json);
			String fileName = getFileName();
			if (!Algorithms.isEmpty(fileName)) {
				if (fileName.endsWith(File.separator)) {
					fileName = fileName.substring(0, fileName.length() - 1);
				}
				json.put("file", fileName);
			}
		}

		@Override
		public boolean applyFileName(@NonNull String fileName) {
			if (fileName.endsWith(File.separator)) {
				return false;
			}
			String itemFileName = getFileName();
			if (itemFileName != null && itemFileName.endsWith(File.separator)) {
				if (fileName.startsWith(itemFileName)) {
					this.file = new File(getPluginPath(), fileName);
					return true;
				} else {
					return false;
				}
			} else {
				return super.applyFileName(fileName);
			}
		}

		@Nullable
		@Override
		public SettingsItemWriter getWriter() {
			return null;
		}
	}

	public static class QuickActionsSettingsItem extends CollectionSettingsItem<QuickAction> {

		private QuickActionRegistry actionRegistry;

		public QuickActionsSettingsItem(@NonNull OsmandApplication app, @NonNull List<QuickAction> items) {
			super(app, items);
		}

		QuickActionsSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			actionRegistry = app.getQuickActionRegistry();
			existingItems = actionRegistry.getQuickActions();
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.QUICK_ACTIONS;
		}

		@Override
		public boolean isDuplicate(@NonNull QuickAction item) {
			return !actionRegistry.isNameUnique(item, app);
		}

		@NonNull
		@Override
		public QuickAction renameItem(@NonNull QuickAction item) {
			return actionRegistry.generateUniqueName(item, app);
		}

		@Override
		public void apply() {
			List<QuickAction> newItems = getNewItems();
			if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
				appliedItems = new ArrayList<>(newItems);
				List<QuickAction> newActions = new ArrayList<>(existingItems);
				if (!duplicateItems.isEmpty()) {
					if (shouldReplace) {
						for (QuickAction duplicateItem : duplicateItems) {
							for (QuickAction savedAction : existingItems) {
								if (duplicateItem.getName(app).equals(savedAction.getName(app))) {
									newActions.remove(savedAction);
								}
							}
						}
					} else {
						for (QuickAction duplicateItem : duplicateItems) {
							renameItem(duplicateItem);
						}
					}
					appliedItems.addAll(duplicateItems);
				}
				newActions.addAll(appliedItems);
				actionRegistry.updateQuickActions(newActions);
			}
		}

		@Override
		public boolean shouldReadOnCollecting() {
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

		@Override
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			try {
				if (!json.has("items")) {
					return;
				}
				Gson gson = new Gson();
				Type type = new TypeToken<HashMap<String, String>>() {
				}.getType();
				QuickActionRegistry quickActionRegistry = app.getQuickActionRegistry();
				JSONArray itemsJson = json.getJSONArray("items");
				for (int i = 0; i < itemsJson.length(); i++) {
					JSONObject object = itemsJson.getJSONObject(i);
					String name = object.getString("name");
					QuickAction quickAction = null;
					if (object.has("actionType")) {
						quickAction = quickActionRegistry.newActionByStringType(object.getString("actionType"));
					} else if (object.has("type")) {
						quickAction = quickActionRegistry.newActionByType(object.getInt("type"));
					}
					if (quickAction != null) {
						String paramsString = object.getString("params");
						HashMap<String, String> params = gson.fromJson(paramsString, type);

						if (!name.isEmpty()) {
							quickAction.setName(name);
						}
						quickAction.setParams(params);
						items.add(quickAction);
					} else {
						warnings.add(app.getString(R.string.settings_item_read_error, name));
					}
				}
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
				throw new IllegalArgumentException("Json parse error", e);
			}
		}

		@Override
		void writeItemsToJson(@NonNull JSONObject json) {
			JSONArray jsonArray = new JSONArray();
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<String, String>>() {
			}.getType();
			if (!items.isEmpty()) {
				try {
					for (QuickAction action : items) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("name", action.hasCustomName(app)
								? action.getName(app) : "");
						jsonObject.put("actionType", action.getActionType().getStringId());
						jsonObject.put("params", gson.toJson(action.getParams(), type));
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					LOG.error("Failed write to json", e);
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return getJsonReader();
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	public static class PoiUiFilterSettingsItem extends CollectionSettingsItem<PoiUIFilter> {

		public PoiUiFilterSettingsItem(@NonNull OsmandApplication app, @NonNull List<PoiUIFilter> items) {
			super(app, items);
		}

		PoiUiFilterSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			existingItems = app.getPoiFilters().getUserDefinedPoiFilters(false);
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.POI_UI_FILTERS;
		}

		@Override
		public void apply() {
			List<PoiUIFilter> newItems = getNewItems();
			if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
				appliedItems = new ArrayList<>(newItems);

				for (PoiUIFilter duplicate : duplicateItems) {
					appliedItems.add(shouldReplace ? duplicate : renameItem(duplicate));
				}
				for (PoiUIFilter filter : appliedItems) {
					app.getPoiFilters().createPoiFilter(filter, false);
				}
				app.getSearchUICore().refreshCustomPoiFilters();
			}
		}

		@Override
		public boolean isDuplicate(@NonNull PoiUIFilter item) {
			String savedName = item.getName();
			for (PoiUIFilter filter : existingItems) {
				if (filter.getName().equals(savedName)) {
					return true;
				}
			}
			return false;
		}

		@NonNull
		@Override
		public PoiUIFilter renameItem(@NonNull PoiUIFilter item) {
			int number = 0;
			while (true) {
				number++;
				PoiUIFilter renamedItem = new PoiUIFilter(item,
						item.getName() + "_" + number,
						item.getFilterId() + "_" + number);
				if (!isDuplicate(renamedItem)) {
					return renamedItem;
				}
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
			return "poi_ui_filters";
		}

		@Override
		public boolean shouldReadOnCollecting() {
			return true;
		}

		@Override
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			try {
				if (!json.has("items")) {
					return;
				}
				JSONArray jsonArray = json.getJSONArray("items");
				Gson gson = new Gson();
				Type type = new TypeToken<HashMap<String, LinkedHashSet<String>>>() {
				}.getType();
				MapPoiTypes poiTypes = app.getPoiTypes();
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject object = jsonArray.getJSONObject(i);
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
					items.add(filter);
				}
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
				throw new IllegalArgumentException("Json parse error", e);
			}
		}

		@Override
		void writeItemsToJson(@NonNull JSONObject json) {
			JSONArray jsonArray = new JSONArray();
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<PoiCategory, LinkedHashSet<String>>>() {
			}.getType();
			if (!items.isEmpty()) {
				try {
					for (PoiUIFilter filter : items) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("name", filter.getName());
						jsonObject.put("filterId", filter.getFilterId());
						jsonObject.put("acceptedTypes", gson.toJson(filter.getAcceptedTypes(), type));
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					LOG.error("Failed write to json", e);
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return getJsonReader();
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	public static class MapSourcesSettingsItem extends CollectionSettingsItem<ITileSource> {

		private List<String> existingItemsNames;

		public MapSourcesSettingsItem(@NonNull OsmandApplication app, @NonNull List<ITileSource> items) {
			super(app, items);
		}

		MapSourcesSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			existingItemsNames = new ArrayList<>(app.getSettings().getTileSourceEntries().values());
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.MAP_SOURCES;
		}

		@Override
		public void apply() {
			List<ITileSource> newItems = getNewItems();
			if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
				appliedItems = new ArrayList<>(newItems);
				if (shouldReplace) {
					for (ITileSource tileSource : duplicateItems) {
						if (tileSource instanceof SQLiteTileSource) {
							File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + tileSource.getName() + IndexConstants.SQLITE_EXT);
							if (f != null && f.exists() && Algorithms.removeAllFiles(f)) {
								appliedItems.add(tileSource);
							}
						} else if (tileSource instanceof TileSourceManager.TileSourceTemplate) {
							File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + tileSource.getName());
							if (f != null && f.exists() && f.isDirectory() && Algorithms.removeAllFiles(f)) {
								appliedItems.add(tileSource);
							}
						}
					}
				} else {
					for (ITileSource tileSource : duplicateItems) {
						appliedItems.add(renameItem(tileSource));
					}
				}
				for (ITileSource tileSource : appliedItems) {
					if (tileSource instanceof TileSourceManager.TileSourceTemplate) {
						app.getSettings().installTileSource((TileSourceManager.TileSourceTemplate) tileSource);
					} else if (tileSource instanceof SQLiteTileSource) {
						((SQLiteTileSource) tileSource).createDataBase();
					}
				}
			}
		}

		@NonNull
		@Override
		public ITileSource renameItem(@NonNull ITileSource item) {
			int number = 0;
			while (true) {
				number++;
				if (item instanceof SQLiteTileSource) {
					SQLiteTileSource oldItem = (SQLiteTileSource) item;
					SQLiteTileSource renamedItem = new SQLiteTileSource(
							oldItem,
							oldItem.getName() + "_" + number,
							app);
					if (!isDuplicate(renamedItem)) {
						return renamedItem;
					}
				} else if (item instanceof TileSourceManager.TileSourceTemplate) {
					TileSourceManager.TileSourceTemplate oldItem = (TileSourceManager.TileSourceTemplate) item;
					oldItem.setName(oldItem.getName() + "_" + number);
					if (!isDuplicate(oldItem)) {
						return oldItem;
					}
				}
			}
		}

		@Override
		public boolean isDuplicate(@NonNull ITileSource item) {
			for (String name : existingItemsNames) {
				if (name.equals(item.getName())) {
					return true;
				}
			}
			return false;
		}

		@NonNull
		@Override
		public String getName() {
			return "map_sources";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return "map_sources";
		}

		@Override
		public boolean shouldReadOnCollecting() {
			return true;
		}

		@Override
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			try {
				if (!json.has("items")) {
					return;
				}
				JSONArray jsonArray = json.getJSONArray("items");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject object = jsonArray.getJSONObject(i);
					boolean sql = object.optBoolean("sql");
					String name = object.optString("name");
					int minZoom = object.optInt("minZoom");
					int maxZoom = object.optInt("maxZoom");
					String url = object.optString("url");
					String randoms = object.optString("randoms");
					boolean ellipsoid = object.optBoolean("ellipsoid", false);
					boolean invertedY = object.optBoolean("inverted_y", false);
					String referer = object.optString("referer");
					boolean timeSupported = object.optBoolean("timesupported", false);
					long expire = object.optLong("expire", -1);
					boolean inversiveZoom = object.optBoolean("inversiveZoom", false);
					String ext = object.optString("ext");
					int tileSize = object.optInt("tileSize");
					int bitDensity = object.optInt("bitDensity");
					int avgSize = object.optInt("avgSize");
					String rule = object.optString("rule");

					if (expire > 0 && expire < 3600000) {
						expire = expire * 60 * 1000L;
					}

					ITileSource template;
					if (!sql) {
						TileSourceTemplate tileSourceTemplate = new TileSourceTemplate(name, url, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
						tileSourceTemplate.setRule(rule);
						tileSourceTemplate.setRandoms(randoms);
						tileSourceTemplate.setReferer(referer);
						tileSourceTemplate.setEllipticYTile(ellipsoid);
						tileSourceTemplate.setInvertedYTile(invertedY);
						tileSourceTemplate.setExpirationTimeMillis(timeSupported ? expire : -1);

						template = tileSourceTemplate;
					} else {
						template = new SQLiteTileSource(app, name, minZoom, maxZoom, url, randoms, ellipsoid, invertedY, referer, timeSupported, expire, inversiveZoom, rule);
					}
					items.add(template);
				}
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
				throw new IllegalArgumentException("Json parse error", e);
			}
		}

		@Override
		void writeItemsToJson(@NonNull JSONObject json) {
			JSONArray jsonArray = new JSONArray();
			if (!items.isEmpty()) {
				try {
					for (ITileSource template : items) {
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
						jsonObject.put("expire", template.getExpirationTimeMinutes());
						jsonObject.put("inversiveZoom", template.getInversiveZoom());
						jsonObject.put("ext", template.getTileFormat());
						jsonObject.put("tileSize", template.getTileSize());
						jsonObject.put("bitDensity", template.getBitDensity());
						jsonObject.put("avgSize", template.getAvgSize());
						jsonObject.put("rule", template.getRule());
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);

				} catch (JSONException e) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					LOG.error("Failed write to json", e);
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return getJsonReader();
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	public static class AvoidRoadsSettingsItem extends CollectionSettingsItem<AvoidRoadInfo> {

		private OsmandSettings settings;
		private AvoidSpecificRoads specificRoads;

		public AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @NonNull List<AvoidRoadInfo> items) {
			super(app, items);
		}

		AvoidRoadsSettingsItem(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
			super(app, json);
		}

		@Override
		protected void init() {
			super.init();
			settings = app.getSettings();
			specificRoads = app.getAvoidSpecificRoads();
			existingItems = new ArrayList<>(specificRoads.getImpassableRoads().values());
		}

		@NonNull
		@Override
		public SettingsItemType getType() {
			return SettingsItemType.AVOID_ROADS;
		}

		@NonNull
		@Override
		public String getName() {
			return "avoid_roads";
		}

		@NonNull
		@Override
		public String getPublicName(@NonNull Context ctx) {
			return "avoid_roads";
		}

		@Override
		public void apply() {
			List<AvoidRoadInfo> newItems = getNewItems();
			if (!newItems.isEmpty() || !duplicateItems.isEmpty()) {
				appliedItems = new ArrayList<>(newItems);
				for (AvoidRoadInfo duplicate : duplicateItems) {
					if (shouldReplace) {
						LatLon latLon = new LatLon(duplicate.latitude, duplicate.longitude);
						if (settings.removeImpassableRoad(latLon)) {
							settings.addImpassableRoad(duplicate);
						}
					} else {
						settings.addImpassableRoad(renameItem(duplicate));
					}
				}
				for (AvoidRoadInfo avoidRoad : appliedItems) {
					settings.addImpassableRoad(avoidRoad);
				}
				specificRoads.loadImpassableRoads();
				specificRoads.initRouteObjects(true);
			}
		}

		@Override
		public boolean isDuplicate(@NonNull AvoidRoadInfo item) {
			return existingItems.contains(item);
		}

		@Override
		public boolean shouldReadOnCollecting() {
			return true;
		}

		@NonNull
		@Override
		public AvoidRoadInfo renameItem(@NonNull AvoidRoadInfo item) {
			int number = 0;
			while (true) {
				number++;
				AvoidRoadInfo renamedItem = new AvoidRoadInfo();
				renamedItem.name = item.name + "_" + number;
				if (!isDuplicate(renamedItem)) {
					renamedItem.id = item.id;
					renamedItem.latitude = item.latitude;
					renamedItem.longitude = item.longitude;
					renamedItem.appModeKey = item.appModeKey;
					return renamedItem;
				}
			}
		}

		@Override
		void readItemsFromJson(@NonNull JSONObject json) throws IllegalArgumentException {
			try {
				if (!json.has("items")) {
					return;
				}
				JSONArray jsonArray = json.getJSONArray("items");
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject object = jsonArray.getJSONObject(i);
					double latitude = object.optDouble("latitude");
					double longitude = object.optDouble("longitude");
					String name = object.optString("name");
					String appModeKey = object.optString("appModeKey");
					AvoidRoadInfo roadInfo = new AvoidRoadInfo();
					roadInfo.id = 0;
					roadInfo.latitude = latitude;
					roadInfo.longitude = longitude;
					roadInfo.name = name;
					if (ApplicationMode.valueOfStringKey(appModeKey, null) != null) {
						roadInfo.appModeKey = appModeKey;
					} else {
						roadInfo.appModeKey = app.getRoutingHelper().getAppMode().getStringKey();
					}
					items.add(roadInfo);
				}
			} catch (JSONException e) {
				warnings.add(app.getString(R.string.settings_item_read_error, String.valueOf(getType())));
				throw new IllegalArgumentException("Json parse error", e);
			}
		}

		@Override
		void writeItemsToJson(@NonNull JSONObject json) {
			JSONArray jsonArray = new JSONArray();
			if (!items.isEmpty()) {
				try {
					for (AvoidRoadInfo avoidRoad : items) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("latitude", avoidRoad.latitude);
						jsonObject.put("longitude", avoidRoad.longitude);
						jsonObject.put("name", avoidRoad.name);
						jsonObject.put("appModeKey", avoidRoad.appModeKey);
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					warnings.add(app.getString(R.string.settings_item_write_error, String.valueOf(getType())));
					LOG.error("Failed write to json", e);
				}
			}
		}

		@Nullable
		@Override
		SettingsItemReader getReader() {
			return getJsonReader();
		}

		@Nullable
		@Override
		SettingsItemWriter getWriter() {
			return null;
		}
	}

	private static class SettingsItemsFactory {

		private OsmandApplication app;
		private List<SettingsItem> items = new ArrayList<>();

		SettingsItemsFactory(OsmandApplication app, String jsonStr) throws IllegalArgumentException, JSONException {
			this.app = app;
			collectItems(new JSONObject(jsonStr));
		}

		private void collectItems(JSONObject json) throws IllegalArgumentException, JSONException {
			JSONArray itemsJson = json.getJSONArray("items");
			int version = json.has("version") ? json.getInt("version") : 1;
			if (version > VERSION) {
				throw new IllegalArgumentException("Unsupported osf version: " + version);
			}
			Map<String, List<SettingsItem>> pluginItems = new HashMap<>();
			for (int i = 0; i < itemsJson.length(); i++) {
				JSONObject itemJson = itemsJson.getJSONObject(i);
				SettingsItem item;
				try {
					item = createItem(itemJson);
					items.add(item);
					String pluginId = item.getPluginId();
					if (pluginId != null && item.getType() != SettingsItemType.PLUGIN) {
						List<SettingsItem> items = pluginItems.get(pluginId);
						if (items != null) {
							items.add(item);
						} else {
							items = new ArrayList<>();
							items.add(item);
							pluginItems.put(pluginId, items);
						}
					}
				} catch (IllegalArgumentException e) {
					LOG.error("Error creating item from json: " + itemJson, e);
				}
			}
			if (items.size() == 0) {
				throw new IllegalArgumentException("No items");
			}
			for (SettingsItem item : items) {
				if (item instanceof PluginSettingsItem) {
					PluginSettingsItem pluginSettingsItem = ((PluginSettingsItem) item);
					List<SettingsItem> pluginDependentItems = pluginItems.get(pluginSettingsItem.getName());
					if (!Algorithms.isEmpty(pluginDependentItems)) {
						pluginSettingsItem.getPluginDependentItems().addAll(pluginDependentItems);
					}
				}
			}
		}

		@NonNull
		public List<SettingsItem> getItems() {
			return items;
		}

		@Nullable
		public SettingsItem getItemByFileName(@NonNull String fileName) {
			for (SettingsItem item : items) {
				if (Algorithms.stringsEqual(item.getFileName(), fileName)) {
					return item;
				}
			}
			return null;
		}

		@NonNull
		private SettingsItem createItem(@NonNull JSONObject json) throws IllegalArgumentException, JSONException {
			SettingsItem item = null;
			SettingsItemType type = SettingsItem.parseItemType(json);
			OsmandSettings settings = app.getSettings();
			switch (type) {
				case GLOBAL:
					item = new GlobalSettingsItem(settings);
					break;
				case PROFILE:
					item = new ProfileSettingsItem(app, json);
					break;
				case PLUGIN:
					item = new PluginSettingsItem(app, json);
					break;
				case DATA:
					item = new DataSettingsItem(app, json);
					break;
				case FILE:
					item = new FileSettingsItem(app, json);
					break;
				case RESOURCES:
					item = new ResourcesSettingsItem(app, json);
					break;
				case QUICK_ACTIONS:
					item = new QuickActionsSettingsItem(app, json);
					break;
				case POI_UI_FILTERS:
					item = new PoiUiFilterSettingsItem(app, json);
					break;
				case MAP_SOURCES:
					item = new MapSourcesSettingsItem(app, json);
					break;
				case AVOID_ROADS:
					item = new AvoidRoadsSettingsItem(app, json);
					break;
				case SUGGESTED_DOWNLOADS:
					item = new SuggestedDownloadsItem(app, json);
					break;
				case DOWNLOADS:
					item = new DownloadsItem(app, json);
					break;
			}
			return item;
		}
	}

	private static class SettingsExporter {

		private Map<String, SettingsItem> items;
		private Map<String, String> additionalParams;
		private boolean exportItemsFiles;

		SettingsExporter(boolean exportItemsFiles) {
			this.exportItemsFiles = exportItemsFiles;
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
			JSONObject json = createItemsJson();
			OutputStream os = new BufferedOutputStream(new FileOutputStream(file), BUFFER);
			ZipOutputStream zos = new ZipOutputStream(os);
			try {
				ZipEntry entry = new ZipEntry("items.json");
				zos.putNextEntry(entry);
				zos.write(json.toString(2).getBytes("UTF-8"));
				zos.closeEntry();
				if (exportItemsFiles) {
					writeItemFiles(zos);
				}
				zos.flush();
				zos.finish();
			} finally {
				Algorithms.closeStream(zos);
				Algorithms.closeStream(os);
			}
		}

		private void writeItemFiles(ZipOutputStream zos) throws IOException {
			for (SettingsItem item : items.values()) {
				SettingsItemWriter writer = item.getWriter();
				if (writer != null) {
					String fileName = item.getFileName();
					if (Algorithms.isEmpty(fileName)) {
						fileName = item.getDefaultFileName();
					}
					ZipEntry entry = new ZipEntry(fileName);
					zos.putNextEntry(entry);
					writer.writeToStream(zos);
					zos.closeEntry();
				}
			}
		}

		private JSONObject createItemsJson() throws JSONException {
			JSONObject json = new JSONObject();
			json.put("version", VERSION);
			for (Map.Entry<String, String> param : additionalParams.entrySet()) {
				json.put(param.getKey(), param.getValue());
			}
			JSONArray itemsJson = new JSONArray();
			for (SettingsItem item : items.values()) {
				itemsJson.put(new JSONObject(item.toJson()));
			}
			json.put("items", itemsJson);
			return json;
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

		private List<SettingsItem> getItemsFromJson(@NonNull File file) throws IOException {
			List<SettingsItem> items = new ArrayList<>();
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			InputStream ois = new BufferedInputStream(zis);
			try {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String fileName = checkEntryName(entry.getName());
					if (fileName.equals("items.json")) {
						String itemsJson = null;
						try {
							itemsJson = Algorithms.readFromInputStream(ois).toString();
						} catch (IOException e) {
							LOG.error("Error reading items.json: " + itemsJson, e);
							throw new IllegalArgumentException("No items");
						} finally {
							zis.closeEntry();
						}
						try {
							SettingsItemsFactory itemsFactory = new SettingsItemsFactory(app, itemsJson);
							items.addAll(itemsFactory.getItems());
						} catch (IllegalArgumentException e) {
							LOG.error("Error parsing items: " + itemsJson, e);
							throw new IllegalArgumentException("No items");
						} catch (JSONException e) {
							LOG.error("Error parsing items: " + itemsJson, e);
							throw new IllegalArgumentException("No items");
						}
						break;
					}
				}
			} catch (IOException ex) {
				LOG.error("Failed to read next entry", ex);
			} finally {
				Algorithms.closeStream(ois);
				Algorithms.closeStream(zis);
			}
			return items;
		}

		private List<SettingsItem> processItems(@NonNull File file, @Nullable List<SettingsItem> items) throws IllegalArgumentException, IOException {
			boolean collecting = items == null;
			if (collecting) {
				items = getItemsFromJson(file);
			} else {
				if (items.size() == 0) {
					throw new IllegalArgumentException("No items");
				}
			}
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			InputStream ois = new BufferedInputStream(zis);
			try {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					String fileName = checkEntryName(entry.getName());
					SettingsItem item = null;
					for (SettingsItem settingsItem : items) {
						if (settingsItem != null && settingsItem.applyFileName(fileName)) {
							item = settingsItem;
							break;
						}
					}
					if (item != null && collecting && item.shouldReadOnCollecting()
							|| item != null && !collecting && !item.shouldReadOnCollecting()) {
						try {
							SettingsItemReader reader = item.getReader();
							if (reader != null) {
								reader.readFromStream(ois);
							}
						} catch (IllegalArgumentException e) {
							item.warnings.add(app.getString(R.string.settings_item_read_error, item.getName()));
							LOG.error("Error reading item data: " + item.getName(), e);
						} catch (IOException e) {
							item.warnings.add(app.getString(R.string.settings_item_read_error, item.getName()));
							LOG.error("Error reading item data: " + item.getName(), e);
						} finally {
							zis.closeEntry();
						}
					}
				}
			} catch (IOException ex) {
				LOG.error("Failed to read next entry", ex);
			} finally {
				Algorithms.closeStream(ois);
				Algorithms.closeStream(zis);
			}
			return items;
		}

		private String checkEntryName(String entryName) {
			String fileExt = OSMAND_SETTINGS_FILE_EXT + "/";
			int index = entryName.indexOf(fileExt);
			if (index != -1) {
				entryName = entryName.substring(index + fileExt.length());
			}
			return entryName;
		}
	}

	@SuppressLint("StaticFieldLeak")
	public class ImportAsyncTask extends AsyncTask<Void, Void, List<SettingsItem>> {

		private File file;
		private String latestChanges;
		private int version;

		private SettingsImportListener importListener;
		private SettingsCollectListener collectListener;
		private CheckDuplicatesListener duplicatesListener;
		private SettingsImporter importer;

		private List<SettingsItem> items = new ArrayList<>();
		private List<SettingsItem> selectedItems = new ArrayList<>();
		private List<Object> duplicates;

		private ImportType importType;
		private boolean importDone;

		ImportAsyncTask(@NonNull File file, String latestChanges, int version, @Nullable SettingsCollectListener collectListener) {
			this.file = file;
			this.collectListener = collectListener;
			this.latestChanges = latestChanges;
			this.version = version;
			importer = new SettingsImporter(app);
			importType = ImportType.COLLECT;
		}

		ImportAsyncTask(@NonNull File file, @NonNull List<SettingsItem> items, String latestChanges, int version, @Nullable SettingsImportListener importListener) {
			this.file = file;
			this.importListener = importListener;
			this.items = items;
			this.latestChanges = latestChanges;
			this.version = version;
			importer = new SettingsImporter(app);
			importType = ImportType.IMPORT;
		}

		ImportAsyncTask(@NonNull File file, @NonNull List<SettingsItem> items, @NonNull List<SettingsItem> selectedItems, @Nullable CheckDuplicatesListener duplicatesListener) {
			this.file = file;
			this.items = items;
			this.duplicatesListener = duplicatesListener;
			this.selectedItems = selectedItems;
			importer = new SettingsImporter(app);
			importType = ImportType.CHECK_DUPLICATES;
		}

		@Override
		protected void onPreExecute() {
			ImportAsyncTask importTask = SettingsHelper.this.importTask;
			if (importTask != null && !importTask.importDone) {
				finishImport(importListener, false, items);
			}
			SettingsHelper.this.importTask = this;
		}

		@Override
		protected List<SettingsItem> doInBackground(Void... voids) {
			switch (importType) {
				case COLLECT:
					try {
						return importer.collectItems(file);
					} catch (IllegalArgumentException e) {
						LOG.error("Failed to collect items from: " + file.getName(), e);
					} catch (IOException e) {
						LOG.error("Failed to collect items from: " + file.getName(), e);
					}
					break;
				case CHECK_DUPLICATES:
					this.duplicates = getDuplicatesData(selectedItems);
					return selectedItems;
				case IMPORT:
					return items;
			}
			return null;
		}

		@Override
		protected void onPostExecute(@Nullable List<SettingsItem> items) {
			if (items != null && importType != ImportType.CHECK_DUPLICATES) {
				this.items = items;
			} else {
				selectedItems = items;
			}
			switch (importType) {
				case COLLECT:
					importDone = true;
					collectListener.onSettingsCollectFinished(true, false, this.items);
					break;
				case CHECK_DUPLICATES:
					importDone = true;
					if (duplicatesListener != null) {
						duplicatesListener.onDuplicatesChecked(duplicates, selectedItems);
					}
					break;
				case IMPORT:
					if (items != null && items.size() > 0) {
						for (SettingsItem item : items) {
							item.apply();
						}
						new ImportItemsAsyncTask(file, importListener, items).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
					break;
			}
		}

		public List<SettingsItem> getItems() {
			return items;
		}

		public File getFile() {
			return file;
		}

		public void setImportListener(SettingsImportListener importListener) {
			this.importListener = importListener;
		}

		public void setDuplicatesListener(CheckDuplicatesListener duplicatesListener) {
			this.duplicatesListener = duplicatesListener;
		}

		ImportType getImportType() {
			return importType;
		}

		boolean isImportDone() {
			return importDone;
		}

		public List<Object> getDuplicates() {
			return duplicates;
		}

		public List<SettingsItem> getSelectedItems() {
			return selectedItems;
		}

		private List<Object> getDuplicatesData(List<SettingsItem> items) {
			List<Object> duplicateItems = new ArrayList<>();
			for (SettingsItem item : items) {
				if (item instanceof ProfileSettingsItem) {
					if (item.exists()) {
						duplicateItems.add(((ProfileSettingsItem) item).getModeBean());
					}
				} else if (item instanceof CollectionSettingsItem) {
					List duplicates = ((CollectionSettingsItem) item).processDuplicateItems();
					if (!duplicates.isEmpty()) {
						duplicateItems.addAll(duplicates);
					}
				} else if (item instanceof FileSettingsItem) {
					if (item.exists()) {
						duplicateItems.add(((FileSettingsItem) item).getFile());
					}
				}
			}
			return duplicateItems;
		}
	}

	@Nullable
	public ImportAsyncTask getImportTask() {
		return importTask;
	}

	@Nullable
	public ImportType getImportTaskType() {
		ImportAsyncTask importTask = this.importTask;
		return importTask != null ? importTask.getImportType() : null;
	}

	public boolean isImportDone() {
		ImportAsyncTask importTask = this.importTask;
		return importTask == null || importTask.isImportDone();
	}

	public boolean isFileExporting(File file) {
		return exportAsyncTasks.containsKey(file);
	}

	public void updateExportListener(File file, SettingsExportListener listener) {
		ExportAsyncTask exportAsyncTask = exportAsyncTasks.get(file);
		if (exportAsyncTask != null) {
			exportAsyncTask.listener = listener;
		}
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
			finishImport(listener, success, items);
		}
	}

	private void finishImport(@Nullable SettingsImportListener listener, boolean success, @NonNull List<SettingsItem> items) {
		importTask = null;
		List<String> warnings = new ArrayList<>();
		for (SettingsItem item : items) {
			warnings.addAll(item.getWarnings());
		}
		if (!warnings.isEmpty()) {
			app.showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
		}
		if (listener != null) {
			listener.onSettingsImportFinished(success, items);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ExportAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private SettingsExporter exporter;
		private File file;
		private SettingsExportListener listener;

		ExportAsyncTask(@NonNull File settingsFile,
						@Nullable SettingsExportListener listener,
						@NonNull List<SettingsItem> items, boolean exportItemsFiles) {
			this.file = settingsFile;
			this.listener = listener;
			this.exporter = new SettingsExporter(exportItemsFiles);
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
			exportAsyncTasks.remove(file);
			if (listener != null) {
				listener.onSettingsExportFinished(file, success);
			}
		}
	}

	public void collectSettings(@NonNull File settingsFile, String latestChanges, int version,
								@Nullable SettingsCollectListener listener) {
		new ImportAsyncTask(settingsFile, latestChanges, version, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void checkDuplicates(@NonNull File file, @NonNull List<SettingsItem> items, @NonNull List<SettingsItem> selectedItems, CheckDuplicatesListener listener) {
		new ImportAsyncTask(file, items, selectedItems, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void importSettings(@NonNull File settingsFile, @NonNull List<SettingsItem> items, String latestChanges, int version, @Nullable SettingsImportListener listener) {
		new ImportAsyncTask(settingsFile, items, latestChanges, version, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName, @Nullable SettingsExportListener listener, @NonNull List<SettingsItem> items, boolean exportItemsFiles) {
		File file = new File(fileDir, fileName + OSMAND_SETTINGS_FILE_EXT);
		ExportAsyncTask exportAsyncTask = new ExportAsyncTask(file, listener, items, exportItemsFiles);
		exportAsyncTasks.put(file, exportAsyncTask);
		exportAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName, @Nullable SettingsExportListener listener,
	                           boolean exportItemsFiles, @NonNull SettingsItem... items) {
		exportSettings(fileDir, fileName, listener, new ArrayList<>(Arrays.asList(items)), exportItemsFiles);
	}

	public enum ImportType {
		COLLECT,
		CHECK_DUPLICATES,
		IMPORT
	}
}