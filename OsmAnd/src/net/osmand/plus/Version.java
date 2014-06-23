package net.osmand.plus;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Version {
	
	private final String appVersion; 
	private final String appName;
	private final static String FREE_VERSION_NAME = "net.osmand";
	private final static String SHERPAFY_VERSION_NAME = "net.osmand.sherpafy";
	
	
	public static boolean isGpsStatusEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+gps_status") && !isBlackberry(ctx);
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
		return "http://osmand.net/apps?"; 
	}
	
	public static boolean isAmazonEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+amazon");
	}
	
	public static boolean isGooglePlayEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+play_market");
	}
	
	public static boolean isFreeVersionEnabled(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+free_version");
	}
	
	public static boolean isParkingPluginInlined(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+parking_plugin");
	}
	
	public static boolean isRouteNavPluginInlined(OsmandApplication ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+route_nav");
	}
	
	public static boolean isSherpafy(OsmandApplication ctx) {
		return ctx.getPackageName().equals(SHERPAFY_VERSION_NAME);
	}
	
	private Version(OsmandApplication ctx) {
		appVersion = ctx.getString(R.string.app_version);
		appName = ctx.getString(R.string.app_name);
	}

	private static Version ver = null;
	private static Version getVersion(OsmandApplication ctx){
		if(ver == null){
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
		return ctx.getPackageName().equals(FREE_VERSION_NAME) || isFreeVersionEnabled(ctx);
		
	}
	
	public static boolean isDeveloperVersion(OsmandApplication ctx){
		return "osmand~".equalsIgnoreCase(getAppName(ctx));
		
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
