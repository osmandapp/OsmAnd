package net.osmand.plus;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

class AppVersionUpgradeOnInit {
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
	// 3701 - 3.7.0.1
	public static final int VERSION_3_7_0_1 = 3701; // from 3.7.0.1 - 4 digit version

	private static final String VERSION_INSTALLED_NUMBER = "VERSION_INSTALLED_NUMBER";
	static final String VERSION_INSTALLED = "VERSION_INSTALLED";

	private OsmandApplication app;
	private int prevAppVersion;
	private boolean appVersionChanged;

	AppVersionUpgradeOnInit(OsmandApplication app) {
		this.app = app;
	}

	@SuppressLint("ApplySharedPref")
	void upgradeVersion(SharedPreferences startPrefs, int versionToUpgrade) {
		prevAppVersion = startPrefs.getInt(VERSION_INSTALLED_NUMBER, 0);
		if (needsUpgrade(startPrefs, versionToUpgrade)) {
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
			if (prevAppVersion < VERSION_3_7_0_1) {
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
				startPrefs.edit().putInt(VERSION_INSTALLED_NUMBER, VERSION_3_7_0_1).commit();
			}

			startPrefs.edit().putString(VERSION_INSTALLED, Version.getFullVersion(app)).commit();
			appVersionChanged = true;
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
}
