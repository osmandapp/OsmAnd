package net.osmand.core.samples.android.sample1;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;

import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.LogSeverityLevel;
import net.osmand.core.jni.Logger;
import net.osmand.core.samples.android.sample1.SampleFormatter.MetricsConstants;
import net.osmand.core.samples.android.sample1.SampleFormatter.SpeedConstants;
import net.osmand.core.samples.android.sample1.search.QuickSearchHelper;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Locale;

import static net.osmand.core.samples.android.sample1.data.PointDescription.FORMAT_DEGREES;

public class SampleApplication extends Application {
	public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 5 ;
	private CoreResourcesFromAndroidAssets assetsCustom;
	private MapPoiTypes poiTypes;
	private IconsCache iconsCache;
	private Handler uiHandler;
	private long lastTimeInternetConnectionChecked = 0;
	private boolean internetConnectionAvailable = true;

	private int coordinatesFormat = FORMAT_DEGREES;
	private MetricsConstants metricsConstants = MetricsConstants.KILOMETERS_AND_METERS;
	private SpeedConstants speedConstants = SpeedConstants.KILOMETERS_PER_HOUR;

	private SampleLocationProvider locationProvider;
	private QuickSearchHelper searchUICore;

	public static final String LANGUAGE;

	static {
		String langCode = Locale.getDefault().getLanguage();
		if (langCode.isEmpty()) {
			langCode = "en";
		}
		LANGUAGE = langCode;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		OAResources.init(this);
		locationProvider = new SampleLocationProvider(this);
		searchUICore = new QuickSearchHelper(this);
		uiHandler = new Handler();

		poiTypes = MapPoiTypes.getDefaultNoInit();
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED) {
			initPoiTypes();
		}

		// Initialize native core
		if (NativeCore.isAvailable() && !NativeCore.isLoaded()) {
			assetsCustom = CoreResourcesFromAndroidAssets.loadFromCurrentApplication(this);
			NativeCore.load(assetsCustom);
		}
		Logger.get().setSeverityLevelThreshold(LogSeverityLevel.Debug);

		iconsCache = new IconsCache(assetsCustom, this);
	}

	public int getCoordinatesFormat() {
		return coordinatesFormat;
	}

	public MetricsConstants getMetricsConstants() {
		return metricsConstants;
	}

	public SpeedConstants getSpeedConstants() {
		return speedConstants;
	}

	public SampleLocationProvider getLocationProvider() {
		return locationProvider;
	}

	public QuickSearchHelper getSearchUICore() {
		return searchUICore;
	}

	public MapPoiTypes getPoiTypes() {
		return poiTypes;
	}

	public IconsCache getIconsCache() {
		return iconsCache;
	}

	public void initPoiTypes() {
		File poiTypesFile = new File(Environment.getExternalStorageDirectory() + "/osmand/poi_types.xml");
		if (poiTypesFile.exists()) {
			poiTypes.init(poiTypesFile.getAbsolutePath());
		} else {
			poiTypes.init();
		}
		poiTypes.setPoiTranslator(new MapPoiTypes.PoiTranslator() {

			@Override
			public String getTranslation(AbstractPoiType type) {
				if(type.getBaseLangType() != null) {
					return getTranslation(type.getBaseLangType()) +  " (" + getLangTranslation(type.getLang()).toLowerCase() +")";
				}
				try {
					Field f = R.string.class.getField("poi_" + type.getIconKeyName());
					if (f != null) {
						Integer in = (Integer) f.get(null);
						return getString(in);
					}
				} catch (Exception e) {
					System.err.println("No translation for "+ type.getIconKeyName() + " " + e.getMessage());
				}
				return null;
			}

			@Override
			public String getTranslation(String keyName) {
				return null;
			}
		});

		searchUICore.initSearchUICore();
	}

	public String getAbsoluteAppPath() {
		return Environment.getExternalStorageDirectory() + "/osmand";
	}

	public File getAppPath(String path) {
		if (path == null) {
			path = "";
		}
		return new File(getAbsoluteAppPath(), path);
	}

	public String getLangTranslation(String l) {
		try {
			java.lang.reflect.Field f = R.string.class.getField("lang_"+l);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return l;
	}

	public void runInUIThread(Runnable run) {
		uiHandler.post(run);
	}

	public void runInUIThread(Runnable run, long delay) {
		uiHandler.postDelayed(run, delay);
	}

	// Check internet connection available every 15 seconds
	public boolean isInternetConnectionAvailable() {
		return isInternetConnectionAvailable(false);
	}

	public boolean isInternetConnectionAvailable(boolean update) {
		long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
		if (delta < 0 || delta > 15000 || update) {
			internetConnectionAvailable = isInternetConnected();
		}
		return internetConnectionAvailable;
	}

	public boolean isWifiConnected() {
		ConnectivityManager mgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = mgr.getActiveNetworkInfo();
		return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}

	private boolean isInternetConnected() {
		ConnectivityManager mgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo active = mgr.getActiveNetworkInfo();
		if (active == null) {
			return false;
		} else {
			NetworkInfo.State state = active.getState();
			return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
		}
	}

	public static SampleApplication getApp(Context ctx) {
		return (SampleApplication) ctx.getApplicationContext();
	}

	public String getString(String osmandId) {
		return OAResources.getString(osmandId);
	}

	public String getString(String osmandId, Object... formatArgs) {
		return OAResources.getString(osmandId, formatArgs);
	}
}