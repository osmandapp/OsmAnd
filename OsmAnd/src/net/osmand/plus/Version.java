package net.osmand.plus;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import net.osmand.plus.inapp.InAppPurchaseHelper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Version {
	
	private final String appVersion; 
	private final String appName;
	private final static String FREE_VERSION_NAME = "net.osmand";
	private final static String FREE_DEV_VERSION_NAME = "net.osmand.dev";
	private final static String FREE_CUSTOM_VERSION_NAME = "net.osmand.freecustom";
	private final static String UTM_REF = "&referrer=utm_source%3Dosmand";
	
	public static boolean isGpsStatusEnabled(OsmandApplication ctx) {
		return isGooglePlayEnabled(ctx) && !isBlackberry(ctx);
	}
	
	public static boolean isBlackberry(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+blackberry");
	}
	
	public static boolean isHuawei(OsmandApplication ctx) {
		return ctx.getPackageName().endsWith(".huawei");
	}
	
	public static boolean isMarketEnabled(OsmandApplication ctx) {
		return isGooglePlayEnabled(ctx) || isAmazonEnabled(ctx);
	}

	public static boolean isGooglePlayInstalled(OsmandApplication ctx) {
		try {
			ctx.getPackageManager().getPackageInfo("com.android.vending", 0);
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
		return true;
	}
	
	public static String marketPrefix(OsmandApplication ctx) {
		if (isAmazonEnabled(ctx)) {
			return "amzn://apps/android?p=";
		} else if (isGooglePlayEnabled(ctx) && isGooglePlayInstalled(ctx)) {
			return "market://details?id=";
		} 
		return "https://osmand.net/apps?id=";
	}

	public static String getUrlWithUtmRef(OsmandApplication ctx, String appName) {
		return marketPrefix(ctx) + appName + UTM_REF;
	}
	
	private static boolean isAmazonEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+amazon");
	}
	
	public static boolean isGooglePlayEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+play_market");
	}
	
	
	private Version(OsmandApplication ctx) {
		String appVersion = "";
		int versionCode = -1;
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			appVersion = packageInfo.versionName;  //Version suffix  ctx.getString(R.string.app_version_suffix)  already appended in build.gradle
			versionCode = packageInfo.versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		this.appVersion = appVersion;
		appName = ctx.getString(R.string.app_name);
	}

	private static Version ver = null;
	private static Version getVersion(OsmandApplication ctx){
		if (ver == null) {
			ver = new Version(ctx);
		}
		return ver;
	}
	
	public static String getFullVersion(OsmandApplication ctx){
		Version v = getVersion(ctx);
		return v.appName + " " + v.appVersion;
	}
	
	public static String getAppVersion(OsmandApplication ctx){
		Version v = getVersion(ctx);
		return v.appVersion;
	}

	public static String getBuildAppEdition(OsmandApplication ctx){
		return ctx.getString(R.string.app_edition);
	}
	
	public static String getAppName(OsmandApplication ctx){
		Version v = getVersion(ctx);
		return v.appName;
	}
	
	public static boolean isProductionVersion(OsmandApplication ctx){
		Version v = getVersion(ctx);
		return !v.appVersion.contains("#");
	}

	public static String getVersionAsURLParam(OsmandApplication ctx) {
		try {
			return "osmandver=" + URLEncoder.encode(getVersionForTracker(ctx), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static boolean isFreeVersion(OsmandApplication ctx){
		return ctx.getPackageName().equals(FREE_VERSION_NAME) || 
				ctx.getPackageName().equals(FREE_DEV_VERSION_NAME) ||
				ctx.getPackageName().equals(FREE_CUSTOM_VERSION_NAME) ||
				isHuawei(ctx);
	}

	public static boolean isPaidVersion(OsmandApplication ctx) {
		return !isFreeVersion(ctx)
				|| InAppPurchaseHelper.isFullVersionPurchased(ctx)
				|| InAppPurchaseHelper.isSubscribedToLiveUpdates(ctx);
	}
	
	public static boolean isDeveloperVersion(OsmandApplication ctx){
		return getAppName(ctx).contains("~") || ctx.getPackageName().equals(FREE_DEV_VERSION_NAME);
	}

	public static boolean isDeveloperBuild(OsmandApplication ctx){
		return getAppName(ctx).contains("~");
	}

	public static String getVersionForTracker(OsmandApplication ctx) {
		String v = Version.getAppName(ctx);
		if(Version.isProductionVersion(ctx)){
			v = Version.getFullVersion(ctx);
		} else {
			v +=" test";
		}
		return v;
	}

	public static boolean isOpenGlAvailable(OsmandApplication app) {
		if ("qnx".equals(System.getProperty("os.name"))) {
			return false;
		}
		File nativeLibraryDir = new File(app.getApplicationInfo().nativeLibraryDir);
		if (nativeLibraryDir.exists() && nativeLibraryDir.canRead()) {
			File[] files = nativeLibraryDir.listFiles();
			if (files != null) {
				for (File file : files) {
					if ("libOsmAndCoreWithJNI.so".equals(file.getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}
}