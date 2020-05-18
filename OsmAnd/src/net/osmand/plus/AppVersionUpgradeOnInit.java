package net.osmand.plus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

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
	private SharedPreferences startPrefs;
	private int prevAppVersion;
	private boolean appVersionChanged;
	private boolean firstTime;

	AppVersionUpgradeOnInit(OsmandApplication app) {
		this.app = app;
		startPrefs = app.getSharedPreferences(
				getLocalClassName(app.getAppCustomization().getMapActivity().getName()),
				Context.MODE_PRIVATE);
	}

	@SuppressLint("ApplySharedPref")
	void upgradeVersion(int lastVersion) {
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

	public void resetFirstTimeRun() {
		if(startPrefs != null) {
			startPrefs.edit().remove(FIRST_TIME_APP_RUN).commit();
		}
	}

	public int getNumberOfStarts() {
		if(startPrefs == null) {
			return 0;
		}
		return startPrefs.getInt(NUMBER_OF_STARTS, 1);
	}

	public long getFirstInstalledDays() {
		if(startPrefs == null) {
			return 0;
		}
		long nd = startPrefs.getLong(FIRST_INSTALLED, 0);

		return (System.currentTimeMillis() - nd) / (1000l * 24l * 60l * 60l);
	}

	public boolean isFirstTime() {
		return firstTime;
	}

	private String getLocalClassName(String cls) {
		final String pkg = app.getPackageName();
		int packageLen = pkg.length();
		if (!cls.startsWith(pkg) || cls.length() <= packageLen
				|| cls.charAt(packageLen) != '.') {
			return cls;
		}
		return cls.substring(packageLen + 1);
	}
}
