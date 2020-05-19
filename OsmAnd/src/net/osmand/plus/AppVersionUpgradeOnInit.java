package net.osmand.plus;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.EnumStringPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.util.Algorithms;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class AppVersionUpgradeOnInit {
	public static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	public static final String VERSION_INSTALLED_NUMBER = "VERSION_INSTALLED_NUMBER"; //$NON-NLS-1$
	public static final String NUMBER_OF_STARTS = "NUMBER_OF_STARTS"; //$NON-NLS-1$
	public static final String FIRST_INSTALLED = "FIRST_INSTALLED"; //$NON-NLS-1$

	// 22 - 2.2
	public static final int VERSION_2_2 = 22;
	// 23 - 2.3
	public static final int VERSION_2_3 = 23;
	// 32 - 3.2
	public static final int VERSION_3_2 = 32;
	// 35 - 3.5
	public static final int VERSION_3_5 = 35;
	// 36 - 3.6
	public static final int VERSION_3_6 = 36;
	// 37 - 3.7
	public static final int VERSION_3_7 = 37;
	// 3701 - 3.7-01 (4 digits version)
	// Each upgrade should have independent version!
	// So, we could have multiple upgrades per 1 release i.e. 3701, 3702, 3703, ... - will be for 3.7
	public static final int VERSION_3_7_01 = 3701;


	public static final int LAST_APP_VERSION = VERSION_3_7_01;

	static final String VERSION_INSTALLED = "VERSION_INSTALLED";

	private OsmandApplication app;
	private int prevAppVersion;
	private boolean appVersionChanged;
	private boolean firstTime;

	AppVersionUpgradeOnInit(OsmandApplication app) {
		this.app = app;
	}

	@SuppressLint("ApplySharedPref")
	void upgradeVersion(SharedPreferences startPrefs, int lastVersion) {
		if(!startPrefs.contains(NUMBER_OF_STARTS)) {
			startPrefs.edit().putInt(NUMBER_OF_STARTS, 1).commit();
		} else {
			startPrefs.edit().putInt(NUMBER_OF_STARTS, startPrefs.getInt(NUMBER_OF_STARTS, 0) + 1).commit();
		}
		if (!startPrefs.contains(FIRST_INSTALLED)) {
			startPrefs.edit().putLong(FIRST_INSTALLED, System.currentTimeMillis()).commit();
		}
		if (!startPrefs.contains(FIRST_TIME_APP_RUN)) {
			firstTime = true;
			startPrefs.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			startPrefs.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
			startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, lastVersion).commit();
		} else {
			prevAppVersion = startPrefs.getInt(VERSION_INSTALLED_NUMBER, 0);
			if (needsUpgrade(startPrefs, lastVersion)) {
				if (prevAppVersion < VERSION_2_2) {
					app.getSettings().SHOW_DASHBOARD_ON_START.set(true);
					app.getSettings().SHOW_DASHBOARD_ON_MAP_SCREEN.set(true);
					app.getSettings().SHOW_CARD_TO_CHOOSE_DRAWER.set(true);
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_2_2).commit();
				}
				if (prevAppVersion < VERSION_2_3) {
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_2_3).commit();
				}
				if (prevAppVersion < VERSION_3_2) {
					app.getSettings().BILLING_PURCHASE_TOKENS_SENT.set("");
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_3_2).commit();
				}
				if (prevAppVersion < VERSION_3_5 || Version.getAppVersion(app).equals("3.5.3")
						|| Version.getAppVersion(app).equals("3.5.4")) {
					app.getSettings().migratePreferences();
					app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
						@Override
						public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
							if (event.equals(AppInitializer.InitEvents.FAVORITES_INITIALIZED)) {
								app.getSettings().migrateHomeWorkParkingToFavorites();
							}
						}

						@Override
						public void onFinish(AppInitializer init) {
						}
					});
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_3_5).commit();
				}
				if (prevAppVersion < VERSION_3_6) {
					app.getSettings().migratePreferences();
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_3_6).commit();
				}
				if (prevAppVersion < VERSION_3_7) {
					app.getSettings().migrateEnumPreferences();
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_3_7).commit();
				}
				if (prevAppVersion < VERSION_3_7_01) {
					app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
						@Override
						public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
							if (event.equals(AppInitializer.InitEvents.FAVORITES_INITIALIZED)) {
								app.getFavorites().fixBlackBackground();
							}
						}
						@Override
						public void onFinish(AppInitializer init) {
						}
					});
					startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_3_7_01).commit();
				}
				startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, lastVersion).commit();
				startPrefs.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
				appVersionChanged = true;
			}
		}
	}

	private boolean needsUpgrade(SharedPreferences startPrefs, int maxVersion) {
		return !(Version.getFullVersion(app)).equals(startPrefs.getString(VERSION_INSTALLED, "")) || prevAppVersion < maxVersion;
	}

	boolean isAppVersionChanged() {
		return appVersionChanged;
	}

	int getPrevAppVersion() {
		return prevAppVersion;
	}

	public void resetFirstTimeRun(SharedPreferences startPrefs) {
		if(startPrefs != null) {
			startPrefs.edit().remove(FIRST_TIME_APP_RUN).commit();
		}
	}

	public int getNumberOfStarts(SharedPreferences startPrefs) {
		if(startPrefs == null) {
			return 0;
		}
		return startPrefs.getInt(NUMBER_OF_STARTS, 1);
	}

	public long getFirstInstalledDays(SharedPreferences startPrefs) {
		if(startPrefs == null) {
			return 0;
		}
		long nd = startPrefs.getLong(FIRST_INSTALLED, 0);

		return (System.currentTimeMillis() - nd) / (1000l * 24l * 60l * 60l);
	}

	public boolean isFirstTime() {
		return firstTime;
	}

	private OsmandPreference[] generalPrefs = new OsmandPreference[]{
			EXTERNAL_INPUT_DEVICE,
			CENTER_POSITION_ON_MAP,
			ROTATE_MAP,
			MAP_SCREEN_ORIENTATION,
			LIVE_MONITORING_URL,
			LIVE_MONITORING_MAX_INTERVAL_TO_SEND,
			LIVE_MONITORING_INTERVAL,
			LIVE_MONITORING,
			SHOW_TRIP_REC_NOTIFICATION,
			AUTO_SPLIT_RECORDING,
			SAVE_TRACK_MIN_SPEED,
			SAVE_TRACK_PRECISION,
			SAVE_TRACK_MIN_DISTANCE,
			SAVE_TRACK_INTERVAL,
			TRACK_STORAGE_DIRECTORY,
			SAVE_HEADING_TO_GPX,
			DISABLE_RECORDING_ONCE_APP_KILLED,
			SAVE_TRACK_TO_GPX,
			SAVE_GLOBAL_TRACK_REMEMBER,
			SAVE_GLOBAL_TRACK_INTERVAL,
			MAP_EMPTY_STATE_ALLOWED,
			DO_NOT_USE_ANIMATIONS,
			USE_KALMAN_FILTER_FOR_COMPASS,
			USE_MAGNETIC_FIELD_SENSOR_COMPASS,
			USE_TRACKBALL_FOR_MOVEMENTS,
			SPEED_SYSTEM,
			ANGULAR_UNITS,
			METRIC_SYSTEM,
			DRIVING_REGION,
			DRIVING_REGION_AUTOMATIC
	};

	public void migratePreferences() {
		migrateEnumPreferences();
		SharedPreferences globalSharedPreferences = (SharedPreferences) globalPreferences;
		Map<String, ?> globalPrefsMap = globalSharedPreferences.getAll();
		for (String key : globalPrefsMap.keySet()) {
			OsmandPreference pref = getPreference(key);
			if (pref instanceof CommonPreference) {
				CommonPreference commonPreference = (CommonPreference) pref;
				if (!commonPreference.global) {
					for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
						if (!commonPreference.isSetForMode(mode) && !commonPreference.hasDefaultValueForMode(mode)) {
							setPreference(key, globalPrefsMap.get(key), mode);
						}
					}
				}
			}
		}
		SharedPreferences defaultProfilePreferences = (SharedPreferences) getProfilePreferences(ApplicationMode.DEFAULT);
		Map<String, ?> defaultPrefsMap = defaultProfilePreferences.getAll();
		for (String key : defaultPrefsMap.keySet()) {
			OsmandPreference pref = getPreference(key);
			if (pref instanceof CommonPreference) {
				CommonPreference commonPreference = (CommonPreference) pref;
				if (commonPreference.global && !commonPreference.isSet()) {
					setPreference(key, defaultPrefsMap.get(key));
				}
			}
		}
		for (OsmandPreference pref : generalPrefs) {
			if (pref instanceof CommonPreference) {
				CommonPreference commonPref = (CommonPreference) pref;
				Object defaultVal = commonPref.getModeValue(ApplicationMode.DEFAULT);
				for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
					if (!commonPref.isSetForMode(mode) && !commonPref.hasDefaultValueForMode(mode)) {
						setPreference(commonPref.getId(), defaultVal, mode);
					}
				}
			}
		}

		String json = settingsAPI.getString(globalPreferences, "custom_app_profiles", "");
		if (!Algorithms.isEmpty(json)) {
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			Type t = new TypeToken<ArrayList<ApplicationMode.ApplicationModeBean>>() {
			}.getType();
			List<ApplicationMode.ApplicationModeBean> customProfiles = gson.fromJson(json, t);
			if (!Algorithms.isEmpty(customProfiles)) {
				for (ApplicationMode.ApplicationModeBean modeBean : customProfiles) {
					ApplicationMode.ApplicationModeBuilder builder = ApplicationMode.fromModeBean(ctx, modeBean);
					ApplicationMode.saveProfile(builder, ctx);
				}
			}
		}
	}

	public void migrateEnumPreferences() {
		for (OsmandPreference pref : registeredPreferences.values()) {
			if (pref instanceof EnumStringPreference) {
				EnumStringPreference enumPref = (EnumStringPreference) pref;
				if (enumPref.isGlobal()) {
					migrateEnumPref(enumPref, (SharedPreferences) globalPreferences);
				} else {
					for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
						migrateEnumPref(enumPref, (SharedPreferences) getProfilePreferences(mode));
					}
				}
			}
		}
	}

	private void migrateEnumPref(EnumStringPreference enumPref, SharedPreferences sharedPreferences) {
		Object value = sharedPreferences.getAll().get(enumPref.getId());
		if (value instanceof Integer) {
			int enumIndex = (int) value;
			if (enumIndex >= 0 && enumIndex < enumPref.values.length) {
				Enum savedValue = enumPref.values[enumIndex];
				enumPref.setValue(sharedPreferences, savedValue);
			}
		}
	}

	public void migrateHomeWorkParkingToFavorites() {
		FavouritesDbHelper favorites = ctx.getFavorites();

		LatLon homePoint = null;
		float lat = settingsAPI.getFloat(globalPreferences, "home_point_lat", 0);
		float lon = settingsAPI.getFloat(globalPreferences, "home_point_lon", 0);
		if (lat != 0 || lon != 0) {
			homePoint = new LatLon(lat, lon);
		}
		LatLon workPoint = null;
		lat = settingsAPI.getFloat(globalPreferences, "work_point_lat", 0);
		lon = settingsAPI.getFloat(globalPreferences, "work_point_lon", 0);
		if (lat != 0 || lon != 0) {
			workPoint = new LatLon(lat, lon);
		}
		if (homePoint != null) {
			favorites.setSpecialPoint(homePoint, FavouritePoint.SpecialPointType.HOME, null);
		}
		if (workPoint != null) {
			favorites.setSpecialPoint(workPoint, FavouritePoint.SpecialPointType.WORK, null);
		}
	}
}
