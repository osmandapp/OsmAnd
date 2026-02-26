package net.osmand.plus.feedback;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkResult;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AnalyticsHelper extends SQLiteOpenHelper {

	private static final Log LOG = PlatformUtil.getLog(AnalyticsHelper.class);

	private static final String ANALYTICS_UPLOAD_URL = "https://osmand.net/api/submit_analytics";
	private static final String ANALYTICS_FILE_NAME = "analytics.json";

	private static final int ROUTING_DATA_PARCEL_SIZE = 10; // 10 events
	private static final int DATA_PARCEL_SIZE = 500; // 500 events
	private static final int SUBMIT_DATA_INTERVAL = 60 * 60 * 1000; // 1 hour

	private static final String PARAM_OS = "os";
	private static final String PARAM_START_DATE = "startDate";
	private static final String PARAM_FINISH_DATE = "finishDate";
	private static final String PARAM_FIRST_INSTALL_DAYS = "nd";
	private static final String PARAM_NUMBER_OF_STARTS = "ns";
	private static final String PARAM_USER_ID = "aid";
	private static final String PARAM_VERSION = "version";
	private static final String PARAM_LANG = "lang";
	private static final String PARAM_EVENTS = "events";

	private static final String JSON_DATE = "date";
	private static final String JSON_EVENT = "event";

	private static final String DATABASE_NAME = "analytics";
	private static final int DATABASE_VERSION = 1;

	private static final String TABLE_NAME = "app_events";
	private static final String COL_DATE = "date";
	private static final String COL_TYPE = "event_type";
	private static final String COL_EVENT = "event";

	public static final int EVENT_TYPE_APP_USAGE = 1;
	public static final int EVENT_TYPE_MAP_DOWNLOAD = 2;
	public static final int EVENT_TYPE_ROUTING = 3;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({EVENT_TYPE_APP_USAGE, EVENT_TYPE_MAP_DOWNLOAD, EVENT_TYPE_ROUTING})
	public @interface EventType {
	}

	private final OsmandApplication app;
	private final String insertEventScript;
	private long lastSubmittedTime;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<?> submittingTask;

	private static class AnalyticsItem {
		long date;
		int type;
		String event;

		@NonNull
		@Override
		public String toString() {
			return "AnalyticsItem{" +
					"date=" + date +
					", type=" + type +
					", event=" + event +
					'}';
		}
	}

	private static class AnalyticsData {
		long startDate;
		long finishDate;
		List<AnalyticsItem> items;
	}

	public AnalyticsHelper(@NonNull OsmandApplication app) {
		super(app, DATABASE_NAME, null, DATABASE_VERSION);
		this.app = app;
		insertEventScript = "INSERT INTO " + TABLE_NAME + " VALUES (?, ?, ?)";
		submitCollectedDataAsync();
		clearDB(Collections.singletonList(EVENT_TYPE_ROUTING), System.currentTimeMillis());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTable(db);
	}

	private void createTable(@NonNull SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COL_DATE + " long, " + COL_TYPE + " int, " + COL_EVENT + " text )");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	private long getCollectedRowsCount() {
		long res = -1;
		try {
			SQLiteDatabase db = getWritableDatabase();
			if (db != null && db.isOpen()) {
				try {
					res = DatabaseUtils.queryNumEntries(db, TABLE_NAME);
				} catch (Exception e) {
					LOG.error(e);
				} finally {
					db.close();
				}
			}
		} catch (Exception e) {
			LOG.error(e);
		}
		return res;
	}

	private void clearDB(@NonNull List<Integer> allowedTypes, long finishDate) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null && db.isOpen()) {
			try {
				String types = formatAllowedTypes(allowedTypes);
				db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_DATE + " <= ?" + " AND " + COL_TYPE + " IN " + types, new Object[] {finishDate});
			} catch (Exception e) {
				LOG.error(e);
			} finally {
				db.close();
			}
		}
	}

	private String formatAllowedTypes(@NonNull List<Integer> allowedTypes) {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		for (int i = 0; i < allowedTypes.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(allowedTypes.get(i));
		}
		builder.append(")");
		return builder.toString();
	}

	public boolean submitCollectedDataAsync() {
		if (app.getSettings().isInternetConnectionAvailable()) {
			long collectedRowsCount = getCollectedRowsCount();
			if (collectedRowsCount > DATA_PARCEL_SIZE) {
				List<Integer> allowedTypes = new ArrayList<>();
				if (app.getSettings().SEND_ANONYMOUS_MAP_DOWNLOADS_DATA.get()) {
					allowedTypes.add(EVENT_TYPE_MAP_DOWNLOAD);
				}
				if (app.getSettings().SEND_ANONYMOUS_APP_USAGE_DATA.get()) {
					allowedTypes.add(EVENT_TYPE_APP_USAGE);
				}
				if ((submittingTask == null || submittingTask.isDone()) && allowedTypes.size() > 0) {
					submittingTask = executor.submit(() -> submitCollectedData(allowedTypes));
					return true;
				}
			}
		}
		return false;
	}

	private void submitCollectedData(@NonNull List<Integer> allowedTypes) {
		List<AnalyticsData> data = collectRecordedData(allowedTypes);
		for (AnalyticsData d : data) {
			if (d.items != null && d.items.size() > 0) {
				try {
					JSONArray jsonItemsArray = new JSONArray();
					for (AnalyticsItem item : d.items) {
						JSONObject jsonItem = new JSONObject();
						jsonItem.put(JSON_DATE, item.date);
						jsonItem.put(JSON_EVENT, item.event);
						jsonItemsArray.put(jsonItem);
					}

					Map<String, String> additionalData = new LinkedHashMap<>();
					additionalData.put(PARAM_OS, "android");
					additionalData.put(PARAM_START_DATE, String.valueOf(d.startDate));
					additionalData.put(PARAM_FINISH_DATE, String.valueOf(d.finishDate));
					additionalData.put(PARAM_VERSION, Version.getFullVersion(app));
					additionalData.put(PARAM_LANG, app.getLanguage() + "");
					additionalData.put(PARAM_FIRST_INSTALL_DAYS, String.valueOf(app.getAppInitializer().getFirstInstalledDays()));
					additionalData.put(PARAM_NUMBER_OF_STARTS, String.valueOf(app.getAppInitializer().getNumberOfStarts()));
					if (app.isUserAndroidIdAllowed()) {
						additionalData.put(PARAM_USER_ID, app.getUserAndroidId());
					}

					JSONObject json = new JSONObject();
					for (Map.Entry<String, String> entry : additionalData.entrySet()) {
						json.put(entry.getKey(), entry.getValue());
					}
					json.put(PARAM_EVENTS, jsonItemsArray);

					String jsonStr = json.toString();
					InputStream inputStream = new ByteArrayInputStream(jsonStr.getBytes());
					NetworkResult networkResult = AndroidNetworkUtils.uploadFile(ANALYTICS_UPLOAD_URL,
							inputStream, ANALYTICS_FILE_NAME, true, additionalData, null, null);
					if (networkResult.getError() != null) {
						return;
					}
				} catch (Exception e) {
					LOG.error(e);
					return;
				}
				clearDB(allowedTypes, d.finishDate);
			}
		}
	}

	@NonNull
	public List<String> getRoutingRecordedData() {
		int counter = 0;
		List<String> routingData = new ArrayList<>();
		for (AnalyticsData data : collectRecordedData(Collections.singletonList(EVENT_TYPE_ROUTING))) {
			for (AnalyticsItem item : data.items) {
				routingData.add(item.toString());

				counter++;
				if (counter >= ROUTING_DATA_PARCEL_SIZE) {
					return routingData;
				}
			}
		}
		return routingData;
	}

	@NonNull
	private List<AnalyticsData> collectRecordedData(@NonNull List<Integer> allowedTypes) {
		List<AnalyticsData> data = new ArrayList<>();
		SQLiteDatabase db = getReadableDatabase();
		if (db != null && db.isOpen()) {
			try {
				collectDBData(db, data, allowedTypes);
			} catch (Exception e) {
				LOG.error(e);
			} finally {
				db.close();
			}
		}
		return data;
	}

	private void collectDBData(@NonNull SQLiteDatabase db, @NonNull List<AnalyticsData> data, @NonNull List<Integer> allowedTypes) {
		String types = formatAllowedTypes(allowedTypes);
		Cursor query = db.rawQuery("SELECT " + COL_DATE + "," + COL_TYPE + "," + COL_EVENT
				+ " FROM " + TABLE_NAME + " WHERE " + COL_TYPE + " IN " + types
				+ " ORDER BY " + COL_DATE + " ASC", null);
		List<AnalyticsItem> items = new ArrayList<>();
		int itemsCounter = 0;
		long startDate = Long.MAX_VALUE;
		long finishDate = 0;
		if (query.moveToFirst()) {
			do {
				AnalyticsItem item = new AnalyticsItem();
				long date = query.getLong(0);
				item.date = date;
				item.type = query.getInt(1);
				item.event = query.getString(2);
				items.add(item);
				itemsCounter++;

				if (startDate > date) {
					startDate = date;
				}
				if (finishDate < date) {
					finishDate = date;
				}

				if (itemsCounter >= DATA_PARCEL_SIZE) {
					AnalyticsData d = new AnalyticsData();
					d.startDate = startDate;
					d.finishDate = finishDate;
					d.items = items;
					data.add(d);
					items = new ArrayList<>();
					itemsCounter = 0;
					startDate = Long.MAX_VALUE;
					finishDate = 0;
				}

			} while (query.moveToNext());

			if (itemsCounter > 0) {
				AnalyticsData d = new AnalyticsData();
				d.startDate = startDate;
				d.finishDate = finishDate;
				d.items = items;
				data.add(d);
			}
		}
		query.close();
	}

	public void addEvent(@NonNull String event, @EventType int type) {
		SQLiteDatabase db = getWritableDatabase();
		if (db != null && db.isOpen()) {
			try {
				db.execSQL(insertEventScript, new Object[] {System.currentTimeMillis(), type, event});
			} catch (Exception e) {
				LOG.error(e);
			} finally {
				db.close();
			}
		}
		long currentTimeMillis = System.currentTimeMillis();
		if (lastSubmittedTime + SUBMIT_DATA_INTERVAL < currentTimeMillis) {
			if (!submitCollectedDataAsync()) {
				lastSubmittedTime = currentTimeMillis - SUBMIT_DATA_INTERVAL / 4;
			} else {
				lastSubmittedTime = currentTimeMillis;
			}
		}
	}
}
