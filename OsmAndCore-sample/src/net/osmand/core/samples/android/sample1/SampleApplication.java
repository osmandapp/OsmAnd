package net.osmand.core.samples.android.sample1;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.LogSeverityLevel;
import net.osmand.core.jni.Logger;
import net.osmand.core.samples.android.sample1.SampleFormatter.MetricsConstants;
import net.osmand.core.samples.android.sample1.SampleFormatter.SpeedConstants;
import net.osmand.core.samples.android.sample1.resources.ResourceManager;
import net.osmand.core.samples.android.sample1.search.QuickSearchHelper;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.util.Algorithms;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Locale;

import static net.osmand.core.samples.android.sample1.data.PointDescription.FORMAT_DEGREES;

public class SampleApplication extends Application {
	public static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 5 ;
	public static final int PERMISSION_REQUEST_LOCATION_ON_RESUME = 6 ;
	public static final int PERMISSION_REQUEST_LOCATION_ON_BUTTON = 7 ;
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
	private GeocodingLookupService geocodingLookupService;
	private OsmandRegions regions;
	private ResourceManager resourceManager;

	public static String LANGUAGE;
	public static boolean TRANSLITERATE = false;

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
		OsmandResources.init(this);
		locationProvider = new SampleLocationProvider(this);
		searchUICore = new QuickSearchHelper(this);
		geocodingLookupService = new GeocodingLookupService(this);
		resourceManager = new ResourceManager(this);
		regions = new OsmandRegions();
		updateRegionVars();

		uiHandler = new Handler();

		poiTypes = MapPoiTypes.getDefaultNoInit();
		if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				== PackageManager.PERMISSION_GRANTED) {
			new Thread(new Runnable() { //$NON-NLS-1$
				@Override
				public void run() {
					try {
						indexRegionsBoundaries();
						initPoiTypes();
					} finally {
						//applicationBgInitializing = false;
					}
				}
			}, "Initializing app").start();
		}

		// Initialize native core
		if (NativeCore.isAvailable() && !NativeCore.isLoaded()) {
			assetsCustom = CoreResourcesFromAndroidAssets.loadFromCurrentApplication(this);
			NativeCore.load(assetsCustom);
		}
		Logger.get().setSeverityLevelThreshold(LogSeverityLevel.Debug);

		iconsCache = new IconsCache(assetsCustom, this);
	}

	private void updateRegionVars() {
		regions.setTranslator(new OsmandRegions.RegionTranslation() {

			@Override
			public String getTranslation(String id) {
				if(WorldRegion.AFRICA_REGION_ID.equals(id)){
					return getString("index_name_africa");
				} else if(WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID.equals(id)){
					return getString("index_name_oceania");
				} else if(WorldRegion.ASIA_REGION_ID.equals(id)){
					return getString("index_name_asia");
				} else if(WorldRegion.CENTRAL_AMERICA_REGION_ID.equals(id)){
					return getString("index_name_central_america");
				} else if(WorldRegion.EUROPE_REGION_ID.equals(id)){
					return getString("index_name_europe");
				} else if(WorldRegion.RUSSIA_REGION_ID.equals(id)){
					return getString("index_name_russia");
				} else if(WorldRegion.NORTH_AMERICA_REGION_ID.equals(id)){
					return getString("index_name_north_america");
				} else if(WorldRegion.SOUTH_AMERICA_REGION_ID.equals(id)){
					return getString("index_name_south_america");
				}
				return null;
			}
		});
		regions.setLocale(LANGUAGE);
	}

	private void indexRegionsBoundaries() {
		try {
			File file = getAppPath("regions.ocbf");
			if (file != null) {
				if (!file.exists()) {
					file = new File(getInternalAppPath(), "regions.ocbf");
					if (!file.exists()) {
						Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"),
								new FileOutputStream(file));
					}
				}
				regions.prepareFile(file.getAbsolutePath());

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public OsmandRegions getRegions() {
		return regions;
	}

	public GeocodingLookupService getGeocodingLookupService() {
		return geocodingLookupService;
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
						String val = getString(in);
						if(val != null) {
							int ind = val.indexOf(';');
							if (ind > 0) {
								return val.substring(0, ind);
							}
						}
						return val;
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


			@Override
			public String getEnTranslation(AbstractPoiType type) {
				if(type.getBaseLangType() != null) {
					return getEnTranslation(type.getBaseLangType()) +  " (" + getLangTranslation(type.getLang()).toLowerCase() +")";
				}
				return getEnTranslation(type.getIconKeyName());
			}


			@Override
			public String getEnTranslation(String keyName) {
				return Algorithms.capitalizeFirstLetter(
						keyName.replace('_', ' '));

			}
			@Override
			public String getSynonyms(AbstractPoiType type) {
				AbstractPoiType baseLangType = type.getBaseLangType();
				if (baseLangType != null) {
					return getSynonyms(baseLangType);
				}
				return getSynonyms(type.getIconKeyName());
			}

			@Override
			public String getSynonyms(String keyName) {
				try {
					Field f = R.string.class.getField("poi_" + keyName);
					if (f != null) {
						Integer in = (Integer) f.get(null);
						String val = getString(in);
						if(val != null) {
							int ind = val.indexOf(';');
							if (ind > 0) {
								return val.substring(ind + 1) ;
							}
						}
						return val;
					}
				} catch (Exception e) {
				}
				return "";
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
		return OsmandResources.getString(osmandId);
	}

	public String getString(String osmandId, Object... formatArgs) {
		return OsmandResources.getString(osmandId, formatArgs);
	}

	public File getInternalAppPath() {
		if (Build.VERSION.SDK_INT >= 21) {
			File fl = getNoBackupFilesDir();
			if (fl != null) {
				return fl;
			}
		}
		return getFilesDir();
	}

	public void showShortToastMessage(final int msgId, final Object... args) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(SampleApplication.this, getString(msgId, args), Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showShortToastMessage(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(SampleApplication.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showToastMessage(final int msgId, final Object... args) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(SampleApplication.this, getString(msgId, args), Toast.LENGTH_LONG).show();
			}
		});
	}

	public void showToastMessage(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(SampleApplication.this, msg, Toast.LENGTH_LONG).show();
			}
		});
	}
}