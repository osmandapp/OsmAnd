/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite persistence for Driver Break configuration and history.
 * Schema aligned with driver_break_db.c defaults and keys.
 */
public class DriverBreakDatabase extends SQLiteOpenHelper {

	private static final Log LOG = PlatformUtil.getLog(DriverBreakDatabase.class);

	public static final int DB_VERSION = 1;

	private static final String TABLE_CONFIG = "config";
	private static final String TABLE_REST_STOP_HISTORY = "rest_stop_history";
	private static final String TABLE_FUEL_STOP_HISTORY = "fuel_stop_history";
	private static final String TABLE_FUEL_SAMPLES = "fuel_samples";

	private static final String CREATE_CONFIG =
			"CREATE TABLE IF NOT EXISTS " + TABLE_CONFIG + " ("
					+ "key TEXT PRIMARY KEY,"
					+ "value TEXT NOT NULL"
					+ ");";

	private static final String CREATE_REST_STOP_HISTORY =
			"CREATE TABLE IF NOT EXISTS " + TABLE_REST_STOP_HISTORY + " ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "timestamp INTEGER NOT NULL,"
					+ "lat REAL NOT NULL,"
					+ "lon REAL NOT NULL,"
					+ "mode TEXT NOT NULL,"
					+ "duration_s INTEGER,"
					+ "poi_json TEXT"
					+ ");";

	private static final String CREATE_FUEL_STOP_HISTORY =
			"CREATE TABLE IF NOT EXISTS " + TABLE_FUEL_STOP_HISTORY + " ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "timestamp INTEGER NOT NULL,"
					+ "lat REAL NOT NULL,"
					+ "lon REAL NOT NULL,"
					+ "litres REAL"
					+ ");";

	private static final String CREATE_FUEL_SAMPLES =
			"CREATE TABLE IF NOT EXISTS " + TABLE_FUEL_SAMPLES + " ("
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "timestamp INTEGER NOT NULL,"
					+ "distance_m REAL NOT NULL,"
					+ "fuel_litres REAL NOT NULL,"
					+ "mode TEXT NOT NULL"
					+ ");";

	/** Default config from driver_break_config_default() in driver_break.c. */
	private static final Map<String, String> DEFAULT_CONFIG = new LinkedHashMap<>();

	static {
		DEFAULT_CONFIG.put("travel_mode", "car");
		DEFAULT_CONFIG.put("total_weight", "80.0");
		DEFAULT_CONFIG.put("energy_drag_cd", "0.30");
		DEFAULT_CONFIG.put("energy_frontal_area_sqm", "2.2");
		DEFAULT_CONFIG.put("use_energy_routing", "0");
		DEFAULT_CONFIG.put("use_ecu_route_cost", "0");
		DEFAULT_CONFIG.put("ecu_enabled", "0");
		DEFAULT_CONFIG.put("adaptive_fuel_enabled", "0");
		DEFAULT_CONFIG.put("car_soft_limit_h", "7");
		DEFAULT_CONFIG.put("car_max_limit_h", "10");
		DEFAULT_CONFIG.put("car_break_interval_h", "4");
		DEFAULT_CONFIG.put("car_break_duration_min", "30");
		DEFAULT_CONFIG.put("truck_mandatory_break_h", "4");
		DEFAULT_CONFIG.put("truck_break_duration_min", "45");
		DEFAULT_CONFIG.put("truck_max_daily_h", "9");
		DEFAULT_CONFIG.put("hiking_main_dist_km", "11.295");
		DEFAULT_CONFIG.put("hiking_alt_dist_km", "2.2752");
		DEFAULT_CONFIG.put("hiking_alt_enabled", "1");
		DEFAULT_CONFIG.put("hiking_max_daily_km", "40.0");
		DEFAULT_CONFIG.put("cycling_main_dist_km", "28.24");
		DEFAULT_CONFIG.put("cycling_alt_dist_km", "5.69");
		DEFAULT_CONFIG.put("cycling_alt_enabled", "1");
		DEFAULT_CONFIG.put("cycling_max_daily_km", "100.0");
		DEFAULT_CONFIG.put("moto_soft_limit_min", "120");
		DEFAULT_CONFIG.put("moto_mandatory_break_min", "210");
		DEFAULT_CONFIG.put("moto_break_duration_min", "20");
		DEFAULT_CONFIG.put("moto_max_daily_h", "8");
		DEFAULT_CONFIG.put("moto_terrain", "road");
		DEFAULT_CONFIG.put("water_pois_enabled", "0");
		DEFAULT_CONFIG.put("poi_radius_m", "15000");
		DEFAULT_CONFIG.put("water_radius_m", "2000");
		DEFAULT_CONFIG.put("cabin_radius_m", "5000");
		DEFAULT_CONFIG.put("network_cabin_radius_m", "25000");
		DEFAULT_CONFIG.put("min_dist_buildings_m", "150");
		DEFAULT_CONFIG.put("min_dist_glaciers_m", "300");
		DEFAULT_CONFIG.put("dnt_priority", "0");
		DEFAULT_CONFIG.put("hiking_pilgrimage", "0");
		DEFAULT_CONFIG.put("adaptive_learned_lkm", "0");
		DEFAULT_CONFIG.put("timer_last_break_elapsed_ms", "0");
		DEFAULT_CONFIG.put("timer_accumulated_distance_m", "0");
		DEFAULT_CONFIG.put("timer_break_in_progress", "0");
	}

