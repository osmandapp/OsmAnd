package net.osmand.plus;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Version {
	
	private final String appVersion; 
	private final String appName;
	private final static String FREE_VERSION_NAME = "net.osmand";
	
	
	public static boolean isGpsStatusEnabled(ClientContext ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+gps_status") && !isBlackberry(ctx);
	}
	
	public static boolean isBlackberry(ClientContext ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+blackberry");
	}
	
	public static boolean isMarketEnabled(ClientContext ctx) {
		return isGooglePlayEnabled(ctx) || isAmazonEnabled(ctx);
	}
	
	public static String marketPrefix(ClientContext ctx) {
		if (isAmazonEnabled(ctx)) {
			return "amzn://apps/android?p=";
		} else if (isGooglePlayEnabled(ctx)) {
			return "market://search?q=pname:";
		} 
		return "http://osmand.net/apps?"; 
	}
	
	public static boolean isAmazonEnabled(ClientContext ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+amazon");
	}
	
	public static boolean isGooglePlayEnabled(ClientContext ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+play_market");
	}
	
	public static boolean isFreeVersionEnabled(ClientContext ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+free_version");
	}
	
	public static boolean isParkingPluginInlined(ClientContext ctx) {
		return ctx.getString(R.string.versionFeatures).contains("+parking_plugin");
	}
	
	private Version(ClientContext ctx) {
		appVersion = ctx.getString(R.string.app_version);
		appName = ctx.getString(R.string.app_name);
	}

	private static Version ver = null;
	private static Version getVersion(ClientContext ctx){
		if(ver == null){
			ver = new Version(ctx);
		}
		return ver;
	}
	
	public static String getFullVersion(ClientContext ctx){
		Version v = getVersion(ctx);
		return v.appName + " " + v.appVersion;
	}
	
	public static String getAppVersion(ClientContext ctx){
		Version v = getVersion(ctx);
		return v.appVersion;
	}
	
	public static String getAppName(ClientContext ctx){
		Version v = getVersion(ctx);
		return v.appName;
	}
	
	public static boolean isProductionVersion(ClientContext ctx){
		Version v = getVersion(ctx);
		return !v.appVersion.contains("#");
	}

	public static String getVersionAsURLParam(ClientContext ctx) {
		try {
			return "osmandver=" + URLEncoder.encode(getVersionForTracker(ctx), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static boolean isFreeVersion(ClientContext ctx){
		return ctx.getInternalAPI().getPackageName().equals(FREE_VERSION_NAME) || isFreeVersionEnabled(ctx);
		
	}
	
	public static boolean isDeveloperVersion(ClientContext ctx){
		return "osmand~".equalsIgnoreCase(getAppName(ctx));
		
	}
	
	public static String getVersionForTracker(ClientContext ctx) {
		String v = Version.getAppName(ctx);
		if(Version.isProductionVersion(ctx)){
			v = Version.getFullVersion(ctx);
		} else {
			v +=" test";
		}
		return v;
	}

}
