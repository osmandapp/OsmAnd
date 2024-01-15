package net.osmand.plus;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.core.android.NativeCore;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Version {

	private static final Log log = PlatformUtil.getLog(Version.class);

	public static final String TRIPLTEK_NAME = "TRIPLTEK";
	public static final String FULL_VERSION_NAME = "net.osmand.plus";
	private static final String FREE_VERSION_NAME = "net.osmand";
	private static final String FREE_DEV_VERSION_NAME = "net.osmand.dev";
	private static final String UTM_REF = "&referrer=utm_source%3Dosmand";

	private final String appName;
	private final String appVersion;

	public static boolean isHuawei() {
		return getBuildFlavor().contains("huawei");
	}

	public static boolean isAmazon() {
		return getBuildFlavor().contains("amazon");
	}

	private static String getBuildFlavor() {
		return net.osmand.plus.BuildConfig.FLAVOR;
	}

	public static boolean isGooglePlayEnabled() {
		return !isHuawei() && !isAmazon();
	}

	public static boolean isMarketEnabled() {
		return isGooglePlayEnabled() || isAmazon();
	}

	public static boolean isInAppPurchaseSupported() {
		return isGooglePlayEnabled() || isHuawei() || isAmazon();
	}

	public static boolean isGooglePlayInstalled(@NonNull OsmandApplication app) {
		try {
			app.getPackageManager().getPackageInfo("com.android.vending", 0);
		} catch (NameNotFoundException e) {
			return false;
		}
		return true;
	}

	public static String marketPrefix(@NonNull OsmandApplication app) {
		if (isAmazon()) {
			return "amzn://apps/android?p=";
		} else if (isGooglePlayEnabled() && isGooglePlayInstalled(app)) {
			return "market://details?id=";
		}
		return "https://osmand.net/apps?id=";
	}

	public static String getUrlWithUtmRef(@NonNull OsmandApplication app, String appName) {
		return marketPrefix(app) + appName + UTM_REF;
	}

	private Version(@NonNull OsmandApplication app) {
		String appVersion = "";
		try {
			PackageInfo packageInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
			appVersion = packageInfo.versionName;  //Version suffix  app.getString(R.string.app_version_suffix)  already appended in build.gradle
		} catch (NameNotFoundException e) {
			log.error(e);
		}
		this.appVersion = appVersion;
		appName = app.getString(R.string.app_name);
	}

	private static Version version;

	private static Version getVersion(@NonNull OsmandApplication app) {
		if (version == null) {
			version = new Version(app);
		}
		return version;
	}

	public static String getFullVersion(@NonNull OsmandApplication app) {
		Version version = getVersion(app);
		return version.appName + " " + version.appVersion;
	}

	public static String getAppVersion(@NonNull OsmandApplication app) {
		Version version = getVersion(app);
		return version.appVersion;
	}

	public static String getFullVersionWithReleaseDate(@NonNull OsmandApplication app) {
		String appEdition = getBuildAppEdition(app);
		if (!Algorithms.isEmpty(appEdition)) {
			String release = app.getString(R.string.shared_string_release).toLowerCase();
			return Version.getFullVersion(app) + ", " + release + ": " + appEdition;
		}
		return Version.getFullVersion(app);
	}

	public static String getBuildAppEdition(@NonNull OsmandApplication app) {
		return app.getString(R.string.app_edition);
	}

	public static String getAppName(@NonNull OsmandApplication app) {
		Version version = getVersion(app);
		return version.appName;
	}

	public static boolean isProductionVersion(@NonNull OsmandApplication app) {
		Version version = getVersion(app);
		return !version.appVersion.contains("#");
	}

	public static String getVersionAsURLParam(@NonNull OsmandApplication app) {
		try {
			return "osmandver=" + URLEncoder.encode(getVersionForTracker(app), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static boolean isFreeVersion(@NonNull OsmandApplication app) {
		return CollectionUtils.equalsToAny(app.getPackageName(), FREE_VERSION_NAME, FREE_DEV_VERSION_NAME) || isHuawei();
	}

	public static boolean isFullVersion(@NonNull OsmandApplication app) {
		return app.getPackageName().equals(FULL_VERSION_NAME);
	}

	public static boolean isPaidVersion(@NonNull OsmandApplication app) {
		return !isFreeVersion(app)
				|| InAppPurchaseUtils.isFullVersionAvailable(app)
				|| InAppPurchaseUtils.isLiveUpdatesAvailable(app)
				|| InAppPurchaseUtils.isMapsPlusAvailable(app)
				|| InAppPurchaseUtils.isOsmAndProAvailable(app)
				|| InAppPurchaseUtils.isTripltekPromoAvailable(app);
	}

	public static boolean isDeveloperVersion(@NonNull OsmandApplication app) {
		return getAppName(app).contains("~") || app.getPackageName().equals(FREE_DEV_VERSION_NAME);
	}

	public static boolean isDeveloperBuild(@NonNull OsmandApplication app) {
		return getAppName(app).contains("~");
	}

	public static boolean isTripltekBuild() {
		return CollectionUtils.equalsToAny(TRIPLTEK_NAME, Build.BRAND, Build.MANUFACTURER);
	}

	public static String getVersionForTracker(@NonNull OsmandApplication app) {
		String v = getAppName(app);
		if (isProductionVersion(app)) {
			v = getFullVersion(app);
		} else {
			v += " test";
		}
		return v;
	}

	public static boolean isOpenGlAvailable(@NonNull OsmandApplication app) {
		if (!NativeCore.isAvailable() || isQnxOperatingSystem() || !isOpenGlEsVersionSupported(app)) {
			return false;
		}
		File nativeLibraryDir = new File(app.getApplicationInfo().nativeLibraryDir);
		if (checkOpenGlExists(nativeLibraryDir)) return true;
		// check opengl doesn't work correctly on some devices when native libs are not unpacked
		return true;
	}

	public static boolean isOpenGlEsVersionSupported(@NonNull OsmandApplication app) {
		ActivityManager activityManager = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
		ConfigurationInfo deviceConfigurationInfo = activityManager.getDeviceConfigurationInfo();
		int majorVersion = (deviceConfigurationInfo.reqGlEsVersion & 0xffff0000) >> 16;
		return majorVersion >= 3;
	}

	public static boolean isQnxOperatingSystem() {
		return "qnx".equals(System.getProperty("os.name"));
	}

	public static boolean checkOpenGlExists(@NonNull File nativeLibraryDir) {
		if (nativeLibraryDir.exists() && nativeLibraryDir.canRead()) {
			File[] files = nativeLibraryDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						if (checkOpenGlExists(file)) {
							return true;
						}
					} else if ("libOsmAndCoreWithJNI.so".equals(file.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static long getInstallTime(@NonNull OsmandApplication app) {
		return app.getAppInitializer().getFirstInstalledTime();
	}

	public static long getUpdateTime(@NonNull OsmandApplication app) {
		return app.getAppInitializer().getUpdateVersionTime();
	}
}