	public DriverBreakDatabase(@NonNull Context context, @NonNull File dbFile) {
		super(context, dbFile.getAbsolutePath(), null, DB_VERSION);
		File parent = dbFile.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			LOG.warn("DriverBreak: could not create database directory " + parent);
		}
	}

	@Override
	public void onCreate(@NonNull SQLiteDatabase db) {
		db.execSQL(CREATE_CONFIG);
		db.execSQL(CREATE_REST_STOP_HISTORY);
		db.execSQL(CREATE_FUEL_STOP_HISTORY);
		db.execSQL(CREATE_FUEL_SAMPLES);
		seedDefaults(db);
	}

	@Override
	public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
		// Future migrations
	}

	/**
	 * @return config value or default; never null
	 */
	@NonNull
	public String getConfig(@NonNull String key) {
		SQLiteDatabase db = getReadableDatabase();
		try (Cursor cursor = db.query(TABLE_CONFIG, new String[] {"value"},
				"key = ?", new String[] {key}, null, null, null)) {
			if (cursor.moveToFirst()) {
				String value = cursor.getString(0);
				if (value != null) {
					return value;
				}
			}
		} catch (SQLiteException e) {
			LOG.error("DriverBreak: read config " + key, e);
		}
		String fallback = DEFAULT_CONFIG.get(key);
		return fallback != null ? fallback : "";
	}

	public void setConfig(@NonNull String key, @NonNull String value) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL("INSERT OR REPLACE INTO " + TABLE_CONFIG + " (key, value) VALUES (?, ?);",
					new Object[] {key, value});
		} catch (SQLiteException e) {
			LOG.error("DriverBreak: write config " + key, e);
		}
	}

	public void addFuelSample(long timestampSec, double distanceM, double fuelLitres, @NonNull String mode) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL("INSERT INTO " + TABLE_FUEL_SAMPLES
							+ " (timestamp, distance_m, fuel_litres, mode) VALUES (?, ?, ?, ?);",
					new Object[] {timestampSec, distanceM, fuelLitres, mode});
		} catch (SQLiteException e) {
			LOG.error("DriverBreak: add fuel sample", e);
		}
	}

	@NonNull
	public List<FuelSampleRow> getFuelSamples() {
		List<FuelSampleRow> rows = new ArrayList<>();
		SQLiteDatabase db = getReadableDatabase();
		try (Cursor cursor = db.query(TABLE_FUEL_SAMPLES,
				new String[] {"timestamp", "distance_m", "fuel_litres", "mode"},
				null, null, null, null, "timestamp ASC")) {
			while (cursor.moveToNext()) {
				rows.add(new FuelSampleRow(cursor.getLong(0), cursor.getDouble(1),
						cursor.getDouble(2), cursor.getString(3)));
			}
		} catch (SQLiteException e) {
			LOG.error("DriverBreak: read fuel samples", e);
		}
		return rows;
	}

	public void addRestStopHistory(long timestampSec, double lat, double lon, @NonNull String mode,
			@Nullable Integer durationSec, @Nullable String poiJson) {
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL("INSERT INTO " + TABLE_REST_STOP_HISTORY
							+ " (timestamp, lat, lon, mode, duration_s, poi_json) VALUES (?, ?, ?, ?, ?, ?);",
					new Object[] {timestampSec, lat, lon, mode, durationSec, poiJson});
		} catch (SQLiteException e) {
			LOG.error("DriverBreak: add rest stop history", e);
		}
	}

	private void seedDefaults(@NonNull SQLiteDatabase db) {
		for (Map.Entry<String, String> entry : DEFAULT_CONFIG.entrySet()) {
			db.execSQL("INSERT OR IGNORE INTO " + TABLE_CONFIG + " (key, value) VALUES (?, ?);",
					new Object[] {entry.getKey(), entry.getValue()});
		}
	}

	/**
	 * One row from fuel_samples table.
	 */
	public static final class FuelSampleRow {
		public final long timestampSec;
		public final double distanceM;
		public final double fuelLitres;
		public final String mode;

		public FuelSampleRow(long timestampSec, double distanceM, double fuelLitres, @Nullable String mode) {
			this.timestampSec = timestampSec;
			this.distanceM = distanceM;
			this.fuelLitres = fuelLitres;
			this.mode = mode != null ? mode : TravelMode.CAR.getConfigKey();
		}
	}
}
