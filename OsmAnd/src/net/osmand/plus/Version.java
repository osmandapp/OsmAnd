package net.osmand.plus;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import android.content.pm.PackageInfo;
import 	android.content.pm.PackageManager;

public class Version {
	
	private final String appVersion; 
	private final String appName;
	private final static String FREE_VERSION_NAME = "net.osmand";
	private final static String FREE_DEV_VERSION_NAME = "net.osmand.dev";
	private final static String SHERPAFY_VERSION_NAME = "net.osmand.sherpafy";
	
	
	public static boolean isGpsStatusEnabled(OsmandApplication ctx) {
		return isGooglePlayEnabled(ctx) && !isBlackberry(ctx);
	}
	
	public static boolean isBlackberry(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+blackberry");
	}
	
	public static boolean isMarketEnabled(OsmandApplication ctx) {
		return isGooglePlayEnabled(ctx) || isAmazonEnabled(ctx);
	}
	
	public static String marketPrefix(OsmandApplication ctx) {
		if (isAmazonEnabled(ctx)) {
			return "amzn://apps/android?p=";
		} else if (isGooglePlayEnabled(ctx)) {
			return "market://search?q=pname:";
		} 
		return "http://osmand.net/apps?id="; 
	}
	
	private static boolean isAmazonEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+amazon");
	}
	
	public static boolean isGooglePlayEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+play_market");
	}
	
	public static boolean isSherpafy(OsmandApplication ctx) {
		return ctx.getPackageName().equals(SHERPAFY_VERSION_NAME);
	}
	
	private Version(OsmandApplication ctx) {
		// appVersion = ctx.getString(R.string.app_version);
		String appVersion = "";
		int versionCode = -1;
		try {
			PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
			appVersion = packageInfo.versionName;
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
		return ctx.getPackageName().equals(FREE_VERSION_NAME) || ctx.getPackageName().equals(FREE_DEV_VERSION_NAME);
	}

	public static boolean isPaidVersion(OsmandApplication ctx) {
		return !isFreeVersion(ctx)
				|| ctx.getSettings().FULL_VERSION_PURCHASED.get()
				|| ctx.getSettings().LIVE_UPDATES_PURCHASED.get();
	}
	
	public static boolean isDeveloperVersion(OsmandApplication ctx){
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

}
