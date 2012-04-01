package net.osmand;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import net.osmand.plus.R;
import android.content.Context;

public class Version {
	
	private final String appVersion; 
	private final String appName;
	
	private Version(Context ctx) {
		appVersion = ctx.getString(R.string.app_version);
		appName = ctx.getString(R.string.app_name);
	}

	private static Version ver = null;
	private static Version getVersion(Context ctx){
		if(ver == null){
			ver = new Version(ctx);
		}
		return ver;
	}
	
	public static String getFullVersion(Context ctx){
		Version v = getVersion(ctx);
		return v.appName + " " + v.appVersion;
	}
	
	public static String getAppVersion(Context ctx){
		Version v = getVersion(ctx);
		return v.appVersion;
	}
	
	public static String getAppName(Context ctx){
		Version v = getVersion(ctx);
		return v.appName;
	}
	
	public static boolean isProductionVersion(Context ctx){
		Version v = getVersion(ctx);
		return !v.appVersion.contains("#");
	}

	public static String getVersionAsURLParam(Context ctx) {
		try {
			return "osmandver=" + URLEncoder.encode(getFullVersion(ctx), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}
