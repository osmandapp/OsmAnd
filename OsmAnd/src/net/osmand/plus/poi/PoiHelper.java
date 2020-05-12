package net.osmand.plus.poi;


import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoiHelper {

	private static final Log LOG = PlatformUtil.getLog(PoiHelper.class);
	private OsmandApplication app;
	private PoiDbHelper helper;
	private Map<String, Long> files;
	private Map<String, List<String>> categories;

	public PoiHelper(OsmandApplication app) {
		this.app = app;
		helper = new PoiDbHelper(app);
	}

	public Map<String, Long> getFiles() {
		if (files == null) {
			files = helper.getFiles();
		}
		return files;
	}

	public Map<String, List<String>> getCategories() {
		if (categories == null) {
			categories = helper.getCategories();
		}
		return categories;
	}

	public void readPoiTypesFromMap() {
		LocalIndexHelper localIndexHelper = new LocalIndexHelper(app);
		List<LocalIndexInfo> localMapsIndexes = localIndexHelper.getLocalFullMaps(new AbstractLoadLocalIndexTask() {
			@Override
			public void loadFile(LocalIndexInfo... loaded) {
			}
		});
		Map<String, Long> savedFiles = getFiles();
		if (savedFiles.size() == localMapsIndexes.size()) {
			for (LocalIndexInfo info : localMapsIndexes) {
				File f = new File(info.getPathToData());
				String name = f.getName();
				long date = f.lastModified();
				if (!savedFiles.containsKey(name) || savedFiles.get(name) != date) {
					initCategoriesFromFiles();
					replaceSavedFiles(localMapsIndexes);
					return;
				}
			}
		} else {
			initCategoriesFromFiles();
			replaceSavedFiles(localMapsIndexes);
			return;
		}
		readCategoriesFromDb();
	}

	@SuppressLint("StaticFieldLeak")
	public void readPoiTypesFromMapAsync() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... voids) {
				app.getPoiTypes().init();
				readPoiTypesFromMap();
				return null;
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void readCategoriesFromDb() {
		for (Map.Entry<String, List<String>> entry : getCategories().entrySet()) {
			PoiCategory poiCategory = app.getPoiTypes().getPoiCategoryByName(entry.getKey(), true);
			for (String s : entry.getValue()) {
				PoiType poiType = new PoiType(MapPoiTypes.getDefault(), poiCategory, null, s);
				List<String> filters = new ArrayList<>();
				for (PoiType poi : poiCategory.getPoiTypes()) {
					filters.add(poi.getKeyName());
				}
				if (!filters.contains(s)) {
					poiCategory.getPoiTypes().add(poiType);
				}
			}
		}
	}

	private void replaceSavedFiles(List<LocalIndexInfo> localMapsIndexes) {
		helper.deleteFilesTable(helper.getWritableDatabase());
		for (LocalIndexInfo info : localMapsIndexes) {
			File f = new File(info.getPathToData());
			helper.addFile(f, helper.getWritableDatabase());
		}
	}

	private void initCategoriesFromFiles() {
		app.getPoiTypes().clearCreatedCategories();
		final BinaryMapIndexReader[] currentFile = app.getResourceManager().getPoiSearchFiles();
		for (BinaryMapIndexReader r : currentFile) {
			try {
				r.initCategories();
			} catch (IOException e) {
				LOG.error("Error while read poi types from map " + e);
			}
		}
		replaceSavedCategories(app.getPoiTypes().getCreatedCategories());
	}

	private void replaceSavedCategories(List<PoiCategory> poiCategories) {
		helper.deletePoiTypesTable(helper.getWritableDatabase());
		for (PoiCategory category : poiCategories) {
			helper.addCategory(category, helper.getWritableDatabase());
		}
	}

	public class PoiDbHelper {

		private static final String DATABASE_NAME = "poi_types_cache";
		private static final int DATABASE_VERSION = 1;

		private static final String FILES_TABLE_NAME = "files";
		private static final String FILE_NAME = "name";
		private static final String FILE_DATE = "date";

		private static final String FILES_TABLE_CREATE = "CREATE TABLE " +
				FILES_TABLE_NAME + " (" +
				FILE_NAME + ", " +
				FILE_DATE + ");";

		private static final String POI_TYPES_TABLE_NAME = "poi_types";
		private static final String POI_CATEGORY = "category";
		private static final String POI_SUBCATEGORIES = "subcategories";

		private static final String POI_TYPES_TABLE_CREATE = "CREATE TABLE " +
				POI_TYPES_TABLE_NAME + " (" +
				POI_CATEGORY + ", " +
				POI_SUBCATEGORIES + ");";

		private OsmandApplication context;
		private SQLiteAPI.SQLiteConnection conn;

		PoiDbHelper(OsmandApplication context) {
			this.context = context;
		}

		public SQLiteAPI.SQLiteConnection getWritableDatabase() {
			return openConnection(false);
		}

		public void close() {
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}

		public SQLiteAPI.SQLiteConnection getReadableDatabase() {
			return openConnection(true);
		}

		private SQLiteAPI.SQLiteConnection openConnection(boolean readonly) {
			conn = context.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, readonly);
			if (conn.getVersion() < DATABASE_VERSION) {
				if (readonly) {
					conn.close();
					conn = context.getSQLiteAPI().getOrCreateDatabase(DATABASE_NAME, false);
				}
				int version = conn.getVersion();
				conn.setVersion(DATABASE_VERSION);
				if (version == 0) {
					onCreate(conn);
				}
			}
			return conn;
		}

		public void onCreate(SQLiteAPI.SQLiteConnection conn) {
			conn.execSQL(FILES_TABLE_CREATE);
			conn.execSQL(POI_TYPES_TABLE_CREATE);
		}

		protected void addFile(File f, SQLiteAPI.SQLiteConnection db) {
			if (db != null) {
				db.execSQL("INSERT INTO " + FILES_TABLE_NAME + " VALUES (?, ?)",
						new Object[]{f.getName(), f.lastModified()});
			}
		}

		protected void deleteFilesTable(SQLiteAPI.SQLiteConnection db) {
			if (db != null) {
				db.execSQL("DELETE FROM " + FILES_TABLE_NAME);
			}
		}

		protected void deletePoiTypesTable(SQLiteAPI.SQLiteConnection db) {
			if (db != null) {
				db.execSQL("DELETE FROM " + POI_TYPES_TABLE_NAME);
			}
		}

		protected void addCategory(PoiCategory poiCategory, SQLiteAPI.SQLiteConnection db) {
			if (db != null) {
				db.execSQL("INSERT INTO " + POI_TYPES_TABLE_NAME + " VALUES (?, ?)",
						new Object[]{poiCategory.getKeyName(), getSubCategoriesJson(poiCategory.getPoiTypes())});
			}
		}

		protected Map<String, Long> getFiles() {
			Map<String, Long> files = new HashMap<>();
			SQLiteAPI.SQLiteConnection conn = getReadableDatabase();
			if (conn != null) {
				SQLiteAPI.SQLiteCursor query = conn.rawQuery("SELECT " +
						FILE_NAME + ", " +
						FILE_DATE +
						" FROM " +
						FILES_TABLE_NAME, null);
				if (query != null && query.moveToFirst()) {
					do {
						String fileName = query.getString(0);
						Long date = query.getLong(1);
						files.put(fileName, date);
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			}
			close();
			return files;
		}

		protected Map<String, List<String>> getCategories() {
			Map<String, List<String>> categories = new HashMap<>();
			SQLiteAPI.SQLiteConnection conn = getReadableDatabase();
			if (conn != null) {
				SQLiteAPI.SQLiteCursor query = conn.rawQuery("SELECT " +
						POI_CATEGORY + ", " +
						POI_SUBCATEGORIES +
						" FROM " +
						POI_TYPES_TABLE_NAME, null);
				if (query != null && query.moveToFirst()) {
					do {
						String categoryName = query.getString(0);
						List<String> subCategories = getSubCategories(query.getString(1));
						categories.put(categoryName, subCategories);
					} while (query.moveToNext());
				}
				if (query != null) {
					query.close();
				}
			}
			close();
			return categories;
		}

		private List<String> getSubCategories(@NonNull String json) {
			List<String> subCategories = new ArrayList<>();
			try {
				JSONArray jsonArray = new JSONArray(json);
				for (int i = 0; i < jsonArray.length(); i++) {
					subCategories.add(jsonArray.optString(i));
				}
			} catch (JSONException e) {
				LOG.error("Error parsing subCategories json: " + e);
			}
			return subCategories;
		}

		private String getSubCategoriesJson(@NonNull List<PoiType> poiTypes) {
			JSONArray jsonArray = new JSONArray();
			for (PoiType subCategory : poiTypes) {
				jsonArray.put(subCategory.getKeyName());
			}
			return jsonArray.toString();
		}
	}
}
